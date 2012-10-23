/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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
import java.util.Hashtable;

import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import net.sf.mzmine.data.MassList;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
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

	private Hashtable<Object, DefaultMutableTreeNode> treeObjects = new Hashtable<Object, DefaultMutableTreeNode>();

	private DefaultMutableTreeNode rootNode;

	public ProjectTreeModel(MZmineProject project) {

		super(new DefaultMutableTreeNode(project));

		rootNode = (DefaultMutableTreeNode) super.getRoot();

		insertNodeInto(dataFilesNode, rootNode, 0);
		insertNodeInto(peakListsNode, rootNode, 1);

	}

	/**
	 * This method must be called from Swing thread
	 */
	public void addObject(final Object object) {

	    	assert object != null;
	    	
		if (!SwingUtilities.isEventDispatchThread()) {
			throw new IllegalStateException(
					"This method must be called from Swing thread");
		}

		// Create new node
		final DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(
				object);
		
		treeObjects.put(object, newNode);

		if (object instanceof PeakList) {
			int childCount = getChildCount(peakListsNode);
			insertNodeInto(newNode, peakListsNode, childCount);
			final PeakList peakList = (PeakList) object;
			PeakListRow rows[] = peakList.getRows();
			for (int i = 0; i < rows.length; i++) {
				DefaultMutableTreeNode rowNode = new DefaultMutableTreeNode(
						rows[i]);
				treeObjects.put(rows[i], rowNode);
				insertNodeInto(rowNode, newNode, i);
			}
		}

		if (object instanceof RawDataFile) {

			int childCount = getChildCount(dataFilesNode);
			insertNodeInto(newNode, dataFilesNode, childCount);
			RawDataFile dataFile = (RawDataFile) object;
			int scanNumbers[] = dataFile.getScanNumbers();
			for (int i = 0; i < scanNumbers.length; i++) {
				Scan scan = dataFile.getScan(scanNumbers[i]);
				DefaultMutableTreeNode scanNode = new DefaultMutableTreeNode(
						scan);
				treeObjects.put(scan, scanNode);
				insertNodeInto(scanNode, newNode, i);

				MassList massLists[] = scan.getMassLists();
				for (int j = 0; j < massLists.length; j++) {
					DefaultMutableTreeNode mlNode = new DefaultMutableTreeNode(
							massLists[j]);
					treeObjects.put(massLists[j], mlNode);
					insertNodeInto(mlNode, scanNode, j);

				}
			}

		}

		if (object instanceof MassList) {
			Scan scan = ((MassList) object).getScan();

			final DefaultMutableTreeNode scNode = treeObjects.get(scan);
			assert scNode != null;

			int index = scNode.getChildCount();
			insertNodeInto(newNode, scNode, index);
		}

	}

	/**
	 * This method must be called from Swing thread
	 */
	public void removeObject(final Object object) {

		if (!SwingUtilities.isEventDispatchThread()) {
			throw new IllegalStateException(
					"This method must be called from Swing thread");
		}

		final DefaultMutableTreeNode node = treeObjects.get(object);

		assert node != null;

		// Remove all children from treeObjects
		Enumeration e = node.depthFirstEnumeration();
		while (e.hasMoreElements()) {
			DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) e
					.nextElement();
			Object nodeObject = childNode.getUserObject();
			treeObjects.remove(nodeObject);
		}

		// Remove the node from the tree, that also remove child
		// nodes
		removeNodeFromParent(node);

		// Remove the node object from treeObjects
		treeObjects.remove(object);

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
		if (rootNode.getUserObject() == object) {
			if (structureChanged)
				nodeStructureChanged(rootNode);
			else
				nodeChanged(rootNode);
			return;
		}
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
