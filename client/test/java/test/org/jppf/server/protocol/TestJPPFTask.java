/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

package test.org.jppf.server.protocol;

import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.client.JPPFJob;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.server.protocol.JPPFTask;
import org.junit.Test;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for {@link JPPFTask}.
 * In this class, we test that the behavior is the expected one, from the client point of view,
 * as specified in the job SLA.
 * @author Laurent Cohen
 */
public class TestJPPFTask extends Setup1D1N1C
{
  /**
   * Count of the number of jobs created.
   */
  private static AtomicInteger jobCount = new AtomicInteger(0);
  /**
   * A "short" duration for this test.
   */
  private static final long TIME_SHORT = 1000L;
  /**
   * A "long" duration for this test.
   */
  private static final long TIME_LONG = 3000L;
  /**
   * A "rest" duration for this test.
   */
  private static final long TIME_REST = 1L;
  /**
   * A the date format used in the tests.
   */
  private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

  /**
   * We test a job with 2 tasks, the 2nd task having a timeout duration set.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testTaskTimeout() throws Exception
  {
    JPPFJob job = BaseSetup.createJob("testTaskTimeoutDuration", true, false, 2, LifeCycleTask.class, TIME_LONG);
    List<JPPFTask> tasks = job.getTasks();
    JPPFSchedule schedule = new JPPFSchedule(TIME_SHORT);
    tasks.get(1).setTimeoutSchedule(schedule);
    List<JPPFTask> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), 2);
    LifeCycleTask task = (LifeCycleTask) results.get(0);
    assertNotNull(task.getResult());
    assertFalse(task.isTimedout());
    task = (LifeCycleTask) results.get(1);
    assertNull(task.getResult());
    assertTrue(task.isTimedout());
  }

  /**
   * Simply test that a job does expires at a specified date.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testTaskExpirationDate() throws Exception
  {
    JPPFJob job = BaseSetup.createJob("testTaskTimeoutDate", true, false, 2, LifeCycleTask.class, TIME_LONG);
    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
    Date date = new Date(System.currentTimeMillis() + TIME_LONG + TIME_SHORT + 10L);
    JPPFSchedule schedule = new JPPFSchedule(sdf.format(date), DATE_FORMAT);
    List<JPPFTask> tasks = job.getTasks();
    tasks.get(1).setTimeoutSchedule(schedule);
    List<JPPFTask> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), 2);
    LifeCycleTask task = (LifeCycleTask) results.get(0);
    assertNotNull(task.getResult());
    assertFalse(task.isTimedout());
    task = (LifeCycleTask) results.get(1);
    assertNull(task.getResult());
    assertTrue(task.isTimedout());
  }
}
