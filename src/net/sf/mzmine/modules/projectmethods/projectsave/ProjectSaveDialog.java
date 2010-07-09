/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.projectmethods.projectsave;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.reflect.Method;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.components.HelpButton;
import net.sf.mzmine.util.dialogs.ExitCode;

/**
 * Project save dialog
 */
class ProjectSaveDialog extends JDialog implements ActionListener {

	private JFileChooser fileChooser;
	private JButton commitButton, cancelButton;

	private ExitCode exitCode = ExitCode.UNKNOWN;

	ProjectSaveDialog(File lastPath, String helpID) {

		super(MZmineCore.getDesktop().getMainFrame());

		JPanel mainPanel = new JPanel(new BorderLayout());

		fileChooser = new JFileChooser();

		if (lastPath != null)
			fileChooser.setCurrentDirectory(lastPath);

		fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
		fileChooser.setControlButtonsAreShown(false);

		FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"MZmine 2 projects", "mzmine");

		fileChooser.setFileFilter(filter);
		mainPanel.add(fileChooser, BorderLayout.CENTER);

		JPanel buttonsPanel = new JPanel();

		commitButton = GUIUtils.addButton(buttonsPanel, "Save project", null,
				this);

		cancelButton = GUIUtils.addButton(buttonsPanel, "Cancel", null, this);

		JButton helpButton = new HelpButton(helpID);
		buttonsPanel.add(helpButton);

		mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

		add(mainPanel);

		setModal(true);
		setTitle("Save project");
		pack();

		setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());

	}

	String getCurrentDirectory() {
		return fileChooser.getCurrentDirectory().getPath();
	}

	File getSelectedFile() {

		/*
		 * This is a workaround for a bug which causes the filename to be null
		 * unless Enter is pressed. This happens for all JFileChoosers which
		 * don't use the default control buttons. See
		 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4528663
		 */
		try {
			Method getFileNameMethod = fileChooser.getUI().getClass()
					.getDeclaredMethod("getFileName", new Class[] {});

			// We need to ensure accesibility, otherwise we get exception when
			// using GTKFileChooserUI
			getFileNameMethod.setAccessible(true);

			String fileName = (String) getFileNameMethod.invoke(
					fileChooser.getUI(), new Object[] {});

			if ((fileName == null) || (fileName.trim().length() == 0))
				return null;

			return new File(fileChooser.getCurrentDirectory(), fileName);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	ExitCode getExitCode() {
		return exitCode;
	}

	public void actionPerformed(ActionEvent e) {

		Object src = e.getSource();

		if (src == commitButton) {

			// if there is no file selected, ignore this
			if (getSelectedFile() == null)
				return;
			exitCode = ExitCode.OK;
		}

		if (src == cancelButton) {
			exitCode = ExitCode.CANCEL;
		}

		dispose();

	}

}
