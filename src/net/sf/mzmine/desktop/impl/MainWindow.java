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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.desktop.helpsystem.HelpMainMenuItem;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.impl.TaskControllerImpl;
import net.sf.mzmine.util.NumberFormatter;
import net.sf.mzmine.util.components.TaskProgressWindow;

/**
 * This class is the main window of application
 * 
 */
public class MainWindow extends JFrame implements MZmineModule, Desktop,
        WindowListener {

    private DesktopParameters parameters;

    private JDesktopPane desktopPane;

    private JSplitPane split;

    private ItemSelector itemSelector;

    private TaskProgressWindow taskList;

    private HelpMainMenuItem help;

    public TaskProgressWindow getTaskList() {
        return taskList;
    }

    private MainMenu menuBar;

    private Statusbar statusBar;

    public MainMenu getMainMenu() {
        return menuBar;
    }

    public void addInternalFrame(JInternalFrame frame) {
        desktopPane.add(frame, JLayeredPane.DEFAULT_LAYER);
        // TODO: adjust frame position
        frame.setVisible(true);
    }

    /**
     * This method returns the desktop
     */
    public JDesktopPane getDesktopPane() {
        return desktopPane;
    }

    /**
     * WindowListener interface implementation
     */
    public void windowOpened(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        MZmineCore.exitMZmine();
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void setStatusBarText(String text) {
        setStatusBarText(text, Color.black);
    }

    /**
     * @see net.sf.mzmine.desktop.Desktop#displayMessage(java.lang.String)
     */
    public void displayMessage(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Message",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void displayErrorMessage(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Sorry",
                JOptionPane.ERROR_MESSAGE);
    }

    public void addMenuItem(MZmineMenu parentMenu, JMenuItem newItem) {
        menuBar.addMenuItem(parentMenu, newItem);
    }

    /**
     * @see net.sf.mzmine.desktop.Desktop#getSelectedDataFiles()
     */
    public RawDataFile[] getSelectedDataFiles() {
        return itemSelector.getSelectedRawData();
    }

    public PeakList[] getSelectedPeakLists() {
        return itemSelector.getSelectedPeakLists();
    }

    /**
     */
    public void initModule() {

        SwingParameters.initSwingParameters();

        parameters = new DesktopParameters();

        try {
            BufferedImage MZmineIcon = ImageIO.read(new File(
                    "icons/MZmineIcon.png"));
            setIconImage(MZmineIcon);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Initialize item selector
        itemSelector = new ItemSelector(this);

        // Place objects on main window
        desktopPane = new JDesktopPane();
        desktopPane.setBackground(new Color(65, 105, 170));

        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, itemSelector,
                desktopPane);

        desktopPane.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);

        desktopPane.setBorder(new EtchedBorder(EtchedBorder.RAISED));
        Container c = getContentPane();
        c.setLayout(new BorderLayout());
        c.add(split, BorderLayout.CENTER);

        statusBar = new Statusbar();
        c.add(statusBar, BorderLayout.SOUTH);

        // Construct menu
        menuBar = new MainMenu();
        help = new HelpMainMenuItem();
        help.addMenuItem(menuBar);
        setJMenuBar(menuBar);

        // Initialize window listener for responding to user events
        addWindowListener(this);

        pack();

        // TODO: check screen size?
        setBounds(0, 0, 1000, 700);
        setLocationRelativeTo(null);

        // Application wants to control closing by itself
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        setTitle("MZmine 2");

        taskList = new TaskProgressWindow(
                (TaskControllerImpl) MZmineCore.getTaskController());
        desktopPane.add(taskList, JLayeredPane.DEFAULT_LAYER);

    }

    /**
     * @see net.sf.mzmine.desktop.Desktop#getMainFrame()
     */
    public JFrame getMainFrame() {
        return this;
    }

    public HelpMainMenuItem getHelp() {
        return help;
    }

    /**
     * @see net.sf.mzmine.desktop.Desktop#addMenuItem(net.sf.mzmine.desktop.Desktop.BatchStepCategory,
     *      java.lang.String, java.awt.event.ActionListener, java.lang.String,
     *      int, boolean, boolean)
     */
    public JMenuItem addMenuItem(MZmineMenu parentMenu, String text,
            String toolTip, int mnemonic, ActionListener listener,
            String actionCommand) {
        return menuBar.addMenuItem(parentMenu, text, toolTip, mnemonic,
                listener, actionCommand);
    }

    /**
     * @see net.sf.mzmine.desktop.Desktop#addMenuSeparator(net.sf.mzmine.desktop.Desktop.BatchStepCategory)
     */
    public void addMenuSeparator(MZmineMenu parentMenu) {
        menuBar.addMenuSeparator(parentMenu);

    }

    /**
     * @see net.sf.mzmine.desktop.Desktop#getSelectedFrame()
     */
    public JInternalFrame getSelectedFrame() {
        return desktopPane.getSelectedFrame();
    }

    /**
     * @see net.sf.mzmine.desktop.Desktop#getInternalFrames()
     */
    public JInternalFrame[] getInternalFrames() {
        return desktopPane.getAllFrames();
    }

    /**
     * @see net.sf.mzmine.desktop.Desktop#setStatusBarText(java.lang.String,
     *      java.awt.Color)
     */
    public void setStatusBarText(String text, Color textColor) {
        statusBar.setStatusText(text, textColor);
    }

    /**
     * @see net.sf.mzmine.desktop.Desktop#getMZFormatProvider()
     */
    public NumberFormatter getMZFormat() {
        return parameters.getMZFormat();
    }

    /**
     * @see net.sf.mzmine.desktop.Desktop#getRTFormatProvider()
     */
    public NumberFormatter getRTFormat() {
        return parameters.getRTFormat();
    }

    /**
     * @see net.sf.mzmine.desktop.Desktop#getIntensityFormatProvider()
     */
    public NumberFormatter getIntensityFormat() {
        return parameters.getIntensityFormat();
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
     */
    public DesktopParameters getParameterSet() {
        return parameters;
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
     */
    public void setParameters(ParameterSet parameterValues) {
        this.parameters = (DesktopParameters) parameterValues;
    }

    public ItemSelector getItemSelector() {
        return itemSelector;
    }

    public void reloadProject() {
        MZmineProject project = MZmineCore.getCurrentProject();
        
        itemSelector.reloadDataModel();

        if (project.getProjectFile() != null) {
            String projectName = project.getProjectFile().getName();
            if (projectName.endsWith(".mzmine")) {
                projectName = projectName.substring(0, projectName.length() - 7);
            }
            setTitle("MZmine 2: " + projectName);
        }

    }

}
