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
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.io.IOController;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.io.export.PeakListExportDialog;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.AboutDialog;
import net.sf.mzmine.userinterface.dialogs.FileOpenDialog;
import net.sf.mzmine.userinterface.dialogs.FormatSetupDialog;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.LookAndFeelChanger;
import ca.guydavis.swing.desktop.CascadingWindowPositioner;
import ca.guydavis.swing.desktop.JWindowsMenu;

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
    private JMenu rawDataVisualizationMenu;
    private JMenu dataAnalysisMenu;
    private JMenu toolsMenu;
    private JMenu lookAndFeelMenu;
    private JWindowsMenu windowsMenu;
    private JMenu helpMenu;

    private JMenuItem editCopy;

    private JMenuItem fileOpen, fileClose, fileExportPeakList,
            fileExportAlignmentResult, fileSaveParameters, fileLoadParameters,
            filePrint, fileExit;

    private JMenuItem toolsFormat;

    private JMenuItem hlpAbout;

    private MZmineCore core;
    private IOController ioController;
    private Desktop desktop;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    MainMenu(MZmineCore core) {

        this.core = core;
        this.ioController = core.getIOController();
        this.desktop = core.getDesktop();

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
         * fileMenu.addSeparator(); filePrint = GUIUtils.addMenuItem(fileMenu,
         * "Print figure...", this, KeyEvent.VK_P, true);
         */
        fileMenu.addSeparator();
        fileExit = GUIUtils.addMenuItem(fileMenu, "Exit", this, KeyEvent.VK_X,
                true);

        editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        this.add(editMenu);

        /*
         * editCopy = GUIUtils.addMenuItem(editMenu, "Copy", this,
         * KeyEvent.VK_C, true);
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

        rawDataVisualizationMenu = new JMenu("Visualization");
        rawDataVisualizationMenu.setMnemonic(KeyEvent.VK_V);
        this.add(rawDataVisualizationMenu);

        dataAnalysisMenu = new JMenu("Data analysis");
        dataAnalysisMenu.setMnemonic(KeyEvent.VK_S);
        this.add(dataAnalysisMenu);

        toolsMenu = new JMenu("Configure");
        toolsMenu.setMnemonic(KeyEvent.VK_C);
        this.add(toolsMenu);

        toolsFormat = GUIUtils.addMenuItem(toolsMenu, "Set number formats...",
                this, KeyEvent.VK_F);

        lookAndFeelMenu = new JMenu("Look and feel");
        toolsMenu.add(lookAndFeelMenu);

        UIManager.LookAndFeelInfo lookAndFeels[] = UIManager.getInstalledLookAndFeels();
        LookAndFeelChanger lfChanger = new LookAndFeelChanger();

        for (UIManager.LookAndFeelInfo lfInfo : lookAndFeels) {
            GUIUtils.addMenuItem(lookAndFeelMenu, lfInfo.getName(), lfChanger,
                    lfInfo.getClassName());
        }

        windowsMenu = new JWindowsMenu(((MainWindow) desktop).getDesktopPane());
        windowsMenu.setWindowPositioner(new CascadingWindowPositioner(
                ((MainWindow) desktop).getDesktopPane()));
        windowsMenu.setMnemonic(KeyEvent.VK_W);
        this.add(windowsMenu);

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
            rawDataVisualizationMenu.add(newItem);
            break;
        case ANALYSIS:
            dataAnalysisMenu.add(newItem);
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
            rawDataVisualizationMenu.addSeparator();
            break;
        case ANALYSIS:
            dataAnalysisMenu.addSeparator();
            break;

        }
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        Object src = e.getSource();

        if (src == fileExit) {
            core.exitMZmine();
        }

        // File -> Open
        if (src == fileOpen) {
            
            MainWindow mainWindow = MainWindow.getInstance();
            DesktopParameters parameters = (DesktopParameters) mainWindow.getParameterSet();
            String lastPath = parameters.getLastOpenPath();
            FileOpenDialog fileOpenDialog = new FileOpenDialog(ioController,
                    desktop, lastPath);
            fileOpenDialog.setVisible(true);
            parameters.setLastOpenPath(fileOpenDialog.getCurrentDirectory());

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

            PeakList[] alignmentResults = desktop.getSelectedAlignmentResults();
            if (alignmentResults.length > 0)
                new PeakListExportDialog(alignmentResults);

        }

        if (src == toolsFormat) {
            FormatSetupDialog formatDialog = new FormatSetupDialog(
                    MainWindow.getInstance(),
                    MainWindow.getInstance().getMZFormat(),
                    MainWindow.getInstance().getRTFormat(),
                    MainWindow.getInstance().getIntensityFormat());
            formatDialog.setVisible(true);
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
