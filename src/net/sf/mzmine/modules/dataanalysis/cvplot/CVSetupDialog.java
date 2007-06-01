/*
    Copyright 2005-2007 The MZmine Development Team

    This file is part of MZmine.

    MZmine is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    MZmine is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MZmine; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/
package net.sf.mzmine.modules.dataanalysis.cvplot;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.dialogs.ExitCode;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;

public class CVSetupDialog extends JDialog implements java.awt.event.ActionListener {

	private static final int LISTBOXWIDTH = 250;
	private static final int LISTBOXHEIGHT = 200;
	private static final int BUTTONWIDTH = 90;
	private static final int BUTTONHEIGHT = 35;

	// Selections
	Vector<OpenedRawDataFile> availableRawDataFiles;
	Vector<OpenedRawDataFile> selectedRawDataFiles;

	private Desktop desktop;
	
	private SimpleParameterSet parameters;
	
	private ExitCode exitCode = ExitCode.UNKNOWN;


    /**
     * Constructor: creates new form SelectOneGroupDialog
     */
    public CVSetupDialog(Desktop desktop, OpenedRawDataFile[] dataFiles, SimpleParameterSet parameters) {

    	super(desktop.getMainFrame(), "Select raw data files for CV analysis", true);
    	
    	this.desktop = desktop;
    	this.parameters = parameters;
    	
		availableRawDataFiles = new Vector<OpenedRawDataFile>();
		for (OpenedRawDataFile rf : dataFiles) 
			availableRawDataFiles.add(rf);
		
		// Build the form
        initComponents();

		// Put items to list boxes
		selectedRawDataFiles = new Vector<OpenedRawDataFile>();
		listAvailableFiles.setListData(availableRawDataFiles);
		listSelectedFiles.setListData(selectedRawDataFiles);

    }

	/**
	 * Implementation of ActionListener interface
	 */
    public void actionPerformed(java.awt.event.ActionEvent e) {
		Object src = e.getSource();

		// OK button
		if (src == buttonOK) {

			// Validate group selections
			if ((selectedRawDataFiles.size()<3)) {
				desktop.displayErrorMessage("Please select at least three raw data files.");
				return;
			}
			
			if (radioButtonHeight.isSelected()) 
				parameters.setParameterValue(CVAnalyzer.MeasurementType, CVAnalyzer.MeasurementTypeHeight);
			if (radioButtonArea.isSelected()) 
				parameters.setParameterValue(CVAnalyzer.MeasurementType, CVAnalyzer.MeasurementTypeArea);
			
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
		if (src == buttonSelectFiles) {
			// Get selected items in source list
			Object[] selected = listAvailableFiles.getSelectedValues();

			// Put them to g1 target and remove from source list
			for (Object rf : selected) {
				selectedRawDataFiles.add((OpenedRawDataFile)rf);
				availableRawDataFiles.remove((OpenedRawDataFile)rf);
			}

			listAvailableFiles.setListData(availableRawDataFiles);
			listSelectedFiles.setListData(selectedRawDataFiles);

		}

		// Remove from group one button
		if (src == buttonUnselectFiles) {
			// Get selected items in source list
			Object[] selected = listSelectedFiles.getSelectedValues();

			// Put them to g1 target and remove from source list
			for (Object rf : selected) {
				selectedRawDataFiles.add((OpenedRawDataFile)rf);
				availableRawDataFiles.remove((OpenedRawDataFile)rf);
			}

			listAvailableFiles.setListData(availableRawDataFiles);
			listSelectedFiles.setListData(selectedRawDataFiles);

		}

	}



    private void initComponents() {
		
		

        // Left: list of available files
		listAvailableFiles = new JList();
		scrollAvailableFiles = new JScrollPane();
        scrollAvailableFiles.setViewportView(listAvailableFiles);
        scrollAvailableFiles.setMinimumSize(new Dimension(LISTBOXWIDTH,LISTBOXHEIGHT));
        scrollAvailableFiles.setPreferredSize(new Dimension(LISTBOXWIDTH,LISTBOXHEIGHT));
        
        panelAvailableFiles = new JPanel();
        panelAvailableFiles.setLayout(new BorderLayout());
        panelAvailableFiles.setBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(5, 5, 5, 5)));
        panelAvailableFiles.add(scrollAvailableFiles, BorderLayout.CENTER);
        

        // Mid: selection buttons
		panelSelectionFillMidUpper = new JPanel();
		
		buttonSelectFiles = new JButton();
        buttonSelectFiles.setText(">");
        buttonSelectFiles.addActionListener(this);
        buttonSelectFiles.setMinimumSize(new Dimension(BUTTONWIDTH, BUTTONHEIGHT));
        buttonSelectFiles.setPreferredSize(new Dimension(BUTTONWIDTH, BUTTONHEIGHT));

        buttonUnselectFiles = new JButton();
        buttonUnselectFiles.setText("<");
        buttonUnselectFiles.addActionListener(this);
        buttonUnselectFiles.setMinimumSize(new Dimension(BUTTONWIDTH, BUTTONHEIGHT));
        buttonUnselectFiles.setPreferredSize(new Dimension(BUTTONWIDTH, BUTTONHEIGHT));
        
        panelSelectionFillMidLower = new JPanel();
        
        panelSelectionButtons = new JPanel();
        panelSelectionButtons.setLayout(new java.awt.GridLayout(4, 1));
        panelSelectionButtons.setBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(5, 1, 1, 5)));
        panelSelectionButtons.add(panelSelectionFillMidUpper);
        panelSelectionButtons.add(buttonSelectFiles);
        panelSelectionButtons.add(buttonUnselectFiles);
        panelSelectionButtons.add(panelSelectionFillMidLower);

        

        // Right: list of selected files
        listSelectedFiles = new JList();
        scrollSelectedFiles = new JScrollPane();
        scrollSelectedFiles.setViewportView(listSelectedFiles);
        scrollSelectedFiles.setMinimumSize(new Dimension(LISTBOXWIDTH,LISTBOXHEIGHT));
        scrollSelectedFiles.setPreferredSize(new Dimension(LISTBOXWIDTH,LISTBOXHEIGHT));
        
        panelSelectedFiles = new JPanel();
        panelSelectedFiles.setLayout(new BorderLayout());
        panelSelectedFiles.setBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(5, 5, 5, 5)));        
        panelSelectedFiles.add(scrollSelectedFiles, BorderLayout.CENTER);
        
		panelLists = new JPanel();
        panelLists.setLayout(new BorderLayout());        
        panelLists.add(panelAvailableFiles, BorderLayout.WEST);
        panelLists.add(panelSelectionButtons, BorderLayout.CENTER);
        panelLists.add(panelSelectedFiles, BorderLayout.EAST);

        
        // Additional method parameters
        labelHeightAreaParamCaption = new JLabel("For computing CV, use peak's ");
        buttonGroupHeightArea = new ButtonGroup();
        radioButtonHeight = new JRadioButton("height");
        buttonGroupHeightArea.add(radioButtonHeight);
        radioButtonArea = new JRadioButton("area");
        buttonGroupHeightArea.add(radioButtonArea);
        if (parameters.getParameterValue(CVAnalyzer.MeasurementType)==CVAnalyzer.MeasurementTypeArea)
        	radioButtonArea.setSelected(true);
        if (parameters.getParameterValue(CVAnalyzer.MeasurementType)==CVAnalyzer.MeasurementTypeHeight)
        	radioButtonHeight.setSelected(true);
        
        panelParameters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelParameters.add(labelHeightAreaParamCaption);
        panelParameters.add(radioButtonHeight);
        panelParameters.add(radioButtonArea);
        panelParameters.add(radioButtonArea);

      
        panelListsAndParameters = new JPanel(new BorderLayout());
        panelListsAndParameters.add(panelParameters, BorderLayout.SOUTH);
        panelListsAndParameters.add(panelLists, BorderLayout.CENTER);
        
        
        
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
        
        
        panelAll = new JPanel();
        panelAll.setLayout(new BorderLayout());
        panelAll.add(panelListsAndParameters, BorderLayout.CENTER);
        panelAll.add(panelOKCancelButtons, BorderLayout.SOUTH);        

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
	public OpenedRawDataFile[] getSelectedFiles() {
		return selectedRawDataFiles.toArray(new OpenedRawDataFile[0]);
	}


	/**
	 * Returns the exit code
	 */
	public ExitCode getExitCode() {
		return exitCode;
	}

	private JPanel panelAll;	// contains everything

		private JPanel panelListsAndParameters;
		
			private JPanel panelLists;	// contains all stuff needed for selecting items to the first group (G1)

				private JPanel panelAvailableFiles;	// contains source list for the first group
					private JScrollPane scrollAvailableFiles;
						private JList listAvailableFiles;
				private JPanel panelSelectionButtons;	// contains buttons for moving items between source and target in the first group
					private JPanel panelSelectionFillMidUpper;	// fillers used in mid panel
					private JButton buttonSelectFiles;
					private JButton buttonUnselectFiles;
					private JPanel panelSelectionFillMidLower;
				private JPanel panelSelectedFiles;	// contains target list for the first group
					private JScrollPane scrollSelectedFiles;
						private JList listSelectedFiles;
						
			private JPanel panelParameters;
				private JLabel labelHeightAreaParamCaption;
				private ButtonGroup buttonGroupHeightArea;
				private JRadioButton radioButtonHeight;
				private JRadioButton radioButtonArea;
				
			

		private JPanel panelOKCancelButtons;
			private JButton buttonOK;
			private JButton buttonCancel;

}

