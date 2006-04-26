/**
 * 
 */
package net.sf.mzmine.visualizers.rawdata.tic;

import java.awt.Component;
import java.awt.Cursor;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;


/**
 *
 */
class TICPopupMenu extends JPopupMenu {
    
    private JMenuItem zoomOutMenuItem;
    private JMenuItem showSpectrumMenuItem;
    private JMenuItem changeTicXicModeMenuItem;
    
    TICPopupMenu(TICVisualizer masterFrame) {
        
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

        zoomOutMenuItem = new JMenuItem("Zoom out");
        zoomOutMenuItem.addActionListener(masterFrame);
        zoomOutMenuItem.setActionCommand("ZOOM_OUT");
        zoomOutMenuItem.setEnabled(false);
        add(zoomOutMenuItem);
        
        addSeparator();

        showSpectrumMenuItem = new JMenuItem("Show spectrum");
        showSpectrumMenuItem.addActionListener(masterFrame);
        showSpectrumMenuItem.setActionCommand("SHOW_SPECTRUM");
        showSpectrumMenuItem.setEnabled(false);
        add(showSpectrumMenuItem);

        addSeparator();

        changeTicXicModeMenuItem = new JMenuItem("Switch to XIC");
        changeTicXicModeMenuItem.addActionListener(masterFrame);
        changeTicXicModeMenuItem.setActionCommand("CHANGE_XIC_TIC");
        add(changeTicXicModeMenuItem);
        
    }
    
    void setTicXicMenuItem(String text) {
        changeTicXicModeMenuItem.setText(text);
    }
    
    void setZoomOutMenuItem(boolean enabled) {
        zoomOutMenuItem.setEnabled(enabled);
    }
}
