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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.main.mzmineviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
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
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.border.EtchedBorder;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.desktop.impl.DesktopParameters;
import net.sf.mzmine.desktop.impl.ProjectTree;
import net.sf.mzmine.desktop.impl.StatusBar;
import net.sf.mzmine.desktop.impl.SwingParameters;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.main.mzmineclient.MZmineModule;
import net.sf.mzmine.modules.io.xmlimport.XMLImporter;
import net.sf.mzmine.taskcontrol.impl.TaskControllerImpl;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.NumberFormatter;
import net.sf.mzmine.util.NumberFormatter.FormatterType;
import net.sf.mzmine.util.components.TaskProgressWindow;
import net.sf.mzmine.util.dialogs.FormatSetupDialog;
import ca.guydavis.swing.desktop.CascadingWindowPositioner;
import ca.guydavis.swing.desktop.JWindowsMenu;

public class MZviewerWindow extends JFrame implements MZmineModule, Desktop,
        WindowListener, InternalFrameListener, ActionListener {

    private JDesktopPane desktopPane;
    private JMenuBar menuBar;
    private JMenu fileMenu, visualizationMenu;
    private JSplitPane split;
    private StatusBar statusBar;
    private ProjectTree itemSelector;
    private TaskProgressWindow taskList;

    private static int openFrameCount = 0;
    private static final int xOffset = 30, yOffset = 30;

	private static NumberFormatter mzFormat = new NumberFormatter(FormatterType.NUMBER, "0.000");
	private static NumberFormatter rtFormat = new NumberFormatter(FormatterType.TIME, "m:ss");
	private static NumberFormatter intensityFormat = new NumberFormatter(FormatterType.NUMBER, "0.00");

    
    public TaskProgressWindow getTaskList() {
        return taskList;
    }

    public void addInternalFrame(JInternalFrame frame) {
        desktopPane.add(frame, JLayeredPane.DEFAULT_LAYER);
        // TODO: adjust frame position
        frame.setVisible(true);
        frame.addInternalFrameListener(this);
        openFrameCount++;
        frame.setLocation(xOffset * openFrameCount, yOffset * openFrameCount);
        desktopPane.validate();
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
        switch (parentMenu) {
        case PEAKLISTFILTERING:
            break;
        case ALIGNMENT:
            break;
        case NORMALIZATION:
            break;
        case IDENTIFICATION:
            break;
        case PEAKLISTEXPORT:
            fileMenu.add(newItem);
            break;
        case VISUALIZATIONPEAKLIST:
            visualizationMenu.add(newItem);
            break;
        case DATAANALYSIS:
            break;
        case HELPSYSTEM:
            break;
        }    }


    public PeakList[] getSelectedPeakLists() {
        return itemSelector.getSelectedObjects(PeakList.class);
    }

    /**
     */
    public void initModule() {

        SwingParameters.initSwingParameters();

        try {
            BufferedImage MZmineIcon = ImageIO.read(new File(
                    "icons/MZmineIcon.png"));
            setIconImage(MZmineIcon);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Initialize item selector
        itemSelector = new ProjectTree();

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

        statusBar = new StatusBar();
        c.add(statusBar, BorderLayout.SOUTH);

        // Construct menu
        menuBar = new JMenuBar();

        fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        JMenu editMenu = new JMenu("Edit");
        JMenuItem formatMenuItem = new JMenuItem("Set number formats...");
        formatMenuItem.addActionListener(this);
        editMenu.add(formatMenuItem);
        menuBar.add(editMenu);
        visualizationMenu = new JMenu("Visualization");
        menuBar.add(visualizationMenu);
        
        JDesktopPane mainDesktopPane = getDesktopPane();
        JWindowsMenu windowsMenu = new JWindowsMenu(mainDesktopPane);
        CascadingWindowPositioner positioner = new CascadingWindowPositioner(
                mainDesktopPane);
        windowsMenu.setWindowPositioner(positioner);
        windowsMenu.getMenuComponent(2).setVisible(false);
        windowsMenu.getMenuComponent(3).setVisible(false);
        windowsMenu.setMnemonic(KeyEvent.VK_W);
        
        menuBar.add(windowsMenu);

        setJMenuBar(menuBar);

        // Initialize window listener for responding to user events
        addWindowListener(this);

        pack();

        // TODO: check screen size?
        setBounds(0, 0, 1000, 700);
        setLocationRelativeTo(null);

        // Application wants to control closing by itself
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        setTitle("MZviewer");

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
        return mzFormat;
    }

    /**
     * @see net.sf.mzmine.desktop.Desktop#getRTFormatProvider()
     */
    public NumberFormatter getRTFormat() {
        return rtFormat;
    }

    /**
     * @see net.sf.mzmine.desktop.Desktop#getIntensityFormatProvider()
     */
    public NumberFormatter getIntensityFormat() {
        return intensityFormat;
    }

    /**
     * @see net.sf.mzmine.main.mzmineclient.MZmineModule#getParameterSet()
     */
    public DesktopParameters getParameterSet() {
        return null;
    }

    /**
     * @see net.sf.mzmine.main.mzmineclient.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
     */
    public void setParameters(ParameterSet parameterValues) {
    }

    public void internalFrameActivated(InternalFrameEvent e) {
    }

    public void internalFrameClosed(InternalFrameEvent e) {
        openFrameCount--;
    }

    public void internalFrameClosing(InternalFrameEvent e) {
    }

    public void internalFrameDeactivated(InternalFrameEvent e) {
    }

    public void internalFrameDeiconified(InternalFrameEvent e) {
    }

    public void internalFrameIconified(InternalFrameEvent e) {
    }

    public void internalFrameOpened(InternalFrameEvent e) {
    }

	public JMenuItem addMenuItem(MZmineMenu parentMenu, String text,
			String toolTip, int mnemonic, boolean setAccelerator,
			ActionListener listener, String actionCommand) {
        JMenuItem newItem = new JMenuItem(text);
        if (listener != null)
            newItem.addActionListener(listener);
        if (actionCommand != null)
            newItem.setActionCommand(actionCommand);
        if (toolTip != null)
            newItem.setToolTipText(toolTip);
        if (mnemonic > 0) {
            newItem.setMnemonic(mnemonic);
            if (setAccelerator)
                newItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic,
                        ActionEvent.CTRL_MASK));
        }
        addMenuItem(parentMenu, newItem);
        return newItem;	}

	public RawDataFile[] getSelectedDataFiles() {
		return null;
	}
	
	public void addPeakLists(String[] args){
		XMLImporter.myInstance.loadPeakLists(args);
	}

	public void actionPerformed(ActionEvent e) {
        FormatSetupDialog formatDialog = new FormatSetupDialog();
        formatDialog.setVisible(true);
    }

    public void displayException(Exception e) {
        displayErrorMessage(ExceptionUtils.exceptionToString(e));
    }
    
}
