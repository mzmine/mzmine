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

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.components.DragOrderedJList;
import net.sf.mzmine.userinterface.dialogs.ExitCode;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.NumberFormatter.FormatterType;

/**
 * 
 */
public class CustomDBSearchDialog extends JDialog implements ActionListener {

    public static final int TEXTFIELD_COLUMNS = 12;

    private ExitCode exitCode = ExitCode.UNKNOWN;

    private Desktop desktop;
    private TaskController taskController;

    // Dialog controles
    private JButton btnOK, btnCancel, btnBrowse;
    private JTextField fileNameField, fieldSeparatorField;
    private JFormattedTextField mzToleranceField, rtToleranceField;
    private JCheckBox ignoreFirstLineBox, setRowCommentBox;
    private DragOrderedJList fieldOrderList;

    /**
     * Constructor
     */
    public CustomDBSearchDialog(TaskController taskController, Desktop desktop,
            OpenedRawDataFile dataFile) {

        // Make dialog modal
        super(desktop.getMainFrame(), "DB search parameters", true);

        this.taskController = taskController;
        this.desktop = desktop;

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
        JPanel fieldSeparatorPanel = new JPanel();
        fieldSeparatorPanel.add(fieldSeparatorField);
        pnlLabelsAndFields.add(fieldSeparatorPanel);
        
        

        // Create buttons
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
        MainWindow desktop = MainWindow.getInstance();

        if (src == btnOK) {

            exitCode = ExitCode.OK;
            dispose();

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
