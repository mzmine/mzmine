/**
 * 
 */
package net.sf.mzmine.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
    
    public static JMenuItem addMenuItem(JMenu menu, String text, ActionListener listener, String actionCommand) {
        JMenuItem  item = new JMenuItem(text);
        item.addActionListener(listener);
        item.setActionCommand(actionCommand);
        menu.add(item);
        return item;
    }
    
    public static JMenuItem addMenuItem(JPopupMenu menu, String text, ActionListener listener, String actionCommand) {
        JMenuItem  item = new JMenuItem(text);
        item.addActionListener(listener);
        item.setActionCommand(actionCommand);
        menu.add(item);
        return item;
    }
    
}
