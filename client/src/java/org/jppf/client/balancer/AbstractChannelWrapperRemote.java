/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.client.balancer;

import java.net.InetSocketAddress;
import java.util.*;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.comm.socket.SocketWrapper;
import org.jppf.load.balancer.persistence.LoadBalancerPersistenceManager;
import org.jppf.management.*;
import org.jppf.node.protocol.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Context associated with a remote channel serving state and tasks submission.
 * @author Martin JANDA
 * @author Laurent Cohen
 */
public abstract class AbstractChannelWrapperRemote extends ChannelWrapper implements ClientConnectionStatusHandler {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractChannelWrapperRemote.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The channel to the driver to use.
   */
  final JPPFClientConnection channel;
  /**
   * Unique identifier of the client.
   */
  protected String uuid;

  /**
   * Default initializer for remote channel wrapper.
   * @param channel to the driver to use.
   */
  public AbstractChannelWrapperRemote(final JPPFClientConnection channel) {
    if (channel == null) throw new IllegalArgumentException("channel is null");
    this.channel = channel;
    final JPPFConnectionPool pool = channel.getConnectionPool();
    this.uuid = pool.getDriverUuid();
    priority = pool.getPriority();
    systemInfo = new JPPFSystemInformation(channel.getConnectionPool().getClient().getConfig(), this.uuid, false, true);
    managementInfo = new JPPFManagementInfo("remote", "remote", -1, getConnectionUuid(), JPPFManagementInfo.DRIVER, pool.isSslEnabled());
    managementInfo.setSystemInfo(systemInfo);
  }

  @Override
  public void setSystemInformation(final JPPFSystemInformation systemInfo) {
    super.setSystemInformation(systemInfo);
  }

  @Override
  public void initChannelID() {
    if (channelID != null) return;
    if (systemInfo == null) return;
    if (uuid == null) {
      uuid = systemInfo.getUuid().getProperty("jppf.uuid");
      if ((uuid != null) && uuid.isEmpty()) uuid = null;
    }
    try {
      final TaskServerConnectionHandler handler = ((JPPFClientConnectionImpl) channel).getTaskServerConnection();
      final SocketWrapper socketClient = handler.getSocketClient();
      if (socketClient != null) {
        final StringBuilder sb = new StringBuilder();
        final String ip = channel.getConnectionPool().getDriverIPAddress();
        sb.append(channel.getName());
        sb.append('[').append(ip == null ? "localhost" : ip).append(']');
        final InetSocketAddress sa = (InetSocketAddress) socketClient.getSocket().getRemoteSocketAddress();
        sb.append(sa.getAddress().getHostAddress()).append(':').append(socketClient.getPort());
        sb.append(channel.isSSLEnabled());
        final String s = sb.toString();
        channelID = new Pair<>(s, CryptoUtils.computeHash(s, channel.getConnectionPool().getClient().getBundlerFactory().getHashAlgorithm()));
        if (debugEnabled) log.debug("computed channelID for {} : {}", this, channelID);
      }
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  @Override
  public String getUuid() {
    return uuid;
  }

  @Override
  public String getConnectionUuid() {
    return channel.getConnectionUuid();
  }

  @Override
  public JPPFClientConnectionStatus getStatus() {
    return channel.getStatus();
  }

  @Override
  public void setStatus(final JPPFClientConnectionStatus status) {
    setOldStatus(getStatus());
    channel.setStatus(status);
  }

  /**
   * Get the wrapped channel.
   * @return a <code>AbstractJPPFClientConnection</code> instance.
   */
  public JPPFClientConnection getChannel() {
    return channel;
  }

  @Override
  public void addClientConnectionStatusListener(final ClientConnectionStatusListener listener) {
    channel.addClientConnectionStatusListener(listener);
  }

  @Override
  public void removeClientConnectionStatusListener(final ClientConnectionStatusListener listener) {
    channel.removeClientConnectionStatusListener(listener);
  }

  @Override
  public boolean isLocal() {
    return false;
  }

  /**
   * Called when reconnection of this channel is required.
   */
  void reconnect() {
    if (channel.isClosed()) {
      if (debugEnabled) log.debug("connection is closed, will not reconnect");
      return;
    }
    channel.setStatus(JPPFClientConnectionStatus.DISCONNECTED);
    ((JPPFClientConnectionImpl) channel).submitInitialization();
  }

  @Override
  public boolean cancel(final ClientTaskBundle bundle) {
    if (bundle.isCancelled()) return false;
    final String uuid = bundle.getClientJob().getUuid();
    if (debugEnabled) log.debug("requesting cancel of jobId=" + uuid);
    bundle.cancel();
    return true;
  }

  @Override
  LoadBalancerPersistenceManager getLoadBalancerPersistenceManager() {
    return (LoadBalancerPersistenceManager) channel.getConnectionPool().getClient().getLoadBalancerPersistenceManagement();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("[channel=");
    try {
      sb.append(channel);
    } catch (final Exception e) {
      sb.append(ExceptionUtils.getMessage(e));
    }
    sb.append(']');
    return sb.toString();
  }


  /**
   * Create a new job based on the initial one.
   * @param job   initial job.
   * @param tasks the tasks to execute.
   * @return a new {@link JPPFJob} with the same characteristics as the initial one, except for the tasks.
   * @throws Exception if any error occurs.
   */
  static JPPFJob createNewJob(final ClientTaskBundle job, final List<Task<?>> tasks) throws Exception {
    final JPPFJob newJob = new JPPFJob(job.getClientJob().getUuid());
    newJob.setDataProvider(job.getJob().getDataProvider());
    newJob.setSLA(job.getSLA());
    newJob.setClientSLA(job.getJob().getClientSLA());
    newJob.setMetadata(job.getMetadata());
    newJob.setName(job.getName());
    for (final Task<?> task : tasks) {
      // needed as JPPFJob.addTask() resets the position
      final int pos = task.getPosition();
      newJob.add(task);
      task.setPosition(pos);
    }
    return newJob;
  }

  /**
   * Create a task bundle for the specified job.
   * @param job the job to use as a base.
   * @param bundleId the id of the bundle to send.
   * @return a JPPFTaskBundle instance.
   */
  static TaskBundle createBundle(final JPPFJob job, final long bundleId) {
    final TaskBundle bundle = new JPPFTaskBundle();
    bundle.setUuid(job.getUuid());
    bundle.setParameter(BundleParameter.CLIENT_BUNDLE_ID, bundleId);
    return bundle;
  }

  /**
   * Return class loader for the specified job.
   * @param job the job used to determine class loader.
   * @return a list of ClassLoader instances, possibly empty.
   */
  Collection<ClassLoader> registerClassLoaders(final JPPFJob job) {
    if (job == null) throw new IllegalArgumentException("job is null");
    final Set<ClassLoader> result = new HashSet<>();
    if (!job.getJobTasks().isEmpty()) {
      final JPPFClient client = channel.getConnectionPool().getClient();
      for (final Task<?> task: job.getJobTasks()) {
        if (task != null) {
          final Object o = task.getTaskObject();
          final ClassLoader cl = (o != null) ? o.getClass().getClassLoader() : task.getClass().getClassLoader();
          if ((cl != null) && !result.contains(cl)) {
            client.registerClassLoader(cl, job.getUuid());
            result.add(cl);
          }
        }
      }
    }
    return result;
  }
}
