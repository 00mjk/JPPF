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

package test.org.jppf.jca.cci;

import static org.jppf.client.JobStatus.*;
import static org.junit.Assert.*;

import java.util.*;

import org.jppf.client.*;
import org.jppf.jca.cci.JPPFConnection;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.junit.Test;

import test.org.jppf.test.setup.JPPFHelper;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for <code>org.jppf.jca.cci.JPPFConnection</code>.
 * @author Laurent Cohen
 */
public class TestJPPFConnection {

  /**
   * Test submitting a simple job and getting the results.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000L)
  public void testSubmit() throws Exception {
    JPPFConnection connection = null;
    try {
      connection = JPPFHelper.getConnection();
      assertNotNull(connection);
      final JPPFJob job = BaseTestHelper.createJob("JCA testSubmit", true, false, 1, LifeCycleTask.class);
      final String id = connection.submit(job);
      assertNotNull(id);
      final List<Task<?>> results = connection.awaitResults(id);
      assertNotNull(results);
      final int n = results.size();
      assertTrue("results size should be 1 but is " + n, n == 1);
      final Task<?> task = results.get(0);
      assertNotNull(task);
      assertNull(task.getThrowable());
      assertNotNull(task.getResult());
      assertEquals(task.getResult(), BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE);
    } finally {
      if (connection != null) connection.close();
    }
  }

  /**
   * Test submitting a job and retrieving the results after closing the JCA connection and getting a new one.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000L)
  public void testSubmitAndRetrieve() throws Exception {
    JPPFConnection connection = null;
    final int nbTasks = 10;
    String id = null;
    try {
      connection = JPPFHelper.getConnection();
      assertNotNull(connection);
      final JPPFJob job = BaseTestHelper.createJob("JCA testSubmitAndRetrieve", true, false, nbTasks, LifeCycleTask.class, 500L);
      id = connection.submit(job);
      assertNotNull(id);
    } finally {
      if (connection != null) connection.close();
      connection = null;
    }
    Thread.sleep(1000L);
    try {
      connection = JPPFHelper.getConnection();
      final List<Task<?>> results = connection.awaitResults(id);
      assertNotNull(results);
      final int n = results.size();
      assertEquals("results size should be " + nbTasks + " but is " + n, nbTasks, n);
      final int count = 0;
      for (final Task<?> task: results) {
        assertNotNull("task" + count + " is null", task);
        final Throwable e = task.getThrowable();
        assertNull(task.getId() + " exception should be null but is '" + (e == null ? "" : ExceptionUtils.getMessage(e)) + "'", e);
        assertNotNull(task.getId() + " result is null", task.getResult());
        assertEquals(task.getResult(), BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE);
      }
    } finally {
      if (connection != null) connection.close();
    }
  }

  /**
   * Test submitting a simple job with a status listener.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000L)
  public void testStatusListenerFromSubmit() throws Exception {
    JPPFConnection connection = null;
    try {
      connection = JPPFHelper.getConnection();
      assertNotNull(connection);
      final JPPFJob job = BaseTestHelper.createJob("JCA testStatusListenerFromSubmit", true, false, 1, LifeCycleTask.class);
      final GatheringStatusListener listener = new GatheringStatusListener();
      final String id = connection.submit(job, listener);
      assertNotNull(id);
      final List<Task<?>> results = connection.awaitResults(id);
      assertNotNull(results);
      final int n = results.size();
      assertTrue("results size should be 1 but is " + n, n == 1);
      final List<JobStatus> statuses = listener.getStatuses();
      assertNotNull(statuses);
      assertArrayEquals(new JobStatus[] { PENDING, EXECUTING, COMPLETE }, statuses.toArray(new JobStatus[statuses.size()]));
    } finally {
      if (connection != null) connection.close();
    }
  }

  /**
   * Test cancelling at job after it is submiited and before its completion.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000L)
  public void testCancelJob() throws Exception {
    JPPFConnection connection = null;
    try {
      connection = JPPFHelper.getConnection();
      assertNotNull(connection);
      final JPPFJob job = BaseTestHelper.createJob("JCA testCancelJob", true, false, 1, LifeCycleTask.class, 5000L);
      final String id = connection.submit(job);
      assertNotNull(id);
      Thread.sleep(1000L);
      connection.cancelJob(id);
      final List<Task<?>> results = connection.awaitResults(id);
      assertNotNull(results);
      final int n = results.size();
      assertTrue("results size should be 1 but is " + n, n == 1);
      final Task<?> task = results.get(0);
      assertNotNull(task);
      assertNull(task.getThrowable());
      assertNull("task result should be null but is '" + task.getResult() + "'" , task.getResult());
    } finally {
      if (connection != null) connection.close();
    }
  }

  /**
   * Test cancelling at job after it is submiited and after its completion.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000L)
  public void testCancelJobAfterCompletion() throws Exception {
    JPPFConnection connection = null;
    try {
      connection = JPPFHelper.getConnection();
      assertNotNull(connection);
      final JPPFJob job = BaseTestHelper.createJob("JCA testCancelJobAfterCompletion", true, false, 1, LifeCycleTask.class, 100L);
      final String id = connection.submit(job);
      assertNotNull(id);
      Thread.sleep(3000L);
      connection.cancelJob(id);
      final List<Task<?>> results = connection.awaitResults(id);
      assertNotNull(results);
      final int n = results.size();
      assertTrue("results size should be 1 but is " + n, n == 1);
      final Task<?> task = results.get(0);
      assertNotNull(task);
      assertNull(task.getThrowable());
      assertNotNull(task.getResult());
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
    } finally {
      if (connection != null) connection.close();
    }
  }

  /**
   * Test submitting a simple job and getting the results.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000L)
  public void testJobResults() throws Exception {
    JPPFConnection connection = null;
    final int nbTasks = 1;
    String id = null;
    try {
      connection = JPPFHelper.getConnection();
      assertNotNull(connection);
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), true, false, nbTasks, LifeCycleTask.class, 100L);
      id = connection.submit(job);
      assertNotNull(id);
    } finally {
      if (connection != null) connection.close();
    }
    Thread.sleep(4000L);
    try {
      connection = JPPFHelper.getConnection();
      final List<Task<?>> results = connection.getResults(id);
      assertNotNull(results);
      final int n = results.size();
      assertEquals("results size should be " + nbTasks + " but is " + n, nbTasks, n);
      final int count = 0;
      for (Task<?> task: results) {
        assertNotNull("task" + count + " is null", task);
        final Throwable e = task.getThrowable();
        assertNull(task.getId() + " exception should be null but is '" + (e == null ? "" : ExceptionUtils.getMessage(e)) + "'", e);
        assertNotNull(task.getId() + " result is null", task.getResult());
        assertEquals(task.getResult(), BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE);
      }
    } finally {
      if (connection != null) connection.close();
    }
  }

  /**
   * Test submitting multiple jobs ad retrieving their uuids from a separate JCA connection.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000L)
  public void testGetAllJobIds() throws Exception {
    JPPFConnection connection = null;
    final String prefix = ReflectionUtils.getCurrentClassAndMethod() + " ";
    final int nbJobs = 2;
    final JPPFJob[] jobs = new JPPFJob[nbJobs];
    final String[] ids= new String[nbJobs];
    try {
      connection = JPPFHelper.getConnection();
      assertNotNull(connection);
      // remove existing jobs
      for (final String id: connection.getAllJobIds()) connection.getResults(id);
      for (int i=0; i<nbJobs; i++) {
        final JPPFJob job = new JPPFJob(prefix + i);
        job.add(new LifeCycleTask(1000L));
        jobs[i] = job;
        ids[i] = connection.submit(job);
        assertNotNull(ids[i]);
        assertEquals(prefix + i, ids[i]);
      }
    } finally {
      if (connection != null) connection.close();
    }
    Thread.sleep(500L);
    try {
      connection = JPPFHelper.getConnection();
      final Collection<String> coll = connection.getAllJobIds();
      assertNotNull(coll);
      assertEquals(nbJobs, coll.size());
      for (int i=0; i<nbJobs; i++) assertTrue(coll.contains(ids[i]));
      for (int i=0; i<nbJobs; i++) {
        final List<Task<?>> results = connection.awaitResults(ids[i]);
        assertNotNull(results);
      }
    } finally {
      if (connection != null) connection.close();
    }
  }
}
