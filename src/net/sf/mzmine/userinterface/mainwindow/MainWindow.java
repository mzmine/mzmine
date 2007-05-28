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
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.impl.TaskControllerImpl;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.components.TaskProgressWindow;
import net.sf.mzmine.util.NumberFormatter;
import net.sf.mzmine.util.NumberFormatter.FormatterType;

/**
 * This class is the main window of application
 * 
 */
public class MainWindow extends JFrame implements Desktop, WindowListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    private NumberFormatter mzFormat = new NumberFormatter(FormatterType.NUMBER, "0.000");
    private NumberFormatter rtFormat = new NumberFormatter(FormatterType.TIME, "m:ss");
    private NumberFormatter intensityFormat = new NumberFormatter(FormatterType.NUMBER, "0.00E0");

    private MZmineCore core;

    private JDesktopPane desktopPane;

    private JSplitPane split;

    private ItemSelector itemSelector;

    private TaskProgressWindow taskList;

    private Vector<ListSelectionListener> selectionListeners = new Vector<ListSelectionListener>();

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

    private static MainWindow myInstance;

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
        core.exitMZmine();
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
    public OpenedRawDataFile[] getSelectedDataFiles() {
        return itemSelector.getSelectedRawData();
    }

    public AlignmentResult[] getSelectedAlignmentResults() {
        return itemSelector.getSelectedAlignmentResults();
    }

    public void setSelectedAlignmentResult(AlignmentResult alignmentResult) {
        itemSelector.setActiveAlignmentResult(alignmentResult);
    }

    public void setSelectedDataFile(OpenedRawDataFile dataFile) {
        itemSelector.setActiveRawData(dataFile);
    }

    public void addAlignmentResult(AlignmentResult alignmentResult) {
        itemSelector.addAlignmentResult(alignmentResult);
    }

    public void addDataFile(OpenedRawDataFile dataFile) {
        itemSelector.addRawData(dataFile);
    }

    public void removeAlignmentResult(AlignmentResult alignmentResult) {
        itemSelector.removeAlignmentResult(alignmentResult);
    }

    public void removeDataFile(OpenedRawDataFile dataFile) {
        itemSelector.removeRawData(dataFile);
    }

    /**
     * @see net.sf.mzmine.userinterface.Desktop#addSelectionListener(javax.swing.event.ListSelectionListener)
     */
    public void addSelectionListener(ListSelectionListener listener) {
        itemSelector.addSelectionListener(listener);
        selectionListeners.add(listener);
    }

    public void notifySelectionListeners() {
        for (ListSelectionListener listener : selectionListeners) {
            listener.valueChanged(new ListSelectionEvent(this, 0, 0, false));
        }
    }

    /**
     */
    public void initModule(MZmineCore core) {

        assert myInstance == null;
        myInstance = this;

        this.core = core;

        
        Font defaultFont = new Font("SansSerif", Font.PLAIN, 13);
        Font smallFont = new Font("SansSerif", Font.PLAIN, 11);
        Enumeration keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof Font)
                UIManager.put(key, defaultFont);
        }
        UIManager.put("List.font", smallFont);
        UIManager.put("Table.font", smallFont);



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

        statusBar = new Statusbar(core);
        c.add(statusBar, BorderLayout.SOUTH);

        // Construct menu
        menuBar = new MainMenu(core);
        setJMenuBar(menuBar);

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
     * @see net.sf.mzmine.userinterface.Desktop#isDataFileSelected()
     */
    public boolean isAlignmentResultSelected() {
        return itemSelector.getSelectedAlignmentResults().length > 0;
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
        return mzFormat;
    }

    /**
     * @see net.sf.mzmine.userinterface.Desktop#getRTFormatProvider()
     */
    public NumberFormatter getRTFormat() {
        return rtFormat;
    }

    /**
     * @see net.sf.mzmine.userinterface.Desktop#getIntensityFormatProvider()
     */
    public NumberFormatter getIntensityFormat() {
        return intensityFormat;
    }

}
