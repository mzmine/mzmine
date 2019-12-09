/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.io.rawdataimport;

import java.awt.Window;
import java.io.File;
import java.util.List;
import java.util.concurrent.FutureTask;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.util.ExitCode;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class RawDataImportParameters extends SimpleParameterSet {

    public static final FileNamesParameter fileNames = new FileNamesParameter();

    public RawDataImportParameters() {
        super(new Parameter[] { fileNames });
    }

    @Override
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import raw data files");
        fileChooser.getExtensionFilters().addAll(
                new ExtensionFilter("All raw data files", "*.cdf", "*.nc",
                        "*.mzData", "*.mzML", "*.mzXML", "*.xml", "*.raw",
                        "*.csv", "*.zip", "*.gz"), //
                new ExtensionFilter("NetCDF files", "*.cdf", "*.nc"), //
                new ExtensionFilter("mzML files", "*.mzML"), //
                new ExtensionFilter("mzData files", "*.mzData"), //
                new ExtensionFilter("mzXML files", "*.mzXML"), //
                new ExtensionFilter("Thermo RAW files", "*.raw"), //
                new ExtensionFilter("Waters RAW folders", "*.raw"), //
                new ExtensionFilter("Agilent CSV files", "*.csv"), //
                new ExtensionFilter("Compressed files", "*.zip", "*.gz"), //
                new ExtensionFilter("All Files", "*.*"));

        // We need to allow directories, because Waters raw data come in
        // directories, not files
        // chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        File lastFiles[] = getParameter(fileNames).getValue();
        if ((lastFiles != null) && (lastFiles.length > 0)) {
            File currentDir = lastFiles[0].getParentFile();
            if ((currentDir != null) && (currentDir.exists()))
                fileChooser.setInitialDirectory(currentDir);
        }

        final FutureTask<List<File>> task = new FutureTask<>(
                () -> fileChooser.showOpenMultipleDialog(null));
        Platform.runLater(task);
        try {
            List<File> selectedFiles = task.get();
            if (selectedFiles == null)
                return ExitCode.CANCEL;
            getParameter(fileNames)
                    .setValue(selectedFiles.toArray(new File[0]));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ExitCode.OK;

    }

}
