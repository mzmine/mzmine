/*
 * Copyright 2006-2007 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.identification.custom;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.NumberFormat;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.components.DragOrderedJList;
import net.sf.mzmine.util.dialogs.ExitCode;

/**
 * 
 */
class CustomDBSearchDialog extends JDialog implements ActionListener {

    public static final int TEXTFIELD_COLUMNS = 12;

    private ExitCode exitCode = ExitCode.UNKNOWN;

    private Desktop desktop;
    private CustomDBSearchParameters parameters;

    // Dialog controles
    private JButton btnOK, btnCancel, btnBrowse;
    private JTextField fileNameField, fieldSeparatorField;
    private JFormattedTextField mzToleranceField, rtToleranceField;
    private JCheckBox ignoreFirstLineBox;

    private DefaultListModel fieldOrderModel;
    private DragOrderedJList fieldOrderList;

    /**
     * Constructor
     */
    public CustomDBSearchDialog(CustomDBSearchParameters parameters) {

        // Make dialog modal
        super(MZmineCore.getDesktop().getMainFrame(), "DB search parameters",
                true);

        this.parameters = parameters;

        // Create panel with controls
        JPanel pnlLabelsAndFields = new JPanel(new GridLayout(7, 2));

        // Database file name
        GUIUtils.addLabel(pnlLabelsAndFields, "Database file");
        fileNameField = new JTextField(TEXTFIELD_COLUMNS);
        fileNameField.setText(parameters.getDataBaseFile());
        JPanel fileNamePanel = new JPanel();
        fileNamePanel.add(fileNameField);
        btnBrowse = GUIUtils.addButton(fileNamePanel, "Browse..", null, this);
        pnlLabelsAndFields.add(fileNamePanel);

        // Field separator
        GUIUtils.addLabel(pnlLabelsAndFields, "Field separator");
        fieldSeparatorField = new JTextField(TEXTFIELD_COLUMNS);
        fieldSeparatorField.setText(String.valueOf(parameters.getFieldSeparator()));
        pnlLabelsAndFields.add(fieldSeparatorField);

        // Field order
        GUIUtils.addLabel(pnlLabelsAndFields, "Field order");
        fieldOrderModel = new DefaultListModel();
        for (Object item : parameters.getFieldOrder())
            fieldOrderModel.addElement(item);
        fieldOrderList = new DragOrderedJList(fieldOrderModel);
        pnlLabelsAndFields.add(fieldOrderList);

        // Ignore first line
        GUIUtils.addLabel(pnlLabelsAndFields, "Ignore first line");
        ignoreFirstLineBox = new JCheckBox();
        ignoreFirstLineBox.setSelected(parameters.isIgnoreFirstLine());
        pnlLabelsAndFields.add(ignoreFirstLineBox);

        // m/z tolerance
        GUIUtils.addLabel(pnlLabelsAndFields, "m/z tolerance");
        NumberFormat mzFormat = MZmineCore.getMZFormat();
        mzToleranceField = new JFormattedTextField(mzFormat);
        mzToleranceField.setValue((Double) parameters.getMzTolerance());
        pnlLabelsAndFields.add(mzToleranceField);

        // Retention time tolerance
        GUIUtils.addLabel(pnlLabelsAndFields, "Retention time tolerance (sec)");
        NumberFormat rtFormat = MZmineCore.getRTFormat();
        rtToleranceField = new JFormattedTextField(rtFormat);
        rtToleranceField.setValue((Double) parameters.getRtTolerance());
        pnlLabelsAndFields.add(rtToleranceField);

        // Buttons
        JPanel pnlButtons = new JPanel();
        btnOK = GUIUtils.addButton(pnlButtons, "OK", null, this);
        btnCancel = GUIUtils.addButton(pnlButtons, "Cancel", null, this);

        // Put everything into a main panel
        JPanel pnlAll = new JPanel(new BorderLayout());
        GUIUtils.addMargin(pnlAll, 10);
        pnlAll.add(pnlLabelsAndFields, BorderLayout.CENTER);
        pnlAll.add(pnlButtons, BorderLayout.SOUTH);
        add(pnlAll);

        pack();

        setResizable(false);
        setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());

    }

    /**
     * Implementation for ActionListener interface
     */
    public void actionPerformed(ActionEvent ae) {

        Object src = ae.getSource();

        if (src == btnOK) {

            String dataFilePath = fileNameField.getText();
            if (dataFilePath.length() == 0) {
                desktop.displayErrorMessage("Please select database file");
                return;
            }

            File dataFile = new File(dataFilePath);
            if (!dataFile.exists()) {
                desktop.displayErrorMessage("File " + dataFile
                        + " does not exist");
                return;
            }

            parameters.setDataBaseFile(dataFilePath);
            parameters.setFieldSeparator(fieldSeparatorField.getText().charAt(0));
            parameters.setFieldOrder(fieldOrderModel.toArray());
            parameters.setIgnoreFirstLine(ignoreFirstLineBox.isSelected());
            parameters.setMzTolerance(((Number) mzToleranceField.getValue()).doubleValue());
            parameters.setRtTolerance(((Number) rtToleranceField.getValue()).doubleValue());

            exitCode = ExitCode.OK;
            dispose();

        }

        if (src == btnBrowse) {

            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setMultiSelectionEnabled(false);

            int returnVal = chooser.showOpenDialog(desktop.getMainFrame());

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                String selectedFile = chooser.getSelectedFile().getAbsolutePath();
                fileNameField.setText(selectedFile);
            }

        }

        if (src == btnCancel) {
            exitCode = ExitCode.CANCEL;
            dispose();
        }

    }

    /**
     * Method for reading exit code
     * 
     */
    public ExitCode getExitCode() {
        return exitCode;
    }

}
