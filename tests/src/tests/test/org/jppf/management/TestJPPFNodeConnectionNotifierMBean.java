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

package test.org.jppf.management;

import static org.junit.Assert.*;

import java.util.*;

import javax.management.*;

import org.jppf.management.*;
import org.jppf.management.forwarding.NodeForwardingMBean;
import org.jppf.node.policy.IsMasterNode;
import org.junit.*;

import test.org.jppf.test.setup.*;

/**
 * Unit tests for {@link JPPFNodeConnectionNotifierMBean}.
 * In this class, we test that the functionality of the {@code JPPFNodeConnectionNotifierMBean} from the client point of view.
 * @author Laurent Cohen
 */
public class TestJPPFNodeConnectionNotifierMBean extends AbstractNonStandardSetup implements NotificationListener {
  /**
   *
   */
  private final List<Notification> notifList = new ArrayList<>();

  /**
   * Launches 1 driver with 1 node attached and start the client.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    client = BaseSetup.setup(1, 1, true, true, createConfig("provisioning"));
  }

  /**
   * Test that notifications of node connections are received.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 15000)
  public void testConnectionNotifications() throws Exception {
    final int nbSlaves = 2;
    final JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    print(false, false, "waiting for master node");
    while (driver.nbIdleNodes() < 1) Thread.sleep(10L);
    driver.addNotificationListener(JPPFNodeConnectionNotifierMBean.MBEAN_NAME, this);
    final NodeForwardingMBean forwarder = driver.getForwarder();
    final NodeSelector selector = new ExecutionPolicySelector(new IsMasterNode());
    forwarder.provisionSlaveNodes(selector, nbSlaves);
    print(false, false, "waiting for %d slave nodes", nbSlaves);
    while (driver.nbIdleNodes() < nbSlaves + 1) Thread.sleep(10L);
    print(false, false, "waiting for %d connected notifications", nbSlaves);
    synchronized(notifList) {
      while (notifList.size() < nbSlaves) notifList.wait(10L);
    }
    print(false, false, "terminating slave nodes");
    forwarder.provisionSlaveNodes(selector, 0);
    print(false, false, "waiting for slave nodes termination");
    while (driver.nbIdleNodes() > 1) Thread.sleep(10L);
    print(false, false, "waiting for %d notifications", 2 * nbSlaves);
    synchronized(notifList) {
      while (notifList.size() < 2 * nbSlaves) notifList.wait(10L);
    }
    driver.removeNotificationListener(JPPFNodeConnectionNotifierMBean.MBEAN_NAME, this);
    int connectedCount = 0;
    int disconnectedCount = 0;
    for (Notification notif: notifList) {
      print(false, false, "notifList[%d] = %s, %s", (connectedCount + disconnectedCount), notif.getType(), notif.getUserData());
      assertEquals(JPPFNodeConnectionNotifierMBean.MBEAN_NAME, notif.getSource());
      switch(notif.getType()) {
        case JPPFNodeConnectionNotifierMBean.CONNECTED:
          connectedCount++;
          break;
        case JPPFNodeConnectionNotifierMBean.DISCONNECTED:
          disconnectedCount++;
          break;
        default:
          throw new IllegalStateException(String.format("notification has an invalid type: %s", notif));
      }
      assertTrue(notif.getUserData() instanceof JPPFManagementInfo);
    }
    assertEquals(nbSlaves, connectedCount);
    assertEquals(nbSlaves, disconnectedCount);
  }

  @Override
  public void handleNotification(final Notification notification, final Object handback) {
    final JPPFManagementInfo info = (JPPFManagementInfo) notification.getUserData();
    print(false, false, "received '%s' notification for %s", notification.getType(), info);
    if (info.isMasterNode()) return;
    synchronized(notifList) {
      notifList.add(notification);
    }
  }
}
