/*
 * Copyright 2006 The MZmine Development Team
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
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.impl.TaskControllerImpl;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.components.TaskProgressWindow;

/**
 * This class is the main window of application
 * 
 */
public class MainWindow extends JFrame implements Desktop, WindowListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

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
        frame.addInternalFrameListener(itemSelector);
        frame.setVisible(true);
    }

    private static MainWindow myInstance;

    /**
     * This method returns the desktop
     */
    public JDesktopPane getDesktopPane() {
        return desktopPane;
    }

    public ItemSelector getItemSelector() {
        return itemSelector;
    }

    void tileInternalFrames() {
        logger.finest("Tiling windows");
        JInternalFrame[] frames = getVisibleFrames();
        if (frames.length == 0)
            return;
        Rectangle dBounds = desktopPane.getBounds();

        int cols = (int) Math.sqrt(frames.length);
        int rows = (int) (Math.ceil(((double) frames.length) / cols));
        int lastRow = frames.length - cols * (rows - 1);
        int width, height;

        if (lastRow == 0) {
            rows--;
            height = dBounds.height / rows;
        } else {
            height = dBounds.height / rows;
            if (lastRow < cols) {
                rows--;
                width = dBounds.width / lastRow;
                for (int i = 0; i < lastRow; i++) {
                    frames[cols * rows + i].setBounds(i * width, rows * height,
                            width, height);
                }
            }
        }

        width = dBounds.width / cols;
        for (int j = 0; j < rows; j++) {
            for (int i = 0; i < cols; i++) {
                frames[i + j * cols].setBounds(i * width, j * height, width,
                        height);
            }
        }
    }

    void cascadeInternalFrames() {
        logger.finest("Cascading windows");
        JInternalFrame[] frames = getVisibleFrames();
        if (frames.length == 0)
            return;
        Rectangle dBounds = desktopPane.getBounds();
        int separation = 24;
        int margin = (frames.length - 1) * separation;
        int width = dBounds.width - margin;
        int height = dBounds.height - margin;
        for (int i = 0; i < frames.length; i++) {
            frames[i].setBounds(i * separation, i * separation, width, height);
        }
    }

    /**
     * WindowListener interface implementation
     */
    public void windowOpened(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        exitMZmine();
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

    /**
     * Prepares everything for quit and then shutdowns the application
     */
    public void exitMZmine() {

        // Ask if use really wants to quit
        int selectedValue = JOptionPane.showInternalConfirmDialog(desktopPane,
                "Are you sure you want to exit MZmine?", "Exiting...",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (selectedValue != JOptionPane.YES_OPTION)
            return;

        logger.info("Exiting MZmine");
        dispose();
        System.exit(0);

    }

    public void setStatusBarText(String text) {
        setStatusBarText(text, Color.black);
    }

    public void displayErrorMessage(String msg) {
        // statBar.setStatusText(msg);
        JOptionPane.showMessageDialog(this, msg, "Sorry",
                JOptionPane.ERROR_MESSAGE);
    }

    public void addMenuItem(MZmineMenu parentMenu, JMenuItem newItem) {
        menuBar.addMenuItem(parentMenu, newItem);
    }

    /**
     * @see net.sf.mzmine.userinterface.Desktop#getSelectedDataFiles()
     */
    public OpenedRawDataFile[] getSelectedDataFiles() {
        return itemSelector.getSelectedRawData();
    }

    /**
     * @see net.sf.mzmine.userinterface.Desktop#getFirstSelectedDataFile()
     */
    public OpenedRawDataFile getFirstSelectedDataFile() {
        return itemSelector.getFirstSelectedRawData();
    }

    /**
     * @see net.sf.mzmine.userinterface.Desktop#addSelectionListener(javax.swing.event.ListSelectionListener)
     */
    public void addSelectionListener(ListSelectionListener listener) {
        itemSelector.addSelectionListener(listener);
    }

    /**
     */
    public void initModule(MZmineCore core) {

        assert myInstance == null;
        myInstance = this;

        // Initialize item selector
        itemSelector = new ItemSelector(this);

        // Construct menu

        menuBar = new MainMenu(core.getIOController(), this);

        setJMenuBar(menuBar);

        // Place objects on main window
        desktopPane = new JDesktopPane();

        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, itemSelector,
                desktopPane);

        desktopPane.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);

        desktopPane.setBorder(new EtchedBorder(EtchedBorder.RAISED));
        Container c = getContentPane();
        c.setLayout(new BorderLayout());
        c.add(split, BorderLayout.CENTER);

        statusBar = new Statusbar(core);
        c.add(statusBar, BorderLayout.SOUTH);

        // Initialize window listener for responding to user events
        addWindowListener(this);

        pack();

        // TODO: check screen size?
        setBounds(0, 0, 1000, 700);
        setLocationRelativeTo(null);

        // Application wants to control closing by itself
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        setTitle("MZmine");

        taskList = new TaskProgressWindow(
                (TaskControllerImpl) core.getTaskController());
        desktopPane.add(taskList, JLayeredPane.DEFAULT_LAYER);

    }

    public static MainWindow getInstance() {
        return myInstance;
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

}
