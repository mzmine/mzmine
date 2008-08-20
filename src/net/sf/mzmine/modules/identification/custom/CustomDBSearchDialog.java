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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

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

		JPanel pnlLabelsAndFields = new JPanel();
		pnlLabelsAndFields.setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weighty = 10.0;
		c.weightx = 10.0;
		c.insets = new Insets(5, 5, 5, 5);

		c.gridx = 0;
		c.gridy = 0;
		pnlLabelsAndFields.add(new JLabel("Database file"), c);
		c.gridx = 1;
        fileNameField = new JTextField(TEXTFIELD_COLUMNS);
        fileNameField.setText(parameters.getDataBaseFile());
        JPanel fileNamePanel = new JPanel();
        fileNamePanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        fileNamePanel.add(fileNameField);
        btnBrowse = GUIUtils.addButton(fileNamePanel, "Browse..", null, this);
		pnlLabelsAndFields.add(fileNamePanel, c);

		c.gridx = 0;
		c.gridy = 1;
		pnlLabelsAndFields.add(new JLabel("Field separator"), c);
		c.gridx = 1;
        fieldSeparatorField = new JTextField(TEXTFIELD_COLUMNS);
        fieldSeparatorField.setText(String.valueOf(parameters.getFieldSeparator()));
		pnlLabelsAndFields.add(fieldSeparatorField, c);

		c.gridx = 0;
		c.gridy = 2;
		pnlLabelsAndFields.add(new JLabel("Field order"), c);
		c.gridx = 1;
        fieldOrderModel = new DefaultListModel();
        for (Object item : parameters.getFieldOrder())
            fieldOrderModel.addElement(item);
        fieldOrderList = new DragOrderedJList(fieldOrderModel);
		JScrollPane listScroller = new JScrollPane(fieldOrderList);
		listScroller.setAlignmentX(LEFT_ALIGNMENT);
		JPanel listPanel = new JPanel();
		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.PAGE_AXIS));
		listPanel.add(listScroller);
		listPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		pnlLabelsAndFields.add(listPanel, c);

		c.gridx = 0;
		c.gridy = 3;
		pnlLabelsAndFields.add(new JLabel("Ignore first line"), c);
		c.gridx = 1;
        ignoreFirstLineBox = new JCheckBox();
        ignoreFirstLineBox.setSelected(parameters.isIgnoreFirstLine());
		pnlLabelsAndFields.add(ignoreFirstLineBox, c);

		c.gridx = 0;
		c.gridy = 4;
		pnlLabelsAndFields.add(new JLabel("m/z tolerance"), c);
		c.gridx = 1;
        NumberFormat mzFormat = MZmineCore.getMZFormat();
        mzToleranceField = new JFormattedTextField(mzFormat);
        mzToleranceField.setValue((Double) parameters.getMzTolerance());
		pnlLabelsAndFields.add(mzToleranceField, c);

		c.gridx = 0;
		c.gridy = 5;
		pnlLabelsAndFields.add(new JLabel("Retention time tolerance (sec)"), c);
		c.gridx = 1;
        NumberFormat rtFormat = MZmineCore.getRTFormat();
        rtToleranceField = new JFormattedTextField(rtFormat);
        rtToleranceField.setValue((Double) parameters.getRtTolerance());
		pnlLabelsAndFields.add(rtToleranceField, c);


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
                MZmineCore.getDesktop().displayErrorMessage("Please select database file");
                return;
            }

            File dataFile = new File(dataFilePath);
            if (!dataFile.exists()) {
                MZmineCore.getDesktop().displayErrorMessage("File " + dataFile
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

            int returnVal = chooser.showOpenDialog(MZmineCore.getDesktop().getMainFrame());

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
