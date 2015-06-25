/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package org.jppf.ui.monitoring.node;

import java.text.NumberFormat;

import javax.swing.tree.*;

import org.jppf.client.monitoring.topology.*;
import org.jppf.management.*;
import org.jppf.ui.treetable.AbstractJPPFTreeTableModel;

/**
 * Tree table model for the tree table.
 */
public class NodeTreeTableModel extends AbstractJPPFTreeTableModel {
  /**
   * Column number for the node's url.
   */
  static final int NODE_URL = 0;
  /**
   * Column number for the node's thread pool size.
   */
  static final int NODE_THREADS = 1;
  /**
   * Column number for the node's last event.
   */
  static final int NODE_STATUS = 2;
  /**
   * Column number for the node's last event.
   */
  static final int EXECUTION_STATUS = 3;
  /**
   * Column number for the node's number of tasks executed.
   */
  static final int NB_TASKS = 4;
  /**
   * Column number for the node's number of provisioned slaves.
   */
  static final int NB_SLAVES = 5;
  /**
   * Column number for the node's number of provisioned slaves.
   */
  static final int PENDING_ACTION = 6;
  /**
   * 
   */
  static NumberFormat nf = createNumberFormat();

  /**
   * Initialize this model with the specified tree.
   * @param node the root of the tree.
   */
  public NodeTreeTableModel(final TreeNode node) {
    super(node);
    BASE = "org.jppf.ui.i18n.NodeDataPage";
  }

  @Override
  public int getColumnCount() {
    return 7;
  }

  @Override
  public Object getValueAt(final Object node, final int column) {
    Object res = "";
    if (node instanceof DefaultMutableTreeNode) {
      DefaultMutableTreeNode defNode = (DefaultMutableTreeNode) node;
      if (defNode.getUserObject() instanceof AbstractTopologyComponent) {
        AbstractTopologyComponent info = (AbstractTopologyComponent) defNode.getUserObject();
        JPPFManagementInfo mgtInfo = info.getManagementInfo();
        boolean isNode = (mgtInfo != null) && mgtInfo.isNode();
        if (info.isDriver()) {
          return (column > 0) ? res : info.toString();
        }
        JPPFNodeState state = ((TopologyNode) info).getNodeState();
        if (state == null) return res;
        switch (column) {
          case NODE_URL:
            res = info.toString() + (isNode ? "" : "(peer driver)");
            break;
          case NODE_THREADS:
            if (isNode) {
              int n = state.getThreadPoolSize();
              int p = state.getThreadPriority();
              res = "" + (n <= 0 ? "?" : n) + " / " + (p <= 0 ? "?" : p);
            }
            break;
          case NODE_STATUS:
            if (isNode) res = state.getConnectionStatus();
            break;
          case EXECUTION_STATUS:
            if (isNode) res = state.getExecutionStatus();
            break;
          case NB_TASKS:
            if (isNode) res = nf.format(state.getNbTasksExecuted());
            break;
          case NB_SLAVES:
            if (isNode) {
              if ((mgtInfo != null) && mgtInfo.isMasterNode()) {
                int n = ((TopologyNode) info).getNbSlaveNodes();
                res = n >= 0 ? nf.format(n) : "";
              } else res = "";
            }
            break;
          case PENDING_ACTION:
            if (isNode) res = ((TopologyNode) info).getPendingAction();
            if (res == null) res = "None";
            break;
        }
      } else {
        if (column == 0) res = defNode.getUserObject().toString();
      }
    }
    return res;
  }

  @Override
  protected String getBaseColumnName(final int column) {
    switch (column) {
      case NODE_URL:
        return "column.node.url";
      case NODE_THREADS:
        return "column.node.threads";
      case NODE_STATUS:
        return "column.node.status";
      case EXECUTION_STATUS:
        return "column.node.execution.status";
      case NB_TASKS:
        return "column.nb.tasks";
      case NB_SLAVES:
        return "column.nb.slaves";
      case PENDING_ACTION:
        return "column.pending";
    }
    return "";
  }

  /**
   * Get a number formatter for the number of tasks for each node.
   * @return a <code>NumberFormat</code> instance.
   */
  private static NumberFormat createNumberFormat() {
    NumberFormat nf = NumberFormat.getIntegerInstance();
    nf.setGroupingUsed(true);
    return nf;
  }
}
