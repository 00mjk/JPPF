/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package org.jppf.client;

import static org.jppf.client.JPPFClientConnectionStatus.NEW;

import java.io.NotSerializableException;
import java.nio.channels.AsynchronousCloseException;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

import org.jppf.JPPFException;
import org.jppf.comm.socket.*;
import org.jppf.io.IOHelper;
import org.jppf.node.protocol.*;
import org.jppf.serialization.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class represent connections to remote JPPF drivers.
 * @author Laurent Cohen
 */
abstract class BaseJPPFClientConnection implements JPPFClientConnection {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(BaseJPPFClientConnection.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Determines whether the trace level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Used to prevent parallel deserialization.
   */
  private static Lock lock = new ReentrantLock();
  /**
   * Determines whether tasks deserialization should be sequential rather than parallel.
   */
  private final boolean SEQUENTIAL_DESERIALIZATION;
  /**
   * A sequence number used as suffix for the {@code connectionUuid}.
   */
  static AtomicInteger connectionCount = new AtomicInteger(0);
  /**
   * Handler for the connection to the task server.
   */
  TaskServerConnectionHandler taskServerConnection = null;
  /**
   * Enables loading local classes onto remote nodes.
   */
  ClassServerDelegate delegate = null;
  /**
   * Configuration name for this local client.
   */
  String name = null;
  /**
   * Unique ID for this connection and its two channels.
   */
  String connectionUuid = null;
  /**
   * Status of the connection.
   */
  AtomicReference<JPPFClientConnectionStatus> status = new AtomicReference<>(NEW);
  /**
   * The connection pool this connection belongs to.
   */
  final JPPFConnectionPool pool;

  /**
   * Initialize this connection with a parent pool.
   * @param pool the connection pool this connection belongs to.
   */
  BaseJPPFClientConnection(final JPPFConnectionPool pool) {
    this.pool = pool;
    SEQUENTIAL_DESERIALIZATION = pool.getClient().getConfig().getBoolean("jppf.sequential.deserialization", false);
  }

  /**
   * Initialize this client connection.
   * @exclude
   */
  abstract void init();

  /**
   * Send tasks to the server for execution.
   * @param ser the serializer to use.
   * @param cl classloader used for serialization.
   * @param header the task bundle to send to the driver.
   * @param job the job to execute remotely.
   * @return a list of tasks that couldn't be serialized, possibly empty.
   * @throws Exception if an error occurs while sending the request.
   * @exclude
   */
  public List<Task<?>> sendTasks(final ObjectSerializer ser, final ClassLoader cl, final TaskBundle header, final JPPFJob job) throws Exception {
    final TraversalList<String> uuidPath = new TraversalList<>();
    uuidPath.add(pool.getClient().getUuid());
    header.setUuidPath(uuidPath);
    header.setTaskCount(job.unexecutedTaskCount());
    header.setName(job.getName());
    header.setUuid(job.getUuid());
    header.setSLA(job.getSLA());
    header.setMetadata(job.getMetadata());
    final Task<?>[] tasks = prepareTasksToSend(header, job);

    final SocketWrapper socketClient = taskServerConnection.getSocketClient();
    IOHelper.sendData(socketClient, header, ser);
    try {
      IOHelper.sendData(socketClient, job.getDataProvider(), ser);
    } catch(final NotSerializableException e) {
      log.error("error serializing data provider for {} : {}\nthe job will be cancelled", job, ExceptionUtils.getStackTrace(e));
      IOHelper.sendData(socketClient, null, ser);
    }
    final List<Task<?>> notSerializableTasks = new ArrayList<>(tasks.length);
    for (final Task<?> task : tasks) {
      try {
        IOHelper.sendData(socketClient, task, ser);
      } catch(final NotSerializableException e) {
        log.error("error serializing task {} for {} : {}", new Object[] { task, job, ExceptionUtils.getStackTrace(e) });
        task.setThrowable(e);
        IOHelper.sendNullData(socketClient);
        notSerializableTasks.add(task);
      }
    }
    socketClient.flush();
    return notSerializableTasks;
  }

  /**
   * Prepare the job header for the remaining tasks to send in the job.
   * @param header the job header sent to the driver.
   * @param job the job whose taskss are to be sent.
   * @return an array of the tasks to send.
   */
  private Task<?>[] prepareTasksToSend(final TaskBundle header, final JPPFJob job) {
    final int count = job.unexecutedTaskCount();
    final int[] positions = new int[count];
    final int[] maxResubmits = new int[count];
    final Task<?>[] tasks = new Task<?>[count];
    int i = 0;
    for (final Task<?> task : job) {
      final int pos = task.getPosition();
      if (!job.getResults().hasResult(pos)) {
        tasks[i] = task;
        positions[i] = pos;
        maxResubmits[i] = task.getMaxResubmits();
        i++;
      }
    }
    header.setParameter(BundleParameter.TASK_POSITIONS, positions);
    header.setParameter(BundleParameter.TASK_MAX_RESUBMITS, maxResubmits);
    if (traceEnabled) log.trace(this.toDebugString() + " sending job " + header + ", positions=" + StringUtils.buildString(positions));
    return tasks;
  }

  /**
   * Send a handshake job to the server.
   * @return a {@link TaskBundle} sent by the server in response to the handshake job.
   * @throws Exception if an error occurs while sending the request.
   */
  TaskBundle sendHandshakeJob() throws Exception {
    final TaskBundle header = new JPPFTaskBundle();
    final ObjectSerializer ser = new ObjectSerializerImpl();
    final TraversalList<String> uuidPath = new TraversalList<>();
    uuidPath.add(pool.getClient().getUuid());
    header.setUuidPath(uuidPath);
    if (debugEnabled) log.debug(toDebugString() + " sending handshake job, uuidPath=" + uuidPath);
    header.setUuid(new JPPFUuid().toString());
    header.setName("handshake job");
    header.setHandshake(true);
    header.setUuid(header.getName());
    header.setParameter(BundleParameter.CONNECTION_UUID, connectionUuid);
    header.setSLA(null);
    header.setMetadata(null);
    final SocketWrapper socketClient = taskServerConnection.getSocketClient();
    IOHelper.sendData(socketClient, header, ser);
    IOHelper.sendData(socketClient, null, ser); // null data provider
    socketClient.flush();
    return receiveBundleAndResults(ser, getClass().getClassLoader()).first();
  }

  /**
   * Send a close command job to the server.
   * @throws Exception if an error occurs while sending the request.
   */
  void sendCloseConnectionCommand() throws Exception {
    if (taskServerConnection == null) return;
    final TaskBundle header = new JPPFTaskBundle();
    final ObjectSerializer ser = new ObjectSerializerImpl();
    final TraversalList<String> uuidPath = new TraversalList<>();
    uuidPath.add(pool.getClient().getUuid());
    header.setUuidPath(uuidPath);
    if (debugEnabled) log.debug(toDebugString() + " sending close command job, uuidPath=" + uuidPath);
    header.setName("close command job");
    header.setUuid("close command job");
    header.setParameter(BundleParameter.CONNECTION_UUID, connectionUuid);
    header.setParameter(BundleParameter.CLOSE_COMMAND, true);
    header.setSLA(null);
    header.setMetadata(null);
    final SocketWrapper socketClient = taskServerConnection.getSocketClient();
    if (socketClient != null) {
      IOHelper.sendData(socketClient, header, ser);
      IOHelper.sendData(socketClient, null, ser); // null data provider
      socketClient.flush();
    }
  }

  /**
   * Receive results of tasks execution.
   * @param ser the serializer to use.
   * @param cl the class loader to use for deserializing the tasks.
   * @return a pair of objects representing the executed tasks results, and the index
   * of the first result within the initial task execution request.
   * @throws Exception if an error is raised while reading the results from the server.
   */
  private Pair<TaskBundle, List<Task<?>>> receiveBundleAndResults(final ObjectSerializer ser, final ClassLoader cl) throws Exception {
    final List<Task<?>> taskList = new LinkedList<>();
    TaskBundle bundle = null;
    final ClassLoader ctxCl = Thread.currentThread().getContextClassLoader();
    try {
      final ClassLoader loader = cl == null ? getClass().getClassLoader() : cl;
      Thread.currentThread().setContextClassLoader(loader);
      final SocketWrapper socketClient = taskServerConnection.getSocketClient();
      bundle = (TaskBundle) IOHelper.unwrappedData(socketClient, ser);
      final int count = bundle.getTaskCount();
      final int[] positions = bundle.getParameter(BundleParameter.TASK_POSITIONS);
      if (debugEnabled) {
        log.debug(toDebugString() + " : received bundle " + bundle + ", positions=" + StringUtils.buildString(positions));
      }
      if (SEQUENTIAL_DESERIALIZATION) lock.lock();
      try {
        for (int i = 0; i < count; i++) {
          final Task<?> task = (Task<?>) IOHelper.unwrappedData(socketClient, ser);
          if (task != null) {
            if ((positions != null) && (i < positions.length)) task.setPosition(positions[i]);
            taskList.add(task);
          }
        }
      } finally {
        if (SEQUENTIAL_DESERIALIZATION) lock.unlock();
      }

      // if an exception prevented the node from executing the tasks
      final Throwable t = bundle.getParameter(BundleParameter.NODE_EXCEPTION_PARAM);
      if (t != null) {
        if (debugEnabled) log.debug(toDebugString() + " : server returned exception parameter in the header for job '" + bundle.getName() + "' : " + t);
        final Exception e = (t instanceof Exception) ? (Exception) t : new JPPFException(t);
        for (final Task<?> task : taskList) task.setThrowable(e);
      }
      return new Pair<>(bundle, taskList);
    } catch (final AsynchronousCloseException e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      throw e;
    } finally {
      Thread.currentThread().setContextClassLoader(ctxCl);
    }
  }

  /**
   * Receive results of tasks execution.
   * @param ser the serializer to use.
   * @param cl the context classloader to use to deserialize the results.
   * @return a pair of objects representing the executed tasks results, and the index
   * of the first result within the initial task execution request.
   * @throws Exception if an error is raised while reading the results from the server.
   * @exclude
   */
  public List<Task<?>> receiveResults(final ObjectSerializer ser, final ClassLoader cl) throws Exception {
    return receiveBundleAndResults(ser, cl).second();
  }

  /**
   * Receive results of tasks execution.
   * @param cl the context classloader to use to deserialize the results.
   * @return a pair of objects representing the executed tasks results, and the index
   * of the first result within the initial task execution request.
   * @throws Exception if an error is raised while reading the results from the server.
   * @exclude
   */
  public List<Task<?>> receiveResults(final ClassLoader cl) throws Exception {
    final ObjectSerializer ser = makeHelper(cl, pool.getClient().getSerializationHelperClassName()).getSerializer();
    return receiveBundleAndResults(ser, cl).second();
  }

  /**
   * Instantiate a <code>SerializationHelper</code> using the current context class loader.
   * @param helperClassName the fully qualified class name of the serialization helper to use.
   * @return a <code>SerializationHelper</code> instance.
   * @throws Exception if the serialization helper could not be instantiated.
   */
  SerializationHelper makeHelper(final String helperClassName) throws Exception {
    return makeHelper(null, helperClassName);
  }

  /**
   * Instantiate a <code>SerializationHelper</code> using the current context class loader.
   * @param classLoader the class loader to use to load the serialization helper class.
   * @return a <code>SerializationHelper</code> instance.
   * @throws Exception if the serialization helper could not be instantiated.
   * @exclude
   */
  public SerializationHelper makeHelper(final ClassLoader classLoader) throws Exception {
    return makeHelper(classLoader, pool.getClient().getSerializationHelperClassName());
  }

  /**
   * Instantiate a <code>SerializationHelper</code> using the current context class loader.
   * @param classLoader the class loader to use to load the serialization helper class.
   * @param helperClassName the fully qualified class name of the serialization helper to use.
   * @return a <code>SerializationHelper</code> instance.
   * @throws Exception if the serialization helper could not be instantiated.
   * @exclude
   */
  public SerializationHelper makeHelper(final ClassLoader classLoader, final String helperClassName) throws Exception {
    final ClassLoader[] clArray = { classLoader, Thread.currentThread().getContextClassLoader(), getClass().getClassLoader() };
    Class<?> clazz = null;
    for (final ClassLoader cl: clArray) {
      try {
        if (cl == null) continue;
        clazz = Class.forName(helperClassName, true, cl);
        break;
      } catch (final Exception e) {
        if (debugEnabled) log.debug(e.getMessage(), e);
      }
      if (clazz == null) throw new IllegalStateException("could not load class " + helperClassName + " from any of these class loaders: " + Arrays.asList(clArray));
    }
    return (SerializationHelper) clazz.newInstance();
  }

  /**
   * Get the name assigned to this client connection.
   * @return the name as a string.
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Create a socket initializer.
   * @return an instance of a class implementing <code>SocketInitializer</code>.
   */
  abstract SocketInitializer createSocketInitializer();

  /**
   * Get the handler for the connection to the task server.
   * @return a <code>TaskServerConnectionHandler</code> instance.
   * @exclude
   */
  public TaskServerConnectionHandler getTaskServerConnection() {
    return taskServerConnection;
  }

  /**
   * Get the JPPF client that owns this connection.
   * @return an <code>AbstractGenericClient</code> instance.
   */
  public JPPFClient getClient() {
    return pool.getClient();
  }

  @Override
  public String getDriverUuid() {
    return pool.getDriverUuid();
  }

  @Override
  public String getConnectionUuid() {
    return connectionUuid;
  }

  @Override
  public String getHost() {
    return pool.getDriverHost();
  }

  @Override
  public int getPort() {
    return pool.getDriverPort();
  }

  /**
   * Get a string representing this connection for debugging purposes.
   * @return a string representing this connection.
   * @exclude
   */
  protected String toDebugString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append("connectionUuid=").append(connectionUuid);
    sb.append(", status=").append(status);
    sb.append(']');
    return sb.toString();
  }

  /**
   * Get the pool this connection belongs to.
   * @return a {@link JPPFConnectionPool} instance.
   * @exclude
   */
  public JPPFConnectionPool getPool() {
    return pool;
  }

  /**
   * Get the class server connection.
   * @return a {@link ClassServerDelegate} instance.
   * @exclude
   */
  public ClassServerDelegate getDelegate() {
    return delegate;
  }
}
