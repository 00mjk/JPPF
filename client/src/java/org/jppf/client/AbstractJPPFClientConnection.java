/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.client;

import static org.jppf.client.JPPFClientConnectionStatus.*;

import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.*;
import org.jppf.client.event.*;
import org.jppf.comm.socket.*;
import org.jppf.security.*;
import org.jppf.server.protocol.*;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.*;

/**
 * This class provides an API to submit execution requests and administration
 * commands, and request server information data.<br>
 * It has its own unique identifier, used by the nodes, to determine whether
 * classes from the submitting application should be dynamically reloaded or not
 * depending on whether the uuid has changed or not.
 * @author Laurent Cohen
 */
public abstract class AbstractJPPFClientConnection implements JPPFClientConnection
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(AbstractJPPFClientConnection.class);
	/**
	 * Determines whether the debug level is enabled in the log4j configuration, without the cost of a method call.
	 */
	private boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The socket client used to communicate over a socket connection.
	 */
	protected SocketWrapper socketClient = null;
	/**
	 * Enables loading local classes onto remote nodes.
	 */
	protected ClassServerDelegate delegate = null;
	/**
	 * Utility for deserialization and serialization.
	 */
	protected SerializationHelper helper = null;
	/**
	 * Unique identifier for this JPPF client.
	 */
	protected String appUuid = null;
	/**
	 * Used to synchronize access to the underlying socket from multiple threads.
	 */
	protected SocketInitializer socketInitializer = createSocketInitializer();
	/**
	 * The name or IP address of the host the JPPF driver is running on.
	 */
	protected String host = null;
	/**
	 * The TCP port the JPPF driver listening to for submitted tasks.
	 */
	protected int port = -1;
	/**
	 * The TCP port the class server is listening to.
	 */
	protected int classServerPort = -1;
	/**
	 * Security credentials associated with the application.
	 */
	protected JPPFSecurityContext credentials = null;
	/**
	 * Total count of the tasks submitted by this client.
	 */
	protected int totalTaskCount = 0;
	/**
	 * Configuration name for this local client.
	 */
	protected String name = null;
	/**
	 * Priority given to the driver this client is connected to.
	 * The client is always connected to the available driver(s) with the highest
	 * priority. If multiple drivers have the same priority, they will be used as a
	 * pool and tasks will be evenly distributed among them.
	 */
	protected int priority = 0;
	/**
	 * Status of the connection.
	 */
	protected JPPFClientConnectionStatus status = CONNECTING;
	/**
	 * List of status listeners for this connection.
	 */
	protected List<ClientConnectionStatusListener> listeners = new ArrayList<ClientConnectionStatusListener>();
	/**
	 * Holds the tasks, data provider and submission mode for the current execution.
	 */
	protected ClientExecution currentExecution = null;
	/**
	 * Determines whether this connection has been shut down;
	 */
	protected boolean isShutdown = false;

	/**
	 * Default instantiation of this class is not allowed.
	 */
	protected AbstractJPPFClientConnection()
	{
	}

	/**
	 * Initialize this client connection with the specified parameters.
	 * @param uuid the unique identifier for this local client.
	 * @param name configuration name for this local client.
	 * @param host the name or IP address of the host the JPPF driver is running on.
	 * @param driverPort the TCP port the JPPF driver listening to for submitted tasks.
	 * @param classServerPort the TCP port the class server is listening to.
	 * @param priority the assigned to this client connection.
	 */
	public AbstractJPPFClientConnection(String uuid, String name, String host, int driverPort, int classServerPort, int priority)
	{
		configure(uuid, name, host, driverPort, classServerPort, priority);
	}

	/**
	 * Configure this client connection with the specified parameters.
	 * @param uuid the unique identifier for this local client.
	 * @param name configuration name for this local client.
	 * @param host the name or IP address of the host the JPPF driver is running on.
	 * @param driverPort the TCP port the JPPF driver listening to for submitted tasks.
	 * @param classServerPort the TCP port the class server is listening to.
	 * @param priority the assigned to this client connection.
	 */
	protected void configure(String uuid, String name, String host, int driverPort, int classServerPort, int priority)
	{
		this.appUuid = uuid;
		this.host = host;
		this.port = driverPort;
		this.priority = priority;
		this.classServerPort = classServerPort;
		this.name = name;
	}

	/**
	 * Initialize this client connection.
	 * @see org.jppf.client.JPPFClientConnection#init()
	 */
	public abstract void init();

	/**
	 * Initialize this node's resources.
	 * @throws Exception if an error is raised during initialization.
	 */
	public synchronized void initConnection() throws Exception
	{
		try
		{
			setStatus(CONNECTING);
			if (socketClient == null) initSocketClient();
			String msg = "[client: "+name+"] JPPFClient.init(): Attempting connection to the JPPF driver";
			System.out.println(msg);
			if (debugEnabled) log.debug(msg);
			socketInitializer.initializeSocket(socketClient);
			if (!socketInitializer.isSuccessfull())
			{
				throw new JPPFException("["+name+"] Could not reconnect to the JPPF Driver");
			}
			msg = "[client: "+name+"] JPPFClient.init(): Reconnected to the JPPF driver";
			System.out.println(msg);
			if (debugEnabled) log.debug(msg);
			setStatus(ACTIVE);
		}
		catch(Exception e)
		{
			setStatus(FAILED);
			throw e;
		}
	}

	/**
	 * Initialize this node's resources.
	 * @throws Exception if an error is raised during initialization.
	 */
	public void initSocketClient() throws Exception
	{
		socketClient = new SocketClient();
		socketClient.setHost(host);
		socketClient.setPort(port);
	}

	/**
	 * Initialize this client's security credentials.
	 * @throws Exception if an error is raised during initialization.
	 */
	public void initCredentials() throws Exception
	{
		StringBuilder sb = new StringBuilder("Client:");
		sb.append(VersionUtils.getLocalIpAddress()).append(":");
		TypedProperties props = JPPFConfiguration.getProperties();
		sb.append(props.getInt("class.server.port", 11111)).append(":");
		sb.append(port).append(":");
		credentials = new JPPFSecurityContext(appUuid, sb.toString(), new JPPFCredentials());
	}

	/**
	 * Submit the request to the server.
	 * @param taskList the list of tasks to execute remotely.
	 * @param dataProvider the provider of the data shared among tasks, may be null.
	 * @param listener listener to notify whenever a set of results have been received.
	 * @throws Exception if an error occurs while sending the request.
	 * @see org.jppf.client.JPPFClientConnection#submit(java.util.List, org.jppf.task.storage.DataProvider, org.jppf.client.event.TaskResultListener)
	 */
	public abstract void submit(List<JPPFTask> taskList, DataProvider dataProvider, TaskResultListener listener)
			throws Exception;

	/**
	 * Send tasks to the server for execution.
	 * @param taskList the list of tasks to execute remotely.
	 * @param dataProvider the provider of the data shared among tasks, may be null.
	 * @throws Exception if an error occurs while sending the request.
	 */
	public void sendTasks(List<JPPFTask> taskList, DataProvider dataProvider) throws Exception
	{
		JPPFTaskBundle header = new JPPFTaskBundle();
		header.setRequestType(JPPFTaskBundle.Type.EXECUTION);
		header.setRequestUuid(new JPPFUuid().toString());
		TraversalList<String> uuidPath = new TraversalList<String>();
		uuidPath.add(appUuid);
		header.setUuidPath(uuidPath);
		header.setCredentials(credentials);
		int count = taskList.size();
		header.setTaskCount(count);
		List<JPPFBuffer> bufList = new ArrayList<JPPFBuffer>();
		JPPFBuffer buffer = helper.toBytes(header, false);
		int size = 4 + buffer.getLength();
		bufList.add(buffer);
		buffer = helper.toBytes(dataProvider, true); 
		size += 4 + buffer.getLength();
		bufList.add(buffer);
		for (JPPFTask task : taskList)
		{
			buffer = helper.toBytes(task, true); 
			size += 4 + buffer.getLength();
			bufList.add(buffer);
		}
		byte[] data = new byte[size];
		int pos = 0;
		for (JPPFBuffer buf: bufList)
		{
			//pos = helper.writeInt(buf.getLength(), data, pos);
			pos = helper.copyToBuffer(buf.getBuffer(), data, pos, buf.getLength());
		}

		buffer = new JPPFBuffer(data, size);
		socketClient.sendBytes(buffer);
	}

	/**
	 * Receive results of tasks execution.
	 * @return a pair of objects representing the executed tasks results, and the index
	 * of the first result within the initial task execution request.
	 * @throws Exception if an error is raised while reading the results from the server.
	 */
	public Pair<List<JPPFTask>, Integer> receiveResults() throws Exception
	{
		JPPFBuffer buf = socketClient.receiveBytes(0);
		byte[] data = buf.getBuffer();
		List<JPPFTask> taskList = new ArrayList<JPPFTask>();
		int pos = 0;
		int count = helper.readInt(data, pos);
		pos += 4;
		helper.fromBytes(data, pos, true, taskList, count);
		int startIndex = (taskList.isEmpty()) ? -1 : taskList.get(0).getPosition();
		Pair<List<JPPFTask>, Integer> p = new Pair<List<JPPFTask>, Integer>(taskList, startIndex);
		return p;
	}
	/**
	 * Get the main classloader for the node. This method performs a lazy initialization of the classloader.
	 * @throws Exception if an error occcurs while instantiating the class loader.
	 */
	protected void initHelper() throws Exception
	{
		if (helper == null) helper = new SerializationHelperImpl();
	}

	/**
	 * Get the priority assigned to this connection.
	 * @return a priority as an int value.
	 * @see org.jppf.client.JPPFClientConnection#getPriority()
	 */
	public int getPriority()
	{
		return priority;
	}

	/**
	 * Get the status of this connection.
	 * @return a <code>JPPFClientConnectionStatus</code> enumerated value.
	 * @see org.jppf.client.JPPFClientConnection#getStatus()
	 */
	public synchronized JPPFClientConnectionStatus getStatus()
	{
		return status;
	}

	/**
	 * Set the status of this connection.
	 * @param status  a <code>JPPFClientConnectionStatus</code> enumerated value.
	 * @see org.jppf.client.JPPFClientConnection#setStatus(org.jppf.client.JPPFClientConnectionStatus)
	 */
	public synchronized void setStatus(JPPFClientConnectionStatus status)
	{
		this.status = status;
		fireStatusChanged();
	}

	/**
	 * Add a connection status listener to this connection's list of listeners.
	 * @param listener the listener to add to the list.
	 * @see org.jppf.client.JPPFClientConnection#addClientConnectionStatusListener(org.jppf.client.event.ClientConnectionStatusListener)
	 */
	public synchronized void addClientConnectionStatusListener(ClientConnectionStatusListener listener)
	{
		listeners.add(listener);
	}

	/**
	 * Remove a connection status listener from this connection's list of listeners.
	 * @param listener the listener to remove from the list.
	 * @see org.jppf.client.JPPFClientConnection#removeClientConnectionStatusListener(org.jppf.client.event.ClientConnectionStatusListener)
	 */
	public synchronized void removeClientConnectionStatusListener(ClientConnectionStatusListener listener)
	{
		listeners.remove(listener);
	}

	/**
	 * Notify all listeners that the status of this connection has changed.
	 */
	protected synchronized void fireStatusChanged()
	{
		ClientConnectionStatusEvent event = new ClientConnectionStatusEvent(this);
		// to avoid ConcurrentModificationException
		ClientConnectionStatusListener[] array = listeners.toArray(new ClientConnectionStatusListener[0]);
		for (ClientConnectionStatusListener listener: array)
		{
			listener.statusChanged(event);
		}
	}

	/**
	 * Shutdown this client and retrieve all pending executions for resubmission.
	 * @return a list of <code>ClientExecution</code> instances to resubmit.
	 * @see org.jppf.client.JPPFClientConnection#close()
	 */
	public abstract List<ClientExecution> close();

	/**
	 * Get the name assigned tothis client connection.
	 * @return the name as a string.
	 * @see org.jppf.client.JPPFClientConnection#getName()
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Get a string representation of this client connection.
	 * @return a string representing this connection.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return name + " : " + status;
	}

	/**
	 * Create a socket initializer.
	 * @return an instance of a class implementing <code>SocketInitializer</code>.
	 */
	abstract protected SocketInitializer createSocketInitializer();

	/**
	 * Get the object that holds the tasks, data provider and submission mode for the current execution.
	 * @return a <code>ClientExecution</code> instance.
	 */
	public ClientExecution getCurrentExecution()
	{
		return currentExecution;
	}

	/**
	 * Set the object that holds the tasks, data provider and submission mode for the current execution.
	 * @param currentExecution a <code>ClientExecution</code> instance.
	 */
	public void setCurrentExecution(ClientExecution currentExecution)
	{
		this.currentExecution = currentExecution;
	}
}
