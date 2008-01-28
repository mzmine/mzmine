/*
 * Copyright 2006 The MZmine Development Team
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

package net.sf.mzmine.userinterface.dialogs;

import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.impl.MZmineProjectImpl;
import net.sf.mzmine.userinterface.dialogs.ProjectInitDialog.ProjectNameListener;
import net.sf.mzmine.userinterface.mainwindow.DesktopParameters;
import net.sf.mzmine.util.GUIUtils;
/**
 * File open dialog
 */
public class ProjectSaveAsDialog extends JDialog implements ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    private JFileChooser fileChooser;
	static final int PADDING_SIZE = 5;
	private MZmineProjectImpl project;
	private File projectDir,projectParentDir;
	private String projectName;
	private JTextField fldProjectName;
	private JLabel lblProjectPath;    
	
    public ProjectSaveAsDialog(File currProjectDir) {

    		super(MZmineCore.getDesktop().getMainFrame(),
    				"Welcome to MZmine", true);

    		logger.finest("Displaying Project save as Dialog");

    		projectName = currProjectDir.getName();
    		projectParentDir = currProjectDir.getParentFile();
    		projectDir = new File(projectParentDir,projectName);
    		
    		GridBagConstraints constraints = new GridBagConstraints();

    		// set default layout constraints
    		constraints.fill = GridBagConstraints.HORIZONTAL;
    		constraints.anchor = GridBagConstraints.WEST;
    		constraints.insets = new Insets(PADDING_SIZE, PADDING_SIZE,
    				PADDING_SIZE, PADDING_SIZE);

    		JComponent comp;
    		GridBagLayout layout = new GridBagLayout();
    		JPanel panel = new JPanel(layout);
    		int gridy = 0;

    		comp = GUIUtils.addLabel(panel,
    				"Please select a new name for the current project");
    		constraints.gridx = 0;
    		constraints.gridy = gridy;
    		constraints.gridwidth = 3;
    		constraints.gridheight = 1;
    		layout.setConstraints(comp, constraints);

    		gridy++;

    		comp = GUIUtils.addSeparator(panel, PADDING_SIZE);
    		constraints.gridx = 0;
    		constraints.gridy = gridy;
    		constraints.gridwidth = 5;
    		constraints.gridheight = 1;
    		layout.setConstraints(comp, constraints);

    		gridy++;
    		comp = GUIUtils.addLabel(panel, "Project name");
    		constraints.gridx = 1;
    		constraints.gridy = gridy;
    		constraints.gridwidth = 1;
    		constraints.gridheight = 1;
    		layout.setConstraints(comp, constraints);

    		fldProjectName = new JTextField(20);
    		fldProjectName.getDocument().addDocumentListener(
    				new ProjectNameListener());
    		constraints.weightx = 1;
    		constraints.gridx = 2;
    		constraints.gridy = gridy;
    		constraints.gridwidth = 1;
    		constraints.gridheight = 1;
    		panel.add(fldProjectName, constraints);
    		constraints.weightx = 0;

    		gridy++;
    		comp = GUIUtils.addLabel(panel, "Creating ");
    		constraints.gridx = 1;
    		constraints.gridy = gridy;
    		constraints.gridwidth = 1;
    		constraints.gridheight = 1;
    		layout.setConstraints(comp, constraints);

    		lblProjectPath = new JLabel(projectDir.toString());
    		constraints.weightx = 1;
    		constraints.gridx = 2;
    		constraints.gridy = gridy;
    		constraints.gridwidth = 2;
    		constraints.gridheight = 1;
    		panel.add(lblProjectPath, constraints);
    		constraints.weightx = 0;

    		JButton btnSelectProjectDir = GUIUtils.addButton(panel, "Browse", null,
    				this, "Browse");
    		constraints.gridx = 4;
    		constraints.gridy = gridy;
    		constraints.gridwidth = 1;
    		constraints.gridheight = 1;
    		layout.setConstraints(btnSelectProjectDir, constraints);

    		gridy++;
    		JButton btnCreateProject = GUIUtils.addButton(panel,
    				"Create a new project", null, this, "Create");
    		constraints.gridx = 4;
    		constraints.gridy = gridy;
    		constraints.gridwidth = 1;
    		constraints.gridheight = 1;
    		layout.setConstraints(btnCreateProject, constraints);

    		gridy++;
    		comp = GUIUtils.addSeparator(panel, PADDING_SIZE);
    		constraints.gridx = 0;
    		constraints.gridy = gridy;
    		constraints.gridwidth = 5;
    		constraints.gridheight = 1;
    		layout.setConstraints(comp, constraints);

    		gridy++;
    		JButton btnQuit = GUIUtils.addButton(panel, "Cancel", null, this, "Cancel");
    		constraints.gridx = 4;
    		constraints.gridy = gridy;
    		constraints.gridwidth = 1;
    		constraints.gridheight = 1;
    		layout.setConstraints(btnQuit, constraints);

    		add(panel);
    		pack();
    		setResizable(false);
    		setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());


    	}
    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
        
    	public void actionPerformed(ActionEvent event) {

    		String command = event.getActionCommand();

    		// check if user clicked "Open"
    		if (command.equals("Browse")) {
    			//reset project parent directory
    			DesktopParameters parameters = (DesktopParameters) MZmineCore
    					.getDesktop().getParameterSet();
    			String lastPath = parameters.getLastOpenPath();
    			JFileChooser fileChooser = new JFileChooser(lastPath);
    			fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    			fileChooser.setDialogTitle("Please select a place to make project.");
    			
    			int returnVal = fileChooser.showDialog(this, "Select");
    			if (returnVal == JFileChooser.APPROVE_OPTION) {
    				projectParentDir = fileChooser.getSelectedFile();
    				if (!projectParentDir.isDirectory()){
    					projectParentDir = projectParentDir.getParentFile();
    				}
    				projectDir = new File(projectParentDir,projectName);
    				this.lblProjectPath.setText(projectDir.toString());
    				parameters.setLastOpenPath(projectDir.toString());
    			}

    		} else if (command.equals("Create")) {
    			try {
    				MZmineCore.getIOController().saveProject(projectDir);
    			} catch (IOException e) {
    				// TODO Auto-generated catch block
    				logger.fine("Could not create new project");
    			}
    			// discard this dialog
    			dispose();
    		} else if (command.equals("Cancel")) {
    			dispose();
    		}
    	}
	protected class ProjectNameListener implements DocumentListener {

		public void changedUpdate(DocumentEvent e) {
			projectDir = new File(projectParentDir, fldProjectName.getText());
			String warning = "";	
			if (projectDir.exists()){
				warning = "File exists ! ";
			}
			lblProjectPath.setText(warning + projectDir.toString());
		}

		public void insertUpdate(DocumentEvent e) {
			changedUpdate(e);
		}

		public void removeUpdate(DocumentEvent e) {
			changedUpdate(e);
		}
	}
 
}
