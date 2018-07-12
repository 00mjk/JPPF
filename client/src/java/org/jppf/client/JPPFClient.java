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
package org.jppf.client;

import java.util.*;

import org.jppf.client.balancer.JobManagerClient;
import org.jppf.client.debug.Debug;
import org.jppf.client.event.ConnectionPoolListener;
import org.jppf.discovery.ClientDriverDiscovery;
import org.jppf.load.balancer.LoadBalancingInformation;
import org.jppf.load.balancer.spi.JPPFBundlerFactory;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.jppf.utils.Operator;
import org.jppf.utils.concurrent.*;
import org.slf4j.*;

/**
 * This class provides an API to submit execution requests and administration commands,
 * and request server information data.<br>
 * It has its own unique identifier, used by the nodes, to determine whether classes from
 * the submitting application should be dynamically reloaded or not, depending on whether
 * the uuid has changed or not.
 * @author Laurent Cohen
 */
public class JPPFClient extends AbstractGenericClient {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFClient.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * Initialize this client with an automatically generated application UUID.
   */
  public JPPFClient() {
    this(null, JPPFConfiguration.getProperties());
  }

  /**
   * Initialize this client with the specified application UUID.
   * @param uuid the unique identifier for this local client.
   */
  public JPPFClient(final String uuid) {
    this(uuid, JPPFConfiguration.getProperties());
  }

  /**
   * Initialize this client with an automatically generated application UUID.
   * @param listeners the optional listeners to add to this JPPF client to receive notifications of new connections.
   */
  public JPPFClient(final ConnectionPoolListener... listeners) {
    this(null, JPPFConfiguration.getProperties(), listeners);
  }

  /**
   * Initialize this client with the specified application UUID and new connection listeners.
   * @param uuid the unique identifier for this local client.
   * @param listeners the optional listeners to add to this JPPF client to receive notifications of new connections.
   */
  public JPPFClient(final String uuid, final ConnectionPoolListener... listeners) {
    this(uuid, JPPFConfiguration.getProperties(), listeners);
  }

  /**
   * Initialize this client with the specified configuration and connection listeners.
   * @param config the JPPF configuration to use for this client.
   * @param listeners the optional listeners to add to this JPPF client to receive notifications of new connections.
   * @exclude
   */
  public JPPFClient(final TypedProperties config, final ConnectionPoolListener... listeners) {
    this(null, config, listeners);
  }

  /**
   * Initialize this client with the specified application UUID and new connection listeners.
   * @param uuid the unique identifier for this local client.
   * @param config the JPPF configuration to use for this client.
   * @param listeners the optional listeners to add to this JPPF client to receive notifications of new connections.
   * @exclude
   */
  public JPPFClient(final String uuid, final TypedProperties config, final ConnectionPoolListener... listeners) {
    super(uuid, config, listeners);
    Debug.register(this);
  }

  @Override
  AbstractJPPFClientConnection createConnection(final String name, final JPPFConnectionPool pool) {
    return new JPPFClientConnectionImpl(this, name, pool);
  }

