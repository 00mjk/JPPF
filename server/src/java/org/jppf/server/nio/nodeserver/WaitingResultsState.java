/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import static org.jppf.node.protocol.BundleParameter.*;
import static org.jppf.server.nio.nodeserver.NodeTransition.*;

import java.util.*;
import java.util.concurrent.locks.Lock;

import org.jppf.io.DataLocation;
import org.jppf.job.JobReturnReason;
import org.jppf.load.balancer.*;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.nio.ChannelWrapper;
import org.jppf.node.protocol.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.protocol.*;
import org.jppf.utils.*;
import org.jppf.utils.stats.*;
import org.slf4j.*;

/**
 * This class performs performs the work of reading a task bundle execution response from a node.
 * @author Laurent Cohen
 */
class WaitingResultsState extends NodeServerState {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(WaitingResultsState.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * Initialize this state.
   * @param server the server that handles this state.
   */
  public WaitingResultsState(final NodeNioServer server) {
    super(server);
  }

  @Override
  public NodeTransition performTransition(final ChannelWrapper<?> channel) throws Exception {
    //if (debugEnabled) log.debug("exec() for " + channel);
    final AbstractNodeContext context = (AbstractNodeContext) channel.getContext();
    if (context.readMessage(channel)) {
      final BundleResults received = context.deserializeBundle();
      return process(received, context);
    }
    return TO_WAITING_RESULTS;
  }

  /**
   * Process the bundle that was just read.
   * @param received holds the received bundle along with the tasks.
   * @param context the channel from which the bundle was read.
   * @return the enxt transition to perform.
   * @throws Exception if any error occurs.
   */
  public NodeTransition process(final BundleResults received, final AbstractNodeContext context) throws Exception {
    final ServerTaskBundleNode nodeBundle = context.getBundle();
    server.getDispatchExpirationHandler().cancelAction(ServerTaskBundleNode.makeKey(nodeBundle), false);
    //server.getNodeReservationHandler().removeReservation(context);
    boolean requeue = false;
    try {
      final TaskBundle newBundle = received.bundle();
      if (debugEnabled) log.debug("read bundle " + newBundle + " from node " + context);
      requeue = processResults(context, received);
    } catch (final Throwable t) {
      log.error(t.getMessage(), t);
      nodeBundle.setJobReturnReason(JobReturnReason.DRIVER_PROCESSING_ERROR);
      nodeBundle.resultsReceived(t);
    } finally {
      context.setBundle(null);
    }
    if (requeue) nodeBundle.resubmit();
    // there is nothing left to do, so this instance will wait for a task bundle
    // make sure the context is reset so as not to resubmit the last bundle executed by the node.
    context.setMessage(null);
    return context.isPeer() ? TO_IDLE_PEER : TO_IDLE;
  }

  /**
   * Process the results received from the node.
   * @param context the context associated witht he node channel.
   * @param received groups the job header and resuls of the tasks.
   * @return A boolean requeue indicator.
   * @throws Exception if any error occurs.
   */
  private boolean processResults(final AbstractNodeContext context, final BundleResults received) throws Exception {
    final TaskBundle newBundle = received.bundle();
    // if an exception prevented the node from executing the tasks or sending back the results
    final Throwable t = newBundle.getParameter(NODE_EXCEPTION_PARAM);
    Bundler<?> bundler = context.getBundler();
    final ServerTaskBundleNode nodeBundle = context.getBundle();
    final Lock lock = nodeBundle.getClientJob().getLock();
    lock.lock();
    try {
      if (t != null) {
        if (debugEnabled) log.debug("node " + context.getChannel() + " returned exception parameter in the header for bundle " + newBundle + " : " + ExceptionUtils.getMessage(t));
        nodeBundle.setJobReturnReason(JobReturnReason.NODE_PROCESSING_ERROR);
        nodeBundle.resultsReceived(t);
      } else if (nodeBundle.getServerJob().isCancelled()) {
        if (debugEnabled) log.debug("received bundle with " + received.second().size() + " tasks for already cancelled bundle : " + received.bundle());
      } else {
        if (debugEnabled) log.debug("received bundle with " + received.second().size() + " tasks, taskCount=" + newBundle.getTaskCount() + " : " + received.bundle());
        if (nodeBundle.getJobReturnReason() == null) nodeBundle.setJobReturnReason(JobReturnReason.RESULTS_RECEIVED);
        if (!nodeBundle.isExpired()) {
          Set<Integer> resubmitSet = null;
          final int[] resubmitPositions = newBundle.getParameter(BundleParameter.RESUBMIT_TASK_POSITIONS, null);
          if (debugEnabled) log.debug("resubmitPositions = {} for {}", resubmitPositions, newBundle);
          if (resubmitPositions != null) {
            resubmitSet = new HashSet<>();
            for (int n: resubmitPositions) resubmitSet.add(n);
            if (debugEnabled) log.debug("resubmitSet = {} for {}", resubmitSet, newBundle);
          }
          int count = 0;
          for (final ServerTask task: nodeBundle.getTaskList()) {
            if ((resubmitSet != null) && resubmitSet.contains(task.getJobPosition())) {
              if (task.incResubmitCount() <= task.getMaxResubmits()) {
                task.resubmit();
                count++;
              }
            }
          }
          if (count > 0) context.updateStatsUponTaskResubmit(count);
        } else if (debugEnabled) log.debug("bundle has expired: {}", nodeBundle);
        final List<DataLocation> data = received.data();
        if (debugEnabled) log.debug("data received: size={}, content={}", data == null ? -1 : data.size(), data);
        if (debugEnabled) log.debug("nodeBundle={}", nodeBundle);
        nodeBundle.resultsReceived(data);
        final long elapsed = System.nanoTime() - nodeBundle.getJob().getExecutionStartTime();
        updateStats(newBundle.getTaskCount(), elapsed / 1_000_000L, newBundle.getNodeExecutionTime() / 1_000_000L);
        if (bundler == null) bundler = context.checkBundler(server.getBundlerFactory(), server.getJPPFContext());
        if (bundler instanceof BundlerEx) {
          final long accumulatedTime = newBundle.getParameter(NODE_BUNDLE_ELAPSED_PARAM, -1L);
          BundlerHelper.updateBundler((BundlerEx<?>) bundler, newBundle.getTaskCount(), elapsed, accumulatedTime, elapsed - newBundle.getNodeExecutionTime());
        } else BundlerHelper.updateBundler(bundler, newBundle.getTaskCount(), elapsed);
        server.getBundlerHandler().storeBundler(context.nodeIdentifier, bundler, context.bundlerAlgorithm);
      }
    } finally {
      lock.unlock();
    }
    final boolean requeue = newBundle.isRequeue();
    final JPPFSystemInformation systemInfo = newBundle.getParameter(SYSTEM_INFO_PARAM);
    if (systemInfo != null) {
      context.setNodeInfo(systemInfo, true);
      if (bundler instanceof ChannelAwareness) ((ChannelAwareness) bundler).setChannelConfiguration(systemInfo);
    }
    return requeue;
  }

  /**
   * Update the statistcis from the received results.
   * @param nbTasks number of tasks received.
   * @param elapsed server/node round trip time.
   * @param elapsedInNode time spent in the node.
   */
  private static void updateStats(final int nbTasks, final long elapsed, final long elapsedInNode) {
    final JPPFStatistics stats = JPPFDriver.getInstance().getStatistics();
    stats.addValue(JPPFStatisticsHelper.TASK_DISPATCH, nbTasks);
    stats.addValues(JPPFStatisticsHelper.EXECUTION, elapsed, nbTasks);
    stats.addValues(JPPFStatisticsHelper.NODE_EXECUTION, elapsedInNode, nbTasks);
    stats.addValues(JPPFStatisticsHelper.TRANSPORT_TIME, elapsed - elapsedInNode, nbTasks);
  }
}
