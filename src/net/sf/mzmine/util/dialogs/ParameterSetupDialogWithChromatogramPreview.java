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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.sf.mzmine.util.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.tic.TICDataSet;
import net.sf.mzmine.modules.visualization.tic.TICPlot;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.components.ExtendedCheckBox;
import org.jfree.chart.title.LegendTitle;

public class ParameterSetupDialogWithChromatogramPreview extends ParameterSetupDialog
        implements ActionListener, PropertyChangeListener {

    protected SimpleParameterSet TICparameters;
    protected RawDataFile[] selectedFiles;
    private JCheckBox preview;
    protected TICPlot ticPlot;
    private JPanel pnlPlotXY,  pnlFileNameScanNumber;
    protected JPanel components;
    protected List<RawDataFile> rawDataList;
    private List<TICDataSet> ticDataSets;
    protected LegendTitle legend;

    public ParameterSetupDialogWithChromatogramPreview(String name,
            SimpleParameterSet parameters, String helpFile) {
        super(name, parameters, helpFile);

        // Parameters for the visualization       
        TICparameters = new DialogWithChromatogramParameters();
        
        this.ticDataSets = new ArrayList<TICDataSet>();
        this.rawDataList = new ArrayList<RawDataFile>();

        selectedFiles = MZmineCore.getDesktop().getSelectedDataFiles();
        if (selectedFiles.length != 0) {
            TICparameters.setMultipleSelection(DialogWithChromatogramParameters.dataFiles,
                    MZmineCore.getCurrentProject().getDataFiles());
            TICparameters.setParameterValue(DialogWithChromatogramParameters.dataFiles,
                    selectedFiles);
            addActionListener(parameters);
        }

        addComponents();

        // Add the listener for each component of the parameters
        addActionListener(TICparameters);
    }
    

    /**
     * Set a listener in all parameters's fields to add functionality to this dialog
     *
     */
    protected void addActionListener(SimpleParameterSet parameters) {
        try {
            for (Parameter p : parameters.getParameters()) {

                JComponent field = getComponentForParameter(p);
                field.addPropertyChangeListener("value", this);
                if (field instanceof JCheckBox) {
                    ((JCheckBox) field).addActionListener(this);
                }
                if (field instanceof JComboBox) {
                    ((JComboBox) field).addActionListener(this);
                }
                if (field instanceof JPanel) {
                    Component[] panelComponents = field.getComponents();
                    for (Component component : panelComponents) {
                        if (component instanceof JTextField) {
                            component.addPropertyChangeListener("value", this);
                        }
                    }
                }
                if (field instanceof JScrollPane) {

                    Component[] panelComponents = field.getComponents();
                    for (Component component : panelComponents) {

                        if (component instanceof JViewport) {

                            Component[] childComponents = ((JViewport) component).getComponents();
                            JPanel panel = (JPanel) childComponents[0];

                            for (Component childs : panel.getComponents()) {
                                if (childs instanceof JCheckBox) {

                                    ((JCheckBox) childs).addActionListener(this);
                                }
                            }
                        }
                    }
                }

            }
        } catch (Exception e) {
        }
    }

    /**
     * This function add all the additional components for this dialog over the
     * original ParameterSetupDialog.
     *
     */
    protected void addComponents() {

        // Elements of pnlpreview
        JPanel pnlpreview = new JPanel(new BorderLayout());

        preview = new JCheckBox(" Show preview of raw data filter ");
        preview.addActionListener(this);
        preview.setHorizontalAlignment(SwingConstants.CENTER);
        pnlpreview.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);

        pnlpreview.add(new JSeparator(), BorderLayout.NORTH);
        pnlpreview.add(preview, BorderLayout.CENTER);

        components = addDialogComponents();
        pnlFileNameScanNumber = new JPanel(new BorderLayout());
        pnlFileNameScanNumber.add(pnlpreview, BorderLayout.NORTH);
        pnlFileNameScanNumber.add(components, BorderLayout.SOUTH);
        pnlFileNameScanNumber.setVisible(false);

        JPanel pnlVisible = new JPanel(new BorderLayout());
        pnlVisible.add(pnlpreview, BorderLayout.NORTH);

        JPanel tmp = new JPanel();
        tmp.add(pnlFileNameScanNumber);
        pnlVisible.add(tmp, BorderLayout.CENTER);

        // Panel for XYPlot
        pnlPlotXY = new JPanel(new BorderLayout());
        Border one = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
        Border two = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        pnlPlotXY.setBorder(BorderFactory.createCompoundBorder(one, two));
        pnlPlotXY.setBackground(Color.white);

        ticPlot = new TICPlot(this);
        this.legend = ticPlot.getChart().getLegend();

        pnlPlotXY.add(ticPlot, BorderLayout.CENTER);
        componentsPanel.add(pnlVisible, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
    }

    private JPanel addDialogComponents() {

        JComponent component[] = new JComponent[TICparameters.getParameters().length * 3];
        int componentCounter = 0;

        // Create labels and components for each parameter
        for (Parameter p : TICparameters.getParameters()) {
            JLabel label = new JLabel(p.getName());
            component[componentCounter++] = label;
            JComponent comp = null;
            if (p.getType() == ParameterType.MULTIPLE_SELECTION) {
                comp = createMultipleSelectionComponent(p);
            } else {
                comp = createComponentForParameter(p);
            }
            comp.setToolTipText(p.getDescription());
            label.setLabelFor(comp);

            parametersAndComponents.put(p, comp);

            component[componentCounter++] = comp;

            String unitStr = "";
            if (p.getUnits() != null) {
                unitStr = p.getUnits();
            }
            component[componentCounter++] = new JLabel(unitStr);
            setComponentValue(p, TICparameters.getParameterValue(p));

        }

        // Panel collecting all labels, fields and units
        JPanel labelsAndFields = GUIUtils.makeTablePanel(TICparameters.getParameters().length, 3, 1, component);

        return labelsAndFields;
    }

    private JComponent createMultipleSelectionComponent(Parameter p) {

        JComponent comp = null;

        JPanel checkBoxesPanel = new JPanel();
        checkBoxesPanel.setBackground(Color.white);
        checkBoxesPanel.setLayout(new BoxLayout(checkBoxesPanel,
                BoxLayout.Y_AXIS));


        int vertSize = 0,
                numCheckBoxes = 0;
        ExtendedCheckBox<Object> ecb = null;
        Object multipleValues[] = TICparameters.getMultipleSelection(p);
        if (multipleValues == null) {
            multipleValues = p.getPossibleValues();
        }
        if (multipleValues == null) {
            multipleValues = new Object[0];
        }

        for (Object genericObject : multipleValues) {

            ecb = new ExtendedCheckBox<Object>(genericObject, false);
            ecb.setAlignmentX(Component.LEFT_ALIGNMENT);
            checkBoxesPanel.add(ecb);

            if (numCheckBoxes < 7) {
                vertSize += (int) ecb.getPreferredSize().getHeight() + 2;
            }

            numCheckBoxes++;
        }

        if (numCheckBoxes < 3) {
            vertSize += 30;
        }

        JScrollPane peakPanelScroll = new JScrollPane(checkBoxesPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        peakPanelScroll.setPreferredSize(new Dimension(0, vertSize));
        comp = peakPanelScroll;

        return comp;

    }

    /**
     * Set the value of the parameters from their correspondent component
     */
    protected void updateParameterValue() {
        try {
            for (Parameter p : TICparameters.getParameters()) {
                Object value = getComponentValue(p);
                if (value != null) {
                    TICparameters.setParameterValue(p, value);
                }
            }
        } catch (Exception ex) {
        }
    }

    /**
     * Remove all the data files from the plot and add the new selected data files to the plot using the function loadPreview()
     */
    private void reloadPreview() {

        updateParameterValue();

        Object dataFileObjects[] = (Object[]) TICparameters.getParameterValue(DialogWithChromatogramParameters.dataFiles);

        RawDataFile selectedDataFiles[] = CollectionUtils.changeArrayType(
                dataFileObjects, RawDataFile.class);

        Boolean originalData = (Boolean) TICparameters.getParameterValue(DialogWithChromatogramParameters.originalRawData);

       
        for (int index = 0; index < rawDataList.size(); index++) {
            if (!isCheckBoxSelected(selectedDataFiles, rawDataList.get(index)) || !originalData) {
                ticPlot.getXYPlot().setDataset(index,
                        null);
            }
        }

        for (RawDataFile dataFile : selectedDataFiles) {
            loadPreview(dataFile);
        }

    }

    private boolean isCheckBoxSelected(RawDataFile selectedDataFiles[], RawDataFile dataFile) {
        for (RawDataFile selectedDataFile : selectedDataFiles) {
            if (selectedDataFile.equals(dataFile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the parameters related to the plot and call the function addRawDataFile() to add the data file to the plot
     * @param dataFile
     */
    protected void loadPreview(RawDataFile dataFile) {
        Range rtRange = (Range) TICparameters.getParameterValue(DialogWithChromatogramParameters.retentionTimeRange);
        Range mzRange = (Range) TICparameters.getParameterValue(DialogWithChromatogramParameters.mzRange);

        Boolean setLegend = (Boolean) TICparameters.getParameterValue(DialogWithChromatogramParameters.plotLegend);

        if (!setLegend) {
            legend.setVisible(false);
        } else {
            legend.setVisible(true);
        }

        int level = (Integer) TICparameters.getParameterValue(DialogWithChromatogramParameters.msLevel);
        this.addRawDataFile(dataFile, level, mzRange, rtRange);

    }

    /**
     * Add the data file into the plot
     * @param newFile
     * @param level
     * @param mzRange
     * @param rtRange
     */
    protected void addRawDataFile(RawDataFile newFile, int level, Range mzRange, Range rtRange) {
        int scanNumbers[] = newFile.getScanNumbers(level, rtRange);

        TICDataSet ticDataset = new TICDataSet(newFile, scanNumbers, mzRange, this);

        if (!rawDataList.contains(newFile)) {
            ticDataSets.add(ticDataset);
            rawDataList.add(newFile);
            ticPlot.addTICDataset(ticDataset);
        } else {
            int index = rawDataList.indexOf(newFile);
            ticPlot.getXYPlot().setDataset(index,
                    ticDataset);
        }

        setRTRange(rtRange);
    }

    public void setRTRange(Range rtRange) {
        ticPlot.getXYPlot().getDomainAxis().setRange(rtRange.getMin(),
                rtRange.getMax());
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (preview.isSelected()) {
            reloadPreview();
        }
    }

    public void actionPerformed(ActionEvent event) {

        super.actionPerformed(event);

        Object src = event.getSource();

        if (src == preview) {
            if (preview.isSelected()) {
                mainPanel.add(pnlPlotXY, BorderLayout.CENTER);
                reloadPreview();
                pnlFileNameScanNumber.setVisible(true);
                pack();
                this.setResizable(true);
                setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
            } else {
                mainPanel.remove(pnlPlotXY);
                pnlFileNameScanNumber.setVisible(false);
                this.setResizable(false);
                pack();
                setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
            }
        }

        if (((src instanceof JCheckBox) && (src != preview)) || ((src instanceof JComboBox))) {
            if (preview.isSelected()) {
                reloadPreview();
            }
        }

    }
}
