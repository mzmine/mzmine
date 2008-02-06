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

package net.sf.mzmine.userinterface.mainwindow;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.io.PreloadLevel;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.taskcontrol.impl.TaskControllerImpl;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.components.TaskProgressWindow;
import net.sf.mzmine.userinterface.dialogs.AboutDialog;
import net.sf.mzmine.util.NumberFormatter;

import org.simplericity.macify.eawt.Application;
import org.simplericity.macify.eawt.ApplicationEvent;
import org.simplericity.macify.eawt.ApplicationListener;
import org.simplericity.macify.eawt.DefaultApplication;

/**
 * This class is the main window of application
 * 
 */
public class MainWindow extends JFrame implements MZmineModule, Desktop,
        WindowListener, ApplicationListener {

    // default tooltip displaying and dismissing delay in ms
    public static final int DEFAULT_TOOLTIP_DELAY = 50;
    public static final int DEFAULT_TOOLTIP_DISMISS_DELAY = Integer.MAX_VALUE;

    private DesktopParameters parameters;

    private JDesktopPane desktopPane;

    private JSplitPane split;

    private ItemSelector itemSelector;

    private TaskProgressWindow taskList;

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
     * @see net.sf.mzmine.userinterface.Desktop#displayMessage(java.lang.String)
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
     * @see net.sf.mzmine.userinterface.Desktop#getSelectedDataFiles()
     */
    public RawDataFile[] getSelectedDataFiles() {
        return itemSelector.getSelectedRawData();
    }

    public PeakList[] getSelectedPeakLists() {
        return itemSelector.getSelectedPeakLists();
    }

    public void addAlignedPeakList(PeakList alignmentResult) {
        itemSelector.addPeakList(alignmentResult);
    }

    public void addDataFile(RawDataFile dataFile) {
        itemSelector.addRawData(dataFile);
    }

    public void removeAlignedPeakList(PeakList alignmentResult) {
        itemSelector.removePeakList(alignmentResult);
    }

    public void removeDataFile(RawDataFile dataFile) {
        itemSelector.removeRawData(dataFile);
    }



    /**
     */
    public void initModule() {

        parameters = new DesktopParameters();

        // Create an abstract Application, for better Mac OS X support (using
        // macify library http://simplericity.org/macify/)
        Application application = new DefaultApplication();
        application.addApplicationListener(this);
        

        try {
            BufferedImage MZmineIcon = ImageIO.read(new File(
                    "icons/MZmineIcon.png"));
            setIconImage(MZmineIcon);
            application.setApplicationIconImage(MZmineIcon);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Font defaultFont = new Font("SansSerif", Font.PLAIN, 13);
        Font smallFont = new Font("SansSerif", Font.PLAIN, 11);
        Font tinyFont = new Font("SansSerif", Font.PLAIN, 10);
        Enumeration keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof Font)
                UIManager.put(key, defaultFont);
        }
        UIManager.put("List.font", smallFont);
        UIManager.put("Table.font", smallFont);
        UIManager.put("ToolTip.font", tinyFont);

        // Initialize item selector
        itemSelector = new ItemSelector(this);

        // Place objects on main window
        desktopPane = new JDesktopPane();

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

        ToolTipManager tooltipManager = ToolTipManager.sharedInstance();
        tooltipManager.setInitialDelay(DEFAULT_TOOLTIP_DELAY);
        tooltipManager.setDismissDelay(DEFAULT_TOOLTIP_DISMISS_DELAY);

    }

    /**
     * @see net.sf.mzmine.userinterface.Desktop#getMainFrame()
     */
    public JFrame getMainFrame() {
        return this;
    }

    /**
     * @see net.sf.mzmine.userinterface.Desktop#isDataFileSelected()
     */
    public boolean isDataFileSelected() {
        return itemSelector.getSelectedRawData().length > 0;
    }

    /**
     * @see net.sf.mzmine.userinterface.Desktop#isDataFileSelected()
     */
    public boolean isPeakListSelected() {
        return itemSelector.getSelectedPeakLists().length > 0;
    }

    /**
     * @see net.sf.mzmine.userinterface.Desktop#addMenuItem(net.sf.mzmine.userinterface.Desktop.MZmineMenu,
     *      java.lang.String, java.awt.event.ActionListener, java.lang.String,
     *      int, boolean, boolean)
     */
    public JMenuItem addMenuItem(MZmineMenu parentMenu, String text,
            ActionListener listener, String actionCommand, int mnemonic,
            boolean setAccelerator, boolean enabled) {
        return menuBar.addMenuItem(parentMenu, text, listener, actionCommand,
                mnemonic, setAccelerator, enabled);
    }

    /**
     * @see net.sf.mzmine.userinterface.Desktop#addMenuSeparator(net.sf.mzmine.userinterface.Desktop.MZmineMenu)
     */
    public void addMenuSeparator(MZmineMenu parentMenu) {
        menuBar.addMenuSeparator(parentMenu);

    }

    /**
     * @see net.sf.mzmine.userinterface.Desktop#getSelectedFrame()
     */
    public JInternalFrame getSelectedFrame() {
        return desktopPane.getSelectedFrame();
    }

    /**
     * @see net.sf.mzmine.userinterface.Desktop#getVisibleFrames()
     */
    public JInternalFrame[] getVisibleFrames() {
        return getVisibleFrames(JInternalFrame.class);
    }

    /**
     * @see net.sf.mzmine.userinterface.Desktop#getVisibleFrames()
     */
    public JInternalFrame[] getVisibleFrames(Class frameClass) {

        JInternalFrame[] allFrames = desktopPane.getAllFrames();

        ArrayList<JInternalFrame> visibleFrames = new ArrayList<JInternalFrame>();
        for (JInternalFrame frame : allFrames)
            if (frame.isVisible() && (frameClass.isInstance(frame)))
                visibleFrames.add(frame);

        return visibleFrames.toArray(new JInternalFrame[0]);
    }

    /**
     * @see net.sf.mzmine.userinterface.Desktop#setStatusBarText(java.lang.String,
     *      java.awt.Color)
     */
    public void setStatusBarText(String text, Color textColor) {
        statusBar.setStatusText(text, textColor);
    }

    public Statusbar getStatusBar() {
        return statusBar;
    }

    /**
     * @see net.sf.mzmine.userinterface.Desktop#getMZFormatProvider()
     */
    public NumberFormatter getMZFormat() {
        return parameters.getMZFormat();
    }

    /**
     * @see net.sf.mzmine.userinterface.Desktop#getRTFormatProvider()
     */
    public NumberFormatter getRTFormat() {
        return parameters.getRTFormat();
    }

    /**
     * @see net.sf.mzmine.userinterface.Desktop#getIntensityFormatProvider()
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

    public void handleAbout(ApplicationEvent event) {
        AboutDialog dialog = new AboutDialog();
        dialog.setVisible(true);
        event.setHandled(true);
    }

    public void handleOpenApplication(ApplicationEvent event) {
        // ignore
    }

    public void handleOpenFile(ApplicationEvent event) {
        File file = new File(event.getFilename());
        MZmineCore.getIOController().openFiles(new File[] { file },
                PreloadLevel.NO_PRELOAD);
        event.setHandled(true);
    }

    public void handlePreferences(ApplicationEvent event) {
        // ignore
    }

    public void handlePrintFile(ApplicationEvent event) {
        // ignore
    }

    public void handleQuit(ApplicationEvent event) {
        MZmineCore.exitMZmine();
        event.setHandled(false);
    }

    public void handleReopenApplication(ApplicationEvent event) {
        // ignore
    }

}
