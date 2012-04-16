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
package sample.dist.tasklength;

import java.util.*;
import java.util.concurrent.Future;

import org.jppf.client.*;
import org.jppf.client.concurrent.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;
import org.slf4j.*;


/**
 * Runner class for the &quot;Long Task&quot; demo.
 * @author Laurent Cohen
 */
public class LongTaskRunner
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(LongTaskRunner.class);
  /**
   * JPPF client used to submit execution requests.
   */
  private static JPPFClient jppfClient = null;
  /**
   * JMX connection to the driver.
   */
  private static JMXDriverConnectionWrapper jmx = null;
  /**
   * Entry point for this class, submits the tasks with a set duration to the server.
   * @param args not used.
   */
  public static void main(final String...args)
  {
    try
    {
      jppfClient = new JPPFClient("client");
      TypedProperties props = JPPFConfiguration.getProperties();
      int length = props.getInt("longtask.length");
      int nbTask = props.getInt("longtask.number");
      int iterations = props.getInt("longtask.iterations");
      print("Running Long Task demo with " + nbTask + " tasks of length = " + length + " ms for " + iterations + " iterations");
      perform(nbTask, length, iterations);
      //perform3(nbTask, length, iterations);
      //perform4();
      //perform5();
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      if (jppfClient != null) jppfClient.close();
    }
  }

  /**
   * Perform the test using <code>JPPFClient.submit(JPPFJob)</code> to submit the tasks.
   * @param nbTasks the number of tasks to send at each iteration.
   * @param length the executionlength of each task.
   * @param iterations the number of times the the tasks will be sent.
   * @throws Exception if an error is raised during the execution.
   */
  private static void perform(final int nbTasks, final int length, final int iterations) throws Exception
  {
    long totalTime = 0L;
    for (int iter=1; iter<=iterations; iter++)
    {
      long start = System.nanoTime();
      JPPFJob job = new JPPFJob("job_" + iter);
      //job.setName("Long task iteration " + iter);
      job.setName("iter " + iter);
      for (int i=0; i<nbTasks; i++)
      {
        LongTask task = new LongTask(length, false);
        task.setId("" + (iter+1) + ':' + (i+1));
        job.addTask(task);
      }
      List<JPPFTask> results = jppfClient.submit(job);
      for (JPPFTask task: results)
      {
        Exception e = task.getException();
        if (e != null) throw e;
      }
      long elapsed = System.nanoTime() - start;
      print("Iteration #" + iter+" performed in " + StringUtils.toStringDuration(elapsed/1000000L));
      totalTime += elapsed;
    }
    print("Average iteration time: " + StringUtils.toStringDuration(totalTime/(iterations*1000000L)));
    /*
    JPPFStats stats = ((JPPFClientConnectionImpl) jppfClient.getClientConnection()).getJmxConnection().statistics();
    print("End statistics :\n"+stats.toString());
    */
  }

  /**
   * Perform the test using <code>JPPFExecutorService.submit()</code> to submit the tasks individually.
   * @param nbTasks the number of tasks to send at each iteration.
   * @param length the executionlength of each task.
   * @param iterations the number of times the the tasks will be sent.
   * @throws Exception if an error is raised during the execution.
   */
  private static void perform2(final int nbTasks, final int length, final int iterations) throws Exception
  {

    JPPFExecutorService executor = new JPPFExecutorService(jppfClient);
    //executor.setBatchSize(50);
    //executor.setBatchTimeout(1000L);

    long totalTime = System.currentTimeMillis();
    List<Future<?>> futureList = new ArrayList<Future<?>>();
    for (int i=0; i<nbTasks; i++)
    {
      LongTask task = new LongTask(length, false);
      task.setId("" + (i+1));
      futureList.add(executor.submit(task));
    }
    for (Future<?> f: futureList)
    {
      f.get();
      JPPFTask t = ((JPPFTaskFuture<?>) f).getTask();
      if (t.getException() != null) System.out.println("task error: " +  t.getException().getMessage());
      else System.out.println("task result: " + t.getResult());
    }
    totalTime = System.currentTimeMillis() - totalTime;
    print("Computation time: " + StringUtils.toStringDuration(totalTime));
    executor.shutdownNow();
  }

  /**
   * Perform the test using <code>JPPFExecutorService.invokeAll()</code> to submit the tasks.
   * @param nbTasks the number of tasks to send at each iteration.
   * @param length the executionlength of each task.
   * @param iterations the number of times the the tasks will be sent.
   * @throws Exception if an error is raised during the execution.
   */
  private static void perform3(final int nbTasks, final int length, final int iterations) throws Exception
  {

    JPPFExecutorService executor = new JPPFExecutorService(jppfClient);
    //executor.setBatchSize(50);
    //executor.setBatchTimeout(1000L);
    long totalTime = 0L;
    for (int iter=0; iter<iterations; iter++)
    {
      long iterTime = System.currentTimeMillis();
      List<JPPFTaskCallable> tasks = new ArrayList<JPPFTaskCallable>();
      for (int i=0; i<nbTasks; i++)
      {
        LongTask task = new LongTask(length, false);
        task.setId("" + (i+1));
        tasks.add(new JPPFTaskCallable(task));
      }
      List<Future<Object>> futureList = executor.invokeAll(tasks);
      for (Future<?> f: futureList)
      {
        f.get();
        JPPFTask t = ((JPPFTaskFuture<?>) f).getTask();
        if (t.getException() != null) System.out.println("task error: " +  t.getException().getMessage());
        else System.out.println("task result: " + t.getResult());
      }
      iterTime = System.currentTimeMillis() - iterTime;
      print("Computation time for iteration " + (iter+1) + ": " + StringUtils.toStringDuration(iterTime));
      totalTime += iterTime;
    }
    print("Average computation time per iteration: " + StringUtils.toStringDuration(totalTime/iterations));
    executor.shutdownNow();
  }

  /**
   * Print a message tot he log and to the console.
   * @param msg the message to print.
   */
  private static void print(final String msg)
  {
    log.info(msg);
    System.out.println(msg);
  }

  /**
   * A <code>Callable</code> wrapper around a <code>JPPFTask</code>.
   */
  public static class JPPFTaskCallable extends JPPFTask implements JPPFCallable<Object>
  {
    /**
     * The task to run.
     */
    private JPPFTask task = null;

    /**
     * Initialize this callable with the specified jppf task.
     * @param task a <code>JPPFTask</code> instance.
     */
    public JPPFTaskCallable(final JPPFTask task)
    {
      this.task = task;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run()
    {
      task.run();
      setResult(task.getResult());
      setException(task.getException());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object call() throws Exception
    {
      run();
      return getResult();
    }
  }

  /**
   * This test submits a job and suspends, then resumes it using the management APIs.
   * @throws Exception if an error is raised during the execution.
   */
  private static void perform4() throws Exception
  {
    //System.out.println("\n********** job suspend/resume test **********");
    print("\n********** driver restart test **********");
    print("getting the jmx connection");
    long start = System.currentTimeMillis();
    getJmxConnection();
    long elapsed = System.currentTimeMillis() - start;
    print("got it in " + elapsed + " ms");
    start = System.currentTimeMillis();
    JPPFJob job = new JPPFJob();
    job.setName("Long task job");
    LongTask task = new LongTask(10000L, false);
    task.setId("1");
    job.addTask(task);
    JPPFResultCollector collector = new JPPFResultCollector(job);
    job.setResultListener(collector);
    job.setBlocking(false);
    jppfClient.submit(job);
    /*
		DriverJobManagementMBean jobManager = getJobManagement();
		Thread.sleep(3000L);
		System.out.println("suspending the job");
		jobManager.suspendJob(job.getJobUuid(), true);
		System.out.println("job suspended ... resuming in 3 seconds");
		Thread.sleep(3000L);
		System.out.println("resuming the job");
		jobManager.resumeJob(job.getJobUuid());
     */
    Thread.sleep(2000L);
    print("restarting the driver");
    restartDriver();
    List<JPPFTask> results = collector.waitForResults();
    for (JPPFTask t: results)
    {
      Exception e = task.getException();
      if (e != null) throw e;
      else print("result for task " + t.getId() + " : " + t.getResult());
    }
    elapsed = System.currentTimeMillis() - start;
    print("Iteration performed in " + StringUtils.toStringDuration(elapsed));
  }

  /**
   * This test submits a job and suspends, then resumes it using the management APIs.
   * @throws Exception if an error is raised during the execution.
   */
  private static void perform5() throws Exception
  {
    System.out.println("\n********** job cancel test **********");
    long start = System.currentTimeMillis();
    JPPFJob job = new JPPFJob();
    job.setName("Long task job 1");
    LongTask task = new LongTask(6000L, false);
    task.setId("1");
    job.addTask(task);
    JPPFResultCollector collector = new JPPFResultCollector(job);
    job.setResultListener(collector);
    job.setBlocking(false);
    jppfClient.submit(job);
    /*
     */
    DriverJobManagementMBean jobManager = getJobManagement();
    Thread.sleep(3000L);
    System.out.println("cancelling the first job");
    jobManager.cancelJob(job.getUuid());
    List<JPPFTask> results = collector.waitForResults();
    System.out.println("job cancelled");
    job.setName("Long task job 2");
    collector = new JPPFResultCollector(job);
    job.setResultListener(collector);
    System.out.println("submitting the second job");
    jppfClient.submit(job);
    results = collector.waitForResults();
    for (JPPFTask t: results)
    {
      Exception e = task.getException();
      if (e != null) throw e;
      else System.out.println("result for task " + t.getId() + " : " + t.getResult());
    }
    long elapsed = System.currentTimeMillis() - start;
    print("Iteration performed in "+StringUtils.toStringDuration(elapsed));
  }

  /**
   * Get a proxy to the driver's job management MBean.
   * @return an instance of {@link DriverJobManagementMBean}.
   * @throws Exception if any error occurs.
   */
  private static DriverJobManagementMBean getJobManagement() throws Exception
  {
    return getJmxConnection().getProxy(DriverJobManagementMBean.MBEAN_NAME, DriverJobManagementMBean.class);
  }

  /**
   * Get the jmx connection to the driver.
   * @return a {@link JMXDriverConnectionWrapper} instance.
   */
  private static JMXDriverConnectionWrapper getJmxConnection()
  {
    if (jmx == null)
    {
      JPPFClientConnectionImpl c = (JPPFClientConnectionImpl) jppfClient.getClientConnection();
      jmx = c.getJmxConnection();
      boolean b = jmx.isConnected();
      if (!b) jmx.connect();
    }
    return jmx;
  }

  /**
   * Restart the driver.
   * @throws Exception if any error occurs.
   */
  private static void restartDriver() throws Exception
  {
    Runnable r = new Runnable()
    {
      @Override
      public void run()
      {
        try
        {
          String s = getJmxConnection().restartShutdown(100L, 2000L);
          System.out.println("response for restart: " + s);
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    };
    //new Thread(r).start();
    r.run();
  }
}
