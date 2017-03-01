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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.msms;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;

import javax.swing.JFrame;
import javax.swing.JMenuBar;

import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;

import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.desktop.impl.WindowsMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizerModule;
import net.sf.mzmine.modules.visualization.tic.CursorPosition;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.WindowSettingsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.dialogs.AxesSetupDialog;

import com.google.common.collect.Range;

/**
 * MS/MS visualizer using JFreeChart library
 */
public class MsMsVisualizerWindow extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;
    private MsMsToolBar toolBar;
    private MsMsPlot IDAPlot;
    private MsMsBottomPanel bottomPanel;
    private MsMsDataSet dataset;
    private RawDataFile dataFile;
    private boolean tooltipMode;

    public MsMsVisualizerWindow(RawDataFile dataFile, Range<Double> rtRange,
	    Range<Double> mzRange, IntensityType intensityType,
	    NormalizationType normalizationType, Double minPeakInt,
	    ParameterSet parameters) {

	super("MS/MS visualizer: [" + dataFile.getName() + "]");

	setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	setBackground(Color.white);

	this.dataFile = dataFile;
	this.tooltipMode = true;

	dataset = new MsMsDataSet(dataFile, rtRange, mzRange, intensityType,
		normalizationType, minPeakInt, this);

	toolBar = new MsMsToolBar(this);
	add(toolBar, BorderLayout.EAST);

	IDAPlot = new MsMsPlot(this, dataFile, this, dataset, rtRange, mzRange);
	add(IDAPlot, BorderLayout.CENTER);

	bottomPanel = new MsMsBottomPanel(this, dataFile, parameters);
	add(bottomPanel, BorderLayout.SOUTH);

	updateTitle();

	// After we have constructed everything, load the peak lists into the
	// bottom panel
	bottomPanel.rebuildPeakListSelector();

	MZmineCore.getDesktop().addPeakListTreeListener(bottomPanel);

	// Add the Windows menu
	JMenuBar menuBar = new JMenuBar();
	menuBar.add(new WindowsMenu());
	setJMenuBar(menuBar);

	pack();

	// get the window settings parameter
	ParameterSet paramSet = MZmineCore.getConfiguration()
		.getModuleParameters(MsMsVisualizerModule.class);
	WindowSettingsParameter settings = paramSet
		.getParameter(MsMsParameters.windowSettings);

	// update the window and listen for changes
	settings.applySettingsToWindow(this);
	this.addComponentListener(settings);
    }

    public void dispose() {
	super.dispose();
	MZmineCore.getDesktop().removePeakListTreeListener(bottomPanel);
    }

    void updateTitle() {
	StringBuffer title = new StringBuffer();
	title.append("Time vs. m/z for precursor ions\n");
	title.append(dataFile.getName());
	IDAPlot.setTitle(title.toString());
    }

    /**
     * @return current cursor position
     */
    public CursorPosition getCursorPosition() {
	double selectedRT = (double) IDAPlot.getXYPlot()
		.getDomainCrosshairValue();
	double selectedMZ = (double) IDAPlot.getXYPlot()
		.getRangeCrosshairValue();

	int index = dataset.getIndex(selectedRT, selectedMZ);

	if (index >= 0) {
	    double intensity = (double) dataset.getZ(0, index);
	    CursorPosition pos = new CursorPosition(selectedRT, selectedMZ,
		    intensity, dataset.getDataFile(),
		    dataset.getScanNumber(index));
	    return pos;
	}

	return null;
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {

	String command = event.getActionCommand();

	if (command.equals("SHOW_SPECTRUM")) {
	    CursorPosition pos = getCursorPosition();
	    if (pos != null) {
		SpectraVisualizerModule.showNewSpectrumWindow(
			pos.getDataFile(), pos.getScanNumber());
	    }
	}

	if (command.equals("SETUP_AXES")) {
	    AxesSetupDialog dialog = new AxesSetupDialog(this,
		    IDAPlot.getXYPlot());
	    dialog.setVisible(true);
	}

	if (command.equals("SHOW_DATA_POINTS")) {
	    IDAPlot.switchDataPointsVisible();
	}

	if (command.equals("SWITCH_TOOLTIPS")) {
	    if (tooltipMode) {
		IDAPlot.showPeaksTooltips(false);
		toolBar.setTooltipButton(false);
		tooltipMode = false;
	    } else {
		IDAPlot.showPeaksTooltips(true);
		toolBar.setTooltipButton(true);
		tooltipMode = true;
	    }
	}

	if (command.equals("FIND_SPECTRA")) {

	    // Parameters
	    final DoubleParameter inputMZ = new DoubleParameter("Ion m/z",
		    "m/z value of ion to search for.");

	    final MZToleranceParameter inputMZTolerance = new MZToleranceParameter();

	    final DoubleParameter inputIntensity = new DoubleParameter(
		    "Min. ion intensity",
		    "Only ions with intensities above this value will be searched for.");

	    final BooleanParameter inputNL = new BooleanParameter(
		    "Neutral Loss",
		    "If selected, the ion to be searched for will be a neutral loss ion.\nIn this case, only ions above the min. intensity will be examined.",
		    false);

	    final ComboParameter<Colors> inputColors = new ComboParameter<Colors>(
		    "Color",
		    "The color which the data points will be marked with.",
		    Colors.values());

	    Parameter<?>[] parameters = new Parameter<?>[5];
	    parameters[0] = inputMZ;
	    parameters[1] = inputMZTolerance;
	    parameters[2] = inputIntensity;
	    parameters[3] = inputNL;
	    parameters[4] = inputColors;

	    final ParameterSet parametersSearch = new SimpleParameterSet(
		    parameters);
	    ExitCode exitCode = parametersSearch.showSetupDialog(this, true);

	    if (exitCode != ExitCode.OK)
		return;

	    double searchMZ = parametersSearch.getParameter(inputMZ).getValue();
	    MZTolerance searchMZTolerance = parametersSearch.getParameter(inputMZTolerance)
		    .getValue();
	    double minIntensity = parametersSearch.getParameter(inputIntensity)
		    .getValue();
	    boolean neutralLoss = parametersSearch.getParameter(inputNL)
		    .getValue(); 

	    Color highligtColor = Color.red;
	    ;
	    if (parametersSearch.getParameter(inputColors).getValue()
		    .equals(Colors.green)) {
		highligtColor = Color.green;
	    }
	    if (parametersSearch.getParameter(inputColors).getValue()
		    .equals(Colors.blue)) {
		highligtColor = Color.blue;
	    }

	    // Find and highlight spectra with specific ion
	    dataset.highlightSpectra(searchMZ, searchMZTolerance, minIntensity,
		    neutralLoss, highligtColor);

	    // Add legend entry
	    LegendItemCollection chartLegend = IDAPlot.getXYPlot()
		    .getLegendItems();
	    chartLegend.add(new LegendItem("Ion: " + searchMZ, "",
		    "MS/MS spectra which contain the " + searchMZ
			    + " ion\nTolerance: " + searchMZTolerance.toString() + "\nMin intensity: "
			    + minIntensity, "",
		    new Ellipse2D.Double(0, 0, 7, 7), highligtColor));
	    IDAPlot.getXYPlot().setFixedLegendItems(chartLegend);
	}

    }

    MsMsPlot getPlot() {
	return IDAPlot;
    }
}
