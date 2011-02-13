/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

import java.util.Collection;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.ui.treetable.JPPFTreeTable;
import org.slf4j.*;

/**
 * This class manages updates to, and navigation within, the tree table
 * for the node data panel.
 * @author Laurent Cohen
 */
public class NodeDataPanelManager
{
	/**
	 * Logger for this class.
	 */
	static Logger log = LoggerFactory.getLogger(NodeDataPanelManager.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The container for the tree table.
	 */
	private NodeDataPanel panel;

	/**
	 * Initialize this manager.
	 * @param panel the container for the tree table.
	 */
	NodeDataPanelManager(NodeDataPanel panel)
	{
		this.panel = panel;
	}

	/**
	 * Called when the state information of a node has changed.
	 * @param driverName the name of the driver to which the node is attached.
	 * @param nodeName the name of the node to update.
	 */
	void nodeDataUpdated(String driverName, String nodeName)
	{
		final DefaultMutableTreeNode driverNode = findDriver(driverName);
		if (driverNode == null) return;
		final DefaultMutableTreeNode node = findNode(driverNode, nodeName);
		if (node != null) panel.getModel().changeNode(node);
	}

	/**
	 * Called to notify that a driver was added.
	 * @param connection a reference to the driver connection.
	 */
	void driverAdded(final JPPFClientConnection connection)
	{
		JMXDriverConnectionWrapper wrapper = ((JPPFClientConnectionImpl) connection).getJmxConnection();
		String driverName = wrapper.getId();
		int index = driverInsertIndex(driverName);
		if (index < 0) return;
		TopologyData driverData = new TopologyData(connection);
		DefaultMutableTreeNode driverNode = new DefaultMutableTreeNode(driverData);
		if (debugEnabled) log.debug("adding driver: " + driverName + " at index " + index);
		panel.getModel().insertNodeInto(driverNode, panel.getTreeTableRoot(), index);
		if (panel.getListenerMap().get(wrapper.getId()) == null)
		{
			ConnectionStatusListener listener = new ConnectionStatusListener(panel, wrapper.getId());
			connection.addClientConnectionStatusListener(listener);
			panel.getListenerMap().put(wrapper.getId(), listener);
		}
		Collection<JPPFManagementInfo> nodes = null;
		try
		{
			nodes = wrapper.nodesInformation();
		}
		catch(Exception e)
		{
			if (debugEnabled) log.debug(e.getMessage(), e);
			return;
		}
		if (nodes != null) for (JPPFManagementInfo nodeInfo: nodes) nodeAdded(driverNode, nodeInfo);
		if (panel.getTreeTable() != null) panel.getTreeTable().expand(driverNode);
		panel.updateStatusBar("/StatusNbServers", 1);
		repaintTreeTable();
	}

	/**
	 * Called to notify that a driver was removed.
	 * @param driverName the name of the driver to remove.
	 * @param removeNodesOnly true if only the nodes attached to the driver are to be removed.
	 */
	void driverRemoved(String driverName, boolean removeNodesOnly)
	{
		final DefaultMutableTreeNode driverNode = findDriver(driverName);
		if (debugEnabled) log.debug("removing driver: " + driverName);
		if (driverNode == null) return;
		if (removeNodesOnly)
		{
			for (int i=driverNode.getChildCount()-1; i>=0; i--)
			{
				DefaultMutableTreeNode node = (DefaultMutableTreeNode ) driverNode.getChildAt(i);
				panel.getModel().removeNodeFromParent(node);
			}
		}
		else
		{
			panel.getModel().removeNodeFromParent(driverNode);
			panel.updateStatusBar("/StatusNbServers", -1);
		}
		repaintTreeTable();
	}

	/**
	 * Called to notify that a node was added to a driver.
	 * @param driverName the name of the driver to which the node is added.
	 * @param nodeInfo the object that encapsulates the node addition.
	 */
	void nodeAdded(String driverName, JPPFManagementInfo nodeInfo)
	{
		final DefaultMutableTreeNode driverNode = findDriver(driverName);
		if (driverNode == null) return;
		nodeAdded(driverNode, nodeInfo);
	}

	/**
	 * Called to notify that a node was added to a driver.
	 * @param driverNode the driver to which the node is added.
	 * @param nodeInfo the object that encapsulates the node addition.
	 */
	void nodeAdded(DefaultMutableTreeNode driverNode, JPPFManagementInfo nodeInfo)
	{
		String nodeName = nodeInfo.getHost() + ":" + nodeInfo.getPort();
		int index = nodeInsertIndex(driverNode, nodeName);
		if (index < 0) return;
		if (debugEnabled) log.debug("adding node: " + nodeName + " at index " + index);
		TopologyData data = new TopologyData(nodeInfo);
		if (debugEnabled) log.debug("created TopologyData instance");
		DefaultMutableTreeNode nodeNode = new DefaultMutableTreeNode(data);
		panel.getModel().insertNodeInto(nodeNode, driverNode, index);
		if (nodeInfo.getType() == JPPFManagementInfo.NODE) panel.updateStatusBar("/StatusNbNodes", 1);

		for (int i=0; i<panel.getTreeTableRoot().getChildCount(); i++)
		{
			DefaultMutableTreeNode driverNode2 = (DefaultMutableTreeNode) panel.getTreeTableRoot().getChildAt(i);
			if (driverNode2 == driverNode) continue;
			DefaultMutableTreeNode nodeNode2 = findNode(driverNode2, nodeName);
			if (nodeNode2 != null) panel.getModel().removeNodeFromParent(nodeNode2);
		}
		repaintTreeTable();
	}

	/**
	 * Called to notify that a node was removed from a driver.
	 * @param driverName the name of the driver from which the node is removed.
	 * @param nodeName the name of the node to remove.
	 */
	void nodeRemoved(String driverName, String nodeName)
	{
		DefaultMutableTreeNode driverNode = findDriver(driverName);
		if (driverNode == null) return;
		final DefaultMutableTreeNode node = findNode(driverNode, nodeName);
		if (node == null) return;
		if (debugEnabled) log.debug("removing node: " + nodeName);
		panel.getModel().removeNodeFromParent(node);
		TopologyData data = (TopologyData) node.getUserObject();
		if ((data != null) && (data.getNodeInformation().getType() == JPPFManagementInfo.NODE)) panel.updateStatusBar("/StatusNbNodes", -1);
		repaintTreeTable();
	}

	/**
	 * Find the driver tree node with the specified driver name.
	 * @param driverName name of the dirver to find.
	 * @return a <code>DefaultMutableTreeNode</code> or null if the driver could not be found.
	 */
	DefaultMutableTreeNode findDriver(String driverName)
	{
		for (int i=0; i<panel.getTreeTableRoot().getChildCount(); i++)
		{
			DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) panel.getTreeTableRoot().getChildAt(i);
			TopologyData data = (TopologyData) driverNode.getUserObject();
			String name = data.getJmxWrapper().getId();
			if (name.equals(driverName)) return driverNode;
		}
		return null;
	}

