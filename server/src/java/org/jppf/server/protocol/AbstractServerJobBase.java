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

package org.jppf.server.protocol;

import java.util.*;
import java.util.concurrent.locks.Lock;

import org.jppf.io.DataLocation;
import org.jppf.node.protocol.TaskBundle;
import org.jppf.server.JPPFDriver;
import org.jppf.server.submission.SubmissionStatus;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 * @author Martin JANDA
 * @exclude
 */
public class AbstractServerJobBase extends AbstractServerJob {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AbstractServerJobBase.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The list of the tasks.
   */
  protected final List<ServerTask> tasks = new ArrayList<>();
  /**
   * The list of the incoming bundles.
   */
  protected final List<ServerTaskBundleClient> clientBundles = new ArrayList<>();
  /**
   * Set of all dispatched bundles in this job.
   */
  protected final Map<Long, ServerTaskBundleNode> dispatchSet = new LinkedHashMap<>();
  /**
   * The requeue handler.
   */
  protected Runnable onRequeue = null;
  /**
   * The data location of the data provider.
   */
  protected final DataLocation dataProvider;

  /**
   * Initialized client job with task bundle and list of tasks to execute.
   * @param lock used to synchronized access to job.
   * @param notificationEmitter an <code>ChangeListener</code> instance that fires job notifications.
   * @param job  underlying task bundle.
   * @param dataProvider the data location of the data provider.
   */
  public AbstractServerJobBase(final Lock lock, final ServerJobChangeListener notificationEmitter, final TaskBundle job, final DataLocation dataProvider) {
    super(lock, job);
    this.notificationEmitter = notificationEmitter;
    this.dataProvider = dataProvider;
  }

  /**
   * Get list of bundles received from client.
   * @return list of bundles received from client.
   */
  public List<ServerTaskBundleClient> getClientBundles() {
    lock.lock();
    try {
      return new ArrayList<>(clientBundles);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Get the location for data shared between tasks.
   * @return a <code>DataLocation</code> instance.
   */
  public DataLocation getDataProvider() {
    return dataProvider;
  }

  @Override
  public int getTaskCount() {
    lock.lock();
    try {
      return tasks.size();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Merge this client job wrapper with another.
   * @param taskList list of tasks to merge.
   * @param after determines whether the tasks from other should be added first or last.
   * @return <code>true</code> when this client job needs to be requeued.
   */
  protected boolean merge(final List<ServerTask> taskList, final boolean after) {
    lock.lock();
    try {
      final boolean requeue = this.tasks.isEmpty() && !taskList.isEmpty();
      if (!after) this.tasks.addAll(0, taskList);
      else this.tasks.addAll(taskList);
      return requeue;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Called when all or part of a job is dispatched to a node.
   * @param bundle  the dispatched job.
   */
  public void jobDispatched(final ServerTaskBundleNode bundle) {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");
    final boolean empty;
    synchronized (dispatchSet) {
      empty = dispatchSet.isEmpty();
      dispatchSet.put(bundle.getId(), bundle);
    }
    if (debugEnabled) log.debug("added to dispatch set: {}", bundle);
    if (empty) {
      updateStatus(ServerJobStatus.NEW, ServerJobStatus.EXECUTING);
      setSubmissionStatus(SubmissionStatus.EXECUTING);
    }
    fireJobDispatched(bundle.getChannel(), bundle);
  }

  /**
   * Called when all or part of a job is returned from node.
   * @param bundle the returned job.
   */
  public void jobReturned(final ServerTaskBundleNode bundle) {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");

    synchronized (dispatchSet) {
      dispatchSet.remove(bundle.getId());
    }
    if (debugEnabled) log.debug("removed from dispatch set: {}", bundle);
    fireJobReturned(bundle.getChannel(), bundle);
  }

  /**
   * Get indicator whether job has pending tasks.
   * @return <code>true</code> when job has some pending tasks.
   */
  protected boolean hasPending() {
    lock.lock();
    try {
      for (final ServerTaskBundleClient bundle: clientBundles) {
        if (bundle.getPendingTasksCount() > 0) return true;
      }
    } finally {
      lock.unlock();
    }
    return false;
  }

  /**
   * Set the requeue handler.
   * @param onRequeue {@link Runnable} executed on requeue.
   */
  public void setOnRequeue(final Runnable onRequeue) {
    if (getSLA().isBroadcastJob()) return; // broadcast jobs cannot be requeued
    this.onRequeue = onRequeue;
  }

  /**
   * Get count of channels on which this job is executed.
   * @return the number used for job execution.
   */
  public int getNbChannels() {
    synchronized (dispatchSet) {
      return dispatchSet.size();
    }
  }

  /**
   * Get the node bundle with the specified id from the dispatch set.
   * @param id the id of the bundle to find.
   * @return a {@link ServerTaskBundleNode} instance.
   */
  public ServerTaskBundleNode getNodeBundle(final long id) {
    synchronized (dispatchSet) {
      return dispatchSet.get(id);
    }
  }

  /**
   * Get the dispatch set. Used for debugging purposes.
   * @return a set of {@link ServerTaskBundleNode} instances.
   */
  public Set<ServerTaskBundleNode> getDispatchSet() {
    synchronized (dispatchSet) {
      return new LinkedHashSet<>(dispatchSet.values());
    }
  }

  /**
   * Add received bundle to this server job.
   * @param driver reference to the JPPF driver.
   * @param bundle the bundle to add.
   * @return {@code true} when bundle was added to job. {@code false} when job is {@code COMPLETE}.
   * @throws JPPFJobEndedException if the job is already {@code ENDED}.
   */
  public boolean addBundle(final JPPFDriver driver, final ServerTaskBundleClient bundle) throws JPPFJobEndedException {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");
    lock.lock();
    try {
      final SubmissionStatus submissionStatus = getSubmissionStatus();
      if (debugEnabled) log.debug("submissionStatus={}, adding {} to {}", submissionStatus, bundle, this);
      if ((submissionStatus == SubmissionStatus.COMPLETE) || (submissionStatus == SubmissionStatus.ENDED)) {
        throw new JPPFJobEndedException("Job " + submissionStatus);
      } else {
        clientBundles.add(bundle);
        this.tasks.addAll(bundle.getTaskList());
        bundle.addCompletionListener(new BundleCompletionListener(driver, this));
        fireJobUpdated(false);
        return true;
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append("id=").append(id);
    sb.append(", job uuid=").append(uuid);
    sb.append(", name=").append(name);
    sb.append(", status=").append(status);
    if (lock.tryLock()) {
      try {
        sb.append(", taskCount=").append(tasks.size());
      } finally {
        lock.unlock();
      }
    }
    sb.append(", nbBundles=").append(getNbBundles());
    //sb.append(", nbChannels=").append(getNbChannels());
    sb.append(", jobExpired=").append(jobExpired);
    sb.append(", pending=").append(pending);
    sb.append(", suspended=").append(isSuspended());
    sb.append(']');
    return sb.toString();
  }

  /**
   * Get the number of client bundles.
   * @return the number of bundles as an int.
   */
  public int getNbBundles() {
    return clientBundles.size();
  }
}
