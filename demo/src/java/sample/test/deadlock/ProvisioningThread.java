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

package sample.test.deadlock;

import java.util.Map;

import org.jppf.client.JPPFClient;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.node.policy.*;
import org.jppf.utils.concurrent.ThreadSynchronization;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class ProvisioningThread extends ThreadSynchronization implements Runnable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ProvisioningThread.class);
  /**
   * 
   */
  private final JPPFClient client;
  /**
   * 
   */
  private final long waitTime;

  /**
   * 
   * @param client the JPPF client.
   * @param waitTime .
   */
  public ProvisioningThread(final JPPFClient client, final long waitTime) {
    this.client = client;
    this.waitTime = waitTime;
  }

  @Override
  public void run() {
    log.info("starting ProvisioningThread, waitTime={}", waitTime);
    JPPFNodeForwardingMBean forwarder = null;
    final ExecutionPolicy masterPolicy = new IsMasterNode();
    final NodeSelector masterSelector = new ExecutionPolicySelector(masterPolicy);
    while (!isStopped()) {
      if (forwarder == null) {
        try {
          final JMXDriverConnectionWrapper jmx = DeadlockRunner.getJmxConnection(client);
          log.info("getting forwarder");
          forwarder = jmx.getNodeForwarder();
        } catch (final Exception e) {
          e.printStackTrace();
          return;
        }
        log.info("got forwarder");
      }
      if (isStopped()) break;
      goToSleep(1000L);
      if (isStopped()) break;
      try {
        final Map<String, Object> map = forwarder.provisionSlaveNodes(masterSelector, 40);
        for (Map.Entry<String, Object> entry: map.entrySet()) {
          if (entry.getValue() instanceof Exception) throw (Exception) entry.getValue();
        }
      } catch(final Exception e) {
        e.printStackTrace();
        System.exit(1);
        return;
      }
      if (isStopped()) break;
      goToSleep(waitTime);
      if (isStopped()) break;
      try {
        final Map<String, Object> map = forwarder.provisionSlaveNodes(masterSelector, 0);
        for (Map.Entry<String, Object> entry: map.entrySet()) {
          if (entry.getValue() instanceof Exception) throw (Exception) entry.getValue();
        }
      } catch(final Exception e) {
        e.printStackTrace();
        System.exit(1);
        return;
      }
    }
    try {
      forwarder.provisionSlaveNodes(masterSelector, 0);
    } catch(@SuppressWarnings("unused") final Exception e) {
      //e.printStackTrace();
      return;
    }
  }
}
