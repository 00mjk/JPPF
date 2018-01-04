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

package test.org.jppf.jmxremote;

import static org.junit.Assert.*;

import java.lang.management.*;
import java.util.*;

import javax.management.*;
import javax.management.remote.*;

import org.jppf.jmxremote.*;
import org.jppf.nio.NioHelper;
import org.jppf.utils.StringUtils;
import org.jppf.utils.collections.*;
import org.junit.*;

import test.org.jppf.test.setup.*;

/**
 *
 * @author Laurent Cohen
 */
public class TestStandaloneConnector extends BaseTest {
  /**
   * Object name of the ConnectorTestMBean.
   */
  private static ObjectName connectorTestName;
  /**
   * The MBean server used in the tests.
   */
  private static MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
  /**
   * Address of the JMX connector server.
   */
  private static JMXServiceURL url;

  /**
   *
   * @throws Exception  if any error occurs.
   */
  @BeforeClass
  public static void beforeClass() throws Exception {
    BaseSetup.setup(0, 0, false, BaseSetup.DEFAULT_CONFIG);
    BaseSetup.setLoggerLevel(org.apache.log4j.Level.DEBUG, "org.jppf.jmxremote", "org.jppf.nio");
    BaseSetup.setLoggerLevel(org.apache.log4j.Level.INFO, "org.jppf.nio.PlainNioObject", "org.jppf.serialization");
    url = new JMXServiceURL("service:jmx:jppf://localhost:12001");
    //url = new JMXServiceURL("service:jmx:jppf://192.168.1.24:12001");
    connectorTestName = new ObjectName(ConnectorTestMBean.MBEAN_NAME);
    registerMBeans();
  }

  /**
   *
   * @throws Exception  if any error occurs.
   */
  @AfterClass
  public static void afterClass() throws Exception {
    BaseSetup.setLoggerLevel(org.apache.log4j.Level.INFO, "org.jppf.jmxremote", "org.jppf.nio");
    BaseSetup.cleanup();
  }

  /**
   * Test connection.
   * @throws Exception if any error occurs.
   */
  //@Test(timeout = 10000)
  @Test
  public void testConnection() throws Exception {
    print(false, false, "***** starting connector server *****");
    final JMXConnectorServer server = createConnectorServer();
    print(false, true, "***** starting connector client *****");
    final JMXConnector client = createConnectorClient();
    final String connectionID = client.getConnectionId();
    assertNotNull(connectionID);
    assertTrue(connectionID.startsWith("jppf://"));
    final MBeanServerConnection mbsc = client.getMBeanServerConnection();
    print(false, true, "***** testing invoke *****");
    final String invokeResult = (String) mbsc.invoke(connectorTestName, "test1", new Object[] { "testing", 13 }, new String[] { String.class.getName(), int.class.getName() });
    assertEquals("[testing - 13]", invokeResult);
    print(false, true, "***** testing string attribute *****");
    String s = (String) mbsc.getAttribute(connectorTestName, "StringParam");
    assertNull(s);
    mbsc.setAttribute(connectorTestName, new Attribute("StringParam", "string value"));
    s = (String) mbsc.getAttribute(connectorTestName, "StringParam");
    assertNotNull(s);
    assertEquals("string value", s);
    print(false, true, "***** testing int attribute *****");
    int n = (Integer) mbsc.getAttribute(connectorTestName, "IntParam");
    assertEquals(0, n);
    mbsc.setAttribute(connectorTestName, new Attribute("IntParam", 13));
    n = (Integer) mbsc.getAttribute(connectorTestName, "IntParam");
    assertEquals(13, n);
    assertTrue(mbsc.isInstanceOf(connectorTestName, ConnectorTestMBean.class.getName()));
    print(false, false, "***** default domain: %s *****", mbsc.getDefaultDomain());
    final String[] domains = mbsc.getDomains();
    print(false, false, "***** domains: %s *****", Arrays.asList(domains));
    assertNotNull(domains);
    assertTrue(StringUtils.isOneOf("org.jppf", false, domains));

    print(false, true, "***** testing notifications *****");
    final MyListener listener = new MyListener();
    mbsc.addNotificationListener(connectorTestName, listener, null, "l1");
    mbsc.addNotificationListener(connectorTestName, listener, new StartsWithFilter("a"), "l2");
    final String[] messages = { "a1", "b2", "a3" };
    mbsc.invoke(connectorTestName, "triggerNotifications", new Object[] { messages }, new String[] { String[].class.getName() });
    Thread.sleep(250L);
    final CollectionMap<Object, String> infos = listener.infos;
    assertNotNull(infos);
    assertEquals(5, infos.size());
    assertEquals(2, infos.keySet().size());
    assertTrue(infos.containsKey("l1"));
    assertTrue(infos.containsKey("l2"));
    List<String> list = new ArrayList<>(infos.getValues("l1"));
    assertEquals(3, list.size());
    assertTrue(list.contains("a1"));
    assertTrue(list.contains("b2"));
    assertTrue(list.contains("a3"));
    list = new ArrayList<>(infos.getValues("l2"));
    assertEquals(2, list.size());
    assertTrue(list.contains("a1"));
    assertFalse(list.contains("b2"));
    assertTrue(list.contains("a3"));
    infos.clear();
    mbsc.removeNotificationListener(connectorTestName, listener);
    mbsc.invoke(connectorTestName, "triggerNotifications", new Object[] { messages }, new String[] { String[].class.getName() });
    Thread.sleep(250L);
    assertTrue(infos.isEmpty());

    print(false, true, "***** testing registration *****");
    assertTrue(mbsc.isRegistered(connectorTestName));
    mbsc.unregisterMBean(connectorTestName);
    assertFalse(mbsc.isRegistered(connectorTestName));
    print(false, false, "***** JPPF- thread count: %,d *****", countJMXThreads(0L));
    client.close();
    server.stop();
  }

