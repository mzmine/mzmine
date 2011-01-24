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

package net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.TableRowSorter;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimplePeakIdentity;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizer;
import net.sf.mzmine.project.ProjectEvent;
import net.sf.mzmine.project.ProjectEvent.ProjectEventType;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;

public class ResultWindow extends JInternalFrame implements ActionListener {

	private ResultTableModel listElementModel;
	private JButton btnAdd, btnIsotopeViewer;
	private PeakList peakList;
	private PeakListRow peakListRow;
	private JTable IDList;
	private String description;
	private Task searchTask;


	public ResultWindow(PeakList peakList, PeakListRow peakListRow,
			double searchedMass, int charge, 
			IsotopePattern detectedPattern, Task searchTask) {

		super(null, true, true, true, true);

		this.peakList = peakList;
		this.peakListRow = peakListRow;
		this.searchTask = searchTask;

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setBackground(Color.white);

		JPanel pnlLabelsAndList = new JPanel(new BorderLayout());
		pnlLabelsAndList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		pnlLabelsAndList.add(new JLabel("List of possible identities"),
				BorderLayout.NORTH);

		listElementModel = new ResultTableModel(searchedMass);
		IDList = new JTable();
		IDList.setModel(listElementModel);
		IDList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		IDList.getTableHeader().setReorderingAllowed(false);
		
		TableRowSorter<ResultTableModel> sorter = new TableRowSorter<ResultTableModel>(
				listElementModel);
		IDList.setRowSorter(sorter);

		JScrollPane listScroller = new JScrollPane(IDList);
		listScroller.setPreferredSize(new Dimension(350, 100));
		listScroller.setAlignmentX(LEFT_ALIGNMENT);
		JPanel listPanel = new JPanel();
		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.PAGE_AXIS));
		listPanel.add(listScroller);
		listPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		pnlLabelsAndList.add(listPanel, BorderLayout.CENTER);

		JPanel pnlButtons = new JPanel();
		btnAdd = new JButton("Add identity");
		btnAdd.addActionListener(this);
		btnAdd.setActionCommand("ADD");
		btnIsotopeViewer = new JButton("View isotope pattern");
		btnIsotopeViewer.addActionListener(this);
		btnIsotopeViewer.setActionCommand("ISOTOPE_VIEWER");

		pnlButtons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		pnlButtons.add(btnAdd);
		pnlButtons.add(btnIsotopeViewer);

		setLayout(new BorderLayout());
		setSize(500, 200);
		add(pnlLabelsAndList, BorderLayout.CENTER);
		add(pnlButtons, BorderLayout.SOUTH);
		setTitle(description);
		pack();

	}

	public void actionPerformed(ActionEvent e) {

		String command = e.getActionCommand();

		if (command.equals("ADD")) {
			int index = IDList.getSelectedRow();
			index = IDList.convertRowIndexToModel(index);

			if (index < 0) {
				MZmineCore.getDesktop().displayMessage(
						"Select one result to add as compound identity");
				return;

			}

			String formula = (String) listElementModel.getValueAt(index, 0);
			SimplePeakIdentity newIdentity = new SimplePeakIdentity(formula);
			peakListRow.addPeakIdentity(newIdentity, false);

			// Notify the tree that peak list has changed
			ProjectEvent newEvent = new ProjectEvent(
					ProjectEventType.PEAKLIST_CONTENTS_CHANGED, peakList);
			MZmineCore.getProjectManager().fireProjectListeners(newEvent);

			dispose();
		}

		if (command.equals("ISOTOPE_VIEWER")) {

			int index = IDList.getSelectedRow();
			index = IDList.convertRowIndexToModel(index);

			if (index < 0) {
				MZmineCore.getDesktop().displayMessage(
						"Select one result to display the isotope pattern");
				return;
			}

			IsotopePattern predictedPattern = listElementModel
					.getIsotopePattern(index);

			if (predictedPattern == null)
				return;

			ChromatographicPeak peak = peakListRow.getBestPeak();

			RawDataFile dataFile = peak.getDataFile();
			int scanNumber = peak.getRepresentativeScanNumber();
			SpectraVisualizer.showNewSpectrumWindow(dataFile, scanNumber, null,
					peak.getIsotopePattern(), predictedPattern);

		}

	}

	public void addNewListItem(final CandidateFormula candidate) {

		// Update the model in swing thread to avoid exceptions
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				listElementModel.addElement(candidate);
			}
		});
	}

	public String toString() {
		return description;
	}

	public void dispose() {

		// Cancel the search task if it is still running
		TaskStatus searchStatus = searchTask.getStatus();
		if ((searchStatus == TaskStatus.WAITING)
				|| (searchStatus == TaskStatus.PROCESSING))
			searchTask.cancel();

		super.dispose();

	}

}
