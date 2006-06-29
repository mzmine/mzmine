/**
 * 
 */
package net.sf.mzmine.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;


/**
 *
 */
public class GUIUtils {

    public static void registerKeyHandler(JComponent component, KeyStroke stroke, final ActionListener listener, final String actionCommand) {
        component.getInputMap().put(stroke, actionCommand);
        component.getActionMap().put(actionCommand, new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                listener.actionPerformed(new ActionEvent(event.getSource(), ActionEvent.ACTION_PERFORMED, actionCommand));
            }
        });
    }
    
    public static JMenuItem addMenuItem(JMenu menu, String text, ActionListener listener) {
        return addMenuItem(menu, text, listener, null, 0, false);
    }
    
    public static JMenuItem addMenuItem(JMenu menu, String text, ActionListener listener, String actionCommand) {
        return addMenuItem(menu, text, listener, actionCommand, 0, false);
    }
    
    public static JMenuItem addMenuItem(JMenu menu, String text, ActionListener listener, int mnemonic) {
        return addMenuItem(menu, text, listener, null, mnemonic, false);
    }
    
    public static JMenuItem addMenuItem(JMenu menu, String text, ActionListener listener, int mnemonic, boolean setAccelerator) {
        return addMenuItem(menu, text, listener, null, mnemonic, setAccelerator);
    }
    
    public static JMenuItem addMenuItem(JPopupMenu menu, String text, ActionListener listener) {
        return addMenuItem(menu, text, listener, null, 0, false);
    }
    
    public static JMenuItem addMenuItem(JPopupMenu menu, String text, ActionListener listener, String actionCommand) {
        return addMenuItem(menu, text, listener, actionCommand, 0, false);
    }
    
    public static JMenuItem addMenuItem(JPopupMenu menu, String text, ActionListener listener, int mnemonic) {
        return addMenuItem(menu, text, listener, null, mnemonic, false);
    }
    
    public static JMenuItem addMenuItem(JPopupMenu menu, String text, ActionListener listener, int mnemonic, boolean setAccelerator) {
        return addMenuItem(menu, text, listener, null, mnemonic, setAccelerator);
    }
    
    public static JMenuItem addMenuItem(JMenu menu, String text, ActionListener listener, String actionCommand, int mnemonic, boolean setAccelerator) {
        JMenuItem  item = new JMenuItem(text);
        if (listener != null) item.addActionListener(listener);
        if (actionCommand != null) item.setActionCommand(actionCommand);
        if (mnemonic > 0) item.setMnemonic(mnemonic);
        if (setAccelerator) item.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.CTRL_MASK));
        menu.add(item);
        return item;
    }
    
    public static JMenuItem addMenuItem(JPopupMenu menu, String text, ActionListener listener, String actionCommand, int mnemonic, boolean setAccelerator) {
        JMenuItem  item = new JMenuItem(text);
        if (listener != null) item.addActionListener(listener);
        if (actionCommand != null) item.setActionCommand(actionCommand);
        if (mnemonic > 0) item.setMnemonic(mnemonic);
        if (setAccelerator) item.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.CTRL_MASK));
        menu.add(item);
        return item;
    }

    ;
    
}