  /**
   * @param sleepTime how long to sleep in millis before counting.
   * @return the count of live threads whose names starts with "JPPF-".
   * @throws Exception if any error occurs.
   */
  int countJMXThreads(final long sleepTime) throws Exception {
    if (sleepTime > 0L) Thread.sleep(sleepTime);
    final ThreadMXBean mxbean = ManagementFactory.getThreadMXBean();
    final long[] ids = mxbean.getAllThreadIds();
    final ThreadInfo[] allInfo = mxbean.getThreadInfo(ids);
    int count = 0;
    final String prefix = NioHelper.NIO_THREAD_NAME_PREFIX + "-";
    for (final ThreadInfo ti: allInfo) {
      if (ti == null) continue;
      final String name = ti.getThreadName();
      if (name == null) continue;
      if (name.startsWith(prefix)) count++;
    }
    return count;
  }

  /**
   * Register the LBeans used in the tests.
   * @throws Exception if any error occurs.
   */
  static void registerMBeans() throws Exception {
    mbeanServer.registerMBean(new ConnectorTest(), connectorTestName);
  }

  /**
   * Create a connector server.
   * @return a new started {@link JMXConnectorServer}.
   * @throws Exception if any error occurs.
   */
  static JMXConnectorServer createConnectorServer() throws Exception {
    final JMXConnectorServer server = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbeanServer);
    assertTrue(server instanceof JPPFJMXConnectorServer);
    server.start();
    return server;
  }

  /**
   * Create a connector client.
   * @return a new connected {@link JMXConnector}.
   * @throws Exception if any error occurs.
   */
  static JMXConnector createConnectorClient() throws Exception {
    final JMXConnector client = JMXConnectorFactory.connect(url);
    assertTrue(client instanceof JPPFJMXConnector);
    return client;
  }

  /** */
  static class MyListener implements NotificationListener {
    /** */
    CollectionMap<Object, String> infos = new ArrayListHashMap<>();

    @Override
    public void handleNotification(final Notification notification, final Object handback) {
      final String msg = (String) notification.getUserData();
      synchronized(infos) {
        infos.putValue(handback, msg);
      }
    }
  }

  /** */
  static class StartsWithFilter implements NotificationFilter {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;
    /** */
    private final String start;

    /**
     * @param start .
     */
    StartsWithFilter(final String start) {
      this.start = start;
    }

    @Override
    public boolean isNotificationEnabled(final Notification notification) {
      final String msg = (String) notification.getUserData();
      return msg.startsWith(start);
    }
  }
}
