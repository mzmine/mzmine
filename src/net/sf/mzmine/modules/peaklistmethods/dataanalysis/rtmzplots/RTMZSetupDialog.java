/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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
package net.sf.mzmine.modules.peaklistmethods.dataanalysis.rtmzplots;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.util.components.HelpButton;
import net.sf.mzmine.util.dialogs.ExitCode;

public class RTMZSetupDialog extends JDialog implements java.awt.event.ActionListener {

    protected enum SelectionMode {

        SingleGroup, TwoGroups
    };
    private static final int LISTBOXWIDTH = 250;
    private static final int LISTBOXHEIGHT = 200;
    private static final int BUTTONWIDTH = 90;
    private static final int BUTTONHEIGHT = 35;
    // Selections
    Vector<RawDataFile> availableRawDataFiles;
    Vector<RawDataFile> groupOneSelectedRawDataFiles;
    Vector<RawDataFile> groupTwoSelectedRawDataFiles;
    private SelectionMode mode;
    private Desktop desktop;
    private SimpleParameterSet parameters;
    private ExitCode exitCode = ExitCode.UNKNOWN;
    private String helpID;

    /**
     * Constructor: creates new form SelectOneGroupDialog
     */
    public RTMZSetupDialog(Desktop desktop, RawDataFile[] dataFiles, SimpleParameterSet parameters, SelectionMode mode, String helpID) {

        super(desktop.getMainFrame(), null, true);

        String title = null;
        if (mode == SelectionMode.SingleGroup) {
            title = "Select files for analysis";
        }

        if (mode == SelectionMode.TwoGroups) {
            title = "Select two groups of files for analysis";
        }

        setTitle(title);



        this.desktop = desktop;
        this.parameters = parameters;
        this.mode = mode;
        this.helpID = helpID;

        availableRawDataFiles = new Vector<RawDataFile>();
        for (RawDataFile rf : dataFiles) {
            availableRawDataFiles.add(rf);
        }

        // Build the form
        initComponents();

        // Put items to list boxes
        groupOneSelectedRawDataFiles = new Vector<RawDataFile>();
        groupTwoSelectedRawDataFiles = new Vector<RawDataFile>();

        listAvailableFiles.setListData(availableRawDataFiles);
        listGroupOneSelectedFiles.setListData(groupOneSelectedRawDataFiles);
        listGroupTwoSelectedFiles.setListData(groupTwoSelectedRawDataFiles);

    }

    /**
     * Implementation of ActionListener interface
     */
    public void actionPerformed(java.awt.event.ActionEvent e) {
        Object src = e.getSource();

        // OK button
        if (src == buttonOK) {

            // Validate group selections
            if (mode == SelectionMode.SingleGroup) {
                if (groupOneSelectedRawDataFiles.size() < 2) {
                    desktop.displayErrorMessage("Please select at least two files.");
                    return;
                }
            }

            if (mode == SelectionMode.TwoGroups) {
                if ((groupOneSelectedRawDataFiles.size() < 1) ||
                        (groupTwoSelectedRawDataFiles.size() < 1)) {
                    desktop.displayErrorMessage("Please select at least one file to both groups.");
                    return;
                }
            }

            if (radioButtonHeight.isSelected()) {
                parameters.setParameterValue(RTMZAnalyzer.MeasurementType, RTMZAnalyzer.MeasurementTypeHeight);
            }
            if (radioButtonArea.isSelected()) {
                parameters.setParameterValue(RTMZAnalyzer.MeasurementType, RTMZAnalyzer.MeasurementTypeArea);
            }

            // Set exit code
            exitCode = ExitCode.OK;

            // Hide form
            dispose();

        }

        // Cancel button
        if (src == buttonCancel) {

            // Set exit code
            exitCode = ExitCode.CANCEL;

            // Hide form
            dispose();

        }


        // Select to group one button
        if (src == buttonGroupOneSelectFiles) {
            // Get selected items in source list
            Object[] selected = listAvailableFiles.getSelectedValues();

            // Put them to g1 target and remove from source list
            for (Object rf : selected) {
                groupOneSelectedRawDataFiles.add((RawDataFile) rf);
                availableRawDataFiles.remove((RawDataFile) rf);
            }

            listAvailableFiles.setListData(availableRawDataFiles);
            listGroupOneSelectedFiles.setListData(groupOneSelectedRawDataFiles);

        }

        // Remove from group one button
        if (src == buttonGroupOneUnselectFiles) {
            // Get selected items in source list
            Object[] selected = listGroupOneSelectedFiles.getSelectedValues();

            // Put them to g1 target and remove from source list
            for (Object rf : selected) {
                groupOneSelectedRawDataFiles.remove((RawDataFile) rf);
                availableRawDataFiles.add((RawDataFile) rf);
            }

            listAvailableFiles.setListData(availableRawDataFiles);
            listGroupOneSelectedFiles.setListData(groupOneSelectedRawDataFiles);

        }


        // Select to group two button
        if (src == buttonGroupTwoSelectFiles) {
            // Get selected items in source list
            Object[] selected = listAvailableFiles.getSelectedValues();

            // Put them to g1 target and remove from source list
            for (Object rf : selected) {
                groupTwoSelectedRawDataFiles.add((RawDataFile) rf);
                availableRawDataFiles.remove((RawDataFile) rf);
            }

            listAvailableFiles.setListData(availableRawDataFiles);
            listGroupTwoSelectedFiles.setListData(groupTwoSelectedRawDataFiles);

        }

        // Remove from group one button
        if (src == buttonGroupTwoUnselectFiles) {
            // Get selected items in source list
            Object[] selected = listGroupTwoSelectedFiles.getSelectedValues();

            // Put them to g1 target and remove from source list
            for (Object rf : selected) {
                groupTwoSelectedRawDataFiles.remove((RawDataFile) rf);
                availableRawDataFiles.add((RawDataFile) rf);
            }

            listAvailableFiles.setListData(availableRawDataFiles);
            listGroupTwoSelectedFiles.setListData(groupTwoSelectedRawDataFiles);

        }

    }

