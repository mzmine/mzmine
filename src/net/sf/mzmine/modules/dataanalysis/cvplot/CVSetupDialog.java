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
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

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
	
	private ExitCode exitCode = ExitCode.UNKNOWN;


    /**
     * Constructor: creates new form SelectOneGroupDialog
     */
    public CVSetupDialog(Desktop desktop, OpenedRawDataFile[] dataFiles) {

    	super(desktop.getMainFrame(), "Select raw data files for CV analysis", true);
    	
    	this.desktop = desktop;
    	
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
			OpenedRawDataFile[] selected = (OpenedRawDataFile[])listAvailableFiles.getSelectedValues();

			// Put them to g1 target and remove from source list
			for (OpenedRawDataFile rf : selected) {
				selectedRawDataFiles.add(rf);
				availableRawDataFiles.remove(rf);
			}

			listAvailableFiles.setListData(availableRawDataFiles);
			listSelectedFiles.setListData(selectedRawDataFiles);

		}

		// Remove from group one button
		if (src == buttonUnselectFiles) {
			// Get selected items in source list
			OpenedRawDataFile[] selected = (OpenedRawDataFile[])listSelectedFiles.getSelectedValues();

			// Put them to g1 target and remove from source list
			for (OpenedRawDataFile rf : selected) {
				selectedRawDataFiles.add(rf);
				availableRawDataFiles.remove(rf);
			}

			listAvailableFiles.setListData(availableRawDataFiles);
			listSelectedFiles.setListData(selectedRawDataFiles);

		}

	}



    private void initComponents() {

		panelAll = new JPanel();

		panelLists = new JPanel();
		panelSelectionCaption = new JPanel();
		labelSelectionCaption = new JLabel();
		panelAvailableFiles = new JPanel();
		scrollAvailableFiles = new JScrollPane();
		listAvailableFiles = new JList();
		panelSelectionButtons = new JPanel();
		panelSelectionFillMidUpper = new JPanel();
		buttonSelectFiles = new JButton();
		buttonUnselectFiles = new JButton();
		panelSelectionFillMidLower = new JPanel();
		panelSelectedFiles = new JPanel();
		scrollSelectedFiles = new JScrollPane();
		listSelectedFiles = new JList();

		panelOKCancelButtons = new JPanel();
		buttonOK = new JButton();
		buttonCancel = new JButton();
        

        panelAll.setLayout(new java.awt.BorderLayout());

        // Buttons
		buttonOK.setText("OK");
		buttonCancel.setText("Cancel");
		buttonOK.addActionListener(this);
		buttonCancel.addActionListener(this);
		panelOKCancelButtons.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
		panelOKCancelButtons.add(buttonOK);
		panelOKCancelButtons.add(buttonCancel);

        panelAll.add(panelOKCancelButtons, java.awt.BorderLayout.SOUTH);

        
		//	Raw data file selection

        panelLists.setLayout(new java.awt.BorderLayout());

        // Left: list of available files
        panelAvailableFiles.setLayout(new java.awt.BorderLayout());
        panelAvailableFiles.setBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(5, 5, 5, 5)));
        scrollAvailableFiles.setViewportView(listAvailableFiles);
        scrollAvailableFiles.setMinimumSize(new java.awt.Dimension(LISTBOXWIDTH,LISTBOXHEIGHT));
        scrollAvailableFiles.setPreferredSize(new java.awt.Dimension(LISTBOXWIDTH,LISTBOXHEIGHT));
        panelAvailableFiles.add(scrollAvailableFiles, java.awt.BorderLayout.CENTER);
        panelLists.add(panelAvailableFiles, java.awt.BorderLayout.WEST);

        // Mid: selection buttons
        panelSelectionButtons.setLayout(new java.awt.GridLayout(4, 1));
        panelSelectionButtons.setBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(5, 1, 1, 5)));
        panelSelectionButtons.add(panelSelectionFillMidUpper);

        buttonSelectFiles.setText(">");
        buttonSelectFiles.addActionListener(this);
        buttonSelectFiles.setMinimumSize(new java.awt.Dimension(BUTTONWIDTH, BUTTONHEIGHT));
        buttonSelectFiles.setPreferredSize(new java.awt.Dimension(BUTTONWIDTH, BUTTONHEIGHT));
        panelSelectionButtons.add(buttonSelectFiles);

        buttonUnselectFiles.setText("<");
        buttonUnselectFiles.addActionListener(this);
        buttonUnselectFiles.setMinimumSize(new java.awt.Dimension(BUTTONWIDTH, BUTTONHEIGHT));
        buttonUnselectFiles.setPreferredSize(new java.awt.Dimension(BUTTONWIDTH, BUTTONHEIGHT));
        panelSelectionButtons.add(buttonUnselectFiles);

        panelSelectionButtons.add(panelSelectionFillMidLower);

        panelLists.add(panelSelectionButtons, java.awt.BorderLayout.CENTER);

        // Right: list of selected files
        panelSelectedFiles.setLayout(new java.awt.BorderLayout());
        panelSelectedFiles.setBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(5, 5, 5, 5)));
        scrollSelectedFiles.setViewportView(listSelectedFiles);
        scrollSelectedFiles.setMinimumSize(new java.awt.Dimension(LISTBOXWIDTH,LISTBOXHEIGHT));
        scrollSelectedFiles.setPreferredSize(new java.awt.Dimension(LISTBOXWIDTH,LISTBOXHEIGHT));
        panelSelectedFiles.add(scrollSelectedFiles, java.awt.BorderLayout.CENTER);
        panelLists.add(panelSelectedFiles, java.awt.BorderLayout.EAST);

        // Top: Caption for selection
        panelSelectionCaption.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        labelSelectionCaption.setText("Selected raw data files");
        panelSelectionCaption.add(labelSelectionCaption);
        panelLists.add(panelSelectionCaption, java.awt.BorderLayout.NORTH);

        panelAll.add(panelLists, java.awt.BorderLayout.NORTH);

		// - Finally add everything to the main pane
        getContentPane().add(panelAll, java.awt.BorderLayout.CENTER);
        
        setDefaultCloseOperation(javax.swing.WindowConstants.HIDE_ON_CLOSE);

        pack();
        
    }



	/**
	 * Method for retrieving items selected to the group
	 * @return	Vector containing items selected to the group
	 */
	public Vector<OpenedRawDataFile> getSelectedItems() {
		return selectedRawDataFiles;
	}


	/**
	 * Method for reading exit code
	 * @return	1=OK clicked, -1=cancel clicked
	 */
	public ExitCode getExitCode() {
		return exitCode;
	}


    // Variables declaration - do not modify//GEN-BEGIN:variables

	private javax.swing.JPanel panelAll;	// contains everything

		private javax.swing.JPanel panelLists;	// contains all stuff needed for selecting items to the first group (G1)
			private javax.swing.JPanel panelSelectionCaption;	// contains question for the first group
				private javax.swing.JLabel labelSelectionCaption;
			private javax.swing.JPanel panelAvailableFiles;	// contains source list for the first group
				private javax.swing.JScrollPane scrollAvailableFiles;
					private javax.swing.JList listAvailableFiles;
			private javax.swing.JPanel panelSelectionButtons;	// contains buttons for moving items between source and target in the first group
				private javax.swing.JPanel panelSelectionFillMidUpper;	// fillers used in mid panel
				private javax.swing.JButton buttonSelectFiles;
				private javax.swing.JButton buttonUnselectFiles;
				private javax.swing.JPanel panelSelectionFillMidLower;
			private javax.swing.JPanel panelSelectedFiles;	// contains target list for the first group
				private javax.swing.JScrollPane scrollSelectedFiles;
					private javax.swing.JList listSelectedFiles;

		private javax.swing.JPanel panelOKCancelButtons;
			private javax.swing.JButton buttonOK;
			private javax.swing.JButton buttonCancel;

    // End of variables declaration//GEN-END:variables

}

