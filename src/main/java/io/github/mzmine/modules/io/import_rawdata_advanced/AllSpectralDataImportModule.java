/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.io.import_rawdata_advanced;

import com.google.common.base.Strings;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.TDFImportTask;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.TDFUtils;
import io.github.mzmine.modules.io.import_rawdata_icpms_csv.IcpMsCVSImportTask;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImzMLImportTask;
import io.github.mzmine.modules.io.import_rawdata_mzdata.MzDataImportTask;
import io.github.mzmine.modules.io.import_rawdata_mzml.MzMLImportTask;
import io.github.mzmine.modules.io.import_rawdata_mzxml.MzXMLImportTask;
import io.github.mzmine.modules.io.import_rawdata_netcdf.NetCDFImportTask;
import io.github.mzmine.modules.io.import_rawdata_thermo_raw.ThermoRawImportTask;
import io.github.mzmine.modules.io.import_rawdata_waters_raw.WatersRawImportTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RawDataFileType;
import io.github.mzmine.util.RawDataFileTypeDetector;
import io.github.mzmine.util.RawDataFileUtils;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

/**
 * Raw data import module
 */
public class AllSpectralDataImportModule implements MZmineProcessingModule {

  private static final Logger logger = Logger
      .getLogger(AllSpectralDataImportModule.class.getName());

  private static final String MODULE_NAME = "Import MS data";
  private static final String MODULE_DESCRIPTION = "This module combines the import of different MS data formats and provides advanced options";

