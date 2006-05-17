/**
 * 
 */
package net.sf.mzmine.visualizers.rawdata.experimentaltic;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import net.sf.mzmine.io.RawDataFile;


/**
 *
 */
class RemoveFilePopupMenu extends JMenu implements MenuListener, ActionListener {

    private Hashtable<JMenuItem,RawDataFile> menuItemFiles;
    TICVisualizer visualizer;
    
    RemoveFilePopupMenu(TICVisualizer visualizer) {
        super("Remove plot of file...");
        addMenuListener(this);
        this.visualizer = visualizer;
    }

    /**
     * @see javax.swing.event.MenuListener#menuSelected(javax.swing.event.MenuEvent)
     */
    public void menuSelected(MenuEvent event) {
        removeAll();
        RawDataFile[] files = visualizer.getRawDataFiles();
        
        // if we have only one file, we cannot remove it
        if (files.length == 1) return;
        
        menuItemFiles = new Hashtable<JMenuItem,RawDataFile>();
        for (RawDataFile file : files) {
            JMenuItem newItem = new JMenuItem(file.toString());
            newItem.addActionListener(this);
            menuItemFiles.put(newItem, file);
            add(newItem);
        }
        
    }

    /**
     * @see javax.swing.event.MenuListener#menuDeselected(javax.swing.event.MenuEvent)
     */
    public void menuDeselected(MenuEvent arg0) {
        // TODO Auto-generated method stub
        
    }

    /**
     * @see javax.swing.event.MenuListener#menuCanceled(javax.swing.event.MenuEvent)
     */
    public void menuCanceled(MenuEvent arg0) {
        // TODO Auto-generated method stub
        
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {
         Object src = event.getSource();
         RawDataFile file = menuItemFiles.get(src);
         if (file != null) visualizer.removeRawDataFile(file);
    }
}
