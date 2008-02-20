/*
 * Copyright 2006-2008 The MZmine Development Team
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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.sf.mzmine.util.GUIUtils;

public class ProjectCreatePane extends JPanel implements ActionListener {

	static final int PADDING_SIZE = 5;
	private File projectDir, projectParentDir;
	private String projectName;
	private JTextField fldProjectName;
	private JLabel lblProjectPath, lblShortMessage;
	private String suffix = "mzmine";
	boolean ok;

	public ProjectCreatePane(File projectParentDir) {
		super(new GridBagLayout());
		this.projectParentDir = projectParentDir;

		projectName = "";

		// create suggested projectName
		int index = 0;
		String indexStr;
		String projectNameBase = "MyProject";
		while (true) {
			if (index == 0) {
				indexStr = "";
			} else {
				indexStr = "." + ((Integer) index).toString();
			}
			projectName = projectNameBase + indexStr;
			this.projectDir = new File(projectParentDir, projectName + "."
					+ suffix);
			if (this.projectDir.exists()) {
				index++;
			} else {
				break;
			}
		}
		ok = true;

		GridBagLayout layout = (GridBagLayout) this.getLayout();
		JPanel panel = this;
		GridBagConstraints constraints = new GridBagConstraints();

		// set default layout constraints
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.insets = new Insets(PADDING_SIZE, PADDING_SIZE,
				PADDING_SIZE, PADDING_SIZE);

		JComponent comp;
		int gridy = 0;

		comp = GUIUtils.addSeparator(panel, PADDING_SIZE);
		constraints.gridx = 0;
		constraints.gridy = gridy;
		constraints.gridwidth = 6;
		constraints.gridheight = 1;
		layout.setConstraints(comp, constraints);

		gridy++;
		comp = GUIUtils.addLabel(panel, "Project name");
		constraints.gridx = 1;
		constraints.gridy = gridy;
		constraints.gridwidth = 1;
		constraints.gridheight = 1;
		layout.setConstraints(comp, constraints);

		fldProjectName = new JTextField(projectName);
		fldProjectName.getDocument().addDocumentListener(
				new ProjectNameListener());
		constraints.weightx = 1;
		constraints.gridx = 2;
		constraints.gridy = gridy;
		constraints.gridwidth = 4;
		constraints.gridheight = 1;
		panel.add(fldProjectName, constraints);
		constraints.weightx = 0;

		gridy++;
		JButton btnSelectProjectDir = GUIUtils.addButton(panel, "Browse", null,
				this, "Browse");
		constraints.gridx = 1;
		constraints.gridy = gridy;
		constraints.gridwidth = 1;
		constraints.gridheight = 1;
		layout.setConstraints(btnSelectProjectDir, constraints);

		lblShortMessage = new JLabel("Creating");
		constraints.gridx = 2;
		constraints.gridy = gridy;
		constraints.gridwidth = 1;
		constraints.gridheight = 1;
		panel.add(lblShortMessage, constraints);

		lblProjectPath = new JLabel(this.projectDir.toString());
		constraints.weightx = 1;
		constraints.gridx = 3;
		constraints.gridy = gridy;
		constraints.gridwidth = 3;
		constraints.gridheight = 1;
		panel.add(lblProjectPath, constraints);
		constraints.weightx = 0;
	}

	public File getProjectDir() {
		if (ok == true) {
			return this.projectDir;
		} else {
			return null;
		}
	}

	public void actionPerformed(ActionEvent event) {

		String command = event.getActionCommand();

		// check if user clicked "Open"
		if (command.equals("Browse")) {
			// reset project parent directory
			JFileChooser fileChooser = new JFileChooser(projectParentDir);
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			fileChooser
					.setDialogTitle("Please select a place to make project.");

			int returnVal = fileChooser.showDialog(this, "Select");
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				projectParentDir = fileChooser.getSelectedFile();

				projectDir = new File(projectParentDir, projectName + "."
						+ suffix);
				if (projectDir.exists()) {
					ok = false;
					this.lblShortMessage.setText("File exists !");
				} else if (projectParentDir.toString().substring(
						0,
						projectParentDir.toString().length()
								- ".mzmine".length()) == ".mzmine") {
					ok = false;
					this.lblShortMessage
							.setText("You can not make project in another project ");
				} else {
					ok = true;
					this.lblShortMessage.setText("Creating ");
				}
				this.lblProjectPath.setText(projectDir.toString());
			}
		}
	}

	protected class ProjectNameListener implements DocumentListener {

		public void changedUpdate(DocumentEvent e) {
			projectName = fldProjectName.getText();
			projectDir = new File(projectParentDir, projectName + "." + suffix);

			if (projectDir.exists()) {
				ok = false;
				lblShortMessage.setText("File exists !");
			} else if (projectName == "") {
				ok = false;
				lblShortMessage.setText("Input project name");
			} else {
				ok = true;
				lblShortMessage.setText("Creating ");
			}
			lblProjectPath.setText(projectDir.toString());
		}

		public void insertUpdate(DocumentEvent e) {
			changedUpdate(e);
		}

		public void removeUpdate(DocumentEvent e) {
			changedUpdate(e);
		}
	}
}
