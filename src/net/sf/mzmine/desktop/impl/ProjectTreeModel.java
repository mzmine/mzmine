/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.desktop.impl;

import java.util.Vector;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.project.ProjectEvent;
import net.sf.mzmine.project.ProjectListener;

class ProjectTreeModel implements TreeModel, ProjectListener {

	static final String dataFilesItem = "Raw data files",
			peakListsItem = "Peak lists";

	private JTree projectTree;
	private Vector<TreeModelListener> listeners;

	ProjectTreeModel(JTree projectTree) {
		this.projectTree = projectTree;
		listeners = new Vector<TreeModelListener>();
		MZmineCore.getProjectManager().addProjectListener(this);
	}

	public Object getChild(Object parent, int num) {
		if (parent instanceof MZmineProject) {
			if (num == 0)
				return dataFilesItem;
			else
				return peakListsItem;
		}
		if (parent == dataFilesItem) {
			return MZmineCore.getCurrentProject().getDataFiles()[num];
		}
		if (parent == peakListsItem) {
			return MZmineCore.getCurrentProject().getPeakLists()[num];
		}
		if (parent instanceof PeakList) {
			return ((PeakList) parent).getRow(num);
		}
		if (parent instanceof RawDataFile) {
			int scanNumbers[] = ((RawDataFile) parent).getScanNumbers();
			return ((RawDataFile) parent).getScan(scanNumbers[num]);
		}
		throw (new IllegalArgumentException("Unknown parent " + parent));
	}

	public int getChildCount(Object parent) {

		if (parent instanceof MZmineProject)
			return 2;
		if (parent == dataFilesItem) {
			return MZmineCore.getCurrentProject().getDataFiles().length;
		}
		if (parent == peakListsItem) {
			return MZmineCore.getCurrentProject().getPeakLists().length;
		}
		if (parent instanceof PeakList) {
			return ((PeakList) parent).getNumberOfRows();
		}
		if (parent instanceof RawDataFile) {
			return ((RawDataFile) parent).getNumOfScans();
		}

		throw (new IllegalArgumentException("Unknown parent " + parent));
	}

	public int getIndexOfChild(Object parent, Object child) {
		if (parent instanceof MZmineProject) {
			if (child == dataFilesItem)
				return 0;
			else
				return 1;
		}
		if (parent == dataFilesItem) {
			RawDataFile dataFiles[] = MZmineCore.getCurrentProject()
					.getDataFiles();
			for (int i = 0; i < dataFiles.length; i++)
				if (dataFiles[i] == child)
					return i;
		}
		if (parent == peakListsItem) {
			PeakList peakLists[] = MZmineCore.getCurrentProject()
					.getPeakLists();
			for (int i = 0; i < peakLists.length; i++)
				if (peakLists[i] == child)
					return i;
		}
		if (parent instanceof PeakList) {
			PeakListRow rows[] = ((PeakList) parent).getRows();
			for (int i = 0; i < rows.length; i++)
				if (rows[i] == child)
					return i;
		}
		if (parent instanceof RawDataFile) {
			int scanNumbers[] = ((RawDataFile) parent).getScanNumbers();
			int num = ((Scan) child).getScanNumber();
			for (int i = 0; i < scanNumbers.length; i++)
				if (scanNumbers[i] == num)
					return i;
		}
		throw (new IllegalArgumentException("Unknown parent " + parent));
	}

	public Object getRoot() {
		return MZmineCore.getCurrentProject();
	}

	public boolean isLeaf(Object element) {
		return ((element instanceof PeakListRow) || (element instanceof Scan));
	}

	public void addTreeModelListener(TreeModelListener l) {
		listeners.add(l);
	}

	public void removeTreeModelListener(TreeModelListener l) {
		listeners.remove(l);
	}

	public void valueForPathChanged(TreePath path, Object value) {
		Object object = path.getLastPathComponent();
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

	/**
	 * ProjectListner implementation
	 */
	public void projectModified(final ProjectEvent event) {

		// We have to notify listeners in the Swing event thread, to avoid
		// thread conflicts when re-painting
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (event == ProjectEvent.ALL_CHANGED) {
					for (TreeModelListener l : listeners) {
						l.treeStructureChanged(new TreeModelEvent(this,
								new Object[] { getRoot() }));
					}
					projectTree.expandPath(new TreePath(new Object[] {
							getRoot(), ProjectTreeModel.dataFilesItem }));
					projectTree.expandPath(new TreePath(new Object[] {
							getRoot(), ProjectTreeModel.peakListsItem }));
				}

				if (event == ProjectEvent.PROJECT_NAME_CHANGED) {
					for (TreeModelListener l : listeners) {
						l.treeNodesChanged(new TreeModelEvent(this,
								new Object[] { getRoot() }));
					}
				}

				if ((event == ProjectEvent.DATAFILE_ADDED)
						|| (event == ProjectEvent.DATAFILE_REMOVED)) {
					for (TreeModelListener l : listeners) {
						l.treeStructureChanged(new TreeModelEvent(this,
								new Object[] { getRoot(), dataFilesItem }));
					}
				}

				if ((event == ProjectEvent.PEAKLIST_ADDED)
						|| (event == ProjectEvent.PEAKLIST_REMOVED)) {
					for (TreeModelListener l : listeners) {
						l.treeStructureChanged(new TreeModelEvent(this,
								new Object[] { getRoot(), peakListsItem }));
					}
				}

				if (event == ProjectEvent.PEAKLIST_CONTENTS_CHANGED) {
					PeakList peakLists[] = MZmineCore.getCurrentProject()
							.getPeakLists();
					for (PeakList peakList : peakLists) {
						for (TreeModelListener l : listeners) {
							l.treeStructureChanged(new TreeModelEvent(this,
									new Object[] { getRoot(), peakListsItem,
											peakList }));
						}
					}
				}

			}
		});

	}

}
