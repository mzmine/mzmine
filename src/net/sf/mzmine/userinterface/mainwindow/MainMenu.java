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

package net.sf.mzmine.userinterface.mainwindow;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.io.IOController;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.AboutDialog;
import net.sf.mzmine.userinterface.dialogs.AlignmentResultExportDialog;
import net.sf.mzmine.userinterface.dialogs.FileOpenDialog;
import net.sf.mzmine.util.GUIUtils;

/**
 * 
 */
public class MainMenu extends JMenuBar implements ActionListener,
        ListSelectionListener {

    private JMenu fileMenu;
    private JMenu editMenu;
    private JMenu filterMenu;
    private JMenu peakMenu;
    private JMenu alignmentMenu;
    private JMenu normalizationMenu;
    private JMenu batchMenu;
    private JMenu visualizationMenu;
    private JMenu toolsMenu;
    private JMenu windowMenu;
    private JMenu helpMenu;

    private JMenuItem editCopy;

    private JMenuItem fileOpen, fileClose, fileExportPeakList,
            fileExportAlignmentResult, fileSaveParameters, fileLoadParameters,
            filePrint, fileExit;
             
    /*
     * private JMenu filterMenu; private JMenuItem ssMeanFilter, ssSGFilter,
     * ssChromatographicMedianFilter, ssCropFilter, ssZoomScanFilter; private
     * JMenu peakMenu; private JMenuItem ssRecursiveThresholdPicker,
     * ssLocalPicker, ssCentroidPicker, ssSimpleIsotopicPeaksGrouper,
     * ssCombinatorialDeisotoping, ssIncompleteIsotopePatternFilter; private
     * JMenu alignmentMenu; private JMenuItem tsJoinAligner, tsFastAligner,
     * tsAlignmentFilter, tsEmptySlotFiller; private JMenu normalizationMenu;
     * private JMenuItem normLinear, normStdComp; private JMenu batchMenu;
     * private JMenu visualizationMenu; private JMenuItem visOpenTIC,
     * visOpenSpectra, visOpenTwoD, visOpenThreeD; private JMenuItem
     * visOpenSRView, visOpenSCVView, visOpenCDAView, visOpenSammonsView;
     * private JMenu toolsMenu;
     */

    private JMenuItem batDefine;
    private JMenuItem toolsOptions;

    private JMenuItem windowTileWindows, windowCascadeWindows;
    private JMenuItem hlpAbout;

    private IOController ioController;
    private Desktop desktop;
    
    private Logger logger = Logger.getLogger(this.getClass().getName());

    MainMenu(IOController ioController, Desktop desktop) {

        this.ioController = ioController;
        this.desktop = desktop;

        fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        add(fileMenu);

        fileOpen = GUIUtils.addMenuItem(fileMenu, "Open...", this,
                KeyEvent.VK_O, true);
        fileClose = GUIUtils.addMenuItem(fileMenu, "Close", this, KeyEvent.VK_C);
        fileMenu.addSeparator();
        fileExportPeakList = GUIUtils.addMenuItem(fileMenu,
                "Export peak  list(s)...", this, KeyEvent.VK_E);
        fileExportAlignmentResult = GUIUtils.addMenuItem(fileMenu,
                "Export alignment result(s)...", this, KeyEvent.VK_A);
        fileMenu.addSeparator();
        fileSaveParameters = GUIUtils.addMenuItem(fileMenu,
                "Save parameters...", this, KeyEvent.VK_S);
        fileLoadParameters = GUIUtils.addMenuItem(fileMenu,
                "Load parameters...", this, KeyEvent.VK_S);
        
        /*
        fileMenu.addSeparator();
        filePrint = GUIUtils.addMenuItem(fileMenu, "Print figure...", this,
                KeyEvent.VK_P, true);
         */
        fileMenu.addSeparator();
        fileExit = GUIUtils.addMenuItem(fileMenu, "Exit", this, KeyEvent.VK_X,
                true);

        
        editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        this.add(editMenu);

        /*
        editCopy = GUIUtils.addMenuItem(editMenu, "Copy", this, KeyEvent.VK_C,
                true);
                */

        filterMenu = new JMenu("Raw data filtering");
        filterMenu.setMnemonic(KeyEvent.VK_R);
        this.add(filterMenu);

        peakMenu = new JMenu("Peak detection");
        peakMenu.setMnemonic(KeyEvent.VK_P);
        this.add(peakMenu);

        alignmentMenu = new JMenu("Alignment");
        alignmentMenu.setMnemonic(KeyEvent.VK_A);
        this.add(alignmentMenu);

        normalizationMenu = new JMenu("Normalization");
        normalizationMenu.setMnemonic(KeyEvent.VK_N);
        this.add(normalizationMenu);

        batchMenu = new JMenu("Batch mode");
        batchMenu.setMnemonic(KeyEvent.VK_B);
        this.add(batchMenu);

        visualizationMenu = new JMenu("Visualization");
        visualizationMenu.setMnemonic(KeyEvent.VK_V);
        this.add(visualizationMenu);

        toolsMenu = new JMenu("Configure");
        toolsMenu.setMnemonic(KeyEvent.VK_C);
        this.add(toolsMenu);
      
        toolsOptions = GUIUtils.addMenuItem(toolsMenu, "Preferences...", this,
                KeyEvent.VK_P);

        windowMenu = new JMenu("Window");
        windowMenu.setMnemonic(KeyEvent.VK_W);
        this.add(windowMenu);

        windowTileWindows = GUIUtils.addMenuItem(windowMenu, "Tile windows",
                this, KeyEvent.VK_T, true);
        windowCascadeWindows = GUIUtils.addMenuItem(windowMenu,
                "Cascade windows", this, KeyEvent.VK_A, true);

        helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        this.add(helpMenu);

        hlpAbout = GUIUtils.addMenuItem(helpMenu, "About MZmine...", this,
                KeyEvent.VK_A);

        desktop.addSelectionListener(this);

    }

    public void addMenuItem(MZmineMenu parentMenu, JMenuItem newItem) {
        switch (parentMenu) {
        case FILTERING:
            filterMenu.add(newItem);
            break;
        case PEAKPICKING:
            peakMenu.add(newItem);
            break;
        case ALIGNMENT:
            alignmentMenu.add(newItem);
            break;
        case NORMALIZATION:
            normalizationMenu.add(newItem);
            break;
        case BATCH:
            batchMenu.add(newItem);
            break;
        case VISUALIZATION:
            visualizationMenu.add(newItem);
            break;

        }
    }

    public JMenuItem addMenuItem(MZmineMenu parentMenu, String text,
            ActionListener listener, String actionCommand, int mnemonic,
            boolean setAccelerator, boolean enabled) {

        JMenuItem newItem = new JMenuItem(text);
        if (listener != null)
            newItem.addActionListener(listener);
        if (actionCommand != null)
            newItem.setActionCommand(actionCommand);
        if (mnemonic > 0)
            newItem.setMnemonic(mnemonic);
        if (setAccelerator)
            newItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic,
                    ActionEvent.CTRL_MASK));
        newItem.setEnabled(enabled);
        addMenuItem(parentMenu, newItem);
        return newItem;

    }

    public void addMenuSeparator(MZmineMenu parentMenu) {
        switch (parentMenu) {
        case FILTERING:
            filterMenu.addSeparator();
            break;
        case PEAKPICKING:
            peakMenu.addSeparator();
            break;
        case ALIGNMENT:
            alignmentMenu.addSeparator();
            break;
        case NORMALIZATION:
            normalizationMenu.addSeparator();
            break;
        case BATCH:
            batchMenu.addSeparator();
            break;
        case VISUALIZATION:
            visualizationMenu.addSeparator();
            break;
            

        }
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        Object src = e.getSource();

        if (src == fileExit) {
            desktop.exitMZmine();
        }

        // File -> Open
        if (src == fileOpen) {
            FileOpenDialog fileOpenDialog = new FileOpenDialog(ioController,
                    desktop);
            fileOpenDialog.setVisible(true);

        }

        // File->Close
        if (src == fileClose) {

            // Grab selected raw data files
            OpenedRawDataFile[] selectedFiles = desktop.getSelectedDataFiles();
            for (OpenedRawDataFile file : selectedFiles)
                MZmineProject.getCurrentProject().removeFile(file);

        }
               
        if (src == fileExportPeakList) {
        	// TODO: Implement peak list exporting
        	logger.severe("Peak list export unimplemented");
        }
        
        if (src == fileExportAlignmentResult) {

        	AlignmentResult[] alignmentResults = desktop.getSelectedAlignmentResults();
        	if (alignmentResults.length>0) 
        		new AlignmentResultExportDialog(alignmentResults);
        	
        }
        
        if (src == toolsOptions) {
        	// TODO: Implement Preferences dialog
        	logger.severe("Preferences dialog unimplemented");
        }

        // Window->Tile
        if (src == windowTileWindows) {
            MainWindow mainWindow = (MainWindow) desktop;
            mainWindow.tileInternalFrames();
        }

        // Window->Cascade
        if (src == windowCascadeWindows) {
            MainWindow mainWindow = (MainWindow) desktop;
            mainWindow.cascadeInternalFrames();
        }

        // Help->About
        if (src == hlpAbout) {
            AboutDialog dialog = new AboutDialog(desktop);
            dialog.setVisible(true);
        }

    }

    /**
     * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
     */
    public void valueChanged(ListSelectionEvent e) {
        fileClose.setEnabled(desktop.isDataFileSelected());
    }


}
