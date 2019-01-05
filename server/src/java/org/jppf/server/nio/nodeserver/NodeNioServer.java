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

package org.jppf.server.nio.nodeserver;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.execute.*;
import org.jppf.load.balancer.JPPFContext;
import org.jppf.load.balancer.persistence.LoadBalancerPersistenceManager;
import org.jppf.load.balancer.spi.JPPFBundlerFactory;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.nio.*;
import org.jppf.queue.*;
import org.jppf.scheduling.JPPFScheduleHandler;
import org.jppf.server.JPPFDriver;
import org.jppf.server.event.NodeConnectionEventHandler;
import org.jppf.server.protocol.*;
import org.jppf.server.queue.JPPFPriorityQueue;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ThreadUtils;
import org.jppf.utils.stats.JPPFStatisticsHelper;
import org.slf4j.*;

/**
 * Instances of this class serve task execution requests to the JPPF nodes.
 * @author Laurent Cohen
 */
public class NodeNioServer extends NioServer<NodeState, NodeTransition> implements NodeConnectionCompletionListener {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(NodeNioServer.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The the task bundle sent to a newly connected node.
   */
  private final ServerJob initialServerJob;
  /**
   * A reference to the driver's tasks queue.
   */
  private final JPPFPriorityQueue queue;
  /**
   * Used to create bundler instances.
   */
  private final JPPFBundlerFactory bundlerFactory;
  /**
   * Task that dispatches queued jobs to available nodes.
   */
  private final TaskQueueChecker taskQueueChecker;
  /**
   * A list of all the connections.
   */
  private final Map<String, AbstractNodeContext> allConnections = new ConcurrentHashMap<>();
  /**
   * The thread polling the local channel.
   */
  private ChannelSelectorThread selectorThread = null;
  /**
   * Handles listeners to node connection events.
   */
  final NodeConnectionEventHandler nodeConnectionHandler;
  /**
   * Listener used for monitoring state changes.
   */
  private final ExecutorChannelStatusListener statusListener = new ExecutorChannelStatusListener() {
    @Override
    public void executionStatusChanged(final ExecutorChannelStatusEvent event) {
      if (event.getSource() instanceof AbstractNodeContext) {
        updateConnectionStatus((AbstractNodeContext) event.getSource(), event.getOldValue(), event.getNewValue());
      }
    }
  };
  /**
   * The object that holds the node bundles waiting for a node to reconnect and send the rsults.
   */
  private final OfflineNodeHandler offlineNodeHandler = new OfflineNodeHandler();
  /**
   * Handles expiration of dispatched bundles.
   */
  private final JPPFScheduleHandler dispatchExpirationHandler = new JPPFScheduleHandler("DispatchExpiration");
  /**
   * The peer handler.
   */
  private final PeerAttributesHandler peerHandler;
  /**
   * Handles reservation of nodes to jobs.
   */
  private final NodeReservationHandler nodeReservationHandler;
  /**
   * Handler for th epersistence fo the state of the load-balancers.
   */
  private final LoadBalancerPersistenceManager bundlerHandler;

  /**
   * Initialize this node server.
   * @param driver reference to the driver.
   * @param queue the reference queue to use.
   * @param useSSL determines whether an SSLContext should be created for this server.
   * @throws Exception if the underlying server socket can't be opened.
   */
  public NodeNioServer(final JPPFDriver driver, final JPPFPriorityQueue queue, final boolean useSSL) throws Exception {
    super(JPPFIdentifiers.NODE_JOB_DATA_CHANNEL, useSSL, driver);
    if (driver == null) throw new IllegalArgumentException("driver is null");
    if (queue == null) throw new IllegalArgumentException("queue is null");

    this.queue = queue;
    this.queue.setCallableAllConnections(new Callable<List<BaseNodeContext<?>>>() {
      @Override
      public List<BaseNodeContext<?>> call() throws Exception {
        return getAllChannels();
      }
    });
    this.peerHandler = new PeerAttributesHandler(driver, Math.max(1, driver.getConfiguration().getInt("jppf.peer.handler.threads", 1)));
    nodeConnectionHandler = driver.getInitializer().getNodeConnectionEventHandler();
    bundlerFactory = new JPPFBundlerFactory(driver.getConfiguration());
    bundlerHandler = new LoadBalancerPersistenceManager(bundlerFactory);
    this.selectTimeout = NioConstants.DEFAULT_SELECT_TIMEOUT;
    taskQueueChecker = new TaskQueueChecker(this, queue, driver.getStatistics(), bundlerFactory);
    this.queue.addQueueListener(new QueueListenerAdapter<ServerJob, ServerTaskBundleClient, ServerTaskBundleNode>() {
      @Override
      public void bundleAdded(final QueueEvent<ServerJob, ServerTaskBundleClient, ServerTaskBundleNode> event) {
        if (debugEnabled) log.debug("received queue event {}", event);
        wakeUpSelectorIfNeeded();
        taskQueueChecker.wakeUp();
      }
    });
    initialServerJob = NodeServerUtils.createInitialServerJob(driver);
    nodeReservationHandler = new NodeReservationHandler(driver);
    ThreadUtils.startDaemonThread(taskQueueChecker, "TaskQueueChecker");
  }

  /**
   * Add the specified connection to the list of connections handled by this server.
   * @param nodeContext the connection to add.
   */
  void putConnection(final AbstractNodeContext nodeContext) {
    allConnections.put(nodeContext.getUuid(), nodeContext);
  }

  /**
   * Add the specified connection wrapper to the list of connections handled by this manager.
   * @param nodeContext the connection wrapper to add.
   */
  private void addConnection(final AbstractNodeContext nodeContext) {
    try {
      if (nodeContext == null) throw new IllegalArgumentException("nodeContext is null");
      if (nodeContext.getChannel() == null) throw new IllegalArgumentException("channel is null");
      if (debugEnabled) log.debug("adding connection {}", nodeContext.getChannel());
      final ChannelWrapper<?> channel = nodeContext.getChannel();
      if (channel.isOpen()) {
        if (channel.isOpen()) {
          nodeContext.addExecutionStatusListener(statusListener);
          if (channel.isOpen()) updateConnectionStatus(nodeContext, ExecutorStatus.DISABLED, nodeContext.getExecutionStatus());
        }
      }
      if (!channel.isOpen()) nodeContext.handleException(null);
    } catch(final Exception e) {
      if (debugEnabled) log.debug("error adding connection {} : {}", nodeContext, e);
    }
  }

  /**
   * Remove the specified connection wrapper from the list of connections handled by this manager.
   * @param nodeContext the connection wrapper to remove.
   */
  private void removeConnection(final AbstractNodeContext nodeContext) {
    if (nodeContext == null) throw new IllegalArgumentException("wrapper is null");
    if (debugEnabled) log.debug("removing connection {}", nodeContext.getChannel());
    try {
      taskQueueChecker.removeIdleChannelAsync(nodeContext);
      updateConnectionStatus(nodeContext, nodeContext.getExecutionStatus(), ExecutorStatus.DISABLED);
    } catch(final Exception e) {
      if (debugEnabled) log.debug("error removing connection {} : {}", nodeContext, e);
    } finally {
      try {
        final String uuid = nodeContext.getUuid();
        if (uuid != null) allConnections.remove(uuid);
        nodeContext.removeExecutionStatusListener(statusListener);
      } catch (final Throwable e) {
        if (debugEnabled) log.debug("error removing connection {} : {}", nodeContext, e);
      }
    }
  }

  /**
   * Remove the specified connection wrapper from the list of connections handled by this manager.
   * @param uuid the id of the connection to remove.
   * @return the context of th channel that was removed, or <code>null</code> if the channel was not found.
   */
  private AbstractNodeContext removeConnection(final String uuid) {
    if (uuid == null) return null;
    final AbstractNodeContext nodeContext = getConnection(uuid);
    if (nodeContext != null) removeConnection(nodeContext);
    return nodeContext;
  }

  /**
   * Get the connection wrapper for the specified uuid.
   * @param uuid the id of the connection to get.
   * @return the context of the cpnnect that was found, or <code>null</code> if the channel was not found.
   */
  public AbstractNodeContext getConnection(final String uuid) {
    return uuid == null ? null : allConnections.get(uuid);
  }

  /**
   * Remove the specified connection wrapper from the list of connections handled by this manager.
   * @param uuid the id of the node to activate or deactivate.
   * @param activate {@code true} to activate the node, {@code false} to deactivate it.
   * @return the context of th channel that was removed, or {@code null} if the channel was not found.
   */
  public AbstractNodeContext activateNode(final String uuid, final boolean activate) {
    final AbstractNodeContext nodeContext = getConnection(uuid);
    if (nodeContext == null) return null;
    if (activate != nodeContext.isActive()) nodeContext.setActive(activate);
    return nodeContext;
  }

  /**
   * Initialize the local channel connection.
   * @param localChannel the local channel to use.
   */
  public void initLocalChannel(final ChannelWrapper<?> localChannel) {
    final ChannelSelector channelSelector = new LocalChannelSelector(localChannel);
    localChannel.setSelector(channelSelector);
    selectorThread = new ChannelSelectorThread(channelSelector, this, 1L);
    localChannel.setInterestOps(0);
    ThreadUtils.startDaemonThread(selectorThread, "NodeChannelSelector");
    postAccept(localChannel);
  }

  /**
   * @param nodeContext   the connection wrapper.
   * @param oldStatus the connection status before the change.
   * @param newStatus the connection status after the change.
   */
  private void updateConnectionStatus(final AbstractNodeContext nodeContext, final ExecutorStatus oldStatus, final ExecutorStatus newStatus) {
    if (oldStatus == null) throw new IllegalArgumentException("oldStatus is null");
    if (newStatus == null) throw new IllegalArgumentException("newStatus is null");
    if (nodeContext == null || oldStatus == newStatus) return;
    if (debugEnabled) log.debug("updating status from {} to {} for {}", oldStatus, newStatus, nodeContext);
    if (newStatus == ExecutorStatus.ACTIVE) taskQueueChecker.addIdleChannel(nodeContext);
    else {
      taskQueueChecker.removeIdleChannelAsync(nodeContext);
      if (newStatus == ExecutorStatus.FAILED || newStatus == ExecutorStatus.DISABLED) {
        final String uuid = nodeContext.getUuid();
        transitionManager.execute(() -> queue.getBroadcastManager().cancelBroadcastJobs(uuid));
      }
    }
    queue.updateWorkingConnections(oldStatus, newStatus);
  }

  @Override
  protected NioServerFactory<NodeState, NodeTransition> createFactory() {
    return new NodeServerFactory(this);
  }

  @Override
  public void postAccept(final ChannelWrapper<?> channel) {
    //statsManager.newNodeConnection();
    if (debugEnabled) log.debug("performing post-accept for {}", channel);
    getDriver().getStatistics().addValue(JPPFStatisticsHelper.NODES, 1);
    final AbstractNodeContext context = (AbstractNodeContext) channel.getContext();
    try {
      context.setBundle(getInitialBundle());
      transitionManager.transitionChannel(channel, NodeTransition.TO_SEND_INITIAL);
    } catch (final Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.warn(ExceptionUtils.getMessage(e));
      closeNode(context);
    }
    if (debugEnabled) log.debug("end of post-accept for {}", channel);
  }

  @Override
  public AbstractNodeContext createNioContext(final Object...params) {
    final RemoteNodeContext context = new RemoteNodeContext(this);
    context.setOnClose(() -> {
      if (debugEnabled) log.debug("runninng onClose() for {}", context);
      closeNode(context);
    });
    return context;
  }

  /**
   * Get the task bundle sent to a newly connected node,
   * so that it can check whether it is up to date, without having
   * to wait for an actual request to be sent.
   * @return a {@link ServerJob} instance, with no task in it.
   */
  ServerTaskBundleNode getInitialBundle() {
    return initialServerJob.copy(0);
  }

  /**
   * Close a connection to a node.
   * @param context a <code>SocketChannel</code> that encapsulates the connection.
   */
  public void closeNode(final AbstractNodeContext context) {
    lock.lock();
    try {
      wakeUpSelectorIfNeeded();
      if (debugEnabled) log.debug("closing node {}", context);
      if (context != null) context.close();
    } catch (final Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.warn(ExceptionUtils.getMessage(e));
    } finally {
      lock.unlock();
    }
    try {
      peerHandler.onCloseNode(context);
      if (context != null) {
        JPPFManagementInfo info = context.getManagementInfo();
        if (info == null) info = new JPPFManagementInfo("unknown host", "unknown host", -1, context.getUuid(), context.isPeer() ? JPPFManagementInfo.PEER : JPPFManagementInfo.NODE, context.isSecure());
        if (debugEnabled) log.debug("firing nodeDisconnected() for {}", info);
        nodeConnectionHandler.fireNodeDisconnected(info);
      }
      getDriver().getStatistics().addValue(JPPFStatisticsHelper.NODES, -1);
      removeConnection(context);
    } catch (final Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.warn(ExceptionUtils.getMessage(e));
    }
  }

  /**
   * Get the factory object used to create bundler instances.
   * @return an instance of <code>JPPFBundlerFactory</code>.
   */
  public JPPFBundlerFactory getBundlerFactory() {
    return bundlerFactory;
  }

  /**
   * Get all the node connections handled by this server.
   * @return a list of <code>ChannelWrapper</code> instances.
   */
  public List<BaseNodeContext<?>> getAllChannels() {
    return new ArrayList<>(allConnections.values());
  }

  /**
   * Get all the node connections handled by this server.
   * @return a set of <code>ChannelWrapper</code> instances.
   */
  public Set<BaseNodeContext<?>> getAllChannelsAsSet() {
    return new HashSet<>(allConnections.values());
  }

  /**
   * Get the list of currently idle channels.
   * @return a list of <code>AbstractNodeContext</code> instances.
   */
  public List<BaseNodeContext<?>> getIdleChannels() {
    return taskQueueChecker.getIdleChannels();
  }

  /**
   * Called when the node failed to respond to a heartbeat message.
   * @param channel the channel to close.
   */
  public void connectionFailed(final ChannelWrapper<?> channel) {
    if (channel != null) {
      final AbstractNodeContext context = (AbstractNodeContext) channel.getContext();
      if (debugEnabled) log.debug("about to close channel = {} with uuid = {}", channel, context.getUuid());
      removeConnection(context.getUuid());
      context.handleException(null);
    }
  }

  @Override
  public boolean isIdle(final ChannelWrapper<?> channel) {
    return ((AbstractNodeContext) channel.getContext()).getIdle().get();
  }

  @Override
  public void removeAllConnections() {
    lock.lock();
    try {
      wakeUpSelectorIfNeeded();
      stopped.set(true);
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      lock.unlock();
    }
    try {
      if (taskQueueChecker != null) {
        taskQueueChecker.setStopped(true);
        taskQueueChecker.wakeUp();
        taskQueueChecker.clearIdleChannels();
      }
      queue.close();
      for (AbstractNodeContext channel: allConnections.values()) {
        try {
          channel.close();
        } catch (final Exception e) {
          log.error(e.getMessage(), e);
        }
      }
      allConnections.clear();
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
    }
    super.removeAllConnections();
  }

  @Override
  public void nodeConnected(final BaseNodeContext<?> context) {
    final JPPFManagementInfo info = context.getManagementInfo();
    if (context.getChannel().isOpen()) {
      peerHandler.onNodeConnected(context);
      addConnection((AbstractNodeContext) context);
      if (context.getChannel().isOpen()) {
        if (info != null) nodeConnectionHandler.fireNodeConnected(info);
      }
    }
    if (!context.getChannel().isOpen()) context.handleException(null);
  }

  /**
   * Get the corresponding node's context information.
   * @return a {@link JPPFContext} instance.
   */
  public JPPFContext getJPPFContext() {
    return taskQueueChecker.getJPPFContext();
  }

  /**
   * Get the object that holds the node bundles waiting for a node to reconnect and send the rsults.
   * @return a {@link OfflineNodeHandler} instance.
   */
  public OfflineNodeHandler getOfflineNodeHandler() {
    return offlineNodeHandler;
  }

  /**
   * Get the handler for the expiration of dispatched bundles.
   * @return a {@link JPPFScheduleHandler} instance.
   */
  public JPPFScheduleHandler getDispatchExpirationHandler() {
    return dispatchExpirationHandler;
  }

  /**
   * Get the peer handler.
   * @return a {@link PeerAttributesHandler} instance.
   */
  public PeerAttributesHandler getPeerHandler() {
    return peerHandler;
  }

  /**
   * Get the object that handles reservation of nodes to jobs.
   * @return a {@link NodeReservationHandler} instance.
   */
  public NodeReservationHandler getNodeReservationHandler() {
    return nodeReservationHandler;
  }

  /**
   * Get the task that dispatches queued jobs to available nodes.
   * @return a {@link TaskQueueChecker} object.
   * @exclude
   */
  public TaskQueueChecker getTaskQueueChecker() {
    return taskQueueChecker;
  }

  /**
   * @return the handler for the persistence fo the state of the load-balancers.
   * @exclude
   */
  public LoadBalancerPersistenceManager getBundlerHandler() {
    return bundlerHandler;
  }

  /**
   * @return a reference to the driver. 
   */
  public JPPFDriver getDriver() {
    return (JPPFDriver) attachment;
  }
}
