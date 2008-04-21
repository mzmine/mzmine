/*
 * Copyright 2006-2008 The MZmine Development Team
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

package net.sf.mzmine.modules.io.rawdataimport;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import net.sf.mzmine.data.PreloadLevel;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.dialogs.ExitCode;

import com.sun.java.ExampleFileFilter;

/**
 * File open dialog
 */
public class FileOpenDialog extends JDialog implements ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private ExitCode exitCode = ExitCode.UNKNOWN;

    private RawDataImporterParameters parameters;
    private JFileChooser fileChooser;
    private JComboBox preloadChooser;

    public FileOpenDialog(RawDataImporterParameters parameters) {

        super(MZmineCore.getDesktop().getMainFrame(),
                "Please select data files to open", true);

        this.parameters = parameters;

        logger.finest("Displaying file open dialog");

        fileChooser = new JFileChooser();
        String path = (String) parameters.getParameterValue(RawDataImporterParameters.importDirectory);
        if (path != null)
            fileChooser.setCurrentDirectory(new File(path));
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.addActionListener(this);

        ExampleFileFilter filter = new ExampleFileFilter();
        filter.addExtension("cdf");
        filter.addExtension("nc");
        filter.setDescription("NetCDF files");
        fileChooser.addChoosableFileFilter(filter);

        filter = new ExampleFileFilter();
        filter.addExtension("mzDATA");
        filter.setDescription("mzDATA 1.05 files");
        fileChooser.addChoosableFileFilter(filter);

        filter = new ExampleFileFilter();
        filter.addExtension("mzML");
        filter.setDescription("mzML RAW files");
        fileChooser.addChoosableFileFilter(filter);

        filter = new ExampleFileFilter();
        filter.addExtension("RAW");
        filter.setDescription("XCalibur RAW files");
        fileChooser.addChoosableFileFilter(filter);

        filter = new ExampleFileFilter();
        filter.addExtension("xml");
        filter.addExtension("mzxml");
        filter.setDescription("MZXML files");
        fileChooser.addChoosableFileFilter(filter);

        filter = new ExampleFileFilter();
        filter.addExtension("cdf");
        filter.addExtension("nc");
        filter.addExtension("mzDATA");
        filter.addExtension("mzML");
        filter.addExtension("mzxml");
        filter.addExtension("RAW");
        filter.addExtension("xml");
        filter.setDescription("All raw data files");
        fileChooser.setFileFilter(filter);

        JPanel preloadChooserPanel = new JPanel(new FlowLayout());
        preloadChooser = new JComboBox(PreloadLevel.values());
        PreloadLevel previousPreloadLevel = (PreloadLevel) parameters.getParameterValue(RawDataImporterParameters.preloadLevel);
        if (previousPreloadLevel != null) preloadChooser.setSelectedItem(previousPreloadLevel);
        GUIUtils.addLabel(preloadChooserPanel, "Data storage:");
        preloadChooserPanel.add(preloadChooser);
        GUIUtils.addMargin(preloadChooserPanel, 10);

        add(fileChooser, BorderLayout.CENTER);
        add(preloadChooserPanel, BorderLayout.SOUTH);

        pack();

        setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        // check if user clicked "Open"
        if (command.equals("ApproveSelection")) {

            exitCode = ExitCode.OK;

            File[] selectedFiles = fileChooser.getSelectedFiles();
            parameters.setFileNames(selectedFiles);
            parameters.setParameterValue(
                    RawDataImporterParameters.importDirectory,
                    fileChooser.getCurrentDirectory().toString());
            PreloadLevel preloadLevel = (PreloadLevel) preloadChooser.getSelectedItem();
            parameters.setParameterValue(
                    RawDataImporterParameters.preloadLevel, preloadLevel);

        } else {
            exitCode = ExitCode.CANCEL;
        }

        // discard this dialog
        dispose();

    }

    /**
     * Method for reading exit code
     */
    public ExitCode getExitCode() {
        return exitCode;
    }

}
