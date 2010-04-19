/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;
import javax.swing.tree.TreePath;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.infovisualizer.InfoVisualizer;
import net.sf.mzmine.modules.visualization.peaklist.PeakListTableVisualizer;
import net.sf.mzmine.modules.visualization.peaksummary.PeakSummaryVisualizer;
import net.sf.mzmine.modules.visualization.scatterplot.ScatterPlotVisualizer;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizer;
import net.sf.mzmine.modules.visualization.threed.ThreeDVisualizer;
import net.sf.mzmine.modules.visualization.tic.TICVisualizer;
import net.sf.mzmine.modules.visualization.twod.TwoDVisualizer;
import net.sf.mzmine.util.GUIUtils;

/**
 * This class handles pop-up menus and double click events in the project tree
 */
public class ProjectTreeMouseHandler extends MouseAdapter implements
		ActionListener {

	private ProjectTree tree;
	private JPopupMenu dataFilePopupMenu, peakListPopupMenu, scanPopupMenu,
			peakListRowPopupMenu;

	/**
	 * Constructor
	 */
	public ProjectTreeMouseHandler(ProjectTree tree) {

		this.tree = tree;

		dataFilePopupMenu = new JPopupMenu();

		GUIUtils.addMenuItem(dataFilePopupMenu, "Show TIC", this, "SHOW_TIC");
		GUIUtils.addMenuItem(dataFilePopupMenu, "Show mass spectrum", this,
				"SHOW_SPECTRUM");
		GUIUtils.addMenuItem(dataFilePopupMenu, "Show 2D visualizer", this,
				"SHOW_2D");
		GUIUtils.addMenuItem(dataFilePopupMenu, "Show 3D visualizer", this,
				"SHOW_3D");
		GUIUtils.addMenuItem(dataFilePopupMenu, "Remove", this, "REMOVE_FILE");

		scanPopupMenu = new JPopupMenu();

		GUIUtils.addMenuItem(scanPopupMenu, "Show scan", this, "SHOW_SCAN");

		peakListPopupMenu = new JPopupMenu();

		GUIUtils.addMenuItem(peakListPopupMenu, "Show peak list table", this,
				"SHOW_PEAKLIST_TABLES");
		GUIUtils.addMenuItem(peakListPopupMenu, "Show peak list info", this,
				"SHOW_PEAKLIST_INFO");
		GUIUtils.addMenuItem(peakListPopupMenu, "Show scatter plot", this,
				"SHOW_SCATTER_PLOT");
		GUIUtils.addMenuItem(peakListPopupMenu, "Remove", this,
				"REMOVE_PEAKLIST");

		peakListRowPopupMenu = new JPopupMenu();

		GUIUtils.addMenuItem(peakListRowPopupMenu, "Show peak summary", this,
				"SHOW_PEAK_SUMMARY");

	}

	public void actionPerformed(ActionEvent e) {

		String command = e.getActionCommand();

		// Actions for raw data files

		if (command.equals("SHOW_TIC")) {
			RawDataFile[] selectedFiles = tree
					.getSelectedObjects(RawDataFile.class);
			TICVisualizer.showNewTICVisualizerWindow(selectedFiles, null, null);
		}

		if (command.equals("SHOW_SPECTRUM")) {
			RawDataFile[] selectedFiles = tree
					.getSelectedObjects(RawDataFile.class);
			for (RawDataFile file : selectedFiles) {
				SpectraVisualizer.showSpectrumVisualizerDialog(file);
			}
		}

		if (command.equals("SHOW_2D")) {
			RawDataFile[] selectedFiles = tree
					.getSelectedObjects(RawDataFile.class);
			for (RawDataFile file : selectedFiles) {
				TwoDVisualizer.show2DVisualizerSetupDialog(file);				
			}
		}

		if (command.equals("SHOW_3D")) {
			RawDataFile[] selectedFiles = tree
					.getSelectedObjects(RawDataFile.class);
			for (RawDataFile file : selectedFiles) {
				ThreeDVisualizer.show3DVisualizerSetupDialog(file);
			}
		}

		if (command.equals("REMOVE_FILE")) {
			RawDataFile[] selectedFiles = tree
					.getSelectedObjects(RawDataFile.class);
			for (RawDataFile file : selectedFiles)
				MZmineCore.getCurrentProject().removeFile(file);
		}

		// Actions for scans

		if (command.equals("SHOW_SCAN")) {
			Scan selectedScans[] = tree.getSelectedObjects(Scan.class);
			for (Scan scan : selectedScans) {
				SpectraVisualizer.showNewSpectrumWindow(scan.getDataFile(),
						scan.getScanNumber());
			}
		}

		// Actions for peak lists

		if (command.equals("SHOW_PEAKLIST_TABLES")) {
			PeakList[] selectedPeakLists = tree
					.getSelectedObjects(PeakList.class);
			for (PeakList peakList : selectedPeakLists) {
				PeakListTableVisualizer
						.showNewPeakListVisualizerWindow(peakList);
			}
		}

		if (command.equals("SHOW_PEAKLIST_INFO")) {
			PeakList[] selectedPeakLists = tree
					.getSelectedObjects(PeakList.class);
			for (PeakList peakList : selectedPeakLists) {
				InfoVisualizer.showNewPeakListInfo(peakList);
			}
		}

		if (command.equals("SHOW_SCATTER_PLOT")) {
			PeakList[] selectedPeakLists = tree
					.getSelectedObjects(PeakList.class);
			for (PeakList peakList : selectedPeakLists) {
				ScatterPlotVisualizer.showNewScatterPlotWindow(peakList);
			}
		}

		if (command.equals("REMOVE_PEAKLIST")) {
			PeakList[] selectedPeakLists = tree
					.getSelectedObjects(PeakList.class);
			for (PeakList peakList : selectedPeakLists)
				MZmineCore.getCurrentProject().removePeakList(peakList);
		}

		// Actions for peak list rows

		if (command.equals("SHOW_PEAK_SUMMARY")) {
			PeakListRow[] selectedRows = tree
					.getSelectedObjects(PeakListRow.class);
			for (PeakListRow row : selectedRows) {
				PeakSummaryVisualizer.showNewPeakSummaryWindow(row);
			}
		}

	}

	public void mousePressed(MouseEvent e) {

		if (e.isPopupTrigger())
			handlePopupTriggerEvent(e);

		if ((e.getClickCount() == 2) && (e.getButton() == MouseEvent.BUTTON1))
			handleDoubleClickEvent(e);

	}

	public void mouseReleased(MouseEvent e) {
		if (e.isPopupTrigger())
			handlePopupTriggerEvent(e);
	}

	private void handlePopupTriggerEvent(MouseEvent e) {
		TreePath clickedPath = tree.getPathForLocation(e.getX(), e.getY());
		Object clickedObject = null;
		if (clickedPath != null)
			clickedObject = clickedPath.getLastPathComponent();

		if (clickedObject instanceof RawDataFile)
			dataFilePopupMenu.show(e.getComponent(), e.getX(), e.getY());
		if (clickedObject instanceof Scan)
			scanPopupMenu.show(e.getComponent(), e.getX(), e.getY());
		if (clickedObject instanceof PeakList)
			peakListPopupMenu.show(e.getComponent(), e.getX(), e.getY());
		if (clickedObject instanceof PeakListRow)
			peakListRowPopupMenu.show(e.getComponent(), e.getX(), e.getY());
	}

	private void handleDoubleClickEvent(MouseEvent e) {
		TreePath clickedPath = tree.getPathForLocation(e.getX(), e.getY());
		Object clickedObject = null;
		if (clickedPath != null)
			clickedObject = clickedPath.getLastPathComponent();

		if (clickedObject instanceof RawDataFile) {
			RawDataFile clickedFile = (RawDataFile) clickedObject;
			TICVisualizer.showNewTICVisualizerWindow(
					new RawDataFile[] { clickedFile }, null, null);
		}

		if (clickedObject instanceof PeakList) {
			PeakList clickedPeakList = (PeakList) clickedObject;
			PeakListTableVisualizer
					.showNewPeakListVisualizerWindow(clickedPeakList);
		}

		if (clickedObject instanceof Scan) {
			Scan clickedScan = (Scan) clickedObject;
			SpectraVisualizer.showNewSpectrumWindow(clickedScan.getDataFile(),
					clickedScan.getScanNumber());
		}

		if (clickedObject instanceof PeakListRow) {
			PeakListRow clickedPeak = (PeakListRow) clickedObject;
			PeakSummaryVisualizer.showNewPeakSummaryWindow(clickedPeak);
		}

	}

}