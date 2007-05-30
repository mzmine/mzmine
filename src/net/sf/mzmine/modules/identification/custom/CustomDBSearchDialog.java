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

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.components.DragOrderedJList;
import net.sf.mzmine.userinterface.dialogs.ExitCode;
import net.sf.mzmine.util.GUIUtils;

/**
 * 
 */
class CustomDBSearchDialog extends JDialog implements ActionListener {

    public static final int TEXTFIELD_COLUMNS = 12;

    private ExitCode exitCode = ExitCode.UNKNOWN;

    private Desktop desktop;
    private TaskController taskController;
    private CustomDBSearchParameters parameters;

    // Dialog controles
    private JButton btnOK, btnCancel, btnBrowse;
    private JTextField fileNameField, fieldSeparatorField;
    private JFormattedTextField mzToleranceField, rtToleranceField;
    private JCheckBox ignoreFirstLineBox, updateRowCommentBox;

    private DefaultListModel fieldOrderModel;
    private DragOrderedJList fieldOrderList;

    /**
     * Constructor
     */
    public CustomDBSearchDialog(MZmineCore core, CustomDBSearchParameters parameters) {

        // Make dialog modal
        super(core.getDesktop().getMainFrame(), "DB search parameters", true);

        this.taskController = core.getTaskController();
        this.desktop = core.getDesktop();
        this.parameters = parameters;

        // Create panel with controls
        JPanel pnlLabelsAndFields = new JPanel(new GridLayout(7, 2));

        // Database file name
        GUIUtils.addLabel(pnlLabelsAndFields, "Database file");
        fileNameField = new JTextField(TEXTFIELD_COLUMNS);
        JPanel fileNamePanel = new JPanel();
        fileNamePanel.add(fileNameField);
        btnBrowse = GUIUtils.addButton(fileNamePanel, "Browse..", null, this);
        pnlLabelsAndFields.add(fileNamePanel);

        // Field separator
        GUIUtils.addLabel(pnlLabelsAndFields, "Field separator");
        fieldSeparatorField = new JTextField(TEXTFIELD_COLUMNS);
        pnlLabelsAndFields.add(fieldSeparatorField);

        // Field order
        GUIUtils.addLabel(pnlLabelsAndFields, "Field order");
        fieldOrderModel = new DefaultListModel();
        fieldOrderList = new DragOrderedJList(fieldOrderModel);
        pnlLabelsAndFields.add(fieldOrderList);

        // Ignore first line
        GUIUtils.addLabel(pnlLabelsAndFields, "Ignore first line");
        ignoreFirstLineBox = new JCheckBox();
        pnlLabelsAndFields.add(ignoreFirstLineBox);

        // m/z tolerance
        GUIUtils.addLabel(pnlLabelsAndFields, "m/z tolerance");
        mzToleranceField = new JFormattedTextField(desktop.getMZFormat());
        pnlLabelsAndFields.add(mzToleranceField);

        // Retention time tolerance
        GUIUtils.addLabel(pnlLabelsAndFields, "Retention time tolerance");
        rtToleranceField = new JFormattedTextField(desktop.getRTFormat());
        pnlLabelsAndFields.add(rtToleranceField);

        // Update row comment
        GUIUtils.addLabel(pnlLabelsAndFields, "Update row comment");
        updateRowCommentBox = new JCheckBox();
        pnlLabelsAndFields.add(updateRowCommentBox);

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
        setLocationRelativeTo(desktop.getMainFrame());

    }

    /**
     * Implementation for ActionListener interface
     */
    public void actionPerformed(ActionEvent ae) {

        Object src = ae.getSource();

        if (src == btnOK) {

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
