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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.text.MessageFormat;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.impl.MZmineProjectImpl;
import net.sf.mzmine.userinterface.mainwindow.DesktopParameters;
import net.sf.mzmine.util.GUIUtils;

import com.sun.java.ExampleFileFilter;

/**
 * File open dialog
 */
public class ProjectInitDialog extends JDialog implements ActionListener {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private JFileChooser fileChooser;

	static final int PADDING_SIZE = 5;
	private MZmineProjectImpl project;
	private File projectDir, projectParentDir;
	private String projectName;
	private JTextField fldProjectName;
	private JLabel lblProjectPath;

	public ProjectInitDialog() {

		super(MZmineCore.getDesktop().getMainFrame(), "Welcome to MZmine", true);

		logger.finest("Displaying Project Initializing Dialog");

		projectName = "";
		// set default project directory
		DesktopParameters parameters = (DesktopParameters) MZmineCore
				.getDesktop().getParameterSet();
		File path = new File(parameters.getLastOpenPath());
		if (!path.exists()) {
			projectParentDir = new File("/");
		} else {
			projectParentDir = new File(parameters.getLastOpenPath());
		}

		projectDir = new File(projectParentDir, projectName);

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
				"Please create a new project or open existing one");
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
		comp = GUIUtils.addLabel(panel, "Open Project");
		constraints.gridx = 0;
		constraints.gridy = gridy;
		constraints.gridwidth = 1;
		constraints.gridheight = 1;
		layout.setConstraints(comp, constraints);

		JButton btnOpenProject = GUIUtils.addButton(panel, "Open project",
				null, this, "Open");
		constraints.gridx = 4;
		constraints.gridy = gridy;
		constraints.gridwidth = 1;
		constraints.gridheight = 1;
		layout.setConstraints(btnOpenProject, constraints);

		gridy++;
		comp = GUIUtils.addSeparator(panel, PADDING_SIZE);
		constraints.gridx = 0;
		constraints.gridy = gridy;
		constraints.gridwidth = 5;
		constraints.gridheight = 1;
		layout.setConstraints(comp, constraints);

		gridy++;
		comp = GUIUtils.addLabel(panel, "Create new project");
		constraints.gridx = 0;
		constraints.gridy = gridy;
		constraints.gridwidth = 1;
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
		JButton btnQuit = GUIUtils.addButton(panel, "Quit", null, this, "Quit");
		constraints.gridx = 4;
		constraints.gridy = gridy;
		constraints.gridwidth = 1;
		constraints.gridheight = 1;
		layout.setConstraints(btnQuit, constraints);

		add(panel);
		pack();
		setResizable(false);
		setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());

		parameters.setLastOpenPath(projectDir.toString());

	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent event) {

		String command = event.getActionCommand();

		// check if user clicked "Open"
		if (command.equals("Open")) {
			// discard this dialog anyway
			dispose();
			
			DesktopParameters parameters = (DesktopParameters) MZmineCore
					.getDesktop().getParameterSet();
			String lastPath = parameters.getLastOpenPath();
			ProjectOpenDialog projectOpenDialog = new ProjectOpenDialog(
					lastPath);

			projectOpenDialog.setVisible(true);



		} else if (command.equals("Browse")) {
			// reset project parent directory
			DesktopParameters parameters = (DesktopParameters) MZmineCore
					.getDesktop().getParameterSet();
			String lastPath = parameters.getLastOpenPath();
			JFileChooser fileChooser = new JFileChooser(lastPath);
			fileChooser
					.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			fileChooser
					.setDialogTitle("Please select a place to make project.");

			int returnVal = fileChooser.showDialog(this, "Select");
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				projectParentDir = fileChooser.getSelectedFile();
				if (!projectParentDir.isDirectory()) {
					projectParentDir = projectParentDir.getParentFile();
				}
				projectDir = new File(projectParentDir, projectName);
				this.lblProjectPath.setText(projectDir.toString());
				parameters.setLastOpenPath(projectDir.toString());
			}

		} else if (command.equals("Create")) {
			if (this.projectName.equals("")){
				return;
			}
			// discard this dialog
			dispose();

			try {

				MZmineCore.getIOController().createProject(projectDir);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.fine("Could not create new project");
			}
		} else if (command.equals("Quit")) {
			dispose();
			System.exit(0);
		}
	}

	protected class ProjectNameListener implements DocumentListener {

		public void changedUpdate(DocumentEvent e) {
			projectName = fldProjectName.getText();
			projectDir = new File(projectParentDir, projectName);
			String warning = "";
			if (projectDir.exists()) {
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
