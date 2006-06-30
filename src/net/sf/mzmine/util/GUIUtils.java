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

package net.sf.mzmine.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;


/**
 * GUI related utilities
 */
public class GUIUtils {

    /**
     * Registers a keyboard handler to a given component
     * @param component Component to register the handler to
     * @param stroke Keystroke to activate the handler
     * @param listener ActionListener to handle the key press
     * @param actionCommand Action command string
     */
    public static void registerKeyHandler(JComponent component, KeyStroke stroke, final ActionListener listener, final String actionCommand) {
        component.getInputMap().put(stroke, actionCommand);
        component.getActionMap().put(actionCommand, new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                ActionEvent newEvent = new ActionEvent(event.getSource(), ActionEvent.ACTION_PERFORMED, actionCommand);
                listener.actionPerformed(newEvent);
            }
        });
    }
    
    /**
     * Add a new menu item to a given menu
     * @param menu Menu to add the item to
     * @param text Menu item text
     * @param listener Menu item's ActionListener or null
     * @return Created menu item
     */
    public static JMenuItem addMenuItem(JComponent menu, String text, ActionListener listener) {
        return addMenuItem(menu, text, listener, null, 0, false);
    }
    
    /**
     * Add a new menu item to a given menu
     * @param menu Menu to add the item to
     * @param text Menu item text
     * @param listener Menu item's ActionListener or null
     * @param actionCommand Menu item's action command or null 
     * @return Created menu item
     */
    public static JMenuItem addMenuItem(JComponent menu, String text, ActionListener listener, String actionCommand) {
        return addMenuItem(menu, text, listener, actionCommand, 0, false);
    }
    
    /**
     * Add a new menu item to a given menu
     * @param menu Menu to add the item to
     * @param text Menu item text
     * @param listener Menu item's ActionListener or null
     * @param mnemonic Menu item's mnemonic (virtual key code) or 0 
     * @return Created menu item
     */
    public static JMenuItem addMenuItem(JComponent menu, String text, ActionListener listener, int mnemonic) {
        return addMenuItem(menu, text, listener, null, mnemonic, false);
    }
    
    /**
     * Add a new menu item to a given menu
     * @param menu Menu to add the item to
     * @param text Menu item text
     * @param listener Menu item's ActionListener or null 
     * @param mnemonic Menu item's mnemonic (virtual key code) or 0 
     * @param setAccelerator Indicates whether to set key accelerator to CTRL + the key specified by mnemonic parameter
     * @return Created menu item
     */
    public static JMenuItem addMenuItem(JComponent menu, String text, ActionListener listener, int mnemonic, boolean setAccelerator) {
        return addMenuItem(menu, text, listener, null, mnemonic, setAccelerator);
    }
    
    /**
     * Add a new menu item to a given menu
     * @param menu Menu to add the item to
     * @param text Menu item text
     * @param listener Menu item's ActionListener or null
     * @param actionCommand Menu item's action command or null 
     * @param mnemonic Menu item's mnemonic (virtual key code) or 0 
     * @param setAccelerator Indicates whether to set key accelerator to CTRL + the key specified by mnemonic parameter
     * @return Created menu item
     */
    public static JMenuItem addMenuItem(JComponent menu, String text, ActionListener listener, String actionCommand, int mnemonic, boolean setAccelerator) {
        JMenuItem  item = new JMenuItem(text);
        if (listener != null) item.addActionListener(listener);
        if (actionCommand != null) item.setActionCommand(actionCommand);
        if (mnemonic > 0) item.setMnemonic(mnemonic);
        if (setAccelerator) item.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.CTRL_MASK));
        menu.add(item);
        return item;
    }

    /**
     * Add a new button to a given component
     * @param component Component to add the button to
     * @param text Button's text or null
     * @param icon Button's icon or null
     * @param listener Button's ActionListener or null
     * @return Created button
     */
    public static JButton addButton(JComponent component, String text, Icon icon, ActionListener listener) {
        return addButton(component, text, icon, listener, null, 0, null);
    }
    
    /**
     * Add a new button to a given component
     * @param component Component to add the button to
     * @param text Button's text or null
     * @param icon Button's icon or null
     * @param listener Button's ActionListener or null
     * @param actionCommand Button's action command or null 
     * @return Created button
     */
    public static JButton addButton(JComponent component, String text, Icon icon, ActionListener listener, String actionCommand) {
        return addButton(component, text, icon, listener, actionCommand, 0, null);
    }
    
    /**
     * Add a new button to a given component
     * @param component Component to add the button to
     * @param text Button's text or null
     * @param icon Button's icon or null
     * @param listener Button's ActionListener or null
     * @param actionCommand Button's action command or null 
     * @param toolTip Button's tooltip text or null
     * @return Created button
     */
    public static JButton addButton(JComponent component, String text, Icon icon, ActionListener listener, String actionCommand, String toolTip) {
        return addButton(component, text, icon, listener, actionCommand, 0, toolTip);
    }
    
    /**
     * Add a new button to a given component
     * @param component Component to add the button to
     * @param text Button's text or null
     * @param icon Button's icon or null
     * @param listener Button's ActionListener or null
     * @param actionCommand Button's action command or null 
     * @param mnemonic Button's mnemonic (virtual key code) or 0 
     * @param toolTip Button's tooltip text or null
     * @return Created button
     */
    public static JButton addButton(JComponent component, String text, Icon icon, ActionListener listener, String actionCommand, int mnemonic, String toolTip) {
        JButton button = new JButton(text, icon);
        if (listener != null) button.addActionListener(listener);
        if (actionCommand != null) button.setActionCommand(actionCommand);
        if (mnemonic > 0) button.setMnemonic(mnemonic);
        if (toolTip != null) button.setToolTipText(toolTip);
        component.add(button);
        return button;
    }
    
    
}
