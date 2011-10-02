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

import java.awt.Component;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.*;

import org.jppf.management.JMXConnectionWrapper;
import org.jppf.ui.treetable.*;

/**
 * Table cell renderer used to render the alignment of cell values in the table.
 * @author Laurent Cohen
 */
public class NodeTableCellRenderer extends DefaultTableCellRenderer
{
	/**
	 * The insets for this renderer.
	 */
	private Border border = BorderFactory.createEmptyBorder(0, 2, 0, 2);

	/**
	 * Returns the default table cell renderer.
	 * @param table the JTable to which this renderer applies. 
	 * @param value the value of the rendered cell.
	 * @param isSelected determines whether the cell is selected.
	 * @param hasFocus determines whether the cell has the focus.
	 * @param row the row of the rendered cell.
	 * @param column the column of the rendered cell.
	 * @return the default table cell renderer.
	 * @see javax.swing.table.DefaultTableCellRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
	 */
	@Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		DefaultTableCellRenderer renderer = (DefaultTableCellRenderer) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		if (column != 0)
		{
			int alignment = SwingConstants.LEFT;
			switch(column)
			{
				case NodeTreeTableModel.NB_TASKS:
					alignment = SwingConstants.RIGHT;
					break;
				case NodeTreeTableModel.NODE_THREADS:
					alignment = SwingConstants.CENTER;
					break;
				default:
					break;
			}
			JPPFTreeTable treeTable = (JPPFTreeTable) table;
			TreePath path = treeTable.getPathForRow(row);
			if (path != null)
			{
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
				//System.out.println("row="+row+", col="+column+", node="+node);
				Object o = node.getUserObject();
				if (o instanceof TopologyData)
				{
					TopologyData data = (TopologyData) o;
					if (TopologyDataType.NODE.equals(data.getType()))
					{
						JMXConnectionWrapper wrapper = data.getJmxWrapper();
						if ((wrapper == null) || !wrapper.isConnected()) renderer.setForeground(AbstractTreeCellRenderer.UNMANAGED_COLOR);
						else renderer.setForeground(
							isSelected ? AbstractTreeCellRenderer.DEFAULT_SELECTION_FOREGROUND : AbstractTreeCellRenderer.DEFAULT_FOREGROUND);
					}
				}
			}
			renderer.setHorizontalAlignment(alignment);
			renderer.setBorder(border);
		}
		return renderer;
	}
}
