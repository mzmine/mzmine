/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.help.HelpBroker;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.helpsystem.HelpImpl;
import net.sf.mzmine.desktop.impl.helpsystem.MZmineHelpSet;
import net.sf.mzmine.desktop.preferences.MZminePreferences;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.WindowSettingsParameter;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.TextUtils;

/**
 * This class is the main window of application
 * 
 */
public class MainWindow extends JFrame implements MZmineModule, Desktop,
	WindowListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    static final String aboutHelpID = "net/sf/mzmine/desktop/help/AboutMZmine.html";

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private MainPanel mainPanel;
    private StatusBar statusBar;

    private MainMenu menuBar;

    private HelpImpl help;

    public MainMenu getMainMenu() {
	return menuBar;
    }

    public HelpImpl getHelpImpl() {
	return help;
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

    public void setStatusBarText(String text) {
	setStatusBarText(text, Color.black);
    }

    /**
     */
    public void displayMessage(Window window, String msg) {
	displayMessage(window, "Message", msg, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     */
    public void displayMessage(Window window, String title, String msg) {
	displayMessage(window, title, msg, JOptionPane.INFORMATION_MESSAGE);
    }

    public void displayErrorMessage(Window window, String msg) {
	displayMessage(window, "Error", msg);
    }

    public void displayErrorMessage(Window window, String title, String msg) {
	displayMessage(window, title, msg, JOptionPane.ERROR_MESSAGE);
    }

    public void displayMessage(Window window, String title, String msg, int type) {

	assert msg != null;

	// If the message does not contain newline characters, wrap it
	// automatically
	String wrappedMsg;
	if (msg.contains("\n"))
	    wrappedMsg = msg;
	else
	    wrappedMsg = TextUtils.wrapText(msg, 80);

	JOptionPane.showMessageDialog(window, wrappedMsg, title, type);
    }

    public void addMenuItem(MZmineModuleCategory parentMenu, JMenuItem newItem) {
	menuBar.addMenuItem(parentMenu, newItem);
    }

    /**
     * @see net.sf.mzmine.desktop.Desktop#getSelectedDataFiles()
     */
    public RawDataFile[] getSelectedDataFiles() {
	return mainPanel.getRawDataTree().getSelectedObjects(RawDataFile.class);
    }

    public PeakList[] getSelectedPeakLists() {
	return mainPanel.getPeakListTree().getSelectedObjects(PeakList.class);
    }

    public void initModule() {

	assert SwingUtilities.isEventDispatchThread();

	DesktopSetup desktopSetup = new DesktopSetup();
	desktopSetup.init();

	help = new HelpImpl();

	try {
	    BufferedImage MZmineIcon = ImageIO.read(new File(
		    "icons/MZmineIcon.png"));
	    setIconImage(MZmineIcon);
	} catch (IOException e) {
	    logger.log(Level.WARNING, "Could not set application icon", e);
	}

	setLayout(new BorderLayout());

	mainPanel = new MainPanel();
	add(mainPanel, BorderLayout.CENTER);

	statusBar = new StatusBar();
	add(statusBar, BorderLayout.SOUTH);

	// Construct menu
	menuBar = new MainMenu();
	setJMenuBar(menuBar);

	// Initialize window listener for responding to user events
	addWindowListener(this);

	pack();

	Toolkit toolkit = Toolkit.getDefaultToolkit();
	Dimension screenSize = toolkit.getScreenSize();

	// Set initial window size to 1000x700 pixels, but check the screen size
	// first
	int width = Math.min(screenSize.width, 1000);
	int height = Math.min(screenSize.height, 700);
	setBounds(0, 0, width, height);
	setLocationRelativeTo(null);

	// Application wants to control closing by itself
	setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

	updateTitle();

	// get the window settings parameter
	ParameterSet paramSet = MZmineCore.getConfiguration().getPreferences();
	WindowSettingsParameter settings = paramSet
		.getParameter(MZminePreferences.windowSetttings);

	// listen for changes
	this.addComponentListener(settings);

    }

    public void updateTitle() {
	String projectName = MZmineCore.getProjectManager().getCurrentProject()
		.toString();
	setTitle("MZmine " + MZmineCore.getMZmineVersion() + ": " + projectName);
    }

    /**
     * @see net.sf.mzmine.desktop.Desktop#getMainFrame()
     */
    public JFrame getMainWindow() {
	return this;
    }

    /**
     * @see net.sf.mzmine.desktop.Desktop#setStatusBarText(java.lang.String,
     *      java.awt.Color)
     */
    public void setStatusBarText(String text, Color textColor) {

	// If the request was caused by exception during MZmine startup, desktop
	// may not be initialized yet
	if ((mainPanel == null) || (statusBar == null))
	    return;

	statusBar.setStatusText(text, textColor);
    }

    public void displayException(Window window, Exception e) {
	displayErrorMessage(window, ExceptionUtils.exceptionToString(e));
    }

    public MainPanel getMainPanel() {
	return mainPanel;
    }

    public void showAboutDialog() {

	MZmineHelpSet hs = help.getHelpSet();
	if (hs == null)
	    return;

	HelpBroker hb = hs.createHelpBroker();
	hs.setHomeID(aboutHelpID);

	hb.setDisplayed(true);
    }

    @Override
    public void addRawDataTreeListener(TreeModelListener listener) {
	TreeModel model = getMainPanel().getRawDataTree().getModel();
	model.addTreeModelListener(listener);
    }

    @Override
    public void removeRawDataTreeListener(TreeModelListener listener) {
	TreeModel model = getMainPanel().getRawDataTree().getModel();
	model.removeTreeModelListener(listener);
    }

    @Override
    public void addPeakListTreeListener(TreeModelListener listener) {
	TreeModel model = getMainPanel().getPeakListTree().getModel();
	model.addTreeModelListener(listener);
    }

    @Override
    public void removePeakListTreeListener(TreeModelListener listener) {
	TreeModel model = getMainPanel().getPeakListTree().getModel();
	model.removeTreeModelListener(listener);
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
	return SimpleParameterSet.class;
    }

    @Override
    public @Nonnull ExitCode exitMZmine() {

	int selectedValue = JOptionPane.showInternalConfirmDialog(
		this.getContentPane(), "Are you sure you want to exit?",
		"Exiting...", JOptionPane.YES_NO_OPTION,
		JOptionPane.WARNING_MESSAGE);

	if (selectedValue != JOptionPane.YES_OPTION)
	    return ExitCode.CANCEL;

	this.dispose();

	logger.info("Exiting MZmine");

	System.exit(0);

	return ExitCode.OK;
    }

    @Override
    public @Nonnull String getName() {
	return "MZmine main window";
    }

}
