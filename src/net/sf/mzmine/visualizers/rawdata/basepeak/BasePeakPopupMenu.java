/**
 * 
 */
package net.sf.mzmine.visualizers.rawdata.basepeak;

import java.awt.Component;
import java.awt.Cursor;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;


/**
 *
 */
class BasePeakPopupMenu extends JPopupMenu {
    
    private JMenuItem zoomOutMenuItem;
    private JMenuItem showSpectrumMenuItem;
    private JMenuItem annotationsMenuItem;
    
    BasePeakPopupMenu(BasePeakVisualizer masterFrame) {
        
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

        zoomOutMenuItem = new JMenuItem("Zoom out");
        zoomOutMenuItem.addActionListener(masterFrame);
        zoomOutMenuItem.setActionCommand("ZOOM_OUT");
        zoomOutMenuItem.setEnabled(false);
        add(zoomOutMenuItem);
        
        addSeparator();
        
        annotationsMenuItem = new JMenuItem("Hide peak values");
        annotationsMenuItem.addActionListener(masterFrame);
        annotationsMenuItem.setActionCommand("SHOW_ANNOTATIONS");
        add(annotationsMenuItem);
        
        showSpectrumMenuItem = new JMenuItem("Show spectrum");
        showSpectrumMenuItem.addActionListener(masterFrame);
        showSpectrumMenuItem.setActionCommand("SHOW_SPECTRUM");
        showSpectrumMenuItem.setEnabled(false);
        add(showSpectrumMenuItem);
       
    }
    
    void setZoomOutMenuItem(boolean enabled) {
        zoomOutMenuItem.setEnabled(enabled);
    }
    
    void setAnnotationsMenuItem(String text) {
        annotationsMenuItem.setText(text);
    }
    
}
