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

package net.sf.mzmine.desktop.impl.projecttree;

import java.util.Vector;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.project.ProjectEvent;
import net.sf.mzmine.project.ProjectListener;

public class ProjectTreeModel implements TreeModel, ProjectListener {

	static final String dataFilesItem = "Raw data files",
			peakListsItem = "Peak lists";

	private Vector<TreeModelListener> listeners = new Vector<TreeModelListener>();

	private ProjectTree projectTree;

	ProjectTreeModel(ProjectTree projectTree) {
		this.projectTree = projectTree;
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
			RawDataFile dataFiles[] = MZmineCore.getCurrentProject()
					.getDataFiles();
			// We have to check the size of the array, because the item may have
			// been removed by another thread between calling getChildCount()
			// and getChild()
			if (num > dataFiles.length - 1)
				return "";
			return dataFiles[num];
		}
		if (parent == peakListsItem) {
			PeakList peakLists[] = MZmineCore.getCurrentProject()
					.getPeakLists();
			// Check size (see above)
			if (num > peakLists.length - 1)
				return "";
			return peakLists[num];
		}
		if (parent instanceof PeakList) {
			return ((PeakList) parent).getRow(num);
		}
		if (parent instanceof RawDataFile) {
			RawDataFile parentFile = (RawDataFile) parent;
			int scanNumbers[] = parentFile.getScanNumbers();
			// Check size (see above)
			if (num > scanNumbers.length - 1)
				return "";
			return parentFile.getScan(scanNumbers[num]);
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
	 * ProjectListener implementation - we have to reflect the changes of the
	 * project by updating the tree model.
	 */
	public void projectModified(final ProjectEvent projectEvent) {

		TreeModelEvent treeEvent;
		TreePath modifiedPath;

		// Create a new tree event depending on the type of project event
		switch (projectEvent.getType()) {

		case ALL_CHANGED:
			modifiedPath = new TreePath(getRoot());
			treeEvent = new TreeModelEvent(this, modifiedPath);
			for (TreeModelListener l : listeners) {
				l.treeStructureChanged(treeEvent);
			}
			projectTree.expandPath(new TreePath(new Object[] { getRoot(),
					ProjectTreeModel.dataFilesItem }));
			projectTree.expandPath(new TreePath(new Object[] { getRoot(),
					ProjectTreeModel.peakListsItem }));

			break;

		case PROJECT_NAME_CHANGED:
			modifiedPath = new TreePath(getRoot());
			treeEvent = new TreeModelEvent(this, modifiedPath);
			for (TreeModelListener l : listeners) {
				l.treeNodesChanged(treeEvent);
			}
			break;

		case DATAFILE_ADDED:
			modifiedPath = new TreePath(new Object[] { getRoot(),
					ProjectTreeModel.dataFilesItem });
			treeEvent = new TreeModelEvent(this, modifiedPath,
					new int[] { projectEvent.getIndex() },
					new Object[] { projectEvent.getDataFile() });
			for (TreeModelListener l : listeners) {
				l.treeNodesInserted(treeEvent);
			}
			break;

		case DATAFILE_REMOVED:
			modifiedPath = new TreePath(new Object[] { getRoot(),
					ProjectTreeModel.dataFilesItem });
			treeEvent = new TreeModelEvent(this, modifiedPath,
					new int[] { projectEvent.getIndex() },
					new Object[] { projectEvent.getDataFile() });
			for (TreeModelListener l : listeners) {
				l.treeNodesRemoved(treeEvent);
			}
			break;

		case DATAFILES_REORDERED:
			modifiedPath = new TreePath(new Object[] { getRoot(),
					ProjectTreeModel.dataFilesItem });
			treeEvent = new TreeModelEvent(this, modifiedPath);
			for (TreeModelListener l : listeners) {
				l.treeStructureChanged(treeEvent);
			}
			break;

		case PEAKLIST_ADDED:
			modifiedPath = new TreePath(new Object[] { getRoot(),
					ProjectTreeModel.peakListsItem });
			treeEvent = new TreeModelEvent(this, modifiedPath,
					new int[] { projectEvent.getIndex() },
					new Object[] { projectEvent.getPeakList() });

			for (TreeModelListener l : listeners) {
				l.treeNodesInserted(treeEvent);
			}
			break;

		case PEAKLIST_REMOVED:
			modifiedPath = new TreePath(new Object[] { getRoot(),
					ProjectTreeModel.peakListsItem });
			treeEvent = new TreeModelEvent(this, modifiedPath,
					new int[] { projectEvent.getIndex() },
					new Object[] { projectEvent.getPeakList() });

			for (TreeModelListener l : listeners) {
				l.treeNodesRemoved(treeEvent);
			}
			break;

		case PEAKLISTS_REORDERED:
			modifiedPath = new TreePath(new Object[] { getRoot(),
					ProjectTreeModel.peakListsItem });
			treeEvent = new TreeModelEvent(this, modifiedPath);
			for (TreeModelListener l : listeners) {
				l.treeStructureChanged(treeEvent);
			}
			break;

		case PEAKLIST_CONTENTS_CHANGED:
			modifiedPath = new TreePath(
					new Object[] { getRoot(), ProjectTreeModel.peakListsItem,
							projectEvent.getPeakList() });
			treeEvent = new TreeModelEvent(this, modifiedPath);
			for (TreeModelListener l : listeners) {
				l.treeStructureChanged(treeEvent);
			}
			break;

		}

	}

}
