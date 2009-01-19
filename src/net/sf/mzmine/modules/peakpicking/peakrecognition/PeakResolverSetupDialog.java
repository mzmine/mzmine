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

package net.sf.mzmine.modules.peakpicking.peakrecognition;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Constructor;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.MzPeak;
import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.tic.PeakDataSet;
import net.sf.mzmine.modules.visualization.tic.TICPlot;
import net.sf.mzmine.modules.visualization.tic.TICToolBar;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

/**
 * This class extends ParameterSetupDialog class, including a TIC plot.
 */
public class PeakResolverSetupDialog extends ParameterSetupDialog implements
        ActionListener, PropertyChangeListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    // Dialog components
    static final Font comboFont = new Font("SansSerif", Font.PLAIN, 10);
    private JPanel pnlPlotXY, pnlLocal, pnlVisible, pnlLab, pnlFlds;
    private JComboBox comboPeakList, comboPeak;
    private JCheckBox preview;

    // XYPlot
    private TICToolBar toolBar;
    private TICPlot ticPlot;
    private ChromatogramTICDataSet ticDataset;

    // Peak resolver
    private SimpleParameterSet pbParameters;
    private int peakResolverTypeNumber;

    /**
     * @param parameters
     * @param massDetectorTypeNumber
     */
    public PeakResolverSetupDialog(PeakRecognitionParameters parameters,
            int peakResolverTypeNumber) {

        super(
                PeakRecognitionParameters.peakResolverNames[peakResolverTypeNumber]
                        + "'s parameter setup dialog ",
                parameters.getPeakBuilderParameters(peakResolverTypeNumber),
                PeakRecognitionParameters.peakResolverHelpFiles[peakResolverTypeNumber]);

        this.peakResolverTypeNumber = peakResolverTypeNumber;

        // Parameters of local mass detector to get preview values
        pbParameters = parameters.getPeakBuilderParameters(
                peakResolverTypeNumber).clone();

        // Set a listener in all parameters's fields to add functionality to
        // this dialog
        for (Parameter p : pbParameters.getParameters()) {

            JComponent field = getComponentForParameter(p);
            field.addPropertyChangeListener("value", this);
            if (field instanceof JCheckBox)
                ((JCheckBox) field).addActionListener(this);
            if (field instanceof JComboBox)
                ((JComboBox) field).addActionListener(this);
        }

        // Add all complementary components for this dialog
        addComponentsPnl();
        add(pnlLocal);
        pack();
        setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());

    }

    public void actionPerformed(ActionEvent event) {

        Object src = event.getSource();

        if (src == btnOK) {
            super.actionPerformed(event);
            return;
        }

        if (src == btnCancel) {
            dispose();
            return;
        }

        if (src == comboPeakList) {
            PeakList selectedPeakList = (PeakList) comboPeakList.getSelectedItem();
            PeakListRow peaks[] = selectedPeakList.getRows();
            comboPeak.removeActionListener(this);
            comboPeak.removeAllItems();
            for (PeakListRow peak : peaks)
                comboPeak.addItem(peak);
            comboPeak.addActionListener(this);
            comboPeak.setSelectedIndex(0);
            return;
        }

        if (src == preview) {
            if (preview.isSelected()) {
                pnlLocal.add(pnlPlotXY, BorderLayout.CENTER);
                pnlVisible.add(pnlLab, BorderLayout.WEST);
                pnlVisible.add(pnlFlds, BorderLayout.CENTER);
                pack();
                PeakList selected[] = MZmineCore.getDesktop().getSelectedPeakLists();
                if (selected.length > 0)
                    comboPeakList.setSelectedItem(selected[0]);
                else
                    comboPeakList.setSelectedIndex(0);
                setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
                this.setResizable(true);
            } else {
                pnlLocal.remove(pnlPlotXY);
                pnlVisible.remove(pnlLab);
                pnlVisible.remove(pnlFlds);
                pack();
                setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
                this.setResizable(false);
            }
            return;
        }

        // Any other event will cause reloading the preview
        if (preview.isSelected()) {
            loadPreviewPeak();
        }

    }

    public void propertyChange(PropertyChangeEvent e) {
        if (preview.isSelected()) {
            loadPreviewPeak();
        }
    }

    private void loadPreviewPeak() {

        PeakListRow previewRow = (PeakListRow) comboPeak.getSelectedItem();
        if (previewRow == null)
            return;
        logger.finest("Loading new preview peak " + previewRow);
        ChromatographicPeak previewPeak = previewRow.getPeaks()[0];

        int dataSetCount = ticPlot.getXYPlot().getDatasetCount();
        for (int index = 0; index < dataSetCount; index++) {
            ticPlot.getXYPlot().setDataset(index, null);
        }
        ticPlot.startDatasetCounter();

        // Create Peak Builder
        PeakResolver peakResolver;
        pbParameters = buildParameterSet(pbParameters);
        String peakResolverClassName = PeakRecognitionParameters.peakResolverClasses[peakResolverTypeNumber];

        try {
            Class peakResolverClass = Class.forName(peakResolverClassName);
            Constructor peakResolverConstruct = peakResolverClass.getConstructors()[0];
            peakResolver = (PeakResolver) peakResolverConstruct.newInstance(pbParameters);
        } catch (Exception e) {
            String message = "Error trying to make an instance of Peak Builder "
                    + peakResolverClassName;
            MZmineCore.getDesktop().displayErrorMessage(message);
            logger.severe(message);
            return;
        }

        // Load the intensities into array
        RawDataFile dataFile = previewPeak.getDataFile();
        int scanNumbers[] = dataFile.getScanNumbers(1);
        double retentionTimes[] = new double[scanNumbers.length];
        for (int i = 0; i < scanNumbers.length; i++)
            retentionTimes[i] = dataFile.getScan(scanNumbers[i]).getRetentionTime();
        double intensities[] = new double[scanNumbers.length];
        for (int i = 0; i < scanNumbers.length; i++) {
            MzPeak mzPeak = previewPeak.getMzPeak(scanNumbers[i]);
            if (mzPeak != null)
                intensities[i] = mzPeak.getIntensity();
            else
                intensities[i] = 0;
        }
        ChromatographicPeak[] resolvedPeaks = peakResolver.resolvePeaks(
                previewPeak, scanNumbers, retentionTimes, intensities);

        for (int i = 0; i < resolvedPeaks.length; i++) {

            PeakDataSet peakDataSet = new PeakDataSet(resolvedPeaks[i]);
            ticPlot.addPeakDataset(peakDataSet);

            if (i > 30) {
                String message = "Too many peaks detected, please adjust parameter values";
                MZmineCore.getDesktop().displayMessage(message);
                break;
            }

        }

        ticDataset = new ChromatogramTICDataSet(previewRow.getPeaks()[0]);
        ticPlot.addTICDataset(ticDataset);

        // Set auto range to axes
        ticPlot.getXYPlot().getDomainAxis().setAutoRange(true);
        ticPlot.getXYPlot().getDomainAxis().setAutoTickUnitSelection(true);
        ticPlot.getXYPlot().getRangeAxis().setAutoRange(true);
        ticPlot.getXYPlot().getRangeAxis().setAutoTickUnitSelection(true);

    }

    /**
     * This function add all the additional components for this dialog over the
     * original ParameterSetupDialog.
     * 
     */
    private void addComponentsPnl() {

        PeakList peakLists[] = MZmineCore.getCurrentProject().getPeakLists();

        // Elements of pnlpreview
        JPanel pnlpreview = new JPanel(new BorderLayout());

        preview = new JCheckBox(" Show preview of peak building ");
        preview.addActionListener(this);
        preview.setHorizontalAlignment(SwingConstants.CENTER);
        preview.setEnabled(peakLists.length > 0);

        pnlpreview.add(new JSeparator(), BorderLayout.NORTH);
        pnlpreview.add(preview, BorderLayout.CENTER);
        pnlpreview.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);

        // Elements of pnlLab
        pnlLab = new JPanel();
        pnlLab.setLayout(new BoxLayout(pnlLab, BoxLayout.Y_AXIS));
        pnlLab.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JLabel lblFileSelected = new JLabel("Peak list ");
        JLabel lblRetentionTime = new JLabel("Peak");

        pnlLab.add(lblFileSelected);
        pnlLab.add(Box.createVerticalStrut(25));
        pnlLab.add(lblRetentionTime);

        // Elements of pnlFlds
        pnlFlds = new JPanel();
        pnlFlds.setLayout(new BoxLayout(pnlFlds, BoxLayout.Y_AXIS));
        pnlFlds.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        comboPeakList = new JComboBox();
        for (PeakList peakList : peakLists) {
            if (peakList.getNumberOfRawDataFiles() == 1)
                comboPeakList.addItem(peakList);
        }

        comboPeakList.setBackground(Color.WHITE);
        comboPeakList.setFont(comboFont);
        comboPeakList.addActionListener(this);

        comboPeak = new JComboBox();
        comboPeak.setBackground(Color.WHITE);
        comboPeak.setFont(comboFont);
        comboPeak.setRenderer(new PeakPreviewComboRenderer());

        pnlFlds.add(comboPeakList);
        pnlFlds.add(Box.createVerticalStrut(10));
        pnlFlds.add(comboPeak);
        pnlFlds.add(Box.createVerticalStrut(10));

        // Put all together
        pnlVisible = new JPanel(new BorderLayout());
        pnlVisible.add(pnlpreview, BorderLayout.NORTH);

        // Panel for XYPlot
        pnlPlotXY = new JPanel(new BorderLayout());
        Border one = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
        Border two = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        pnlPlotXY.setBorder(BorderFactory.createCompoundBorder(one, two));
        pnlPlotXY.setBackground(Color.white);

        ticPlot = new TICPlot((ActionListener) this);
        pnlPlotXY.add(ticPlot, BorderLayout.CENTER);

        toolBar = new TICToolBar(ticPlot);
        toolBar.getComponentAtIndex(0).setVisible(false);
        pnlPlotXY.add(toolBar, BorderLayout.EAST);

        pnlAll.add(pnlVisible, BorderLayout.CENTER);

        // Complete panel for this dialog including pnlPlotXY
        pnlLocal = new JPanel(new BorderLayout());
        pnlLocal.add(pnlAll, BorderLayout.WEST);
    }

}