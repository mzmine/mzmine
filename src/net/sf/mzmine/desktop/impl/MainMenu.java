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

package net.sf.mzmine.desktop.impl;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.impl.ProjectManagerImpl;
import net.sf.mzmine.project.parameterssetup.ProjectParametersSetupDialog;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.dialogs.FormatSetupDialog;
import ca.guydavis.swing.desktop.CascadingWindowPositioner;
import ca.guydavis.swing.desktop.JWindowsMenu;

/**
 * This class represents main menu of the MZmine desktop
 */
public class MainMenu extends JMenuBar implements ActionListener {

    private JMenu projectMenu, rawDataMenu, peakListMenu, visualizationMenu,
            helpMenu, rawDataFilteringMenu, peakDetectionMenu, isotopesMenu,
            peakListFilteringMenu, alignmentMenu, normalizationMenu,
            identificationMenu, dataAnalysisMenu, peakListExportMenu;

    private JWindowsMenu windowsMenu;

    private JMenuItem projectOpen, projectSave, projectSaveAs,
            projectSampleParameters, projectFormats, projectSaveParameters,
            projectLoadParameters, projectExit;

    private int projectMenuIndex = 4, rawDataMenuIndex = 0,
            visualizationMenuIndex = 0;

    MainMenu() {

        /*
         * Project menu
         */

        projectMenu = new JMenu("Project");
        projectMenu.setMnemonic(KeyEvent.VK_P);
        add(projectMenu);

        projectOpen = GUIUtils.addMenuItem(projectMenu, "Open project...",
                this, KeyEvent.VK_O);

        projectSave = GUIUtils.addMenuItem(projectMenu, "Save project...",
                this, KeyEvent.VK_S);

        projectSaveAs = GUIUtils.addMenuItem(projectMenu,
                "Save project as....", this, KeyEvent.VK_A);

        projectMenu.addSeparator();

        // module items go here (e.g. batch mode)

        projectMenu.addSeparator();

        projectSampleParameters = GUIUtils.addMenuItem(projectMenu,
                "Set sample parameters...", this, KeyEvent.VK_P);

        projectMenu.addSeparator();

        projectFormats = GUIUtils.addMenuItem(projectMenu,
                "Set number formats...", this, KeyEvent.VK_F);

        projectMenu.addSeparator();

        projectSaveParameters = GUIUtils.addMenuItem(projectMenu,
                "Save MZmine parameters...", this, KeyEvent.VK_S);
        projectLoadParameters = GUIUtils.addMenuItem(projectMenu,
                "Load MZmine parameters...", this, KeyEvent.VK_L);

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
        peakListMenu.setMnemonic(KeyEvent.VK_P);
        this.add(peakListMenu);

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

        peakListExportMenu = new JMenu("Export");
        peakListExportMenu.setMnemonic(KeyEvent.VK_E);
        peakListMenu.add(peakListExportMenu);

        /*
         * Visualization menu
         */

        visualizationMenu = new JMenu("Visualization");
        visualizationMenu.setMnemonic(KeyEvent.VK_V);
        this.add(visualizationMenu);

        /*
         * Windows menu
         */

        JDesktopPane mainDesktopPane = ((MainWindow) MZmineCore.getDesktop()).getDesktopPane();
        windowsMenu = new JWindowsMenu(mainDesktopPane);
        CascadingWindowPositioner positioner = new CascadingWindowPositioner(
                mainDesktopPane);
        windowsMenu.setWindowPositioner(positioner);
        windowsMenu.setMnemonic(KeyEvent.VK_W);
        this.add(windowsMenu);

        /*
         * Help menu
         */

        helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        this.add(helpMenu);

    }

    public synchronized void addMenuItem(MZmineMenu parentMenu,
            JMenuItem newItem) {
        switch (parentMenu) {
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

    public JMenuItem addMenuItem(MZmineMenu parentMenu, String text,
            String toolTip, int mnemonic, ActionListener listener,
            String actionCommand) {

        JMenuItem newItem = new JMenuItem(text);
        if (listener != null)
            newItem.addActionListener(listener);
        if (actionCommand != null)
            newItem.setActionCommand(actionCommand);
        if (toolTip != null)
            newItem.setToolTipText(toolTip);
        if (mnemonic > 0) {
            newItem.setMnemonic(mnemonic);
            newItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic,
                    ActionEvent.CTRL_MASK));
        }
        addMenuItem(parentMenu, newItem);
        return newItem;

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        Object src = e.getSource();

        if (src == projectExit) {
            MZmineCore.exitMZmine();
        }

        if (src == projectOpen) {
            ProjectManagerImpl projectManager = ProjectManagerImpl.getInstance();
            projectManager.openProject();
        }

        if (src == projectSave) {
            ProjectManagerImpl projectManager = ProjectManagerImpl.getInstance();
            projectManager.saveProject();
        }

        if (src == projectSaveAs) {
            ProjectManagerImpl projectManager = ProjectManagerImpl.getInstance();
            projectManager.saveProjectAs();
        }

        if (src == projectSaveParameters) {
            JFileChooser chooser = new JFileChooser();
            int returnVal = chooser.showSaveDialog(MZmineCore.getDesktop().getMainFrame());
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File configFile = chooser.getSelectedFile();
                MZmineCore.saveConfiguration(configFile);
            }
        }

        if (src == projectLoadParameters) {
            JFileChooser chooser = new JFileChooser();
            int returnVal = chooser.showOpenDialog(MZmineCore.getDesktop().getMainFrame());
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File configFile = chooser.getSelectedFile();
                MZmineCore.loadConfiguration(configFile);
            }
        }

        if (src == projectSampleParameters) {
            ProjectParametersSetupDialog dialog = new ProjectParametersSetupDialog();
            dialog.setVisible(true);
        }

        if (src == projectFormats) {
            FormatSetupDialog formatDialog = new FormatSetupDialog();
            formatDialog.setVisible(true);
        }

    }
}
