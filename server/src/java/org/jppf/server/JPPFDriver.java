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
package org.jppf.server;

import static org.jppf.utils.stats.JPPFStatisticsHelper.createServerStatistics;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.*;
import org.jppf.classloader.*;
import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.discovery.PeerDriverDiscovery;
import org.jppf.job.JobTasksListenerManager;
import org.jppf.logging.jmx.JmxMessageNotifier;
import org.jppf.management.*;
import org.jppf.nio.*;
import org.jppf.nio.acceptor.AcceptorNioServer;
import org.jppf.node.initialization.OutputRedirectHook;
import org.jppf.node.protocol.JPPFDistributedJob;
import org.jppf.process.LauncherListener;
import org.jppf.serialization.ObjectSerializer;
import org.jppf.server.job.JPPFJobManager;
import org.jppf.server.nio.classloader.LocalClassContext;
import org.jppf.server.nio.classloader.client.ClientClassNioServer;
import org.jppf.server.nio.classloader.node.NodeClassNioServer;
import org.jppf.server.nio.client.ClientNioServer;
import org.jppf.server.nio.client.async.AsyncClientNioServer;
import org.jppf.server.nio.heartbeat.HeartbeatNioServer;
import org.jppf.server.nio.nodeserver.*;
import org.jppf.server.node.JPPFNode;
import org.jppf.server.node.local.*;
import org.jppf.server.protocol.ServerJob;
import org.jppf.server.queue.JPPFPriorityQueue;
import org.jppf.startup.JPPFDriverStartupSPI;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ThreadUtils;
import org.jppf.utils.configuration.JPPFProperties;
import org.jppf.utils.hooks.HookFactory;
import org.jppf.utils.stats.JPPFStatistics;
import org.slf4j.*;

/**
 * This class serves as an initializer for the entire JPPF server. It follows the singleton pattern and provides access,
 * across the JVM, to the tasks execution queue.
 * <p>It also holds a server for incoming client connections, a server for incoming node connections, along with a class server
 * to handle requests to and from remote class loaders.
 * @author Laurent Cohen
 * @author Lane Schwartz (dynamically allocated server port) 
 */
