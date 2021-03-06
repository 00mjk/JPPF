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

package test.org.jppf.classloader;

import static org.junit.Assert.assertTrue;

import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.utils.concurrent.ConcurrentUtils;
import org.jppf.utils.concurrent.ConcurrentUtils.ConditionFalseOnException;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.BaseTestHelper;

/**
 * Unit tests for {@link org.jppf.classloader.AbstractJPPFClassLoader}.
 * @author Laurent Cohen
 */
public class TestLocalJPPFClassLoader extends AbstractClassLoaderTest {
  /** */
  @Rule
  public TestWatcher testLocalJPPFClassLoaderWatcher = new TestWatcher() {
    @Override
    protected void starting(final Description description) {
      BaseTestHelper.printToAll(client, false, false, true, false, true, "start of method %s()", description.getMethodName());
    }
  };

  /**
   * Launches a driver and 1 node and start the client.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    final TestConfiguration config = createConfig("classloader");
    config.driver.jppf = "classes/tests/config/classloader/driver_local_node.properties";
    client = BaseSetup.setup(1, 0, true, true, config);
    try (final JMXDriverConnectionWrapper jmx = new JMXDriverConnectionWrapper("localhost", 11101, false)) {
      assertTrue(jmx.connectAndWait(5_000L));
      assertTrue(ConcurrentUtils.awaitCondition((ConditionFalseOnException) () -> jmx.nbNodes() >= 1, 5_000L, 500L, false));
    }
  }
}
