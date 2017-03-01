/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
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

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizerModule;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizerWindow;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.components.PercentageCellRenderer;

public class ResultWindow extends JFrame implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private final JTable resultsTable;
    private final ResultTableModel resultsTableModel;
    private final TableRowSorter<ResultTableModel> resultsTableSorter;
    private final PeakListRow peakListRow;
    private final Task searchTask;
    private final String title;

    public ResultWindow(String title, PeakListRow peakListRow,
	    double searchedMass, int charge, Task searchTask) {

	super(title);

	this.title = title;
	this.peakListRow = peakListRow;
	this.searchTask = searchTask;

	setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	setBackground(Color.white);

	JPanel pnlLabelsAndList = new JPanel(new BorderLayout());
	pnlLabelsAndList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

	pnlLabelsAndList.add(new JLabel("List of possible formulas"),
		BorderLayout.NORTH);

	resultsTableModel = new ResultTableModel(searchedMass);
	resultsTable = new JTable();
	//int rowHeight = resultsTable.getRowHeight();
	resultsTable.setRowHeight(22);
	resultsTable.setModel(resultsTableModel);
	resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	resultsTable.getTableHeader().setReorderingAllowed(false);

	resultsTableSorter = new TableRowSorter<ResultTableModel>(
		resultsTableModel);

	// set descending order by isotope score
	resultsTableSorter.toggleSortOrder(3);
	resultsTableSorter.toggleSortOrder(3);

	resultsTable.setRowSorter(resultsTableSorter);

	TableColumnModel columnModel = resultsTable.getColumnModel();
	DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
	renderer.setHorizontalAlignment(SwingConstants.LEFT);
	resultsTable.setDefaultRenderer(Double.class, renderer);
	columnModel.getColumn(4).setCellRenderer(new PercentageCellRenderer(1));
	columnModel.getColumn(5).setCellRenderer(new PercentageCellRenderer(1));

	JScrollPane listScroller = new JScrollPane(resultsTable);
	listScroller.setPreferredSize(new Dimension(350, 100));
	listScroller.setAlignmentX(LEFT_ALIGNMENT);
	JPanel listPanel = new JPanel();
	listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.PAGE_AXIS));
	listPanel.add(listScroller);
	listPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
	pnlLabelsAndList.add(listPanel, BorderLayout.CENTER);

	JPanel pnlButtons = new JPanel();
	pnlButtons.setLayout(new BoxLayout(pnlButtons, BoxLayout.X_AXIS));
	pnlButtons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

	GUIUtils.addButton(pnlButtons, "Add identity", null, this, "ADD");
	GUIUtils.addButton(pnlButtons, "Copy to clipboard", null, this, "COPY");
	GUIUtils.addButton(pnlButtons, "Export all", null, this, "EXPORT");
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

	if (command.equals("EXPORT")) {

	    // Ask for filename
	    JFileChooser fileChooser = new JFileChooser();
	    fileChooser.setApproveButtonText("Export");

	    int result = fileChooser.showSaveDialog(MZmineCore.getDesktop()
		    .getMainWindow());
	    if (result != JFileChooser.APPROVE_OPTION)
		return;
	    File outputFile = fileChooser.getSelectedFile();
	    try {
		FileWriter fileWriter = new FileWriter(outputFile);
		BufferedWriter writer = new BufferedWriter(fileWriter);
		writer.write("Formula,Mass,RDBE,Isotope pattern score,MS/MS score");
		writer.newLine();

		for (int row = 0; row < resultsTable.getRowCount(); row++) {
		    int modelRow = resultsTable.convertRowIndexToModel(row);
		    ResultFormula formula = resultsTableModel
			    .getFormula(modelRow);
		    writer.write(formula.getFormulaAsString());
		    writer.write(",");
		    writer.write(String.valueOf(formula.getExactMass()));
		    writer.write(",");
		    if (formula.getRDBE() != null)
			writer.write(String.valueOf(formula.getRDBE()));
		    writer.write(",");
		    if (formula.getIsotopeScore() != null)
			writer.write(String.valueOf(formula.getIsotopeScore()));
		    writer.write(",");
		    if (formula.getMSMSScore() != null)
			writer.write(String.valueOf(formula.getMSMSScore()));
		    writer.newLine();
		}

		writer.close();

	    } catch (Exception ex) {
		MZmineCore.getDesktop().displayErrorMessage(
			MZmineCore.getDesktop().getMainWindow(),
			"Error writing to file " + outputFile + ": "
				+ ExceptionUtils.exceptionToString(ex));
	    }
	    return;

	}

	// The following actions require a single row to be selected

	int index = resultsTable.getSelectedRow();

	if (index < 0) {
	    MZmineCore.getDesktop().displayMessage(
		    MZmineCore.getDesktop().getMainWindow(),
		    "Please select one result");
	    return;
	}
	index = resultsTable.convertRowIndexToModel(index);
	ResultFormula formula = resultsTableModel.getFormula(index);

	if (command.equals("ADD")) {

	    SimplePeakIdentity newIdentity = new SimplePeakIdentity(
		    formula.getFormulaAsString());
	    peakListRow.addPeakIdentity(newIdentity, false);

	    // Notify the GUI about the change in the project
	    MZmineCore.getProjectManager().getCurrentProject()
		    .notifyObjectChanged(peakListRow, false);

	    // Repaint the window to reflect the change in the peak list
	    MZmineCore.getDesktop().getMainWindow().repaint();

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

	    Feature peak = peakListRow.getBestPeak();

	    RawDataFile dataFile = peak.getDataFile();
	    int scanNumber = peak.getRepresentativeScanNumber();
	    SpectraVisualizerModule.showNewSpectrumWindow(dataFile, scanNumber,
		    null, peak.getIsotopePattern(), predictedPattern);

	}

	if (command.equals("SHOW_MSMS")) {

	    Feature bestPeak = peakListRow.getBestPeak();

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
		resultsTableModel.addElement(formula);
		setTitle(title + ", " + resultsTableModel.getRowCount()
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
