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

package net.sf.mzmine.userinterface.mainwindow;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import net.sf.mzmine.main.MZmineClient;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.impl.TaskControllerImpl;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.AboutDialog;
import net.sf.mzmine.userinterface.dialogs.FileOpenDialog;
import net.sf.mzmine.userinterface.dialogs.FormatSetupDialog;
import net.sf.mzmine.userinterface.dialogs.ProjectCreateDialog;
import net.sf.mzmine.userinterface.dialogs.ProjectOpenDialog;
import net.sf.mzmine.util.GUIUtils;
import ca.guydavis.swing.desktop.CascadingWindowPositioner;
import ca.guydavis.swing.desktop.JWindowsMenu;

/**
 * 
 */
class MainMenu extends JMenuBar implements ActionListener {

	private JMenu projectMenu, filterMenu, peakMenu, alignmentMenu,
			normalizationMenu, identificationMenu, rawDataVisualizationMenu,
			dataAnalysisMenu, helpMenu;

	private JWindowsMenu windowsMenu;

	private JMenuItem projectCreate, projectOpen, projectRestore, projectSave,
			projectSaveAs, projectExperimentalParameters, projectFormats,
			projectSaveParameters, projectLoadParameters, projectExit,
			hlpAbout;

	MainMenu() {

		projectMenu = new JMenu("Project");
		projectMenu.setMnemonic(KeyEvent.VK_P);
		add(projectMenu);
		projectCreate = GUIUtils.addMenuItem(projectMenu,
				"Create new project...", this, KeyEvent.VK_N, true);

		projectRestore = GUIUtils.addMenuItem(projectMenu, "Open project...",
				this, KeyEvent.VK_O, true);
		projectSave = GUIUtils.addMenuItem(projectMenu, "Save project...",
				this, KeyEvent.VK_S, false);
		projectSaveAs = GUIUtils.addMenuItem(projectMenu, "Save project as...",
				this, KeyEvent.VK_S, false);
		projectMenu.addSeparator();

		projectOpen = GUIUtils.addMenuItem(projectMenu, "Import raw data...",
				this, KeyEvent.VK_I, true);

		projectMenu.addSeparator();

		projectExperimentalParameters = GUIUtils.addMenuItem(projectMenu,
				"Set project parameters...", this, KeyEvent.VK_P);

		projectFormats = GUIUtils.addMenuItem(projectMenu,
				"Set number formats...", this, KeyEvent.VK_F);

		projectMenu.addSeparator();
		// module items go here (e.g. batch mode)
		projectMenu.addSeparator();

		projectSaveParameters = GUIUtils.addMenuItem(projectMenu,
				"Save MZmine parameters...", this, KeyEvent.VK_S);
		projectLoadParameters = GUIUtils.addMenuItem(projectMenu,
				"Load MZmine parameters...", this, KeyEvent.VK_L);

		projectMenu.addSeparator();
		projectExit = GUIUtils.addMenuItem(projectMenu, "Exit", this,
				KeyEvent.VK_X, true);

		filterMenu = new JMenu("Raw data filtering");
		filterMenu.setMnemonic(KeyEvent.VK_F);
		this.add(filterMenu);

		peakMenu = new JMenu("Peak detection");
		peakMenu.setMnemonic(KeyEvent.VK_D);
		this.add(peakMenu);

		alignmentMenu = new JMenu("Alignment");
		alignmentMenu.setMnemonic(KeyEvent.VK_A);
		this.add(alignmentMenu);

		normalizationMenu = new JMenu("Normalization");
		normalizationMenu.setMnemonic(KeyEvent.VK_N);
		this.add(normalizationMenu);

		identificationMenu = new JMenu("Identification");
		identificationMenu.setMnemonic(KeyEvent.VK_I);
		this.add(identificationMenu);

		rawDataVisualizationMenu = new JMenu("Visualization");
		rawDataVisualizationMenu.setMnemonic(KeyEvent.VK_V);
		this.add(rawDataVisualizationMenu);

		dataAnalysisMenu = new JMenu("Data analysis");
		dataAnalysisMenu.setMnemonic(KeyEvent.VK_S);
		this.add(dataAnalysisMenu);

		JDesktopPane mainDesktopPane = ((MainWindow) MZmineCore.getDesktop())
				.getDesktopPane();
		windowsMenu = new JWindowsMenu(mainDesktopPane);
		CascadingWindowPositioner positioner = new CascadingWindowPositioner(
				mainDesktopPane);
		windowsMenu.setWindowPositioner(positioner);
		windowsMenu.setMnemonic(KeyEvent.VK_W);
		this.add(windowsMenu);

		helpMenu = new JMenu("Help");
		helpMenu.setMnemonic(KeyEvent.VK_H);
		this.add(helpMenu);

		hlpAbout = GUIUtils.addMenuItem(helpMenu, "About MZmine...", this,
				KeyEvent.VK_A);

	}