  @Override
  public List<Task<?>> submitJob(final JPPFJob job) {
    if (isClosed()) throw new IllegalStateException("this client is closed");
    if (job == null) throw new IllegalArgumentException("job cannot be null");
    if (job.getJobTasks().isEmpty()) throw new IllegalStateException("job cannot be empty");
    if (job.client != null) {
      if (!job.isDone()) throw new IllegalStateException("this job is already submitted");
      job.cancelled.set(false);
      job.getResults().clear();
    }
    job.client = this;
    if (debugEnabled) log.debug("submitting job {}", job);
    if (log.isTraceEnabled()) {
      for (Task<?> task: job) {
        log.trace(String.format("task %s, position=%d, taskObject=%s, taskObject class=%s", task, task.getPosition(), task.getTaskObject(),
            (task.getTaskObject() != null) ? task.getTaskObject().getClass() : null));
      }
    }
    getJobManager().submitJob(job);
    if (job.isBlocking()) return job.awaitResults();
    return null;
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  protected JobManager createJobManager() {
    JobManager jobManager = null;
    try {
      jobManager = new JobManagerClient(this, bundlerFactory);
    } catch (final Exception e) {
      log.error("Can't initialize job Manager", e);
    }
    return jobManager;
  }

  /**
   * Reset this client, that is, close it if necessary, reload its configuration, then open it again.
   * If the client is already closed or reseeting, this method has no effect.
   * @see #reset(TypedProperties)
   * @since 4.0
   */
  public void reset() {
    if (isClosed()) return;
    if (debugEnabled) log.debug("resetting client");
    if (resetting.compareAndSet(false, true)) {
      close(true);
      JPPFConfiguration.reset();
      init(JPPFConfiguration.getProperties());
    }
  }

  /**
   * Reset this client, that is, close it if necessary, then open it again, using the specified confguration.
   * If the client is already closed or reseeting, this method has no effect.
   * @param configuration the configuration to initialize this client with.
   * @see #reset()
   * @since 4.0
   */
  public void reset(final TypedProperties configuration) {
    if (isClosed()) return;
    if (debugEnabled) log.debug("resetting client");
    if (resetting.compareAndSet(false, true)) {
      close(true);
      init(configuration);
    }
  }

  /**
   * Wait until there is at least one connection pool with at least one connection in the {@link JPPFClientConnectionStatus#ACTIVE ACTIVE} status.
   * This is a shorthand for {@code awaitConnectionPool(Long.MAX_VALUE, JPPFClientConnectionStatus.ACTIVE)}.
   * @return a {@link JPPFConnectionPool} instance, or null if no pool has a connection in the one of the desird statuses.
   * @since 5.0
   */
  public JPPFConnectionPool awaitActiveConnectionPool() {
    return awaitConnectionPool(Long.MAX_VALUE, JPPFClientConnectionStatus.ACTIVE);
  }

  /**
   * Wait until there is at least one connection pool with at least one connection in the {@link JPPFClientConnectionStatus#ACTIVE ACTIVE}
   * or {@link JPPFClientConnectionStatus#EXECUTING EXECUTING} status.
   * This is a shorthand for {@code awaitConnectionPool(Long.MAX_VALUE, JPPFClientConnectionStatus.ACTIVE, JPPFClientConnectionStatus.EXECUTING)}.
   * @return a {@link JPPFConnectionPool} instance, or null if no pool has a connection in the one of the desird statuses.
   * @since 5.0
   */
  public JPPFConnectionPool awaitWorkingConnectionPool() {
    return awaitConnectionPool(Long.MAX_VALUE, JPPFClientConnectionStatus.ACTIVE, JPPFClientConnectionStatus.EXECUTING);
  }

  /**
   * Wait until there is at least one connection pool with at least one connection in one of the specified statuses.
   * This is a shorthand for {@code awaitConnectionPool(Long.MAX_VALUE, statuses)}.
   * @param statuses the possible statuses of the connections in the pools to wait for.
   * @return a {@link JPPFConnectionPool} instance, or null if no pool has a connection in the one of the desird statuses.
   * @since 5.0
   */
  public JPPFConnectionPool awaitConnectionPool(final JPPFClientConnectionStatus...statuses) {
    return awaitConnectionPool(Long.MAX_VALUE, statuses);
  }

  /**
   * Wait until at least one connection pool with at least one connection in one of the specified statuses,
   * or until the specified timeout to expire, whichever happens first.
   * @param timeout the maximum time to wait, in milliseconds. A value of zero means an infinite timeout.
   * @param statuses the possible statuses of the connections in the pools to wait for.
   * @return a {@link JPPFConnectionPool} instance, or null if no pool has a connection in the one of the desird statuses.
   * @since 5.0
   */
  public JPPFConnectionPool awaitConnectionPool(final long timeout, final JPPFClientConnectionStatus...statuses) {
    final List<JPPFConnectionPool> list = awaitConnectionPools(timeout, statuses);
    return list.isEmpty() ? null : list.get(0);
  }

  /**
   * Wait until there is at least one connection pool with at least one connection in the {@link JPPFClientConnectionStatus#ACTIVE ACTIVE}
   * or {@link JPPFClientConnectionStatus#EXECUTING EXECUTING} status.
   * This is a shorthand for {@code awaitConnectionPools(Long.MAX_VALUE, JPPFClientConnectionStatus.ACTIVE, JPPFClientConnectionStatus.EXECUTING)}.
   * @return a list of {@link JPPFConnectionPool} instances, possibly empty but never null.
   * @since 5.1
   */
  public List<JPPFConnectionPool> awaitWorkingConnectionPools() {
    return awaitConnectionPools(Long.MAX_VALUE, JPPFClientConnectionStatus.ACTIVE, JPPFClientConnectionStatus.EXECUTING);
  }

  /**
   * Wait until there is at least one connection pool with at least one connection in the {@link JPPFClientConnectionStatus#ACTIVE ACTIVE}
   * or {@link JPPFClientConnectionStatus#EXECUTING EXECUTING} status, or the specified tiemoput expires, whichever happens first.
   * This is a shorthand for {@code awaitConnectionPools(tiemout, JPPFClientConnectionStatus.ACTIVE, JPPFClientConnectionStatus.EXECUTING)}.
   * @param timeout the maximum time to wait, in milliseconds. A value of zero means an infinite timeout.
   * @return a list of {@link JPPFConnectionPool} instances, possibly empty but never null.
   * @since 5.1
   */
  public List<JPPFConnectionPool> awaitWorkingConnectionPools(final long timeout) {
    return awaitConnectionPools(timeout, JPPFClientConnectionStatus.ACTIVE, JPPFClientConnectionStatus.EXECUTING);
  }

  /**
   * Wait until at least one connection pool with at least one connection in one of the specified statuses,
   * or until the specified timeout to expire, whichever happens first.
   * @param timeout the maximum time to wait, in milliseconds. A value of zero means an infinite timeout.
   * @param statuses the possible statuses of the connections in the pools to wait for.
   * @return a list of {@link JPPFConnectionPool} instances, possibly empty but never null.
   * @since 5.0
   */
  public List<JPPFConnectionPool> awaitConnectionPools(final long timeout, final JPPFClientConnectionStatus...statuses) {
    final MutableReference<List<JPPFConnectionPool>> ref = new MutableReference<>();
    ConcurrentUtils.awaitCondition(new ConcurrentUtils.Condition() {
      @Override public boolean evaluate() {
        return !ref.setSynchronized(findConnectionPools(statuses), pools).isEmpty();
      }
    }, timeout);
    return ref.get();
  }

  /**
   * Wait until there is at least one connection pool where the number of connections with the specified statuses
   * satisfy the specified condition, or until the specified timeout expires, whichever happens first.
   * @param operator the condition on the number of connections to wait for. If {@code null}, it is assumed to be {@link Operator#EQUAL}.
   * @param expectedConnections the expected number of connections to wait for.
   * @param timeout the maximum time to wait, in milliseconds. A value of zero means an infinite timeout.
   * @param statuses the possible statuses of the connections in the pools to wait for.
   * @return a list of {@link JPPFConnectionPool} instances, possibly empty but never null.
   * @since 5.0
   */
  public List<JPPFConnectionPool> awaitConnectionPools(final ComparisonOperator operator, final int expectedConnections, final long timeout, final JPPFClientConnectionStatus...statuses) {
    return awaitConnectionPools(Operator.AT_LEAST, 1, operator, expectedConnections, timeout, statuses);
  }

  /**
   * Wait until at least the specified expected connection pools satisfy the condition where the number of connections with the specified statuses
   * satisfy the specified connection operator, or until the specified timeout expires, whichever happens first.
   * <p>As an example, to wait for at least 2 pools having each at least one ACTIVE connection, with a timeout of 5 seconds, one would use:
   * <pre>
   * JPPFClient client = new JPPFClient();
   * client.awaitConnectionPools(Operator.AT_LEAST, 2, Operator.AT_LEAST, 1,
   *   5000L, JPPFClientConnectionStatus.ACTIVE);
   * </pre>
   * @param poolOperator the condition on the number of expected pools to wait for. If {@code null}, it is assumed to be {@link Operator#EQUAL}.
   * @param expectedPools the expected number of pools to wait for.
   * @param connectionOperator the condition on the number of connections to wait for. If {@code null}, it is assumed to be {@link Operator#EQUAL}.
   * @param expectedConnections the expected number of connections to wait for.
   * @param timeout the maximum time to wait, in milliseconds. A value of zero means an infinite timeout.
   * @param statuses the possible statuses of the connections in the pools to wait for.
   * @return a list of {@link JPPFConnectionPool} instances, possibly empty but never null.
   * @since 6.0
   */
  public List<JPPFConnectionPool> awaitConnectionPools(final ComparisonOperator poolOperator, final int expectedPools, final ComparisonOperator connectionOperator, final int expectedConnections,
    final long timeout, final JPPFClientConnectionStatus...statuses) {
    final MutableReference<List<JPPFConnectionPool>> ref = new MutableReference<>();
    ConcurrentUtils.awaitCondition(new ConcurrentUtils.Condition() {
      @Override public boolean evaluate() {
        final List<JPPFConnectionPool> result = new ArrayList<>();
        final List<JPPFConnectionPool> temp = findConnectionPools(statuses);
        for (final JPPFConnectionPool pool: temp) {
          final List<JPPFClientConnection> list = pool.getConnections(statuses);
          if (connectionOperator.evaluate(list.size(), expectedConnections)) result.add(pool);
        }
        ref.setSynchronized(result, pools);
        return poolOperator.evaluate(result.size(), expectedPools);
      }
    }, timeout);
    return ref.get();
  }

  /**
   * Wait until there is at least one connection pool where at least one connections passes the specified filter,
   * or until the specified timeout expires, whichever happens first.
   * @param timeout the maximum time to wait, in milliseconds. A value of zero means an infinite timeout.
   * @param filter an implementation of the {@link ConnectionPoolFilter} interface. A {@code null} value is interpreted as no filter (all pools are accepted).
   * @return a list of {@link JPPFConnectionPool} instances, possibly empty but never null.
   * @since 5.0
   */
  public List<JPPFConnectionPool> awaitConnectionPools(final long timeout, final ConnectionPoolFilter<JPPFConnectionPool> filter) {
    final MutableReference<List<JPPFConnectionPool>> ref = new MutableReference<>();
    ConcurrentUtils.awaitCondition(new ConcurrentUtils.Condition() {
      @Override public boolean evaluate() {
        final List<JPPFConnectionPool> result = new ArrayList<>();
        final List<JPPFConnectionPool> temp = getConnectionPools();
        for (final JPPFConnectionPool pool: temp) {
          if (filter.accepts(pool)) result.add(pool);
        }
        return !ref.setSynchronized(result, pools).isEmpty();
      }
    }, timeout);
    return ref.get();
  }

  @Override
  public void close() {
    log.info("closing {}", this);
    Debug.unregister(this);
    super.close();
  }

  /**
   * Remove a custom driver discovery mechanism from those already registered.
   * @param discovery the driver discovery to remove.
   */
  public void removeDriverDiscovery(final ClientDriverDiscovery discovery) {
    discoveryHandler.removeDiscovery(discovery);
  }

  /**
   * Get the current load-balancer settings.
   * @return a {@link LoadBalancingInformation} instance, which encapsulates a load-balancing alfgorithm name, along with its parameters.
   * @since 5.2.7
   */
  public LoadBalancingInformation getLoadBalancerSettings() {
    final JobManager manager = getJobManager();
    return (manager == null) ? null : manager.getLoadBalancerSettings();
  }

  /**
   * Change the load balancer settings.
   * @param algorithm the name of load-balancing alogrithm to use.
   * @param parameters the algorithm's parameters, if any. The parmeter names are assumed no to be prefixed.
   * @throws Exception if any error occurs or if the algorithm name is {@code null} or not known.
   * @since 5.2.7
   */
  public void setLoadBalancerSettings(final String algorithm, final Properties parameters) throws Exception {
    final JobManager manager = getJobManager();
    if (manager != null) manager.setLoadBalancerSettings(algorithm, parameters);
  }

  /**
   * Get the factory that creates load-balancer instances.
   * @return an istance of {@link JPPFBundlerFactory}.
   * @exclude
   */
  public JPPFBundlerFactory getBundlerFactory() {
    return bundlerFactory;
  }

  /**
   * @return the number of idle connections in this client.
   * @exclude
   */
  public int nbIdleCOnnections() {
    final JobManagerClient manager = (JobManagerClient) getJobManager();
    return (manager == null) ? -1 : manager.nbAvailableConnections();
  }
}
