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

package net.sf.mzmine.project.impl;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;

import net.sf.mzmine.desktop.impl.DesktopParameters;
import net.sf.mzmine.main.MZmineClient;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.GUIUtils;

/**
 * Project Create Dialog
 */
public class ProjectCreateDialog extends JDialog implements ActionListener {

	static final int PADDING_SIZE = 5;
	private Logger logger = Logger.getLogger(this.getClass().getName());;
	private String frameTitle;
	private String message;
	private String buttonName;
	private ProjectCreatePane createProjectPane;
	private DialogType dialogType;

	public enum DialogType {
		Create, SaveAs
	}

	public ProjectCreateDialog(File lastProjectDir, DialogType dialogType) {
		super(MZmineCore.getDesktop().getMainFrame(), true);
		this.dialogType = dialogType;
		setup(lastProjectDir);
	}

	public ProjectCreateDialog(File lastProjectDir) {
		super(MZmineCore.getDesktop().getMainFrame(), true);
		this.dialogType = DialogType.Create;
		setup(lastProjectDir);
	}

	protected void setup(File lastProjectDir) {
		if (this.dialogType == DialogType.SaveAs) {
			this.frameTitle = "Saving project...";
			this.message = "Please select a name to save project as";
			this.buttonName = "Save";
		} else {
			this.frameTitle = "Creating new project...";
			this.message = "Please create a directory for the new project";
			this.buttonName = "Create";
		}
		super.setTitle(this.frameTitle);

		// set default project directory
		File projectParentDir = lastProjectDir.getParentFile();
		if (projectParentDir == null || !projectParentDir.exists()) {
			projectParentDir = new File("/");
		}
		createProjectPane = new ProjectCreatePane(projectParentDir);

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

		comp = GUIUtils.addLabel(panel, message);
		constraints.gridx = 0;
		constraints.gridy = gridy;
		constraints.gridwidth = 1;
		constraints.gridheight = 1;
		constraints.weighty = 0;
		layout.setConstraints(comp, constraints);

		gridy++;
		panel.add(createProjectPane);
		constraints.gridx = 0;
		constraints.gridy = gridy;
		constraints.gridwidth = 5;
		constraints.gridheight = 1;
		constraints.weighty = 0;
		constraints.weightx = 1;
		layout.setConstraints(createProjectPane, constraints);

		gridy++;
		comp = GUIUtils.addSeparator(panel, PADDING_SIZE);
		constraints.gridx = 0;
		constraints.gridy = gridy;
		constraints.gridwidth = 5;
		constraints.gridheight = 1;
		constraints.weighty = 0;
		layout.setConstraints(comp, constraints);

		gridy++;
		JButton btnCreate = GUIUtils.addButton(panel, buttonName, null, this,
				"Create");
		constraints.gridx = 3;
		constraints.gridy = gridy;
		constraints.gridwidth = 1;
		constraints.gridheight = 1;
		constraints.weighty = 0;
		layout.setConstraints(btnCreate, constraints);

		JButton btnQuit = GUIUtils.addButton(panel, "Cancel", null, this,
				"Cancel");
		constraints.gridx = 4;
		constraints.gridy = gridy;
		constraints.gridwidth = 1;
		constraints.gridheight = 1;
		constraints.weighty = 1;
		layout.setConstraints(btnQuit, constraints);

		add(panel);
		pack();
		setResizable(true);
		setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent event) {

		String command = event.getActionCommand();

		// check if user clicked "Open"
		if (command.equals("Create")) {
			File projectDir = this.createProjectPane.getProjectDir();

			if (projectDir == null) {
				return;
			} else {
				try {
					if (this.dialogType == DialogType.Create) {
						MZmineClient.getInstance().getProjectManager()
								.createProject(projectDir);
					} else {
						MZmineClient.getInstance().getProjectManager()
								.saveProject(projectDir);
					}
					DesktopParameters parameters = (DesktopParameters) MZmineCore
							.getDesktop().getParameterSet();
					parameters.setLastOpenProjectPath(projectDir.toString());
					// discard this dialog
					dispose();
				} catch (Throwable e) {
					logger.fine("Could not create new project");
				}
				// leave this dialog open
			}
		} else if (command.equals("Cancel")) {
			dispose();
		}
	}
}
