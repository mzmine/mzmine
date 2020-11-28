/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.io.rawdataimport;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;

import com.google.common.base.Strings;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFileWriter;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.io.rawdataimport.fileformats.AgilentCsvReadTask;
import io.github.mzmine.modules.io.rawdataimport.fileformats.CsvReadTask;
import io.github.mzmine.modules.io.rawdataimport.fileformats.MzDataReadTask;
import io.github.mzmine.modules.io.rawdataimport.fileformats.MzMLReadTask;
import io.github.mzmine.modules.io.rawdataimport.fileformats.MzXMLReadTask;
import io.github.mzmine.modules.io.rawdataimport.fileformats.NativeFileReadTask;
import io.github.mzmine.modules.io.rawdataimport.fileformats.NetCDFReadTask;
import io.github.mzmine.modules.io.rawdataimport.fileformats.ZipReadTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Raw data import module
 */
public class RawDataImportModule implements MZmineProcessingModule {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private static final String MODULE_NAME = "Raw data import";
    private static final String MODULE_DESCRIPTION = "This module imports raw data into the project.";
    private String commonPrefix = null;

    @Override
    public @Nonnull
    String getName() {
        return MODULE_NAME;
    }

    @Override
    public @Nonnull
    String getDescription() {
        return MODULE_DESCRIPTION;
    }


    /**
     * return the prefix used in last call to runModule
     *
     * @return String
     */
    public @Nonnull
    String getLastCommonPrefix() {
        if (commonPrefix == null) {
            commonPrefix = "";
        }
        return commonPrefix;
    }

    @Override
    @Nonnull
    public ExitCode runModule(final @Nonnull MZmineProject project, @Nonnull ParameterSet parameters,
                              @Nonnull Collection<Task> tasks) {

        File fileNames[] = parameters.getParameter(RawDataImportParameters.fileNames).getValue();

        // Find common prefix in raw file names if in GUI mode
        String commonPrefix = null;
        if (MZmineCore.getDesktop().getMainWindow() != null && fileNames.length > 1) {
            String fileName = fileNames[0].getName();
            outerloop:
            for (int x = 0; x < fileName.length(); x++) {
                for (int i = 0; i < fileNames.length; i++) {
                    if (!fileName.substring(0, x).equals(fileNames[i].getName().substring(0, x))) {
                        commonPrefix = fileName.substring(0, x - 1);
                        break outerloop;
                    }
                }
            }
            this.commonPrefix = commonPrefix;

            if (!Strings.isNullOrEmpty(commonPrefix)) {

                // Show a dialog to allow user to remove common prefix
                Dialog<ButtonType> dialog = new Dialog<>();
                dialog.setTitle("Common prefix");
                dialog.setContentText(
                        "The files you have chosen have a common prefix. Would you like to remove some or all of this prefix to shorten the names?");

                TextField textField = new TextField();
                textField.setText(commonPrefix);

                VBox panel = new VBox(10.0);
                panel.getChildren().add(new Label("The files you have chosen have a common prefix."));
                panel.getChildren().add(
                        new Label("Would you like to remove some or all of this prefix to shorten the names?"));
                panel.getChildren().add(new Label(" "));
                panel.getChildren().add(new Label("Prefix to remove:"));
                panel.getChildren().add(textField);

                ButtonType removeButtonType = new ButtonType("Remove", ButtonData.YES);
                ButtonType notRemoveButtonType = new ButtonType("Do not remove", ButtonData.NO);
                dialog.getDialogPane().getButtonTypes().addAll(removeButtonType, notRemoveButtonType,
                        ButtonType.CANCEL);
                dialog.getDialogPane().setContent(panel);
                Optional<ButtonType> result = dialog.showAndWait();

                if (!result.isPresent())
                    return ExitCode.CANCEL;

                // Cancel import if user clicked cancel
                if (result.get() == ButtonType.CANCEL) {
                    return ExitCode.CANCEL;
                }

                // Only remove if user selected to do so
                if (result.get() == removeButtonType) {
                    commonPrefix = textField.getText();
                } else {
                    commonPrefix = null;
                    this.commonPrefix = null;
                }
            }
        }

        for (int i = 0; i < fileNames.length; i++) {
            if (fileNames[i] == null) {
                return ExitCode.OK;
            }

            if ((!fileNames[i].exists()) || (!fileNames[i].canRead())) {
                MZmineCore.getDesktop().displayErrorMessage("Cannot read file " + fileNames[i]);
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
                MZmineCore.getDesktop().displayErrorMessage("Could not create a new temporary file " + e);
                logger.log(Level.SEVERE, "Could not create a new temporary file ", e);
                return ExitCode.ERROR;
            }

            RawDataFileType fileType = RawDataFileTypeDetector.detectDataFileType(fileNames[i]);
            logger.finest("File " + fileNames[i] + " type detected as " + fileType);

            if (fileType == null) {
                MZmineCore.getDesktop()
                        .displayErrorMessage("Could not determine the file type of file " + fileNames[i]);
                continue;
            }

            Task newTask = createOpeningTask(fileType, project, fileNames[i], newMZmineFile);

            if (newTask == null) {
                logger.warning("File type " + fileType + " of file " + fileNames[i] + " is not supported.");
                return ExitCode.ERROR;
            }

            tasks.add(newTask);

        }

        return ExitCode.OK;
    }

    @Override
    public @Nonnull
    MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.RAWDATA;
    }

    @Override
    public @Nonnull
    Class<? extends ParameterSet> getParameterSetClass() {
        return RawDataImportParameters.class;
    }

    public static Task createOpeningTask(RawDataFileType fileType, MZmineProject project,
                                         File fileName, RawDataFileWriter newMZmineFile) {
        Task newTask = null;
        switch (fileType) {
            case ICPMSMS_CSV:
                newTask = new CsvReadTask(project, fileName, newMZmineFile);
                break;
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
                newTask = new NativeFileReadTask(project, fileName, fileType, newMZmineFile);
                break;
            case ZIP:
            case GZIP:
                newTask = new ZipReadTask(project, fileName, fileType);
                break;

        }
        return newTask;
    }

}
