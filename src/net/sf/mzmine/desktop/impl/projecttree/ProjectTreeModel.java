/*
 * Copyright 2006-2011 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.desktop.impl.projecttree;

import java.util.Enumeration;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import net.sf.mzmine.data.MassList;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.desktop.impl.projecttree.ProjectTreeNode;
import net.sf.mzmine.project.MZmineProject;

/**
 * Project tree model implementation
 */
public class ProjectTreeModel extends DefaultTreeModel {

	public static final String dataFilesNodeName = "Raw data files";
	public static final String peakListsNodeName = "Peak lists";

	private final ProjectTreeNode dataFilesNode = new ProjectTreeNode(
			dataFilesNodeName);

	private final ProjectTreeNode peakListsNode = new ProjectTreeNode(
			peakListsNodeName);

	private DefaultMutableTreeNode rootNode;

	public ProjectTreeModel(MZmineProject project) {

		super(new DefaultMutableTreeNode(project));

		rootNode = (DefaultMutableTreeNode) super.getRoot();

		insertNodeInto(dataFilesNode, rootNode, 0);
		insertNodeInto(peakListsNode, rootNode, 1);

	}

	public synchronized void addObject(Object object) {

		if (object instanceof PeakList) {
			PeakList peakList = (PeakList) object;
			int childCount = getChildCount(peakListsNode);
			DefaultMutableTreeNode newPeakListNode = new DefaultMutableTreeNode(
					peakList);
			insertNodeInto(newPeakListNode, peakListsNode, childCount);

			PeakListRow rows[] = peakList.getRows();
			for (int i = 0; i < rows.length; i++) {
				MutableTreeNode rowNode = new DefaultMutableTreeNode(rows[i]);
				insertNodeInto(rowNode, newPeakListNode, i);
			}

		}

		if (object instanceof RawDataFile) {
			RawDataFile dataFile = (RawDataFile) object;
			int childCount = getChildCount(dataFilesNode);
			DefaultMutableTreeNode newDataFileNode = new DefaultMutableTreeNode(
					dataFile);
			insertNodeInto(newDataFileNode, dataFilesNode, childCount);

			int scanNumbers[] = dataFile.getScanNumbers();
			for (int i = 0; i < scanNumbers.length; i++) {
				Scan scan = dataFile.getScan(scanNumbers[i]);
				MutableTreeNode scanNode = new DefaultMutableTreeNode(scan);
				insertNodeInto(scanNode, newDataFileNode, i);

				MassList massLists[] = scan.getMassLists();
				for (MassList massList : massLists)
					addObject(massList);
			}
			return;
		}

		if (object instanceof MassList) {
			Scan scan = ((MassList) object).getScan();
			ProjectTreeNode root = dataFilesNode;
			Enumeration nodes = root.breadthFirstEnumeration();
			while (nodes.hasMoreElements()) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes
						.nextElement();

				if (node.getUserObject() == scan) {
					DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(
							object);
					int index = node.getChildCount();
					insertNodeInto(newNode, node, index);
					break;
				}
			}
			return;
		}

	}

	public synchronized void removeObject(Object object) {

		Enumeration nodes = rootNode.breadthFirstEnumeration();
		while (nodes.hasMoreElements()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes
					.nextElement();
			if (node.getUserObject() == object) {
				removeNodeFromParent(node);
				return;
			}
		}

	}

	public synchronized PeakList[] getPeakLists() {
		int childrenCount = getChildCount(peakListsNode);
		PeakList result[] = new PeakList[childrenCount];
		for (int j = 0; j < childrenCount; j++) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) getChild(
					peakListsNode, j);
			result[j] = (PeakList) child.getUserObject();
		}
		return result;
	}

	public synchronized RawDataFile[] getDataFiles() {
		int childrenCount = getChildCount(dataFilesNode);
		RawDataFile result[] = new RawDataFile[childrenCount];
		for (int j = 0; j < childrenCount; j++) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) getChild(
					dataFilesNode, j);
			result[j] = (RawDataFile) child.getUserObject();
		}
		return result;
	}

	public void valueForPathChanged(TreePath path, Object value) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) path
				.getLastPathComponent();
		Object object = node.getUserObject();
		String newName = (String) value;
		if (object instanceof RawDataFile) {
			RawDataFile df = (RawDataFile) object;
			df.setName(newName);
		}
		if (object instanceof PeakList) {
			PeakList pl = (PeakList) object;
			pl.setName(newName);
		}
	}

	public void notifyObjectChanged(Object object, boolean structureChanged) {
		Enumeration nodes = rootNode.breadthFirstEnumeration();
		while (nodes.hasMoreElements()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes
					.nextElement();
			if (node.getUserObject() == object) {
				if (structureChanged)
					nodeStructureChanged(node);
				else
					nodeChanged(node);
				return;
			}
		}

	}

	public DefaultMutableTreeNode getRoot() {
		return rootNode;
	}

}
