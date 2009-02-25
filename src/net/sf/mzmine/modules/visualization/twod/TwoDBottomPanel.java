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

package net.sf.mzmine.modules.visualization.twod;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.project.ProjectEvent;
import net.sf.mzmine.project.ProjectListener;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.Range;

/**
 * 2D visualizer's bottom panel
 */
class TwoDBottomPanel extends JPanel implements ProjectListener,
        InternalFrameListener {

    private static final Font smallFont = new Font("SansSerif", Font.PLAIN, 10);
    private NumberFormat intensityThresholdFormat;

    private JComboBox peakListSelector;
    private JComboBox peakSelector;
    private JTextField peakTextField;
    private PeakThresholdParameters peakThresholdParameters;

    private TwoDVisualizerWindow masterFrame;
    private RawDataFile dataFile;

    TwoDBottomPanel(TwoDVisualizerWindow masterFrame, RawDataFile dataFile,
            PeakThresholdParameters peakThresholdParameters) {

        this.dataFile = dataFile;
        this.masterFrame = masterFrame;

        intensityThresholdFormat = MZmineCore.getIntensityFormat();

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        setBackground(Color.white);
        setBorder(new EmptyBorder(5, 5, 5, 0));

        add(Box.createHorizontalGlue());

        GUIUtils.addLabel(this, "Show: ", SwingConstants.RIGHT);

        peakSelector = new JComboBox();
        peakSelector.setBackground(Color.white);
        peakSelector.setFont(smallFont);
        peakSelector.addActionListener(masterFrame);
        peakSelector.setActionCommand("PEAKS_VIEW_THRESHOLD");
        add(peakSelector);

        this.peakThresholdParameters = peakThresholdParameters;

        JPanel peakThresholdPanel = new JPanel();
        peakThresholdPanel.setBackground(Color.white);
        peakThresholdPanel.setPreferredSize(new Dimension(50, 10));
        peakTextField = new JTextField();
        peakTextField.setPreferredSize(new Dimension(40, 15));
        peakTextField.setFont(smallFont);
        peakTextField.setVisible(false);
        peakTextField.addActionListener(masterFrame);
        peakTextField.setActionCommand("PEAKS_VIEW_TEXTFIELD");
        peakThresholdPanel.add(peakTextField);
        add(peakThresholdPanel);

        GUIUtils.addLabel(this, " from peak list: ", SwingConstants.RIGHT);

        peakListSelector = new JComboBox();
        peakListSelector.setBackground(Color.white);
        peakListSelector.setFont(smallFont);
        peakListSelector.addActionListener(masterFrame);
        peakListSelector.setActionCommand("PEAKLIST_CHANGE");
        add(peakListSelector);

        add(Box.createHorizontalStrut(10));

        MZmineCore.getProjectManager().addProjectListener(this);

        masterFrame.addInternalFrameListener(this);

        add(Box.createHorizontalGlue());

    }

    /**
     * Shows or hides a text field depending on the value of b and loads the
     * text in the text field from the parameters
     */
    void peakTextFieldSetVisible(boolean b) {
        String selectedPeakThreshold = getPeaksSelectedThreshold();
        if (selectedPeakThreshold.equals(PeakThresholdMode.ABOVE_INTENSITY_PEAKS.getName())) {
            peakTextField.setText(String.valueOf((Double) peakThresholdParameters.getParameterValue(PeakThresholdParameters.intensityThreshold)));
        }

        if (selectedPeakThreshold.equals(PeakThresholdMode.TOP_PEAKS.getName())) {
            peakTextField.setText(String.valueOf((Integer) peakThresholdParameters.getParameterValue(PeakThresholdParameters.topThreshold)));
        }

        if (selectedPeakThreshold.equals(PeakThresholdMode.TOP_PEAKS_AREA.getName())) {
            peakTextField.setText(String.valueOf((Integer) peakThresholdParameters.getParameterValue(PeakThresholdParameters.topThresholdArea)));
        }
        peakTextField.setVisible(b);

        int index = peakSelector.getSelectedIndex();
        peakThresholdParameters.setParameterValue(
                PeakThresholdParameters.comboBoxIndexThreshold, index);
    }

    /**
     * Returns selected Item from the "peak threshold" combo box
     */
    String getPeaksSelectedThreshold() {
        String selectedThreshold = (String) this.peakSelector.getSelectedItem();
        return selectedThreshold;
    }

    /**
     * Returns a peak list different peaks depending on the selected option of
     * the "peak Threshold" combo box
     */
    PeakList getPeaksInThreshold() {
        PeakList selectedPeakList = (PeakList) peakListSelector.getSelectedItem();
        String selectedPeakOption = (String) peakSelector.getSelectedItem();
        peakListSelector.setEnabled(true);

        if (selectedPeakOption.equals(PeakThresholdMode.ABOVE_INTENSITY_PEAKS.getName())) {
            Double threshold = (Double) peakThresholdParameters.getParameterValue(PeakThresholdParameters.intensityThreshold);
            try {
                threshold = Double.valueOf(peakTextField.getText());
                peakTextField.setText(intensityThresholdFormat.format(threshold));
                peakThresholdParameters.setParameterValue(
                        PeakThresholdParameters.intensityThreshold, threshold);
                return getIntensityThresholdPeakList(threshold);
            } catch (NumberFormatException exception) {
            }
        }

        if (selectedPeakOption.equals(PeakThresholdMode.TOP_PEAKS.getName())) {
            int threshold = (Integer) peakThresholdParameters.getParameterValue(PeakThresholdParameters.topThreshold);
            try {
                threshold = Integer.valueOf(peakTextField.getText());
                peakThresholdParameters.setParameterValue(
                        PeakThresholdParameters.topThreshold, threshold);
                return getTopThresholdPeakList(threshold);
            } catch (NumberFormatException exception) {
            }
        }

        if (selectedPeakOption.equals(PeakThresholdMode.TOP_PEAKS_AREA.getName())) {
            int threshold = (Integer) peakThresholdParameters.getParameterValue(PeakThresholdParameters.topThresholdArea);
            try {
                threshold = Integer.valueOf(peakTextField.getText());
                peakThresholdParameters.setParameterValue(
                        PeakThresholdParameters.topThresholdArea, threshold);
                return getTopThresholdPeakList(threshold);
            } catch (NumberFormatException exception) {
            }
        }

        if (selectedPeakOption.equals(PeakThresholdMode.ALL_PEAKS.getName())) {
            return selectedPeakList;
        }

        if (selectedPeakOption.equals(PeakThresholdMode.NO_PEAKS.getName())) {
            peakListSelector.setEnabled(false);
        }

        return null;
    }

    /**
     * Returns a peak list with the peaks which intensity is above the parameter
     * "intensity"
     */
    PeakList getIntensityThresholdPeakList(double intensity) {
        PeakList selectedPeakList = (PeakList) peakListSelector.getSelectedItem();
        SimplePeakList newList = new SimplePeakList(selectedPeakList.getName(),
                dataFile);

        for (PeakListRow peakRow : selectedPeakList.getRows()) {
            if (peakRow.getDataPointMaxIntensity() > intensity) {
                newList.addRow(peakRow);
            }
        }
        return newList;
    }

    /**
     * Returns a peak list with the top peaks defined by the parameter
     * "threshold"
     */
    PeakList getTopThresholdPeakList(int threshold) {

        PeakList selectedPeakList = (PeakList) peakListSelector.getSelectedItem();
        SimplePeakList newList = new SimplePeakList(selectedPeakList.getName(),
                dataFile);

        Vector<PeakListRow> peakRows = new Vector<PeakListRow>();

        Range mzRange = selectedPeakList.getRowsMZRange();
        Range rtRange = selectedPeakList.getRowsRTRange();

        String selectedPeakOption = (String) peakSelector.getSelectedItem();
        if (selectedPeakOption.equals(PeakThresholdMode.TOP_PEAKS_AREA.getName())) {
            mzRange = masterFrame.getPlot().getXYPlot().getAxisRange();
            rtRange = masterFrame.getPlot().getXYPlot().getDomainRange();
        }

        for (PeakListRow peakRow : selectedPeakList.getRows()) {
            if (mzRange.contains(peakRow.getAverageMZ())
                    && rtRange.contains(peakRow.getAverageRT())) {
                peakRows.add(peakRow);
            }
        }

        Collections.sort(peakRows, new PeakComparator());

        if (threshold > peakRows.size())
            threshold = peakRows.size();
        for (int i = 0; i < threshold; i++) {
            newList.addRow(peakRows.elementAt(i));
        }
        return newList;
    }

    /**
     * Returns selected peak list
     */
    PeakList getSelectedPeakList() {
        PeakList selectedPeakList = (PeakList) peakListSelector.getSelectedItem();
        return selectedPeakList;
    }

    /**
     * Reloads peak lists from the project to the selector combo box
     */
    void rebuildPeakListSelector() {
        PeakList selectedPeakList = (PeakList) peakListSelector.getSelectedItem();
        PeakList currentPeakLists[] = MZmineCore.getCurrentProject().getPeakLists(
                dataFile);
        peakListSelector.removeAllItems();
        for (int i = currentPeakLists.length - 1; i >= 0; i--) {
            peakListSelector.addItem(currentPeakLists[i]);
        }
        if (selectedPeakList != null)
            peakListSelector.setSelectedItem(selectedPeakList);

    }

    /**
     * Loads the options to the "peak threshold" combo box
     */
    void buildPeakThresholdSelector() {
        PeakList currentPeakLists[] = MZmineCore.getCurrentProject().getPeakLists(
                dataFile);
        int index = (Integer) peakThresholdParameters.getParameterValue(PeakThresholdParameters.comboBoxIndexThreshold);
        if (currentPeakLists.length > 0) {
            for (PeakThresholdMode option : PeakThresholdMode.values()) {
                peakSelector.addItem(option.getName());
            }
        }
        if (index < currentPeakLists.length)
            peakSelector.setSelectedIndex(index);
    }

    /**
     * ProjectListener implementaion
     */
    public void projectModified(ProjectEvent event) {
        rebuildPeakListSelector();
    }

    public void internalFrameActivated(InternalFrameEvent event) {
        // Ignore
    }

    /**
     * We have to remove the listener when the window is closed, because
     * otherwise the project would always keep a reference to this window and
     * the GC would not be able to collect it
     */
    public void internalFrameClosed(InternalFrameEvent event) {
        MZmineCore.getProjectManager().removeProjectListener(this);
        masterFrame.removeInternalFrameListener(this);
    }

    public void internalFrameClosing(InternalFrameEvent event) {
        // Ignore
    }

    public void internalFrameDeactivated(InternalFrameEvent event) {
        // Ignore
    }

    public void internalFrameDeiconified(InternalFrameEvent event) {
        // Ignore
    }

    public void internalFrameIconified(InternalFrameEvent event) {
        // Ignore
    }

    public void internalFrameOpened(InternalFrameEvent event) {
        // Ignore
    }

    class PeakComparator implements Comparator<PeakListRow> {

        public PeakComparator() {
        }

        public int compare(PeakListRow o1, PeakListRow o2) {
            try {
                double value1 = o1.getDataPointMaxIntensity();
                double value2 = o2.getDataPointMaxIntensity();
                return Double.compare(value2, value1);
            } catch (Exception ee) {
                return 1000;
            }
        }
    }

}
