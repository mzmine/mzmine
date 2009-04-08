/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.desktop.impl;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.desktop.helpsystem.HelpMainMenuItem;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.main.mzmineclient.MZmineModule;
import net.sf.mzmine.project.ProjectEvent;
import net.sf.mzmine.project.ProjectListener;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.NumberFormatter;

/**
 * This class is the main window of application
 * 
 */
public class MainWindow extends JFrame implements MZmineModule, Desktop,
        WindowListener, ProjectListener {

    private DesktopParameters parameters;

    private HelpMainMenuItem help;

    private MainPanel mainPanel;

    private MainMenu menuBar;

    public MainMenu getMainMenu() {
        return menuBar;
    }

    public void addInternalFrame(JInternalFrame frame) {
        mainPanel.addInternalFrame(frame);
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
     */
    public void displayMessage(String msg) {
        displayMessage("Message", msg);
    }

    /**
     */
    public void displayMessage(String title, String msg) {
        JOptionPane.showMessageDialog(this, msg, title,
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void displayErrorMessage(String msg) {
        displayErrorMessage("Error", msg);
    }

    public void displayErrorMessage(String title, String msg) {
        JOptionPane.showMessageDialog(this, msg, title,
                JOptionPane.ERROR_MESSAGE);
    }

    public void addMenuItem(MZmineMenu parentMenu, JMenuItem newItem) {
        menuBar.addMenuItem(parentMenu, newItem);
    }

    /**
     * @see net.sf.mzmine.desktop.Desktop#getSelectedDataFiles()
     */
    public RawDataFile[] getSelectedDataFiles() {
        return mainPanel.getProjectTree().getSelectedObjects(RawDataFile.class);
    }

    public PeakList[] getSelectedPeakLists() {
        return mainPanel.getProjectTree().getSelectedObjects(PeakList.class);
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

        mainPanel = new MainPanel();
        add(mainPanel);

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

        updateTitle();

        

        MZmineCore.getProjectManager().addProjectListener(this);

    }
    
    void updateTitle() {
        String projectName = MZmineCore.getCurrentProject().toString();
        setTitle("MZmine 2: " + projectName);
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
            String toolTip, int mnemonic, boolean setAccelerator,
            ActionListener listener, String actionCommand) {
        return menuBar.addMenuItem(parentMenu, text, toolTip, mnemonic,
                setAccelerator, listener, actionCommand);
    }

    /**
     * @see net.sf.mzmine.desktop.Desktop#setStatusBarText(java.lang.String,
     *      java.awt.Color)
     */
    public void setStatusBarText(String text, Color textColor) {
        mainPanel.getStatusBar().setStatusText(text, textColor);
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
     * @see net.sf.mzmine.main.mzmineclient.MZmineModule#getParameterSet()
     */
    public DesktopParameters getParameterSet() {
        return parameters;
    }

    /**
     * @see net.sf.mzmine.main.mzmineclient.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
     */
    public void setParameters(ParameterSet parameterValues) {
        this.parameters = (DesktopParameters) parameterValues;
    }

    public void projectModified(ProjectEvent event) {
        updateTitle();
    }

    public void displayException(Exception e) {
        displayErrorMessage(ExceptionUtils.exceptionToString(e));
    }

    MainPanel getMainPanel() {
        return mainPanel;
    }

    public JInternalFrame[] getInternalFrames() {
        return mainPanel.getDesktopPane().getAllFrames();
    }

    public JInternalFrame getSelectedFrame() {
        return mainPanel.getDesktopPane().getSelectedFrame();
    }

}
