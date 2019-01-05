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

import java.util.List;

import org.jppf.nio.NioHelper;
import org.jppf.server.JPPFDriver;
import org.jppf.server.submission.SubmissionStatus;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * Listener for handling completed bundles.
 */
public class BundleCompletionListener implements ServerTaskBundleClient.CompletionListener {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AbstractServerJobBase.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The job to handle.
   */
  final AbstractServerJobBase serverJob;
  /**
   * Reference to the driver.
   */
  final JPPFDriver driver;

  /**
   * 
   * @param driver reference to the JPPF driver.
   * @param serverJob the job to handle.
   */
  BundleCompletionListener(final JPPFDriver driver, final AbstractServerJobBase serverJob) {
    this.driver = driver;
    this.serverJob = serverJob;
  }

  @Override
  public void taskCompleted(final ServerTaskBundleClient bundle, final List<ServerTask> results) {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");
  }

  @Override
  public void bundleEnded(final ServerTaskBundleClient bundle) {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");
    final Runnable r = () -> {
      if (debugEnabled) log.debug("bundle ended: {}", bundle);
      SubmissionStatus newStatus = null;
      serverJob.lock.lock();
      try {
        bundle.removeCompletionListener(BundleCompletionListener.this);
        serverJob.clientBundles.remove(bundle);
        serverJob.tasks.removeAll(bundle.getTaskList());
        if (serverJob.completionBundles != null) serverJob.completionBundles.remove(bundle);
        if (serverJob.clientBundles.isEmpty() && serverJob.tasks.isEmpty()) newStatus = SubmissionStatus.ENDED;
      } catch(final Exception e) {
        if (debugEnabled) log.debug(e.getMessage(), e);
      } finally {
        serverJob.lock.unlock();
      }
      if (newStatus != null) serverJob.setSubmissionStatus(newStatus);
    };
    NioHelper.getGlobalexecutor().execute(r);
  }
}