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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.io.import_all_data_files;

import com.google.common.base.Strings;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.io.import_bruker_tdf.TDFImportTask;
import io.github.mzmine.modules.io.import_icpms_csv.IcpMsCVSImportTask;
import io.github.mzmine.modules.io.import_imzml.ImzMLImportTask;
import io.github.mzmine.modules.io.import_mzml_msdk.MSDKmzMLImportTask;
import io.github.mzmine.modules.io.import_mzxml.MzXMLImportTask;
import io.github.mzmine.modules.io.import_netcdf.NetCDFImportTask;
import io.github.mzmine.modules.io.import_thermo_raw.ThermoRawImportTask;
import io.github.mzmine.modules.io.import_waters_raw.WatersRawImportTask;
import io.github.mzmine.modules.io.import_zip.ZipImportTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RawDataFileType;
import io.github.mzmine.util.RawDataFileTypeDetector;
import io.github.mzmine.util.RawDataFileUtils;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;

/**
 * Raw data import module
 */
public class AllSpectralDataImportModule implements MZmineProcessingModule {

  private static final Logger logger = Logger
      .getLogger(AllSpectralDataImportModule.class.getName());

  private static final String MODULE_NAME = "Import MS data";
  private static final String MODULE_DESCRIPTION = "This module combines the import of different MS data formats and provides advanced options";

  @Override
  public @Nonnull
  String getName() {
    return MODULE_NAME;
  }

  @Nonnull
  @Override
  public String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Nonnull
  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.RAWDATA;
  }

  @Nonnull
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return AllSpectralDataImportParameters.class;
  }

  @Nonnull
  @Override
  public ExitCode runModule(final @Nonnull MZmineProject project, @Nonnull ParameterSet parameters,
      @Nonnull Collection<Task> tasks) {

    File[] fileNames = parameters.getParameter(AllSpectralDataImportParameters.fileNames)
        .getValue();
    boolean useAdvancedOptions = parameters
        .getParameter(AllSpectralDataImportParameters.advancedImport)
        .getValue();
    AdvancedSpectraImportParameters advancedParam = useAdvancedOptions ? parameters
        .getParameter(AllSpectralDataImportParameters.advancedImport)
        .getEmbeddedParameters() : null;

    if (Arrays.asList(fileNames).contains(null)) {
      logger.warning("List of filenames contains null");
      return ExitCode.ERROR;
    }

    // Find common prefix in raw file names if in GUI mode
    String commonPrefix = RawDataFileUtils.askToRemoveCommonPrefix(fileNames);

    // one storage for all files imported in the same task as they are typically analyzed together
    final MemoryMapStorage storage = MemoryMapStorage.forRawDataFile();

    for (int i = 0; i < fileNames.length; i++) {

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

      RawDataFileType fileType = RawDataFileTypeDetector.detectDataFileType(fileNames[i]);
      logger.finest("File " + fileNames[i] + " type detected as " + fileType);

      try {
        RawDataFile newMZmineFile = createDataFile(fileType, newName, storage);

        final Task newTask = useAdvancedOptions && advancedParam!=null?
            createAdvancedTask(fileType, project, fileNames[i], newMZmineFile, advancedParam) :
            createTask(fileType, project, fileNames[i], newMZmineFile);

        // add task to list
        if (newTask != null) {
          tasks.add(newTask);
        }
      } catch (IOException e) {
        e.printStackTrace();
        MZmineCore.getDesktop().displayErrorMessage("Could not create a new temporary file " + e);
        logger.log(Level.SEVERE, "Could not create a new temporary file ", e);
        return ExitCode.ERROR;
      }
    }

    return ExitCode.OK;
  }

  private Task createTask(RawDataFileType fileType, MZmineProject project, File file,
      RawDataFile newMZmineFile) {
    return switch (fileType) {
      // imaging
      case IMZML -> new ImzMLImportTask(project, file, (ImagingRawDataFile) newMZmineFile);
      case MZML_IMS -> new ImzMLImportTask(project, file, (ImagingRawDataFile) newMZmineFile);
      // IMS
      case BRUKER_TDF -> new TDFImportTask(project, file, (IMSRawDataFile) newMZmineFile);
      // MS
      case MZML -> new MSDKmzMLImportTask(project, file, newMZmineFile);
      case MZXML -> new MzXMLImportTask(project, file, newMZmineFile);
      case MZDATA -> new MzXMLImportTask(project, file, newMZmineFile);
      case NETCDF -> new NetCDFImportTask(project, file, newMZmineFile);
      case WATERS_RAW -> new WatersRawImportTask(project, file, newMZmineFile);
      case THERMO_RAW -> new ThermoRawImportTask(project, file, newMZmineFile);
      case ICPMSMS_CSV -> new IcpMsCVSImportTask(project, file, newMZmineFile);
      default -> throw new IllegalStateException("Unexpected value: " + fileType);
    };
  }
  private Task createAdvancedTask(RawDataFileType fileType, MZmineProject project, File file,
      RawDataFile newMZmineFile, AdvancedSpectraImportParameters advancedParam) {
    return switch (fileType) {
      // MS
      case MZML -> new MSDKmzMLImportTask(project, file, newMZmineFile, advancedParam);
      case MZXML -> new MzXMLImportTask(project, file, newMZmineFile, advancedParam);
      default -> throw new IllegalStateException("Advanced task not supported for data type: " + fileType);
    };
  }

  private RawDataFile createDataFile(RawDataFileType fileType, String newName,
      MemoryMapStorage storage) throws IOException {
    return switch (fileType) {
      case MZML, MZXML, MZDATA, THERMO_RAW, WATERS_RAW, NETCDF, GZIP, ICPMSMS_CSV -> MZmineCore
          .createNewFile(newName, storage);
      case IMZML, MZML_IMS -> MZmineCore.createNewImagingFile(newName, storage);
      case BRUKER_TDF -> MZmineCore.createNewIMSFile(newName, storage);
      default -> throw new IllegalStateException("Unexpected data type: " + fileType);
    };
  }

}
