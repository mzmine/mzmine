/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.help.HelpBroker;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.desktop.impl.helpsystem.MZmineHelpSet;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.project.ProjectEvent;
import net.sf.mzmine.project.ProjectListener;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.TextUtils;

/**
 * This class is the main window of application
 * 
 */
public class MainWindow extends JFrame implements MZmineModule, Desktop,
		WindowListener, ProjectListener {

	static final String aboutHelpID = "net/sf/mzmine/desktop/help/AboutMZmine.html";

	private Logger logger = Logger.getLogger(this.getClass().getName());

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
		displayMessage("Message", msg, JOptionPane.INFORMATION_MESSAGE);
	}

	/**
     */
	public void displayMessage(String title, String msg) {
		displayMessage(title, msg, JOptionPane.INFORMATION_MESSAGE);
	}

	public void displayErrorMessage(String msg) {
		displayMessage("Error", msg);
	}

	public void displayErrorMessage(String title, String msg) {
		displayMessage(title, msg, JOptionPane.ERROR_MESSAGE);
	}

	public void displayMessage(String title, String msg, int type) {
		String wrappedMsg = TextUtils.wrapText(msg, 80);
		JOptionPane.showMessageDialog(this, wrappedMsg, title, type);
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

		DesktopSetup desktopSetup = new DesktopSetup();
		desktopSetup.init();

		try {
			BufferedImage MZmineIcon = ImageIO.read(new File(
					"icons/MZmineIcon.png"));
			setIconImage(MZmineIcon);
		} catch (IOException e) {
			logger.log(Level.WARNING, "Could not set application icon", e);
		}

		mainPanel = new MainPanel();
		add(mainPanel);

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

		MZmineCore.getProjectManager().addProjectListener(this);

	}

	void updateTitle() {
		String projectName = MZmineCore.getCurrentProject().toString();
		setTitle("MZmine " + MZmineCore.getMZmineVersion() + ": " + projectName);
	}

	/**
	 * @see net.sf.mzmine.desktop.Desktop#getMainFrame()
	 */
	public JFrame getMainFrame() {
		return this;
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

		// If the request was caused by exception during MZmine startup, desktop
		// may not be initialized yet
		if ((mainPanel == null) || (mainPanel.getStatusBar() == null))
			return;

		mainPanel.getStatusBar().setStatusText(text, textColor);
	}

	/**
	 * @see net.sf.mzmine.modules.MZmineModule#getParameterSet()
	 */
	public ParameterSet getParameterSet() {
		return null;
	}

	/**
	 * @see net.sf.mzmine.modules.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
	 */
	public void setParameters(ParameterSet parameterValues) {
	}

	public void projectModified(ProjectEvent event) {
		// Modify the GUI in the event dispatching thread
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				updateTitle();
			}
		});
	}

	public void displayException(Exception e) {
		displayErrorMessage(ExceptionUtils.exceptionToString(e));
	}

	MainPanel getMainPanel() {
		return mainPanel;
	}

	public JInternalFrame[] getInternalFrames() {
		return mainPanel.getInternalFrames();
	}

	public JInternalFrame getSelectedFrame() {
		return mainPanel.getSelectedFrame();
	}

	public void showAboutDialog() {

		MZmineHelpSet hs = MZmineCore.getHelpImpl().getHelpSet();
		if (hs == null)
			return;

		HelpBroker hb = hs.createHelpBroker();
		hs.setHomeID(aboutHelpID);

		hb.setDisplayed(true);
	}

}
