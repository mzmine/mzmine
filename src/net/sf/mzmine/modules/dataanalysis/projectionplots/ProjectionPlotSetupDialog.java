package net.sf.mzmine.modules.dataanalysis.projectionplots;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.AbstractTableModel;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.dataanalysis.intensityplot.IntensityPlotParameters;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.components.ExtendedCheckBox;
import net.sf.mzmine.userinterface.dialogs.ExitCode;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.PeakListRowSorterByMZ;

public class ProjectionPlotSetupDialog extends JDialog implements ActionListener {

	private static final Color[] colors = { Color.lightGray, Color.red, Color.green, Color.blue, Color.magenta, Color.orange};

    static final int PADDING_SIZE = 5;

    private ExitCode exitCode = ExitCode.CANCEL;

    private Desktop desktop;
    private PeakList alignedPeakList;
    private ProjectionPlotParameters parameterSet;

    // dialog components
    
    private JComboBox comboColoringMethod;
    private JComboBox comboPeakMeasuringMethod;
    
    private ExtendedCheckBox<RawDataFile> rawDataFileCheckBoxes[];
    private ExtendedCheckBox<PeakListRow> peakCheckBoxes[];
    private JButton btnSelectAllFiles, btnDeselectAllFiles, btnSelectAllPeaks,
            btnDeselectAllPeaks, btnOK, btnCancel;

