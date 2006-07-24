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

package net.sf.mzmine.userinterface.dialogs;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;

import net.sf.mzmine.io.IOController;
import net.sf.mzmine.io.IOController.PreloadLevel;
import net.sf.mzmine.userinterface.Desktop;
import sunutils.ExampleFileFilter;

/**
 * File open dialog
 */
public class FileOpenDialog extends JDialog implements ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    private JFileChooser fileChooser;
    private JCheckBox preloadCheckBox;
    private IOController ioController;
    public FileOpenDialog(IOController ioController, Desktop desktop) {

        super(desktop.getMainFrame(), "Please select data files to open",
                true);
        
        logger.finest("Displaying file open dialog");
        
        this.ioController = ioController;
        fileChooser = new JFileChooser();

        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.addActionListener(this);

        ExampleFileFilter filter = new ExampleFileFilter();
        filter.addExtension("cdf");
        filter.addExtension("nc");
        filter.setDescription("NetCDF files");
        fileChooser.addChoosableFileFilter(filter);

        filter = new ExampleFileFilter();
        filter.addExtension("xml");
        filter.addExtension("mzxml");
        filter.setDescription("MZXML files");
        fileChooser.addChoosableFileFilter(filter);

        filter = new ExampleFileFilter();
        filter.addExtension("cdf");
        filter.addExtension("nc");
        filter.addExtension("xml");
        filter.addExtension("mzxml");
        filter.setDescription("All raw data files");
        fileChooser.setFileFilter(filter);

        preloadCheckBox = new JCheckBox(
                "Preload all data into memory? (use with caution)");
        preloadCheckBox.setMargin(new Insets(10, 10, 10, 10));

        add(fileChooser, BorderLayout.CENTER);
        add(preloadCheckBox, BorderLayout.SOUTH);

        pack();

        setLocationRelativeTo(desktop.getMainFrame());

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        // check if user clicked "Open"
        if (command.equals("ApproveSelection")) {

            File[] selectedFiles = fileChooser.getSelectedFiles();

            PreloadLevel preloadLevel = PreloadLevel.NO_PRELOAD;
            if (preloadCheckBox.isSelected())
                preloadLevel = PreloadLevel.PRELOAD_ALL_SCANS;
            ioController.openFiles(selectedFiles, preloadLevel);

        }

        // discard this dialog
        dispose();

    }

}