public class JPPFDriver {
  // this static block must be the first thing executed when this class is loaded
  static {
    JPPFInitializer.init();
  }
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(JPPFDriver.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Flag indicating whether collection of debug information is available via JMX.
   * @exclude
   */
  public static final boolean JPPF_DEBUG = JPPFConfiguration.get(JPPFProperties.DEBUG_ENABLED);
  /**
   * Singleton instance of the JPPFDriver.
   */
  private static JPPFDriver instance;
  /**
   * Used for serialization / deserialization.
   */
  private static final ObjectSerializer serializer = new ObjectSerializerImpl();
  /**
   * Reference to the local node if it is enabled.
   */
  private JPPFNode localNode;
  /**
   * The queue that handles the tasks to execute. Objects are added to, and removed from, this queue, asynchronously and by multiple threads.
   */
  private final JPPFPriorityQueue taskQueue;
  /**
   * Serves the execution requests coming from client applications.
   */
  private ClientNioServer clientNioServer;
  /**
   * Serves the execution requests coming from client applications.
   */
  private AsyncClientNioServer asyncClientNioServer;
  /**
   * Serves the JPPF nodes.
   */
  private NodeNioServer nodeNioServer;
  /**
   * Serves class loading requests from the JPPF nodes.
   */
  private ClientClassNioServer clientClassServer;
  /**
   * Serves class loading requests from the JPPF nodes.
   */
  private NodeClassNioServer nodeClassServer;
  /**
   * Handles the initial handshake and peer channel identification.
   */
  private AcceptorNioServer acceptorServer;
  /**
   * Handles the heartbeat messages with the nodes.
   */
  private HeartbeatNioServer nodeHeartbeatServer;
  /**
   * Handles the heartbeat messages with the clients.
   */
  private HeartbeatNioServer clientHeartbeatServer;
  /**
   * Determines whether this server has scheduled a shutdown.
   */
  private final AtomicBoolean shutdownSchduled = new AtomicBoolean(false);
  /**
   * Determines whether this server has initiated a shutdown, in which case it does not accept connections anymore.
   */
  final AtomicBoolean shuttingDown = new AtomicBoolean(false);
  /**
   * Holds the statistics monitors.
   */
  private final JPPFStatistics statistics;
  /**
   * Manages and monitors the jobs throughout their processing within this driver.
   */
  private JPPFJobManager jobManager;
  /**
   * Uuid for this driver.
   */
  private final String uuid;
  /**
   * Performs initialization of the driver's components.
   */
  private DriverInitializer initializer;
  /**
   * Configuration for this driver.
   */
  private final TypedProperties config;
  /**
   * System ibnformation for this driver.
   */
  private JPPFSystemInformation systemInformation;

  /**
   * Initialize this JPPFDriver.
   * @exclude
   */
  protected JPPFDriver() {
    config = JPPFConfiguration.getProperties();
    final String s;
    this.uuid = (s = config.getString("jppf.driver.uuid", null)) == null ? JPPFUuid.normalUUID() : s;
    new JmxMessageNotifier(); // initialize the jmx logger
    Thread.setDefaultUncaughtExceptionHandler(new JPPFDefaultUncaughtExceptionHandler());
    new OutputRedirectHook().initializing(new UnmodifiableTypedProperties(config));
    VersionUtils.logVersionInformation("driver", uuid);
    SystemUtils.printPidAndUuid("driver", uuid);
    statistics = createServerStatistics();
    systemInformation = new JPPFSystemInformation(uuid, false, true, statistics);
    statistics.addListener(new StatsSystemInformationUpdater(systemInformation));
    initializer = new DriverInitializer(this, config);
    initializer.initDatasources();
    jobManager = new JPPFJobManager();
    taskQueue = new JPPFPriorityQueue(this, jobManager);
  }

  /**
   * Initialize and start this driver.
   * @throws Exception if the initialization fails.
   * @exclude
   */
  public void run() throws Exception {
    if (debugEnabled) log.debug("starting JPPF driver");
    final JPPFConnectionInformation info = initializer.getConnectionInformation();
    initializer.handleDebugActions();

    final int[] sslPorts = extractValidPorts(info.sslServerPorts);
    final boolean useSSL = (sslPorts != null) && (sslPorts.length > 0);
    if (debugEnabled) log.debug("starting nio servers");
    if (JPPFConfiguration.get(JPPFProperties.RECOVERY_ENABLED)) {
      nodeHeartbeatServer = initHeartbeatServer(JPPFIdentifiers.NODE_HEARTBEAT_CHANNEL, useSSL);
      clientHeartbeatServer = initHeartbeatServer(JPPFIdentifiers.CLIENT_HEARTBEAT_CHANNEL, useSSL);
    }
    NioHelper.putServer(JPPFIdentifiers.CLIENT_CLASSLOADER_CHANNEL, clientClassServer = startServer(new ClientClassNioServer(this, useSSL)));
    NioHelper.putServer(JPPFIdentifiers.NODE_CLASSLOADER_CHANNEL, nodeClassServer = startServer(new NodeClassNioServer(this, useSSL)));
    if (JPPFConfiguration.get(JPPFProperties.CLIENT_ASYNCHRONOUS))
      NioHelper.putServer(JPPFIdentifiers.CLIENT_JOB_DATA_CHANNEL, asyncClientNioServer = startServer(new AsyncClientNioServer(JPPFIdentifiers.CLIENT_JOB_DATA_CHANNEL, useSSL)));
    else NioHelper.putServer(JPPFIdentifiers.CLIENT_JOB_DATA_CHANNEL, clientNioServer = startServer(new ClientNioServer(this, useSSL)));
    NioHelper.putServer(JPPFIdentifiers.NODE_JOB_DATA_CHANNEL, nodeNioServer = startServer(new NodeNioServer(this, taskQueue, useSSL)));
    NioHelper.putServer(JPPFIdentifiers.ACCEPTOR_CHANNEL, acceptorServer = new AcceptorNioServer(extractValidPorts(info.serverPorts), sslPorts, statistics));
    jobManager.loadTaskReturnListeners();
    if (isManagementEnabled(config)) initializer.registerProviderMBeans();
    initializer.initJmxServer();
    HookFactory.registerSPIMultipleHook(JPPFDriverStartupSPI.class, null, null).invoke("run");
    initializer.getNodeConnectionEventHandler().loadListeners();

    startServer(acceptorServer);

    if (config.get(JPPFProperties.LOCAL_NODE_ENABLED)) {
      final LocalClassLoaderChannel localClassChannel = new LocalClassLoaderChannel(new LocalClassContext());
      localClassChannel.getContext().setChannel(localClassChannel);
      final LocalNodeChannel localNodeChannel = new LocalNodeChannel(new LocalNodeContext(nodeNioServer.getTransitionManager()));
      localNodeChannel.getContext().setChannel(localNodeChannel);
      final boolean offline = JPPFConfiguration.get(JPPFProperties.NODE_OFFLINE);
      localNode = new JPPFLocalNode(new LocalNodeConnection(localNodeChannel), offline  ? null : new LocalClassLoaderConnection(localClassChannel));
      nodeClassServer.initLocalChannel(localClassChannel);
      nodeNioServer.initLocalChannel(localNodeChannel);
      ThreadUtils.startDaemonThread(localNode, "Local node");
    }
    initializer.initBroadcaster();
    initializer.initPeers(clientClassServer);
    taskQueue.getPersistenceHandler().loadPersistedJobs();
    if (debugEnabled) log.debug("JPPF Driver initialization complete");
    System.out.println("JPPF Driver initialization complete");
  }

  /**
   * Get the singleton instance of the JPPFDriver.
   * @return a <code>JPPFDriver</code> instance.
   */
  public static JPPFDriver getInstance() {
    return instance;
  }

  /**
   * Get the queue that handles the tasks to execute.
   * @return a JPPFQueue instance.
   * @exclude
   */
  public JPPFPriorityQueue getQueue() {
    return taskQueue;
  }

  /**
   * Get the JPPF client server.
   * @return a {@link ClientNioServer} instance.
   * @exclude
   */
  public ClientNioServer getClientNioServer() {
    return clientNioServer;
  }

  /**
   * Get the JPPF client server.
   * @return a {@link AsyncClientNioServer} instance.
   * @exclude
   */
  public AsyncClientNioServer getAsyncClientNioServer() {
    return asyncClientNioServer;
  }

  /**
   * Get the JPPF class server.
   * @return a <code>ClassNioServer</code> instance.
   * @exclude
   */
  public ClientClassNioServer getClientClassServer() {
    return clientClassServer;
  }

  /**
   * Get the JPPF class server.
   * @return a <code>ClassNioServer</code> instance.
   * @exclude
   */
  public NodeClassNioServer getNodeClassServer() {
    return nodeClassServer;
  }

  /**
   * Get the JPPF nodes server.
   * @return a <code>NodeNioServer</code> instance.
   * @exclude
   */
  public NodeNioServer getNodeNioServer() {
    return nodeNioServer;
  }

  /**
   * Get the server which handles the initial handshake and peer channel identification.
   * @return a {@link AcceptorNioServer} instance.
   * @exclude
   */
  public AcceptorNioServer getAcceptorServer() {
    return acceptorServer;
  }

  /**
   * Get this driver's unique identifier.
   * @return the uuid as a string.
   */
  public String getUuid() {
    return uuid;
  }

  /**
   * Get a server-side representation of a job from its uuid.
   * @param uuid the uuid of the job to lookup.
   * @return a {@link JPPFDistributedJob} instance, or {@code null} if there is no job with the specified uuid.
   */
  public JPPFDistributedJob getJob(final String uuid) {
    final ServerJob serverJob = this.getQueue().getJob(uuid);
    return (serverJob == null) ? null : serverJob.getJob();
  }

  /**
   * Initialize this task with the specified parameters.<br>
   * The shutdown is initiated after the specified shutdown delay has expired.<br>
   * If the restart parameter is set to false then the JVM exits after the shutdown is complete.
   * @param shutdownDelay delay, in milliseconds, after which the server shutdown is initiated. A value of 0 or less
   * means an immediate shutdown.
   * @param restart determines whether the server should restart after shutdown is complete.
   * If set to false, then the JVM will exit.
   * @param restartDelay delay, starting from shutdown completion, after which the server is restarted.
   * A value of 0 or less means the server is restarted immediately after the shutdown is complete.
   * @exclude
   */
  public void initiateShutdownRestart(final long shutdownDelay, final boolean restart, final long restartDelay) {
    if (shutdownSchduled.compareAndSet(false, true)) {
      log.info("Scheduling server shutdown in " + shutdownDelay + " ms");
      final Timer timer = new Timer();
      final ShutdownRestartTask task = new ShutdownRestartTask(restart, restartDelay, this);
      timer.schedule(task, (shutdownDelay <= 0L) ? 0L : shutdownDelay);
    } else {
      log.info("shutdown/restart request ignored because a previous request is already scheduled");
    }
  }

  /**
   * Shutdown this server and all its components.
   * @exclude
   */
  public void shutdown() {
    log.info("Shutting down");
    if (acceptorServer != null) acceptorServer.shutdown();
    if (nodeHeartbeatServer != null) nodeHeartbeatServer.shutdown();
    if (clientHeartbeatServer != null) clientHeartbeatServer.shutdown();
    if (clientClassServer != null) clientClassServer.shutdown();
    if (nodeClassServer != null) nodeClassServer.shutdown();
    if (nodeNioServer != null) nodeNioServer.shutdown();
    if (clientNioServer != null) clientNioServer.shutdown();
    NioHelper.shutdown(true);
    initializer.stopBroadcaster();
    initializer.stopPeerDiscoveryThread();
    initializer.stopJmxServer();
    jobManager.close();
  }

  /**
   * Get the object that manages and monitors the jobs throughout their processing within this driver.
   * @return an instance of <code>JPPFJobManager</code>.
   * @exclude
   */
  public JPPFJobManager getJobManager() {
    return jobManager;
  }

  /**
   * Get the object which manages the registration and unregistration of job
   * dispatch listeners and notifies these listeners of job dispatch events.
   * @return an instance of {@link JobTasksListenerManager}.
   */
  public JobTasksListenerManager getJobTasksListenerManager() {
    return jobManager;
  }

  /**
   * Get this driver's initializer.
   * @return a <code>DriverInitializer</code> instance.
   * @exclude
   */
  public DriverInitializer getInitializer() {
    return initializer;
  }

  /**
   * Start the JPPF server.
   * @param args not used.
   * @exclude
   */
  public static void main(final String...args) {
    try {
      if (debugEnabled) log.debug("starting the JPPF driver");
      if ((args == null) || (args.length <= 0))
        throw new JPPFException("The driver should be run with an argument representing a valid TCP port or 'noLauncher'");
      if (!"noLauncher".equals(args[0])) {
        final int port = Integer.parseInt(args[0]);
        new LauncherListener(port).start();
      }
      instance = new JPPFDriver();
      if (debugEnabled) log.debug("Driver system properties: {}", SystemUtils.printSystemProperties());
      instance.run();
    } catch(final Exception e) {
      e.printStackTrace();
      log.error(e.getMessage(), e);
      if (JPPFConfiguration.get(JPPFProperties.SERVER_EXIT_ON_SHUTDOWN)) System.exit(1);
    }
  }

  /**
   * Start a heartbeat server with the specified channel identifier.
   * @param identifier the channel identifier for the server connections.
   * @param useSSL whether to use SSL connectivity.
   * @return the created server.
   * @throws Exception if any error occurs.
   */
  private static HeartbeatNioServer initHeartbeatServer(final int identifier, final boolean useSSL) throws Exception {
    final HeartbeatNioServer server = startServer(new HeartbeatNioServer(identifier, useSSL));
    NioHelper.putServer(identifier, server);
    return server;
  }

  /**
   * Start server, register it to recovery server if requested and print initialization message.
   * @param <T> the type of the server to start.
   * @param nioServer the nio server to start.
   * @return started nioServer
   */
  private static <T extends NioServer<?, ?>> T startServer(final T nioServer) {
    if (nioServer == null) throw new IllegalArgumentException("nioServer is null");
    if (debugEnabled) log.debug("starting nio server {}", nioServer);
    nioServer.start();
    printInitializedMessage(nioServer.getPorts(), nioServer.getSSLPorts(), nioServer.getName());
    return nioServer;
  }

  /**
   * Print a message to the console to signify that the initialization of a server was successful.
   * @param ports the ports on which the server is listening.
   * @param sslPorts SSL ports for initialization message.
   * @param name the name to use for the server.
   */
  private static void printInitializedMessage(final int[] ports, final int[] sslPorts, final String name) {
    final StringBuilder sb = new StringBuilder();
    if (name != null) {
      sb.append(name);
      sb.append(" initialized");
    }
    if ((ports != null) && (ports.length > 0)) {
      sb.append("\n-  accepting plain connections on port");
      if (ports.length > 1) sb.append('s');
      for (int n: ports) sb.append(' ').append(n);
    }
    if ((sslPorts != null) && (sslPorts.length > 0)) {
      sb.append("\n- accepting secure connections on port");
      if (sslPorts.length > 1) sb.append('s');
      for (int n: sslPorts) sb.append(' ').append(n);
    }
    System.out.println(sb.toString());
    if (debugEnabled) log.debug(sb.toString());
  }

  /**
   * Determine whether management is enabled and if there is an active remote connector server.
   * @return <code>true</code> if management is enabled, <code>false</code> otherwise.
   * @param config the configuration to test whether management is enabled.
   */
  private static boolean isManagementEnabled(final TypedProperties config) {
    return config.get(JPPFProperties.MANAGEMENT_ENABLED);
  }

  /**
   * Get the system ibnformation for this driver.
   * @return a {@link JPPFSystemInformation} instance.
   */
  public JPPFSystemInformation getSystemInformation() {
    return systemInformation;
  }

  /**
   * Extract only th valid ports from the input array.
   * @param ports the array of port numbers to check.
   * @return an array, possibly of length 0, containing all the valid port numbers in the input array.
   */
  private static int[] extractValidPorts(final int[] ports) {
    if ((ports == null) || (ports.length == 0)) return ports;
    final List<Integer> list = new ArrayList<>();
    for (int port: ports) {
      if (port >= 0) list.add(port);
    }
    final int[] result = new int[list.size()];
    for (int i=0; i<result.length; i++) result[i] = list.get(i);
    return result;
  }

  /**
   * Get the object holding the statistics monitors.
   * @return a {@link JPPFStatistics} instance.
   */
  public JPPFStatistics getStatistics() {
    return statistics;
  }

  /**
   * Get the object used for serialization / deserialization.
   * @return an {@link ObjectSerializer} instance.
   * @exclude
   */
  public static ObjectSerializer getSerializer() {
    return serializer;
  }

  /**
   * Get the jmx server used to manage and monitor this driver.
   * @param secure specifies whether to get the ssl-based connector server. 
   * @return a {@link JMXServer} instance.
   */
  public JMXServer getJMXServer(final boolean secure) {
    return initializer.getJmxServer(secure);
  }

  /**
   * Add a custom peer driver discovery mechanism to those already registered, if any.
   * @param discovery the driver discovery to add.
   */
  public void addDriverDiscovery(final PeerDriverDiscovery discovery) {
    initializer.discoveryHandler.addDiscovery(discovery);
  }

  /**
   * Remove a custom peer driver discovery mechanism from those already registered.
   * @param discovery the driver discovery to remove.
   */
  public void removeDriverDiscovery(final PeerDriverDiscovery discovery) {
    initializer.discoveryHandler.removeDiscovery(discovery);
  }

  /**
   * Determine whether this server has initiated a shutdown, in which case it does not accept connections anymore.
   * @return {@code true} if a shutdown is initiated, {@code false} otherwise.
   */
  public boolean isShuttingDown() {
    return shuttingDown.get();
  }
}
