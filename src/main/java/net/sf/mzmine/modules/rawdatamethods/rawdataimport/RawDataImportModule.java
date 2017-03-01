/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.rawdatamethods.rawdataimport;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.google.common.base.Strings;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFileWriter;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.AgilentCsvReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.MzDataReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.MzMLReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.MzXMLReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.NativeFileReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.NetCDFReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.ZipReadTask;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

/**
 * Raw data import module
 */
public class RawDataImportModule implements MZmineProcessingModule {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private static final String MODULE_NAME = "Raw data import";
    private static final String MODULE_DESCRIPTION = "This module imports raw data into the project.";

    @Override
    public @Nonnull String getName() {
        return MODULE_NAME;
    }

    @Override
    public @Nonnull String getDescription() {
        return MODULE_DESCRIPTION;
    }

    @Override
    @Nonnull
    public ExitCode runModule(final @Nonnull MZmineProject project,
            @Nonnull ParameterSet parameters, @Nonnull Collection<Task> tasks) {

        File fileNames[] = parameters
                .getParameter(RawDataImportParameters.fileNames).getValue();

        // Find common prefix in raw file names if in GUI mode
        String commonPrefix = null;
        if (MZmineCore.getDesktop().getMainWindow() != null
                && fileNames.length > 1) {
            String fileName = fileNames[0].getName();
            outerloop: for (int x = 0; x < fileName.length(); x++) {
                for (int i = 0; i < fileNames.length; i++) {
                    if (!fileName.substring(0, x)
                            .equals(fileNames[i].getName().substring(0, x))) {
                        commonPrefix = fileName.substring(0, x - 1);
                        break outerloop;
                    }
                }
            }

            if (!Strings.isNullOrEmpty(commonPrefix)) {

                // Show a dialog to allow user to remove common prefix
                Object[] options1 = { "Remove", "Do not remove", "Cancel" };
                JPanel panel = new JPanel();
                panel.add(new JLabel(
                        "The files you have chosen have a common prefix."));
                panel.add(new JLabel(
                        "Would you like to remove some or all of this prefix to shorten the names?"));
                panel.add(new JLabel(" "));
                panel.add(new JLabel("Prefix to remove:"));
                panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                JTextField textField = new JTextField(6);
                textField.setText(commonPrefix);
                panel.add(textField);

                int result = JOptionPane.showOptionDialog(null, panel,
                        "Common prefix", JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE, null, options1, null);

                // Cancel import if user clicked cancel
                if (result == 2) {
                    return ExitCode.ERROR;
                }

                // Only remove if user selected to do so
                if (result == 0) {
                    commonPrefix = textField.getText();
                } else {
                    commonPrefix = null;
                }
            }
        }

        for (int i = 0; i < fileNames.length; i++) {
            if (fileNames[i] == null) {
                return ExitCode.OK;
            }

            if ((!fileNames[i].exists()) || (!fileNames[i].canRead())) {
                MZmineCore.getDesktop().displayErrorMessage(
                        MZmineCore.getDesktop().getMainWindow(),
                        "Cannot read file " + fileNames[i]);
                logger.warning("Cannot read file " + fileNames[i]);
                return ExitCode.ERROR;
            }

            // Set the new name by removing the common prefix
            String newName;
            if (!Strings.isNullOrEmpty(commonPrefix)) {
                final String regex = "^" + Pattern.quote(commonPrefix);
                newName = fileNames[i].getName().replaceFirst(regex, "");
            } else {
                newName = fileNames[i].getName();
            }

            RawDataFileWriter newMZmineFile;
            try {
                newMZmineFile = MZmineCore.createNewFile(newName);
            } catch (IOException e) {
                MZmineCore.getDesktop().displayErrorMessage(
                        MZmineCore.getDesktop().getMainWindow(),
                        "Could not create a new temporary file " + e);
                logger.log(Level.SEVERE,
                        "Could not create a new temporary file ", e);
                return ExitCode.ERROR;
            }

            RawDataFileType fileType = RawDataFileTypeDetector
                    .detectDataFileType(fileNames[i]);
            logger.finest(
                    "File " + fileNames[i] + " type detected as " + fileType);

            if (fileType == null) {
                MZmineCore.getDesktop().displayErrorMessage(
                        MZmineCore.getDesktop().getMainWindow(),
                        "Could not determine the file type of file "
                                + fileNames[i]);
                continue;
            }

            Task newTask = createOpeningTask(fileType, project, fileNames[i],
                    newMZmineFile);

            if (newTask == null) {
                logger.warning("File type " + fileType + " of file "
                        + fileNames[i] + " is not supported.");
                return ExitCode.ERROR;
            }

            tasks.add(newTask);

        }

        return ExitCode.OK;
    }

    @Override
    public @Nonnull MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.RAWDATA;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return RawDataImportParameters.class;
    }

    public static Task createOpeningTask(RawDataFileType fileType,
            MZmineProject project, File fileName,
            RawDataFileWriter newMZmineFile) {
        Task newTask = null;
        switch (fileType) {
        case MZDATA:
            newTask = new MzDataReadTask(project, fileName, newMZmineFile);
            break;
        case MZML:
            newTask = new MzMLReadTask(project, fileName, newMZmineFile);
            break;
        case MZXML:
            newTask = new MzXMLReadTask(project, fileName, newMZmineFile);
            break;
        case NETCDF:
            newTask = new NetCDFReadTask(project, fileName, newMZmineFile);
            break;
        case AGILENT_CSV:
            newTask = new AgilentCsvReadTask(project, fileName, newMZmineFile);
            break;
        case THERMO_RAW:
        case WATERS_RAW:
            newTask = new NativeFileReadTask(project, fileName, fileType,
                    newMZmineFile);
            break;
        case ZIP:
        case GZIP:
            newTask = new ZipReadTask(project, fileName, fileType);
            break;

        }
        return newTask;
    }

}