    public ProjectionPlotSetupDialog(PeakList peakList,
            ProjectionPlotParameters parameterSet) {

        // make dialog modal
        super(MZmineCore.getDesktop().getMainFrame(), "Projection plot setup",
                true);

        this.desktop = MZmineCore.getDesktop();
        this.alignedPeakList = peakList;
        this.parameterSet = parameterSet;

        List<RawDataFile> selectedDataFiles = Arrays.asList(parameterSet.getSelectedDataFiles());
        List<PeakListRow> selectedRows = Arrays.asList(parameterSet.getSelectedRows());

        GridBagConstraints constraints = new GridBagConstraints();

        // set default layout constraints
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(PADDING_SIZE, PADDING_SIZE,
                PADDING_SIZE, PADDING_SIZE);

        constraints.gridwidth = 1;
        constraints.gridheight = 1;

        JComponent comp;
        GridBagLayout layout = new GridBagLayout();

        JPanel components = new JPanel(layout);

        comp = GUIUtils.addLabel(components, "Data files");
        constraints.gridx = 0;
        constraints.gridy = 0;
        layout.setConstraints(comp, constraints);

        JPanel dataFileCheckBoxesPanel = new JPanel();
        dataFileCheckBoxesPanel.setBackground(Color.white);
        dataFileCheckBoxesPanel.setLayout(new BoxLayout(
                dataFileCheckBoxesPanel, BoxLayout.Y_AXIS));
        rawDataFileCheckBoxes = new ExtendedCheckBox[alignedPeakList.getNumberOfRawDataFiles()];
        int minimumHorizSize = 0;
        RawDataFile files[] = alignedPeakList.getRawDataFiles();
        for (int i = 0; i < files.length; i++) {
            rawDataFileCheckBoxes[i] = new ExtendedCheckBox<RawDataFile>(
                    files[i], selectedDataFiles.contains(files[i]));
            minimumHorizSize = Math.max(minimumHorizSize,
                    rawDataFileCheckBoxes[i].getPreferredWidth());
            dataFileCheckBoxesPanel.add(rawDataFileCheckBoxes[i]);
        }
        int minimumVertSize = (int) rawDataFileCheckBoxes[0].getPreferredSize().getHeight() * 3;
        JScrollPane dataFilePanelScroll = new JScrollPane(
                dataFileCheckBoxesPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        dataFilePanelScroll.setPreferredSize(new Dimension(minimumHorizSize,
                minimumVertSize));
        constraints.gridx = 1;
        components.add(dataFilePanelScroll, constraints);

        JPanel dataFileButtonsPanel = new JPanel();
        dataFileButtonsPanel.setLayout(new BoxLayout(dataFileButtonsPanel,
                BoxLayout.Y_AXIS));
        btnSelectAllFiles = GUIUtils.addButton(dataFileButtonsPanel,
                "Select all", null, this);
        btnDeselectAllFiles = GUIUtils.addButton(dataFileButtonsPanel,
                "Deselect all", null, this);
        dataFileButtonsPanel.add(Box.createGlue());
        constraints.gridx = 2;
        components.add(dataFileButtonsPanel);
        layout.setConstraints(dataFileButtonsPanel, constraints);

        comp = GUIUtils.addLabel(components, "Coloring style");
        constraints.gridx = 0;
        constraints.gridy = 1;
        layout.setConstraints(comp, constraints);

        Parameter projectParameters[] = MZmineCore.getCurrentProject().getParameters();
        Object availableColoringStyles[] = new Object[projectParameters.length + 2];
        availableColoringStyles[0] = ProjectionPlotParameters.ColoringSingleOption;
        availableColoringStyles[1] = ProjectionPlotParameters.ColoringByFileOption;
        for (int i = 0; i < projectParameters.length; i++)
        	availableColoringStyles[i + 2] = "Color by parameter " + projectParameters[i].getName();
        comboColoringMethod = new JComboBox(availableColoringStyles);
        if (parameterSet.getColoringMode() == parameterSet.ColoringSingleOption)
        	comboColoringMethod.setSelectedItem(availableColoringStyles[0]);
        if (parameterSet.getColoringMode() == parameterSet.ColoringByFileOption)
        	comboColoringMethod.setSelectedItem(availableColoringStyles[0]);
        if ( (parameterSet.getColoringMode() == parameterSet.ColoringByParameterValueOption) &&
        	 (parameterSet.getSelectedParameter()!=null) ) 
        	comboColoringMethod.setSelectedItem(parameterSet.getSelectedParameter());
        constraints.gridx = 1;
        components.add(comboColoringMethod, constraints);

        comp = GUIUtils.addLabel(components, "Peak measuring approach");
        constraints.gridx = 0;
        constraints.gridy = 2;
        layout.setConstraints(comp, constraints);

        String availablePeakMeasuringStyles[] = new String[] {
                ProjectionPlotParameters.PeakHeightOption,
                ProjectionPlotParameters.PeakAreaOption };
        comboPeakMeasuringMethod = new JComboBox(availablePeakMeasuringStyles);
        if (parameterSet.getPeakMeasuringMode() == parameterSet.PeakHeightOption)
        	comboPeakMeasuringMethod.setSelectedItem(parameterSet.PeakHeightOption);
        if (parameterSet.getPeakMeasuringMode() == parameterSet.PeakAreaOption)
        	comboPeakMeasuringMethod.setSelectedItem(parameterSet.PeakAreaOption);
        
        constraints.gridx = 1;
        components.add(comboPeakMeasuringMethod, constraints);

        comp = GUIUtils.addLabel(components, "Peaks");
        constraints.gridx = 0;
        constraints.gridy = 3;
        layout.setConstraints(comp, constraints);

        JPanel peakCheckBoxesPanel = new JPanel();
        peakCheckBoxesPanel.setBackground(Color.white);
        peakCheckBoxesPanel.setLayout(new BoxLayout(peakCheckBoxesPanel,
                BoxLayout.Y_AXIS));
        peakCheckBoxes = new ExtendedCheckBox[alignedPeakList.getNumberOfRows()];
        minimumHorizSize = 0;
        PeakListRow rows[] = alignedPeakList.getRows();
        Arrays.sort(rows, new PeakListRowSorterByMZ());
        for (int i = 0; i < rows.length; i++) {
            peakCheckBoxes[i] = new ExtendedCheckBox<PeakListRow>(rows[i],
                    selectedRows.contains(rows[i]));
            minimumHorizSize = Math.max(minimumHorizSize,
                    peakCheckBoxes[i].getPreferredWidth());
            peakCheckBoxesPanel.add(peakCheckBoxes[i]);
        }
        minimumVertSize = (int) peakCheckBoxes[0].getPreferredSize().getHeight() * 6;
        JScrollPane peakPanelScroll = new JScrollPane(peakCheckBoxesPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        peakPanelScroll.setPreferredSize(new Dimension(minimumHorizSize,
                minimumVertSize));
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.weighty = 1;
        components.add(peakPanelScroll, constraints);

        JPanel peakButtonsPanel = new JPanel();
        peakButtonsPanel.setLayout(new BoxLayout(peakButtonsPanel,
                BoxLayout.Y_AXIS));
        btnSelectAllPeaks = GUIUtils.addButton(peakButtonsPanel, "Select all",
                null, this);
        btnDeselectAllPeaks = GUIUtils.addButton(peakButtonsPanel,
                "Deselect all", null, this);
        peakButtonsPanel.add(Box.createGlue());
        constraints.gridx = 2;
        components.add(peakButtonsPanel);
        layout.setConstraints(peakButtonsPanel, constraints);

        comp = GUIUtils.addSeparator(components, PADDING_SIZE);
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.gridheight = 1;
        constraints.gridwidth = 3;
        layout.setConstraints(comp, constraints);

        JPanel buttonsPanel = new JPanel();
        btnOK = GUIUtils.addButton(buttonsPanel, "OK", null, this);
        btnCancel = GUIUtils.addButton(buttonsPanel, "Cancel", null, this);
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.gridwidth = 3;
        components.add(buttonsPanel, constraints);

        GUIUtils.addMargin(components, PADDING_SIZE);
        add(components);

        // finalize the dialog
        pack();
        setLocationRelativeTo(desktop.getMainFrame());
        setResizable(true);

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {

        Object src = event.getSource();

        if (src == btnOK) {

            Vector<RawDataFile> selectedFiles = new Vector<RawDataFile>();
            Vector<PeakListRow> selectedPeaks = new Vector<PeakListRow>();

            for (ExtendedCheckBox<RawDataFile> box : rawDataFileCheckBoxes) {
                if (box.isSelected())
                    selectedFiles.add(box.getObject());
            }

            for (ExtendedCheckBox<PeakListRow> box : peakCheckBoxes) {
                if (box.isSelected())
                    selectedPeaks.add(box.getObject());
            }

            if (selectedFiles.size() == 0) {
                desktop.displayErrorMessage("Please select at least one data file");
                return;
            }

            if (selectedPeaks.size() == 0) {
                desktop.displayErrorMessage("Please select at least one peak");
                return;
            }

            parameterSet.setSourcePeakList(alignedPeakList);
            parameterSet.setSelectedDataFiles(selectedFiles.toArray(new RawDataFile[0]));
            parameterSet.setSelectedRows(selectedPeaks.toArray(new PeakListRow[0]));

            if (comboColoringMethod.getSelectedItem()==parameterSet.ColoringSingleOption)
            	parameterSet.setColoringMode(parameterSet.ColoringSingleOption);
            if (comboColoringMethod.getSelectedItem()==parameterSet.ColoringByFileOption)
            	parameterSet.setColoringMode(parameterSet.ColoringByFileOption);
            if (comboColoringMethod.getSelectedIndex()>1) {
            	parameterSet.setColoringMode(parameterSet.ColoringByParameterValueOption);
            	parameterSet.setSelectedParameter((Parameter)comboColoringMethod.getSelectedItem());
            }
            parameterSet.setPeakMeasuringMode(comboPeakMeasuringMethod.getSelectedItem());

            exitCode = ExitCode.OK;
            dispose();
            return;
        }

        if (src == btnCancel) {
            exitCode = ExitCode.CANCEL;
            dispose();
            return;
        }

        if (src == btnSelectAllFiles) {
            for (JCheckBox box : rawDataFileCheckBoxes)
                box.setSelected(true);
            return;
        }

        if (src == btnDeselectAllFiles) {
            for (JCheckBox box : rawDataFileCheckBoxes)
                box.setSelected(false);
            return;
        }

        if (src == btnSelectAllPeaks) {
            for (JCheckBox box : peakCheckBoxes)
                box.setSelected(true);
            return;
        }

        if (src == btnDeselectAllPeaks) {
            for (JCheckBox box : peakCheckBoxes)
                box.setSelected(false);
            return;
        }

    }

    public ExitCode getExitCode() {
        return exitCode;
    }

}
