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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.desktop.impl;

import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import net.sf.mzmine.main.MZmineCore;

/**
 * Dynamically-built Windows menu.
 * 
 */
public class WindowsMenu extends JMenu implements ActionListener, MenuListener {

    private static final long serialVersionUID = 1L;

    private final JMenuItem closeAllMenuItem;

    /**
     * Create the "Windows" menu for a MDI view
     */
    public WindowsMenu() {

        super("Windows");

        this.addMenuListener(this);

        closeAllMenuItem = new JMenuItem("Close all windows");
        closeAllMenuItem.addActionListener(this);
        add(closeAllMenuItem);

        addSeparator();

    }

    public void actionPerformed(ActionEvent event) {

        Object src = event.getSource();

        if (src instanceof FrameMenuItem) {
            FrameMenuItem item = (FrameMenuItem) src;
            Window frame = item.getFrame();
            frame.toFront();
            frame.requestFocus();
        }

        if (src == closeAllMenuItem) {
            // Close all Swing Frames
            for (Frame window : Frame.getFrames()) {
                if (window != MZmineCore.getDesktop().getMainWindow()) {
                    window.dispose();
                }
            }
        }

    }

    class FrameMenuItem extends JRadioButtonMenuItem {

        private static final long serialVersionUID = 1L;
        private Frame window;

        FrameMenuItem(Frame window, ActionListener listener) {
            super(window.getTitle());
            addActionListener(listener);
            this.window = window;
        }

        Window getFrame() {
            return window;
        }

    }

    public void menuCanceled(MenuEvent event) {
    }

    public void menuDeselected(MenuEvent event) {
    }

    public void menuSelected(MenuEvent event) {

        // Remove all previous items
        while (getItemCount() > 2)
            remove(2);

        int windowsAdded = 0;
        // Create a menu item for each window
        for (Frame window : Frame.getFrames()) {

            if (window.isVisible()) {
                FrameMenuItem newItem = new FrameMenuItem(window, this);
                add(newItem);
                windowsAdded++;
            }
        }

        // Disable the Close all button if we only have the main window
        closeAllMenuItem.setEnabled(windowsAdded > 1);
    }

}