	public void addMenuItem(MZmineMenu parentMenu, JMenuItem newItem) {
		switch (parentMenu) {
		case PROJECT:
			projectMenu.add(newItem, 5);
			break;
		case FILTERING:
			filterMenu.add(newItem);
			break;
		case PEAKPICKING:
			peakMenu.add(newItem);
			break;
		case ALIGNMENT:
			alignmentMenu.add(newItem);
			break;
		case NORMALIZATION:
			normalizationMenu.add(newItem);
			break;
		case IDENTIFICATION:
			identificationMenu.add(newItem);
			break;

		case VISUALIZATION:
			rawDataVisualizationMenu.add(newItem);
			break;
		case ANALYSIS:
			dataAnalysisMenu.add(newItem);
			break;
		}
	}

	public JMenuItem addMenuItem(MZmineMenu parentMenu, String text,
			ActionListener listener, String actionCommand, int mnemonic,
			boolean setAccelerator, boolean enabled) {

		JMenuItem newItem = new JMenuItem(text);
		if (listener != null)
			newItem.addActionListener(listener);
		if (actionCommand != null)
			newItem.setActionCommand(actionCommand);
		if (mnemonic > 0)
			newItem.setMnemonic(mnemonic);
		if (setAccelerator)
			newItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic,
					ActionEvent.CTRL_MASK));
		newItem.setEnabled(enabled);
		addMenuItem(parentMenu, newItem);
		return newItem;

	}

	public void addMenuSeparator(MZmineMenu parentMenu) {
		switch (parentMenu) {
		case FILTERING:
			filterMenu.addSeparator();
			break;
		case PEAKPICKING:
			peakMenu.addSeparator();
			break;
		case ALIGNMENT:
			alignmentMenu.addSeparator();
			break;
		case NORMALIZATION:
			normalizationMenu.addSeparator();
			break;
		case IDENTIFICATION:
			identificationMenu.addSeparator();
			break;

		case VISUALIZATION:
			rawDataVisualizationMenu.addSeparator();
			break;
		case ANALYSIS:
			dataAnalysisMenu.addSeparator();
			break;

		}
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {

		Object src = e.getSource();

		if (src == projectExit) {

			// int result =
			// JOptionPane.showConfirmDialog(MZmineCore.getDesktop()
			// .getMainFrame(),
			// "Do you want to save project before exitting mzmine? ",
			// "Exitting mzmine ...", JOptionPane.OK_CANCEL_OPTION,
			// JOptionPane.QUESTION_MESSAGE);
			// if (result == JOptionPane.YES_OPTION) {
			// try {
			// File projectDir = MZmineCore.getCurrentProject()
			// .getLocation();
			// MZmineClient.getInstance().getProjectManager().saveProject(
			// projectDir);
			// } catch (IOException e1) {
			// JOptionPane.showMessageDialog(this, "Project Saving Error",
			// "Error", JOptionPane.ERROR_MESSAGE);
			// }
			// }
			while (((TaskControllerImpl) MZmineCore.getTaskController())
					.getTaskExists()) {
				// wait unitl all task to finish
			}
			MZmineCore.exitMZmine();
		}

		if (src == projectCreate) {
			int result = JOptionPane
					.showConfirmDialog(
							MZmineCore.getDesktop().getMainFrame(),
							"You are going close current project and creating new project",
							"Creating new project",
							JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.QUESTION_MESSAGE);
			if (result == JOptionPane.YES_OPTION) {
				DesktopParameters parameters = (DesktopParameters) MZmineCore
						.getDesktop().getParameterSet();
                String lastPath = parameters.getLastOpenProjectPath();
                if (lastPath == null) lastPath = "";
                File lastProjectPath = new File(lastPath);
				ProjectCreateDialog projectCreateDialog = new ProjectCreateDialog(
                        lastProjectPath, ProjectCreateDialog.DialogType.Create);
				projectCreateDialog.setVisible(true);
			}

		}
		if (src == projectRestore) {
			DesktopParameters parameters = (DesktopParameters) MZmineCore
					.getDesktop().getParameterSet();
            String lastPath = parameters.getLastOpenPath();
            if (lastPath == null) lastPath = "";
            File lastDir = new File(lastPath);
            ProjectOpenDialog projectOpenDialog = new ProjectOpenDialog(
                    lastDir);
			projectOpenDialog.setVisible(true);
		}
		if (src == projectSave) {
			try {
				if (MZmineCore.getCurrentProject().getIsTemporal() == true) {
					// exec save as
					DesktopParameters parameters = (DesktopParameters) MZmineCore
							.getDesktop().getParameterSet();
                    String lastPath = parameters.getLastOpenProjectPath();
                    if (lastPath == null) lastPath = "";
                    File lastProjectPath = new File(lastPath);
                    ProjectCreateDialog projectSaveAsDialog = new ProjectCreateDialog(
							lastProjectPath,ProjectCreateDialog.DialogType.SaveAs);
					projectSaveAsDialog.setVisible(true);

				} else {

					File projectDir = MZmineCore.getCurrentProject()
							.getLocation();
					MZmineClient.getInstance().getProjectManager().saveProject(
							projectDir);
				}
			} catch (IOException e1) {
				JOptionPane.showMessageDialog(this, "Project Saving Error",
						"Error", JOptionPane.ERROR_MESSAGE);
			}
		}

		if (src == projectSaveAs) {
			DesktopParameters parameters = (DesktopParameters) MZmineCore
					.getDesktop().getParameterSet();
            String lastPath = parameters.getLastOpenProjectPath();
            if (lastPath == null) lastPath = "";
			File lastProjectPath = new File(lastPath);
			ProjectCreateDialog projectSaveAsDialog = new ProjectCreateDialog(
					lastProjectPath, ProjectCreateDialog.DialogType.SaveAs);
			projectSaveAsDialog.setVisible(true);
		}
		if (src == projectOpen) {
			DesktopParameters parameters = (DesktopParameters) MZmineCore
					.getDesktop().getParameterSet();
            String lastPath = parameters.getLastOpenProjectPath();
            if (lastPath == null) lastPath = "";
			FileOpenDialog fileOpenDialog = new FileOpenDialog(lastPath);
			fileOpenDialog.setVisible(true);
			parameters.setLastOpenPath(fileOpenDialog.getCurrentDirectory());
		}

		if (src == projectSaveParameters) {
			JFileChooser chooser = new JFileChooser();
			int returnVal = chooser.showSaveDialog(MZmineCore.getDesktop()
					.getMainFrame());
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File configFile = chooser.getSelectedFile();
				MZmineCore.saveConfiguration(configFile);
			}
		}

		if (src == projectLoadParameters) {
			JFileChooser chooser = new JFileChooser();
			int returnVal = chooser.showOpenDialog(MZmineCore.getDesktop()
					.getMainFrame());
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File configFile = chooser.getSelectedFile();
				MZmineCore.loadConfiguration(configFile);
			}
		}

		if (src == projectExperimentalParameters) {
			/*
			ExperimentalParametersSetupDialog dialog = new ExperimentalParametersSetupDialog();
			dialog.setVisible(true);
			*/
		}

		if (src == projectFormats) {
			FormatSetupDialog formatDialog = new FormatSetupDialog();
			formatDialog.setVisible(true);
		}

		// Help->About
		if (src == hlpAbout) {
			AboutDialog dialog = new AboutDialog();
			dialog.setVisible(true);
		}

	}

}