  // needs a storage for mass lists if advanced import with mass detection was selected but not supported for a MS file type
  private MemoryMapStorage storageMassLists = null;

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @NotNull
  @Override
  public String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @NotNull
  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.RAWDATAIMPORT;
  }

  @NotNull
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return AllSpectralDataImportParameters.class;
  }

  @NotNull
  @Override
  public ExitCode runModule(final @NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks) {

    File[] fileNames = parameters.getParameter(AllSpectralDataImportParameters.fileNames)
        .getValue();
    boolean useAdvancedOptions = parameters
        .getParameter(AllSpectralDataImportParameters.advancedImport).getValue();
    AdvancedSpectraImportParameters advancedParam =
        useAdvancedOptions ? parameters.getParameter(AllSpectralDataImportParameters.advancedImport)
            .getEmbeddedParameters() : null;

    if (Arrays.asList(fileNames).contains(null)) {
      logger.warning("List of filenames contains null");
      return ExitCode.ERROR;
    }

    // Find common prefix in raw file names if in GUI mode
    String commonPrefix = RawDataFileUtils.askToRemoveCommonPrefix(fileNames);

    // one storage for all files imported in the same task as they are typically analyzed together
    final MemoryMapStorage storage = MemoryMapStorage.forRawDataFile();

    final List<RawDataFileType> fileTypes = Arrays.stream(fileNames).<RawDataFileType>mapMulti(
        (filename, consumer) -> consumer
            .accept(RawDataFileTypeDetector.detectDataFileType(filename))).toList();
    final long numTdf = fileTypes.stream().filter(type -> type.equals(RawDataFileType.BRUKER_TDF))
        .count();
    if (numTdf > 0) {
      TDFUtils.setDefaultNumThreads((int) (MZmineCore.getConfiguration().getPreferences()
          .getParameter(MZminePreferences.numOfThreads).getValue() / numTdf));
    }

    for (int i = 0; i < fileNames.length; i++) {
      final File fileName = fileNames[i];

      if ((!fileName.exists()) || (!fileName.canRead())) {
        MZmineCore.getDesktop().displayErrorMessage("Cannot read file " + fileName);
        logger.warning("Cannot read file " + fileName);
        return ExitCode.ERROR;
      }

      // Set the new name by removing the common prefix
      String newName;
      if (!Strings.isNullOrEmpty(commonPrefix)) {
        final String regex = "^" + Pattern.quote(commonPrefix);
        newName = fileName.getName().replaceFirst(regex, "");
      } else {
        newName = fileName.getName();
      }

      final RawDataFileType fileType = fileTypes.get(i);
      logger.finest("File " + fileName + " type detected as " + fileType);

      try {
        RawDataFile newMZmineFile = createDataFile(fileType, newName, storage);

        final AbstractTask newTask =
            useAdvancedOptions && advancedParam != null ? createAdvancedTask(fileType, project,
                fileName, newMZmineFile, advancedParam)
                : createTask(fileType, project, fileName, newMZmineFile);

        // add task to list
        if (newTask != null) {
          tasks.add(newTask);
        }

        if (i == fileName.length() - 1) {
          newTask.addTaskStatusListener((task, newStatus, oldStatus) -> {
            if (newStatus == TaskStatus.CANCELED || newStatus == TaskStatus.FINISHED
                || newStatus == TaskStatus.ERROR) {
              TDFUtils.setDefaultNumThreads(MZmineCore.getConfiguration().getPreferences()
                  .getParameter(MZminePreferences.numOfThreads).getValue());
            }
          });
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

  private AbstractTask createTask(RawDataFileType fileType, MZmineProject project, File file,
      RawDataFile newMZmineFile) {
    return switch (fileType) {
      // imaging
      case IMZML -> new ImzMLImportTask(project, file, (ImagingRawDataFile) newMZmineFile);
      // IMS
      case MZML_IMS -> new MzMLImportTask(project, file, (IMSRawDataFile) newMZmineFile);
      case BRUKER_TDF -> new TDFImportTask(project, file, (IMSRawDataFile) newMZmineFile);
      // MS
      case MZML -> new MzMLImportTask(project, file, newMZmineFile);
      case MZXML -> new MzXMLImportTask(project, file, newMZmineFile);
      case MZDATA -> new MzDataImportTask(project, file, newMZmineFile);
      case NETCDF -> new NetCDFImportTask(project, file, newMZmineFile);
      case WATERS_RAW -> new WatersRawImportTask(project, file, newMZmineFile);
      case THERMO_RAW -> new ThermoRawImportTask(project, file, newMZmineFile);
      case ICPMSMS_CSV -> new IcpMsCVSImportTask(project, file, newMZmineFile);
      default -> throw new IllegalStateException("Unexpected value: " + fileType);
    };
  }

  /**
   * Create a task that imports an directly applies mass detection. Not supported by all imports
   * yet
   *
   * @param advancedParam the advanced parameters
   * @return the task or null if the data format is not supported for direct mass detection
   */
  private AbstractTask createAdvancedTask(RawDataFileType fileType, MZmineProject project,
      File file, RawDataFile newMZmineFile,
      @NotNull AdvancedSpectraImportParameters advancedParam) {
    return switch (fileType) {
      // MS
      case MZML -> new MzMLImportTask(project, file, newMZmineFile, advancedParam);
      case MZXML -> new MzXMLImportTask(project, file, newMZmineFile, advancedParam);
      case BRUKER_TDF -> new TDFImportTask(project, file, (IMSRawDataFile) newMZmineFile,
          advancedParam);
      // all unsupported tasks are wrapped to apply import and mass detection separately
      case MZDATA, THERMO_RAW, WATERS_RAW, NETCDF, GZIP, ICPMSMS_CSV, IMZML, MZML_IMS -> createWrappedAdvancedTask(
          fileType, project, file, newMZmineFile, advancedParam);
      default -> throw new IllegalStateException("Unexpected data type: " + fileType);
    };
  }

  private AbstractTask createWrappedAdvancedTask(RawDataFileType fileType, MZmineProject project,
      File file, RawDataFile newMZmineFile,
      @NotNull AdvancedSpectraImportParameters advancedParam) {
    // log
    logger.warning("Advanced processing is not available for MS data type: " + fileType.toString()
        + " and file " + file.getAbsolutePath());
    // create wrapped task to apply import and mass detection
    return new MsDataImportAndMassDetectWrapperTask(getMassListStorage(), newMZmineFile,
        createTask(fileType, project, file, newMZmineFile), advancedParam);
  }

  private RawDataFile createDataFile(RawDataFileType fileType, String newName,
      MemoryMapStorage storage) throws IOException {
    return switch (fileType) {
      case MZML, MZXML, MZDATA, THERMO_RAW, WATERS_RAW, NETCDF, GZIP, ICPMSMS_CSV -> MZmineCore
          .createNewFile(newName, storage);
      case IMZML -> MZmineCore.createNewImagingFile(newName, storage);
      case BRUKER_TDF, MZML_IMS -> MZmineCore.createNewIMSFile(newName, storage);
      default -> throw new IllegalStateException("Unexpected data type: " + fileType);
    };
  }

  public MemoryMapStorage getMassListStorage() {
    if (storageMassLists == null) {
      this.storageMassLists = MemoryMapStorage.forRawDataFile();
    }
    return storageMassLists;
  }

}
