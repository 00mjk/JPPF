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

package org.jppf.admin.web.jobs.priority;

import java.util.*;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.*;
import org.jppf.admin.web.jobs.JobsConstants;
import org.jppf.admin.web.tabletree.TableTreeData;
import org.jppf.admin.web.utils.AbstractModalLink;
import org.jppf.client.monitoring.jobs.*;
import org.jppf.client.monitoring.topology.TopologyDriver;
import org.jppf.job.*;
import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class PriorityLink extends AbstractModalLink<PriorityForm> {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(PriorityLink.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = log.isDebugEnabled();

  /**
   * @param form .
   */
  public PriorityLink(final Form<String> form) {
    super(JobsConstants.UPDATE_PRIORITY_ACTION, Model.of("Priority"), "priority.gif", PriorityPage.class, form);
    modal.setInitialWidth(350);
    modal.setInitialHeight(150);
  }

  @Override
  protected PriorityForm createForm() {
    return new PriorityForm(modal, () -> doOK());
  }
  
  /**
   * Called when the ok button is clicked.
   */
  private void doOK() {
    final JPPFWebSession session = (JPPFWebSession) getPage().getSession();
    final TableTreeData data = session.getJobsData();
    final List<DefaultMutableTreeNode> selectedNodes = data.getSelectedTreeNodes();
    final CollectionMap<TopologyDriver, String> map = new ArrayListHashMap<>();
    for (final DefaultMutableTreeNode treeNode: selectedNodes) {
      final AbstractJobComponent comp = (AbstractJobComponent) treeNode.getUserObject();
      if ((comp instanceof Job) && (comp.getParent() != null)) {
        final Job job = (Job) comp;
        final List<JobDriver> drivers = JPPFWebConsoleApplication.get().getJobMonitor().getDriversForJob(job.getUuid());
        for (final JobDriver driver: drivers) map.putValue(driver.getTopologyDriver(), job.getUuid());
      }
    }
    final int priority = modalForm.getPriority();
    for (final Map.Entry<TopologyDriver, Collection<String>> entry: map.entrySet()) {
      final TopologyDriver driver = entry.getKey();
      final JobSelector selector = new JobUuidSelector(entry.getValue());
      try {
        driver.getJobManager().updatePriority(selector, priority);
      } catch(final Exception e) {
        log.error(e.getMessage(), e);
      }
    }
  }
}
