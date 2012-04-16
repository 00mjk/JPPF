/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jppf.server.node;

import java.util.List;

import javax.management.*;

import org.jppf.JPPFError;
import org.jppf.classloader.*;
import org.jppf.management.*;
import org.jppf.management.spi.*;
import org.jppf.node.*;
import org.jppf.node.event.LifeCycleEventHandler;
import org.jppf.node.protocol.Task;
import org.jppf.server.protocol.*;
import org.jppf.startup.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class encapsulate execution nodes.
 * @author Laurent Cohen
 * @author Domingos Creado
 */
public abstract class JPPFNode extends AbstractNode
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFNode.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The task execution manager for this node.
   */
  protected NodeExecutionManagerImpl executionManager = null;
  /**
   * The object responsible for this node's I/O.
   */
  protected NodeIO nodeIO = null;
  /**
   * Determines whether JMX management and monitoring is enabled for this node.
   */
  private boolean jmxEnabled = JPPFConfiguration.getProperties().getBoolean("jppf.management.enabled", true);
  /**
   * Action executed when the node exits the main loop, in its {@link #run() run()} method.
   */
  private Runnable exitAction = null;
  /**
   * The default node's management MBean.
   */
  private JPPFNodeAdmin nodeAdmin = null;
  /**
   * The jmx server that handles administration and monitoring functions for this node.
   */
  private static JMXServer jmxServer = null;
  /**
   * Manager for the MBean defined through the service provider interface.
   */
  private JPPFMBeanProviderManager providerManager = null;
  /**
   * Manages the class loaders and how they are used.
   */
  protected AbstractClassLoaderManager classLoaderManager = null;
  /**
   * Handles the firing of node life cycle events and the listeners that subscribe to these events.
   */
  protected LifeCycleEventHandler lifeCycleEventHandler = null;

  /**
   * Default constructor.
   */
  public JPPFNode()
  {
    uuid = NodeRunner.getUuid();
    executionManager = new NodeExecutionManagerImpl(this);
    lifeCycleEventHandler = new LifeCycleEventHandler(executionManager);
  }

  /**
   * Main processing loop of this node.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    setStopped(false);
    boolean initialized = false;
    if (debugEnabled) log.debug("Start of node main loop, nodeUuid=" + uuid);
    while (!isStopped())
    {
      try
      {
        init();
        if (!initialized)
        {
          System.out.println("Node successfully initialized");
          initialized = true;
        }
        perform();
      }
      catch(SecurityException e)
      {
        throw new JPPFError(e);
      }
      catch(Exception e)
      {
        log.error(e.getMessage(), e);
        reset();
      }
    }
    if (debugEnabled) log.debug("End of node main loop");
    if (exitAction != null)
    {
      Runnable r = exitAction;
      setExitAction(null);
      r.run();
    }
  }

  /**
   * Perform the main execution loop for this node. At each iteration, this method listens for a task to execute,
   * receives it, executes it and sends the results back.
   * @throws Exception if an error was raised from the underlying socket connection or the class loader.
   */
  public void perform() throws Exception
  {
    if (debugEnabled) log.debug("Start of node secondary loop");
    while (!isStopped())
    {
      Pair<JPPFTaskBundle, List<Task>> pair = nodeIO.readTask();
      JPPFTaskBundle bundle = pair.first();
      checkInitialBundle(bundle);
      List<Task> taskList = pair.second();
      boolean notEmpty = (taskList != null) && (!taskList.isEmpty());
      if (debugEnabled)
      {
        if (notEmpty) log.debug("received a bundle with " + taskList.size()  + " tasks");
        else log.debug("received an empty bundle");
      }
      if (notEmpty) executionManager.execute(bundle, taskList);
      processResults(bundle, taskList);
    }
    if (debugEnabled) log.debug("End of node secondary loop");
  }

  /**
   * Checks whether the received bundle is the initial one sent by the driver,
   * and prepare a specific response if it is.
   * @param bundle - the bundle to check.
   */
  private void checkInitialBundle(final JPPFTaskBundle bundle)
  {
    if (JPPFTaskBundle.State.INITIAL_BUNDLE.equals(bundle.getState()))
    {
      if (debugEnabled) log.debug("setting initial bundle uuid");
      bundle.setBundleUuid(uuid);
      bundle.setParameter(BundleParameter.NODE_UUID_PARAM, uuid);
      if (isJmxEnabled())
      {
        try
        {
          TypedProperties props = JPPFConfiguration.getProperties();
          //bundle.setParameter(BundleParameter.NODE_MANAGEMENT_HOST_PARAM, NetworkUtils.getManagementHost());
          bundle.setParameter(BundleParameter.NODE_MANAGEMENT_HOST_PARAM, getJmxServer().getManagementHost());
          //bundle.setParameter(BundleParameter.NODE_MANAGEMENT_PORT_PARAM, props.getInt("jppf.management.port", 11198));
          bundle.setParameter(BundleParameter.NODE_MANAGEMENT_PORT_PARAM, getJmxServer().getManagementPort());
          bundle.setParameter(BundleParameter.NODE_MANAGEMENT_ID_PARAM, getJmxServer().getId());
        }
        catch(Exception e)
        {
          log.error(e.getMessage(), e);
        }
      }
    }
  }

  /**
   * Send the results back to the server and perform final checks for the current execution.
   * @param bundle - the bundle that contains the tasks and header information.
   * @param taskList - the tasks results.
   * @throws Exception if any error occurs.
   */
  private void processResults(final JPPFTaskBundle bundle, final List<Task> taskList) throws Exception
  {
    if (debugEnabled) log.debug("processing results for job '" + bundle.getName() + '\'');
    if (executionManager.checkConfigChanged())
    {
      JPPFSystemInformation info = new JPPFSystemInformation(NodeRunner.getUuid());
      info.populate();
      bundle.setParameter(BundleParameter.NODE_SYSTEM_INFO_PARAM, info);
    }
    nodeIO.writeResults(bundle, taskList);
    if ((taskList != null) && (!taskList.isEmpty()))
    {
      //getNodeAdmin().setTaskCounter(getTaskCount() + taskList.size());
      // if jmx is enabled, done by the status notifier
      if (!isJmxEnabled()) setTaskCount(getTaskCount() + taskList.size());
      if (debugEnabled) log.debug("tasks executed: " + getTaskCount());
    }
  }

  /**
   * Initialize this node's resources.
   * @throws Exception if an error is raised during initialization.
   */
  private synchronized void init() throws Exception
  {
    if (debugEnabled) log.debug("start node initialization");
    initHelper();
    if (isJmxEnabled())
    {
      JMXServer jmxServer = null;
      try
      {
        jmxServer = getJmxServer();
        if (!jmxServer.getServer().isRegistered(new ObjectName(JPPFNodeAdminMBean.MBEAN_NAME)))
        {
          registerProviderMBeans();
        }
      }
      catch(Exception e)
      {
        jmxEnabled = false;
        System.out.println("JMX initialization failure - management is disabled for this node");
        System.out.println("see the log file for details");
        try
        {
          if (jmxServer != null) jmxServer.stop();
        }
        catch(Exception e2)
        {
          log.error("Error stopping the JMX server", e2);
        }
        jmxServer = null;
        log.error("Error creating the JMX server", e);
      }
    }
    new JPPFStartupLoader().load(JPPFNodeStartupSPI.class);
    initDataChannel();
    lifeCycleEventHandler.loadListeners();
    lifeCycleEventHandler.fireNodeStarting();
    if (debugEnabled) log.debug("end node initialization");
  }

  /**
   * Initialize this node's data channel.
   * @throws Exception if an error is raised during initialization.
   */
  protected abstract void initDataChannel() throws Exception;

  /**
   * Initialize this node's data channel.
   * @throws Exception if an error is raised during initialization.
   */
  protected abstract void closeDataChannel() throws Exception;

  /**
   * Get the main classloader for the node. This method performs a lazy initialization of the classloader.
   * @return a <code>ClassLoader</code> used for loading the classes of the framework.
   */
  public AbstractJPPFClassLoader getClassLoader()
  {
    return classLoaderManager.getClassLoader();
  }

  /**
   * Set the main classloader for the node.
   * @param cl the class loader to set.
   */
  public void setClassLoader(final JPPFClassLoader cl)
  {
    classLoaderManager.setClassLoader(cl);
  }

  /**
   * Get the main classloader for the node. This method performs a lazy initialization of the classloader.
   * @throws Exception if an error occurs while instantiating the class loader.
   */
  public void initHelper() throws Exception
  {
    if (debugEnabled) log.debug("Initializing serializer");
    Class<?> c = getClassLoader().loadJPPFClass("org.jppf.utils.ObjectSerializerImpl");
    if (debugEnabled) log.debug("Loaded serializer class " + c);
    Object o = c.newInstance();
    serializer = (ObjectSerializer) o;
    c = getClassLoader().loadJPPFClass("org.jppf.utils.SerializationHelperImpl");
    if (debugEnabled) log.debug("Loaded helper class " + c);
    o = c.newInstance();
    helper = (SerializationHelper) o;
    if (debugEnabled) log.debug("Serializer initialized");
  }

  /**
   * Get a reference to the JPPF container associated with an application uuid.
   * @param uuidPath the uuid path containing the key to the container.
   * @return a <code>JPPFContainer</code> instance.
   * @throws Exception if an error occurs while getting the container.
   */
  public JPPFContainer getContainer(final List<String> uuidPath) throws Exception
  {
    return classLoaderManager.getContainer(uuidPath);
  }

  /**
   * Get the administration and monitoring MBean for this node.
   * @return a <code>JPPFNodeAdmin</code>m instance.
   */
  public synchronized JPPFNodeAdmin getNodeAdmin()
  {
    return nodeAdmin;
  }

  /**
   * Set the administration and monitoring MBean for this node.
   * @param nodeAdmin a <code>JPPFNodeAdmin</code>m instance.
   */
  public synchronized void setNodeAdmin(final JPPFNodeAdmin nodeAdmin)
  {
    this.nodeAdmin = nodeAdmin;
  }

  /**
   * Get the task execution manager for this node.
   * @return a <code>NodeExecutionManager</code> instance.
   */
  public NodeExecutionManagerImpl getExecutionManager()
  {
    return executionManager;
  }

  /**
   * Determines whether JMX management and monitoring is enabled for this node.
   * @return true if JMX is enabled, false otherwise.
   */
  boolean isJmxEnabled()
  {
    return jmxEnabled;
  }

  /**
   * Stop this node and release the resources it is using.
   * @see org.jppf.node.Node#stopNode(boolean)
   */
  @Override
  public synchronized void stopNode()
  {
    if (debugEnabled) log.debug("stopping node");
    setStopped(true);
    executionManager.shutdown();
    reset();
  }

  /**
   * Shutdown and eventually restart the node.
   * @param restart determines whether this node should be restarted by the node launcher.
   */
  public void shutdown(final boolean restart)
  {
    lifeCycleEventHandler.fireNodeEnding();
    NodeRunner.shutdown(this, restart);
  }

  /**
   * Reset this node for shutdown/restart/reconnection.
   */
  private void reset()
  {
    lifeCycleEventHandler.fireNodeEnding();
    lifeCycleEventHandler.removeAllListeners();
    setNodeAdmin(null);
    if (classLoaderManager.classLoader != null)
    {
      classLoaderManager.classLoader.close();
      classLoaderManager.classLoader = null;
    }
    try
    {
      synchronized(this)
      {
        closeDataChannel();
        classLoaderManager.containerMap.clear();
        classLoaderManager.containerList.clear();
      }
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
    }
    try
    {
      providerManager.unregisterProviderMBeans();
      if (jmxServer != null) jmxServer.stop();
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Set the action executed when the node exits the main loop.
   * @param exitAction the action to execute.
   */
  public synchronized void setExitAction(final Runnable exitAction)
  {
    this.exitAction = exitAction;
  }

  /**
   * Register all MBeans defined through the service provider interface.
   * @throws Exception if the registration failed.
   */
  @SuppressWarnings("unchecked")
  private void registerProviderMBeans() throws Exception
  {
    ClassLoader cl = getClass().getClassLoader();
    ClassLoader tmp = Thread.currentThread().getContextClassLoader();
    MBeanServer server = getJmxServer().getServer();
    if (providerManager == null) providerManager = new JPPFMBeanProviderManager<JPPFNodeMBeanProvider>(JPPFNodeMBeanProvider.class, server);
    try
    {
      Thread.currentThread().setContextClassLoader(cl);
      List<JPPFNodeMBeanProvider> list = providerManager.getAllProviders(cl);
      for (JPPFNodeMBeanProvider provider: list)
      {
        Object o = provider.createMBean(this);
        Class inf = Class.forName(provider.getMBeanInterfaceName());
        boolean b = providerManager.registerProviderMBean(o, inf, provider.getMBeanName());
        if (debugEnabled) log.debug("MBean registration " + (b ? "succeeded" : "failed") + " for [" + provider.getMBeanName() + ']');
      }
    }
    finally
    {
      Thread.currentThread().setContextClassLoader(tmp);
    }
  }

  /**
   * Get the jmx server that handles administration and monitoring functions for this node.
   * @return a <code>JMXServerImpl</code> instance.
   * @throws Exception if any error occurs.
   */
  public JMXServer getJmxServer() throws Exception
  {
    synchronized(this)
    {
      if ((jmxServer == null) || jmxServer.isStopped())
      {
        jmxServer = JMXServerFactory.createServer(NodeRunner.getUuid(), JPPFAdminMBean.NODE_SUFFIX);
        jmxServer.start(getClass().getClassLoader());
        System.out.println("JPPF Node management initialized");
      }
    }
    return jmxServer;
  }

  /**
   * Get the object that handles the firing of node life cycle events and the listeners that subscribe to these events.
   * @return an instance of <code>LifeCycleEventHandler</code>.
   */
  @Override
  public LifeCycleEventHandler getLifeCycleEventHandler()
  {
    return lifeCycleEventHandler;
  }
}