	/**
	 * Find the node tree node with the specified driver name and node information.
	 * @param driverNode name the parent of the node to find.
	 * @param nodeName the name of the node to find.
	 * @return a <code>DefaultMutableTreeNode</code> or null if the driver could not be found.
	 */
	DefaultMutableTreeNode findNode(DefaultMutableTreeNode driverNode, String nodeName)
	{
		for (int i=0; i<driverNode.getChildCount(); i++)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) driverNode.getChildAt(i);
			TopologyData nodeData = (TopologyData) node.getUserObject();
			if (nodeName.equals(nodeData.getJmxWrapper().getId())) return node;
		}
		return null;
	}

	/**
	 * Find the position at which to insert a driver,
	 * using the sorted lexical order of driver names. 
	 * @param driverName the name of the driver to insert.
	 * @return the index at which to insert the driver, or -1 if the driver is already in the tree.
	 */
	int driverInsertIndex(String driverName)
	{
		int n = panel.getTreeTableRoot().getChildCount();
		for (int i=0; i<n; i++)
		{
			DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) panel.getTreeTableRoot().getChildAt(i);
			TopologyData data = (TopologyData) driverNode.getUserObject();
			String name = data.getJmxWrapper().getId();
			if (name.equals(driverName)) return -1;
			else if (driverName.compareTo(name) < 0) return i;
		}
		return n;
	}

	/**
	 * Find the position at which to insert a node, using the sorted lexical order of node names. 
	 * @param driverNode name the parent of the node to insert.
	 * @param nodeName the name of the node to insert.
	 * @return the index at which to insert the node, or -1 if the node is already in the tree.
	 */
	int nodeInsertIndex(DefaultMutableTreeNode driverNode, String nodeName)
	{
		int n = driverNode.getChildCount();
		for (int i=0; i<n; i++)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) driverNode.getChildAt(i);
			TopologyData nodeData = (TopologyData) node.getUserObject();
			String name = nodeData.getJmxWrapper().getId();
			if (nodeName.equals(name)) return -1;
			else if (nodeName.compareTo(name) < 0) return i;
		}
		return n;
	}

	/**
	 * Repaint the tree table area.
	 */
	void repaintTreeTable()
	{
		JPPFTreeTable treeTable = panel.getTreeTable();
		if (treeTable != null)
		{
			treeTable.invalidate();
			treeTable.repaint();
		}
	}
}
