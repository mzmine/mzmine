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

package net.sf.mzmine.modules.visualization.twod;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JMenuBar;

import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.desktop.impl.WindowsMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.WindowSettingsParameter;
import net.sf.mzmine.util.dialogs.AxesSetupDialog;

import com.google.common.collect.Range;

/**
 * 2D visualizer using JFreeChart library
 */
public class TwoDVisualizerWindow extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;
    private TwoDToolBar toolBar;
    private TwoDPlot twoDPlot;
    private TwoDBottomPanel bottomPanel;
    private TwoDDataSet dataset;
    private RawDataFile dataFile;
    private boolean tooltipMode;
    private boolean logScale;

    public TwoDVisualizerWindow(RawDataFile dataFile, Scan scans[],
            Range<Double> rtRange, Range<Double> mzRange,
            ParameterSet parameters) {

        super("2D view: [" + dataFile.getName() + "]");

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        this.dataFile = dataFile;

        this.tooltipMode = true;

        dataset = new TwoDDataSet(dataFile, scans, rtRange, mzRange, this);
        if (parameters.getParameter(TwoDVisualizerParameters.plotType).getValue()==PlotType.FAST2D) {
            twoDPlot = new TwoDPlot(dataFile, this, dataset, rtRange, mzRange,"default");
            add(twoDPlot, BorderLayout.CENTER);
        }

        if (parameters.getParameter(TwoDVisualizerParameters.plotType).getValue()==PlotType.POINT2D) {
            twoDPlot = new TwoDPlot(dataFile, this, dataset, rtRange, mzRange,"point2D");
            add(twoDPlot, BorderLayout.CENTER);
        }


        toolBar = new TwoDToolBar(this);
        add(toolBar, BorderLayout.EAST);

        bottomPanel = new TwoDBottomPanel(this, dataFile, parameters);
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
                .getModuleParameters(TwoDVisualizerModule.class);
        WindowSettingsParameter settings = paramSet
                .getParameter(TwoDVisualizerParameters.windowSettings);

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
        title.append("[");
        title.append(dataFile.getName());
        title.append("]: 2D view");
        twoDPlot.setTitle(title.toString());
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        if (command.equals("SWITCH_PALETTE")) {
            twoDPlot.getXYPlot().switchPalette();
        }

        if (command.equals("SHOW_DATA_POINTS")) {
            twoDPlot.switchDataPointsVisible();
        }

        if (command.equals("SETUP_AXES")) {
            AxesSetupDialog dialog = new AxesSetupDialog(this,
                    twoDPlot.getXYPlot());
            dialog.setVisible(true);
        }

        if (command.equals("SWITCH_PLOTMODE")) {

            if (twoDPlot.getPlotMode() == PlotMode.CENTROID) {
                toolBar.setCentroidButton(true);
                twoDPlot.setPlotMode(PlotMode.CONTINUOUS);
            } else {
                toolBar.setCentroidButton(false);
                twoDPlot.setPlotMode(PlotMode.CENTROID);
            }
        }

        if (command.equals("SWITCH_TOOLTIPS")) {
            if (tooltipMode) {
                twoDPlot.showPeaksTooltips(false);
                toolBar.setTooltipButton(false);
                tooltipMode = false;
            } else {
                twoDPlot.showPeaksTooltips(true);
                toolBar.setTooltipButton(true);
                tooltipMode = true;
            }
        }

        if (command.equals("SWITCH_LOG_SCALE")) {
            if (twoDPlot != null) {
                logScale = !logScale;
                twoDPlot.setLogScale(logScale);
            }
        }

    }

    TwoDPlot getPlot() {
        return twoDPlot;
    }
}
