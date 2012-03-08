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
import java.beans.PropertyVetoException;

import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.GUIUtils;

/**
 * Dynamically-built windows menu. Tile() and cascade() methods originally based
 * on code by Guy Davis (GPL).
 * 
 */
public class WindowsMenu extends JMenu implements ActionListener, MenuListener {

	private JDesktopPane desktopPane;

	private JMenuItem cascadeItem, tileItem;

	/**
	 * Create the "Windows" menu for a MDI view
	 */
	public WindowsMenu() {

		super("Windows");

		MainWindow mainWindow = (MainWindow) MZmineCore.getDesktop();
		this.desktopPane = mainWindow.getMainPanel().getDesktopPane();

		cascadeItem = GUIUtils.addMenuItem(this, "Cascade", this,
				KeyEvent.VK_C, true);
		tileItem = GUIUtils
				.addMenuItem(this, "Tile", this, KeyEvent.VK_T, true);
		GUIUtils.addSeparator(this);

		this.addMenuListener(this);

	}

	/**
	 * Change the bounds of visible windows to tile them checkerboard-style on
	 * the desktop.
	 */
	private void tile() {

		JInternalFrame frames[] = MZmineCore.getDesktop().getInternalFrames();
		if (frames.length == 0) {
			return;
		}

		double sqrt = Math.sqrt(frames.length);
		int numCols = (int) Math.floor(sqrt);
		int numRows = numCols;
		if ((numCols * numRows) < frames.length) {
			numCols++;
			if ((numCols * numRows) < frames.length) {
				numRows++;
			}
		}

		int newWidth = desktopPane.getWidth() / numCols;
		int newHeight = desktopPane.getHeight() / numRows;

		int y = 0;
		int x = 0;
		int frameIdx = 0;
		for (int i = 0; i < numRows; i++) {
			for (int j = 0; j < numCols; j++) {
				if (frameIdx < frames.length) {
					JInternalFrame frame = frames[frameIdx++];
					if (frame.isMaximum()) {
						try {
							frame.setMaximum(false);
						} catch (PropertyVetoException ex) {
							throw new RuntimeException(ex);
						}
					}
					frame.reshape(x, y, newWidth, newHeight);
					x += newWidth;
				}
			}
			x = 0;
			y += newHeight;
		}
	}

	/**
	 * Change the bounds of visible windows to cascade them down from the top
	 * left of the desktop.
	 */
	private void cascade() {

		JInternalFrame frames[] = MZmineCore.getDesktop().getInternalFrames();
		if (frames.length == 0) {
			return;
		}

		int newWidth = (int) (desktopPane.getWidth() * 0.6);
		int newHeight = (int) (desktopPane.getHeight() * 0.6);
		int x = 0;
		int y = 0;
		for (JInternalFrame frame : frames) {
			if (frame.isMaximum()) {
				try {
					frame.setMaximum(false);
				} catch (PropertyVetoException ex) {
					throw new RuntimeException(ex);
				}
			}
			frame.reshape(x, y, newWidth, newHeight);
			x += 25;
			y += 25;

			if ((x + newWidth) > desktopPane.getWidth()) {
				x = 0;
			}

			if ((y + newHeight) > desktopPane.getHeight()) {
				y = 0;
			}
		}
	}

	public void actionPerformed(ActionEvent event) {

		Object src = event.getSource();

		if (src == cascadeItem)
			cascade();

		if (src == tileItem)
			tile();

		if (src instanceof FrameMenuItem) {
			FrameMenuItem item = (FrameMenuItem) src;
			JInternalFrame frame = item.getFrame();
			desktopPane.getDesktopManager().activateFrame(frame);
		}

	}

	class FrameMenuItem extends JRadioButtonMenuItem {

		private JInternalFrame frame;

		FrameMenuItem(JInternalFrame frame, ActionListener listener) {
			super(frame.getTitle());
			addActionListener(listener);
			this.frame = frame;
		}

		JInternalFrame getFrame() {
			return frame;
		}

	}

	public void menuCanceled(MenuEvent event) {
	}

	public void menuDeselected(MenuEvent event) {
	}

	public void menuSelected(MenuEvent event) {

		// Remove all previous items, except Tile, Cascade and separator
		while (getItemCount() > 3) {
			remove(3);
		}

		// Get all visible frames
		JInternalFrame frames[] = MZmineCore.getDesktop().getInternalFrames();
		JInternalFrame selectedFrame = MZmineCore.getDesktop()
				.getSelectedFrame();

		// Create a menu item for each frame
		for (JInternalFrame frame : frames) {
			FrameMenuItem newItem = new FrameMenuItem(frame, this);
			if (frame == selectedFrame)
				newItem.setSelected(true);
			add(newItem);
		}
	}

}