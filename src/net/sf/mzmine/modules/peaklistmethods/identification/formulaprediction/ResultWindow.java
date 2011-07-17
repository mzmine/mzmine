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
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimplePeakIdentity;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizerModule;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizerWindow;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.components.PercentageCellRenderer;

public class ResultWindow extends JInternalFrame implements ActionListener {

	private ResultTableModel listElementModel;
	private PeakListRow peakListRow;
	private JTable IDList;
	private Task searchTask;
	private String title;

	public ResultWindow(String title, PeakListRow peakListRow,
			double searchedMass, int charge, Task searchTask) {

		super(title, true, true, true, true);

		this.title = title;
		this.peakListRow = peakListRow;
		this.searchTask = searchTask;

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setBackground(Color.white);

		JPanel pnlLabelsAndList = new JPanel(new BorderLayout());
		pnlLabelsAndList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		pnlLabelsAndList.add(new JLabel("List of possible formulas"),
				BorderLayout.NORTH);

		listElementModel = new ResultTableModel(searchedMass);
		IDList = new JTable();
		IDList.setModel(listElementModel);
		IDList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		IDList.getTableHeader().setReorderingAllowed(false);

		TableRowSorter<ResultTableModel> sorter = new TableRowSorter<ResultTableModel>(
				listElementModel);

		// set descending order by isotope score
		sorter.toggleSortOrder(3);
		sorter.toggleSortOrder(3);

		IDList.setRowSorter(sorter);

		TableColumnModel columnModel = IDList.getColumnModel();
		DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
		renderer.setHorizontalAlignment(SwingConstants.LEFT);
		IDList.setDefaultRenderer(Double.class, renderer);
		columnModel.getColumn(3).setCellRenderer(new PercentageCellRenderer(1));
		columnModel.getColumn(4).setCellRenderer(new PercentageCellRenderer(1));

		JScrollPane listScroller = new JScrollPane(IDList);
		listScroller.setPreferredSize(new Dimension(350, 100));
		listScroller.setAlignmentX(LEFT_ALIGNMENT);
		JPanel listPanel = new JPanel();
		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.PAGE_AXIS));
		listPanel.add(listScroller);
		listPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		pnlLabelsAndList.add(listPanel, BorderLayout.CENTER);

		JPanel pnlButtons = new JPanel();
		pnlButtons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		GUIUtils.addButton(pnlButtons, "Add identity", null, this, "ADD");
		GUIUtils.addButton(pnlButtons, "Copy to clipboard", null, this, "COPY");
		GUIUtils.addButton(pnlButtons, "View isotope pattern", null, this,
				"SHOW_ISOTOPES");
		GUIUtils.addButton(pnlButtons, "Show MS/MS", null, this, "SHOW_MSMS");

		setLayout(new BorderLayout());
		setSize(500, 200);
		add(pnlLabelsAndList, BorderLayout.CENTER);
		add(pnlButtons, BorderLayout.SOUTH);
		pack();

	}

	public void actionPerformed(ActionEvent e) {

		String command = e.getActionCommand();

		int index = IDList.getSelectedRow();

		if (index < 0) {
			MZmineCore.getDesktop().displayMessage("Please select one result");
			return;
		}
		index = IDList.convertRowIndexToModel(index);
		ResultFormula formula = listElementModel.getFormula(index);

		if (command.equals("ADD")) {

			SimplePeakIdentity newIdentity = new SimplePeakIdentity(
					formula.getFormulaAsString());
			peakListRow.addPeakIdentity(newIdentity, false);

			// Notify the GUI about the change in the project
			MZmineCore.getCurrentProject().notifyObjectChanged(peakListRow,
					false);

			dispose();
		}

		if (command.equals("COPY")) {

			String formulaString = formula.getFormulaAsString();
			StringSelection stringSelection = new StringSelection(formulaString);
			Clipboard clipboard = Toolkit.getDefaultToolkit()
					.getSystemClipboard();
			clipboard.setContents(stringSelection, null);

		}

		if (command.equals("SHOW_ISOTOPES")) {

			IsotopePattern predictedPattern = formula.getPredictedIsotopes();

			if (predictedPattern == null)
				return;

			ChromatographicPeak peak = peakListRow.getBestPeak();

			RawDataFile dataFile = peak.getDataFile();
			int scanNumber = peak.getRepresentativeScanNumber();
			SpectraVisualizerModule.showNewSpectrumWindow(dataFile, scanNumber, null,
					peak.getIsotopePattern(), predictedPattern);

		}

		if (command.equals("SHOW_MSMS")) {

			ChromatographicPeak bestPeak = peakListRow.getBestPeak();

			RawDataFile dataFile = bestPeak.getDataFile();
			int msmsScanNumber = bestPeak.getMostIntenseFragmentScanNumber();

			if (msmsScanNumber < 1)
				return;

			SpectraVisualizerWindow msmsPlot = SpectraVisualizerModule
					.showNewSpectrumWindow(dataFile, msmsScanNumber);

			if (msmsPlot == null)
				return;
			Map<DataPoint, String> annotation = formula.getMSMSannotation();

			if (annotation == null)
				return;
			msmsPlot.addAnnotation(annotation);

		}

	}

	public void addNewListItem(final ResultFormula formula) {

		// Update the model in swing thread to avoid exceptions
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				listElementModel.addElement(formula);
				setTitle(title + ", " + listElementModel.getRowCount()
						+ " formulas found");
			}
		});
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
