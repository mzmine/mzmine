/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

package net.sf.mzmine.desktop.impl;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.preferences.MZminePreferences;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.RawDataFilesParameter;
import net.sf.mzmine.project.parameterssetup.ProjectParametersSetupDialog;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.GUIUtils;

/**
 * This class represents the main menu of MZmine desktop
 */
public class MainMenu extends JMenuBar implements ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private JMenu projectMenu, rawDataMenu, peakListMenu, visualizationMenu,
	    helpMenu, rawDataFilteringMenu, peakDetectionMenu, gapFillingMenu,
	    isotopesMenu, peakListPeakPickingMenu, peakListFilteringMenu,
	    alignmentMenu, normalizationMenu, identificationMenu,
	    dataAnalysisMenu, peakListExportMenu;

    private WindowsMenu windowsMenu;

    private JMenuItem projectSampleParameters, projectPreferences,
	    projectSaveParameters, projectLoadParameters, projectExit,
	    showAbout;

    private int projectIOMenuIndex = 0, projectMenuIndex = 1,
	    rawDataMenuIndex = 0, visualizationMenuIndex = 0;

    private Map<JMenuItem, MZmineProcessingModule> moduleMenuItems = new Hashtable<JMenuItem, MZmineProcessingModule>();

    MainMenu() {

	/*
	 * Project menu
	 */

	projectMenu = new JMenu("Project");
	projectMenu.setMnemonic(KeyEvent.VK_P);
	add(projectMenu);

	// project IO items go here (e.g. project load, save)

	projectMenu.addSeparator();

	// module items go here (e.g. batch mode)

	projectMenu.addSeparator();

	projectSampleParameters = GUIUtils.addMenuItem(projectMenu,
		"Set sample parameters...", this, KeyEvent.VK_P);

	projectMenu.addSeparator();

	projectPreferences = GUIUtils.addMenuItem(projectMenu,
		"Set preferences...", this, KeyEvent.VK_S);

	projectMenu.addSeparator();

	projectSaveParameters = GUIUtils.addMenuItem(projectMenu,
		"Save MZmine parameters...", this);
	projectLoadParameters = GUIUtils.addMenuItem(projectMenu,
		"Load MZmine parameters...", this);

	projectMenu.addSeparator();

	projectExit = GUIUtils.addMenuItem(projectMenu, "Exit", this,
		KeyEvent.VK_X, true);

	/*
	 * Raw data methods menu
	 */

	rawDataMenu = new JMenu("Raw data methods");
	rawDataMenu.setMnemonic(KeyEvent.VK_R);
	add(rawDataMenu);

	rawDataFilteringMenu = new JMenu("Filtering");
	rawDataFilteringMenu.setMnemonic(KeyEvent.VK_F);
	rawDataMenu.add(rawDataFilteringMenu);

	peakDetectionMenu = new JMenu("Peak detection");
	peakDetectionMenu.setMnemonic(KeyEvent.VK_D);
	rawDataMenu.add(peakDetectionMenu);

	/*
	 * Peak list methods menu
	 */

	peakListMenu = new JMenu("Peak list methods");
	peakListMenu.setMnemonic(KeyEvent.VK_L);
	this.add(peakListMenu);

	peakListPeakPickingMenu = new JMenu("Peak detection");
	peakListPeakPickingMenu.setMnemonic(KeyEvent.VK_P);
	peakListMenu.add(peakListPeakPickingMenu);

	gapFillingMenu = new JMenu("Gap filling");
	gapFillingMenu.setMnemonic(KeyEvent.VK_G);
	peakListMenu.add(gapFillingMenu);

	isotopesMenu = new JMenu("Isotopes");
	isotopesMenu.setMnemonic(KeyEvent.VK_D);
	peakListMenu.add(isotopesMenu);

	peakListFilteringMenu = new JMenu("Filtering");
	peakListFilteringMenu.setMnemonic(KeyEvent.VK_P);
	peakListMenu.add(peakListFilteringMenu);

	alignmentMenu = new JMenu("Alignment");
	alignmentMenu.setMnemonic(KeyEvent.VK_A);
	peakListMenu.add(alignmentMenu);

	normalizationMenu = new JMenu("Normalization");
	normalizationMenu.setMnemonic(KeyEvent.VK_N);
	peakListMenu.add(normalizationMenu);

	identificationMenu = new JMenu("Identification");
	identificationMenu.setMnemonic(KeyEvent.VK_I);
	peakListMenu.add(identificationMenu);

	dataAnalysisMenu = new JMenu("Data analysis");
	dataAnalysisMenu.setMnemonic(KeyEvent.VK_S);
	peakListMenu.add(dataAnalysisMenu);

	peakListExportMenu = new JMenu("Export/Import");
	peakListExportMenu.setMnemonic(KeyEvent.VK_E);
	peakListMenu.add(peakListExportMenu);

	/*
	 * Visualization menu
	 */

	visualizationMenu = new JMenu("Visualization");
	visualizationMenu.setMnemonic(KeyEvent.VK_V);
	this.add(visualizationMenu);

	visualizationMenu.addSeparator();

	/*
	 * Windows menu
	 */

	windowsMenu = new WindowsMenu();
	windowsMenu.setMnemonic(KeyEvent.VK_W);
	this.add(windowsMenu);

	/*
	 * Help menu
	 */

	helpMenu = new JMenu("Help");
	helpMenu.setMnemonic(KeyEvent.VK_H);
	this.add(helpMenu);

	showAbout = new JMenuItem("About MZmine 2 ...");
	showAbout.addActionListener(this);
	addMenuItem(MZmineModuleCategory.HELPSYSTEM, showAbout);

    }

    public synchronized void addMenuItem(MZmineModuleCategory parentMenu,
	    JMenuItem newItem) {
	switch (parentMenu) {
	case PROJECTIO:
	    projectMenu.add(newItem, projectIOMenuIndex);
	    projectIOMenuIndex++;
	    projectMenuIndex++;
	    break;
	case PROJECT:
	    projectMenu.add(newItem, projectMenuIndex);
	    projectMenuIndex++;
	    break;
	case RAWDATA:
	    rawDataMenu.add(newItem, rawDataMenuIndex);
	    rawDataMenuIndex++;
	    break;
	case RAWDATAFILTERING:
	    rawDataFilteringMenu.add(newItem);
	    break;
	case PEAKPICKING:
	    peakDetectionMenu.add(newItem);
	    break;
	case PEAKLISTPICKING:
	    peakListPeakPickingMenu.add(newItem);
	    break;
	case GAPFILLING:
	    gapFillingMenu.add(newItem);
	    break;
	case ISOTOPES:
	    isotopesMenu.add(newItem);
	    break;
	case PEAKLISTFILTERING:
	    peakListFilteringMenu.add(newItem);
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
	case PEAKLISTEXPORT:
	    peakListExportMenu.add(newItem);
	    break;
	case VISUALIZATIONRAWDATA:
	    visualizationMenu.add(newItem, visualizationMenuIndex);
	    visualizationMenuIndex++;
	    break;
	case VISUALIZATIONPEAKLIST:
	    visualizationMenu.add(newItem);
	    break;
	case DATAANALYSIS:
	    dataAnalysisMenu.add(newItem);
	    break;
	case HELPSYSTEM:
	    helpMenu.add(newItem);
	    break;
	}
    }

    public void addMenuItemForModule(MZmineProcessingModule module) {

	MZmineModuleCategory parentMenu = module.getModuleCategory();
	String menuItemText = module.getName();
	String menuItemToolTip = module.getDescription();

	JMenuItem newItem = new JMenuItem(menuItemText);
	newItem.setToolTipText(menuItemToolTip);
	newItem.addActionListener(this);

	moduleMenuItems.put(newItem, module);

	addMenuItem(parentMenu, newItem);

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

	Object src = e.getSource();

	MZmineProcessingModule module = moduleMenuItems.get(src);
	if (module != null) {
	    RawDataFile selectedFiles[] = MZmineCore.getDesktop()
		    .getSelectedDataFiles();
	    PeakList selectedPeakLists[] = MZmineCore.getDesktop()
		    .getSelectedPeakLists();

	    ParameterSet moduleParameters = MZmineCore.getConfiguration()
		    .getModuleParameters(module.getClass());

	    boolean allParametersOK = true;
	    LinkedList<String> errorMessages = new LinkedList<String>();
	    for (Parameter p : moduleParameters.getParameters()) {
		if (p instanceof RawDataFilesParameter) {
		    RawDataFilesParameter rdp = (RawDataFilesParameter) p;
		    rdp.setValue(selectedFiles);
		    boolean checkOK = rdp.checkValue(errorMessages);
		    if (!checkOK) {
			allParametersOK = false;
		    }
		}
		if (p instanceof PeakListsParameter) {
		    PeakListsParameter plp = (PeakListsParameter) p;
		    plp.setValue(selectedPeakLists);
		    boolean checkOK = plp.checkValue(errorMessages);
		    if (!checkOK) {
			allParametersOK = false;
		    }
		}
	    }

	    if (!allParametersOK) {
		StringBuilder message = new StringBuilder();
		for (String m : errorMessages) {
		    message.append(m);
		    message.append("\n");
		}
		MZmineCore.getDesktop().displayMessage(message.toString());
		return;
	    }

	    logger.finest("Setting parameters for module " + module.getName());
	    ExitCode exitCode = moduleParameters.showSetupDialog();
	    if (exitCode == ExitCode.OK) {
		ParameterSet parametersCopy = moduleParameters.cloneParameter();
		logger.finest("Starting module " + module.getName() + " with parameters "
			+ parametersCopy);
		ArrayList<Task> tasks = new ArrayList<Task>();
		module.runModule(parametersCopy, tasks);
		MZmineCore.getTaskController().addTasks(
			tasks.toArray(new Task[0]));
	    }
	    return;
	}

	if (src == projectExit) {
	    MZmineCore.getDesktop().exitMZmine();
	}

	if (src == projectSaveParameters) {
	    JFileChooser chooser = new JFileChooser();
	    int returnVal = chooser.showSaveDialog(MZmineCore.getDesktop()
		    .getMainFrame());
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
		File configFile = chooser.getSelectedFile();
		try {
		    MZmineCore.getConfiguration().saveConfiguration(configFile);
		} catch (Exception ex) {
		    MZmineCore.getDesktop().displayException(ex);
		}
	    }
	}

	if (src == projectLoadParameters) {
	    JFileChooser chooser = new JFileChooser();
	    int returnVal = chooser.showOpenDialog(MZmineCore.getDesktop()
		    .getMainFrame());
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
		File configFile = chooser.getSelectedFile();
		try {
		    MZmineCore.getConfiguration().loadConfiguration(configFile);
		} catch (Exception ex) {
		    MZmineCore.getDesktop().displayException(ex);
		}
	    }
	}

	if (src == projectSampleParameters) {
	    ProjectParametersSetupDialog dialog = new ProjectParametersSetupDialog();
	    dialog.setVisible(true);
	}

	if (src == projectPreferences) {
	    MZminePreferences preferences = MZmineCore.getConfiguration()
		    .getPreferences();
	    preferences.showSetupDialog();
	}

	if (src == showAbout) {
	    MainWindow mainWindow = (MainWindow) MZmineCore.getDesktop();
	    mainWindow.showAboutDialog();
	}

    }
}