    private void initComponents() {

        // Left: list of available files

        listAvailableFiles = new JList();
        scrollAvailableFiles = new JScrollPane();
        scrollAvailableFiles.setViewportView(listAvailableFiles);
        scrollAvailableFiles.setMinimumSize(new Dimension(LISTBOXWIDTH, LISTBOXHEIGHT));
        scrollAvailableFiles.setPreferredSize(new Dimension(LISTBOXWIDTH, LISTBOXHEIGHT));

        panelAvailableFiles = new JPanel();
        panelAvailableFiles.setLayout(new BorderLayout());
        panelAvailableFiles.setBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(5, 5, 5, 5)));
        panelAvailableFiles.add(scrollAvailableFiles, BorderLayout.CENTER);

        // Right Upper: selection buttons for group one & selected files list

        panelGroupOneSelectionFillMidUpper = new JPanel();

        buttonGroupOneSelectFiles = new JButton();
        buttonGroupOneSelectFiles.setText(">");
        buttonGroupOneSelectFiles.addActionListener(this);
        buttonGroupOneSelectFiles.setMinimumSize(new Dimension(BUTTONWIDTH, BUTTONHEIGHT));
        buttonGroupOneSelectFiles.setPreferredSize(new Dimension(BUTTONWIDTH, BUTTONHEIGHT));

        buttonGroupOneUnselectFiles = new JButton();
        buttonGroupOneUnselectFiles.setText("<");
        buttonGroupOneUnselectFiles.addActionListener(this);
        buttonGroupOneUnselectFiles.setMinimumSize(new Dimension(BUTTONWIDTH, BUTTONHEIGHT));
        buttonGroupOneUnselectFiles.setPreferredSize(new Dimension(BUTTONWIDTH, BUTTONHEIGHT));

        panelGroupOneSelectionFillMidLower = new JPanel();

        panelGroupOneSelectionButtons = new JPanel();
        panelGroupOneSelectionButtons.setLayout(new GridLayout(4, 1));
        panelGroupOneSelectionButtons.setBorder(new EmptyBorder(new java.awt.Insets(5, 1, 1, 5)));
        panelGroupOneSelectionButtons.add(panelGroupOneSelectionFillMidUpper);
        panelGroupOneSelectionButtons.add(buttonGroupOneSelectFiles);
        panelGroupOneSelectionButtons.add(buttonGroupOneUnselectFiles);
        panelGroupOneSelectionButtons.add(panelGroupOneSelectionFillMidLower);

        listGroupOneSelectedFiles = new JList();
        scrollGroupOneSelectedFiles = new JScrollPane();
        scrollGroupOneSelectedFiles.setViewportView(listGroupOneSelectedFiles);
        scrollGroupOneSelectedFiles.setMinimumSize(new Dimension(LISTBOXWIDTH, LISTBOXHEIGHT));
        scrollGroupOneSelectedFiles.setPreferredSize(new Dimension(LISTBOXWIDTH, LISTBOXHEIGHT));

        panelGroupOneSelectedFiles = new JPanel();
        panelGroupOneSelectedFiles.setLayout(new BorderLayout());
        panelGroupOneSelectedFiles.setBorder(new EmptyBorder(new java.awt.Insets(5, 5, 5, 5)));
        panelGroupOneSelectedFiles.add(scrollGroupOneSelectedFiles, BorderLayout.CENTER);

        panelGroupOneSelection = new JPanel(new BorderLayout());
        panelGroupOneSelection.add(panelGroupOneSelectionButtons, BorderLayout.WEST);
        panelGroupOneSelection.add(panelGroupOneSelectedFiles, BorderLayout.CENTER);



        // Right lower: selection buttons for group two & selected files list
        panelGroupTwoSelectionFillMidUpper = new JPanel();

        buttonGroupTwoSelectFiles = new JButton();
        buttonGroupTwoSelectFiles.setText(">");
        buttonGroupTwoSelectFiles.addActionListener(this);
        buttonGroupTwoSelectFiles.setMinimumSize(new Dimension(BUTTONWIDTH, BUTTONHEIGHT));
        buttonGroupTwoSelectFiles.setPreferredSize(new Dimension(BUTTONWIDTH, BUTTONHEIGHT));

        buttonGroupTwoUnselectFiles = new JButton();
        buttonGroupTwoUnselectFiles.setText("<");
        buttonGroupTwoUnselectFiles.addActionListener(this);
        buttonGroupTwoUnselectFiles.setMinimumSize(new Dimension(BUTTONWIDTH, BUTTONHEIGHT));
        buttonGroupTwoUnselectFiles.setPreferredSize(new Dimension(BUTTONWIDTH, BUTTONHEIGHT));

        panelGroupTwoSelectionFillMidLower = new JPanel();

        panelGroupTwoSelectionButtons = new JPanel();
        panelGroupTwoSelectionButtons.setLayout(new GridLayout(4, 1));
        panelGroupTwoSelectionButtons.setBorder(new EmptyBorder(new java.awt.Insets(5, 1, 1, 5)));
        panelGroupTwoSelectionButtons.add(panelGroupTwoSelectionFillMidUpper);
        panelGroupTwoSelectionButtons.add(buttonGroupTwoSelectFiles);
        panelGroupTwoSelectionButtons.add(buttonGroupTwoUnselectFiles);
        panelGroupTwoSelectionButtons.add(panelGroupTwoSelectionFillMidLower);

        listGroupTwoSelectedFiles = new JList();
        scrollGroupTwoSelectedFiles = new JScrollPane();
        scrollGroupTwoSelectedFiles.setViewportView(listGroupTwoSelectedFiles);
        scrollGroupTwoSelectedFiles.setMinimumSize(new Dimension(LISTBOXWIDTH, LISTBOXHEIGHT));
        scrollGroupTwoSelectedFiles.setPreferredSize(new Dimension(LISTBOXWIDTH, LISTBOXHEIGHT));

        panelGroupTwoSelectedFiles = new JPanel();
        panelGroupTwoSelectedFiles.setLayout(new BorderLayout());
        panelGroupTwoSelectedFiles.setBorder(new EmptyBorder(new java.awt.Insets(5, 5, 5, 5)));
        panelGroupTwoSelectedFiles.add(scrollGroupTwoSelectedFiles, BorderLayout.CENTER);

        panelGroupTwoSelection = new JPanel(new BorderLayout());
        panelGroupTwoSelection.add(panelGroupTwoSelectionButtons, BorderLayout.WEST);
        panelGroupTwoSelection.add(panelGroupTwoSelectedFiles, BorderLayout.CENTER);

        if (mode == SelectionMode.SingleGroup) {
            panelBothGroups = new JPanel();
            panelBothGroups.add(panelGroupOneSelection);
        }

        if (mode == SelectionMode.TwoGroups) {
            panelBothGroups = new JPanel(new GridLayout(2, 1));
            panelBothGroups.add(panelGroupOneSelection);
            panelBothGroups.add(panelGroupTwoSelection);
        }

        // Additional method parameters
        labelHeightAreaParamCaption = new JLabel("Peak measuring approach ");
        buttonGroupHeightArea = new ButtonGroup();
        radioButtonHeight = new JRadioButton("height");
        buttonGroupHeightArea.add(radioButtonHeight);
        radioButtonArea = new JRadioButton("area");
        buttonGroupHeightArea.add(radioButtonArea);
        if (parameters.getParameterValue(RTMZAnalyzer.MeasurementType) == RTMZAnalyzer.MeasurementTypeArea) {
            radioButtonArea.setSelected(true);
        }
        if (parameters.getParameterValue(RTMZAnalyzer.MeasurementType) == RTMZAnalyzer.MeasurementTypeHeight) {
            radioButtonHeight.setSelected(true);
        }

        panelParameters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelParameters.add(labelHeightAreaParamCaption);
        panelParameters.add(radioButtonHeight);
        panelParameters.add(radioButtonArea);
        panelParameters.add(radioButtonArea);


        // OK & Cancel Buttons
        buttonOK = new JButton();
        buttonCancel = new JButton();
        buttonOK.setText("OK");
        buttonCancel.setText("Cancel");
        buttonOK.addActionListener(this);
        buttonCancel.addActionListener(this);


        panelOKCancelButtons = new JPanel();
        panelOKCancelButtons.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        panelOKCancelButtons.add(buttonOK);
        panelOKCancelButtons.add(buttonCancel);

        if (helpID != null) {
            btnHelp = new HelpButton(helpID);
            panelOKCancelButtons.add(btnHelp);
        }

        panelParametersButtons = new JPanel(new BorderLayout());
        panelParametersButtons.add(panelParameters, BorderLayout.CENTER);
        panelParametersButtons.add(panelOKCancelButtons, BorderLayout.SOUTH);


        panelAll = new JPanel();
        panelAll.setLayout(new BorderLayout());
        panelAll.add(panelAvailableFiles, BorderLayout.WEST);
        panelAll.add(panelBothGroups, BorderLayout.CENTER);
        panelAll.add(panelParametersButtons, BorderLayout.SOUTH);

        // - Finally add everything to the main pane
        getContentPane().add(panelAll, BorderLayout.CENTER);

        setDefaultCloseOperation(javax.swing.WindowConstants.HIDE_ON_CLOSE);

        pack();
        setLocationRelativeTo(desktop.getMainFrame());
        setResizable(false);

    }

    /**
     * Return selected raw data files
     */
    public RawDataFile[] getGroupOneSelectedFiles() {
        return groupOneSelectedRawDataFiles.toArray(new RawDataFile[0]);
    }

    public RawDataFile[] getGroupTwoSelectedFiles() {
        return groupTwoSelectedRawDataFiles.toArray(new RawDataFile[0]);
    }

    /**
     * Returns the exit code
     */
    public ExitCode getExitCode() {
        return exitCode;
    }
    private JPanel panelAll;	// contains everything
    private JPanel panelAvailableFiles;	// contains source list for the first group
    private JScrollPane scrollAvailableFiles;
    private JList listAvailableFiles;
    private JPanel panelBothGroups;
    private JPanel panelGroupOneSelection;
    private JPanel panelGroupOneSelectionButtons;
    private JPanel panelGroupOneSelectionFillMidUpper;	// fillers used in mid panel
    private JButton buttonGroupOneSelectFiles;
    private JButton buttonGroupOneUnselectFiles;
    private JPanel panelGroupOneSelectionFillMidLower;
    private JPanel panelGroupOneSelectedFiles;
    private JScrollPane scrollGroupOneSelectedFiles;
    private JList listGroupOneSelectedFiles;
    private JPanel panelGroupTwoSelection;
    private JPanel panelGroupTwoSelectionButtons;
    private JPanel panelGroupTwoSelectionFillMidUpper;	// fillers used in mid panel
    private JButton buttonGroupTwoSelectFiles;
    private JButton buttonGroupTwoUnselectFiles;
    private JPanel panelGroupTwoSelectionFillMidLower;
    private JPanel panelGroupTwoSelectedFiles;
    private JScrollPane scrollGroupTwoSelectedFiles;
    private JList listGroupTwoSelectedFiles;
    private JPanel panelParametersButtons;
    private JPanel panelParameters;
    private JLabel labelHeightAreaParamCaption;
    private ButtonGroup buttonGroupHeightArea;
    private JRadioButton radioButtonHeight;
    private JRadioButton radioButtonArea;
    private JPanel panelOKCancelButtons;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JButton btnHelp;
}

