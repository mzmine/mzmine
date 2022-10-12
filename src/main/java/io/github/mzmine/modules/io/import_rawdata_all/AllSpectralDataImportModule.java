/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.io.import_rawdata_all;

import com.google.common.base.Strings;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.io.import_rawdata_aird.AirdImportTask;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.TDFImportTask;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.TDFUtils;
import io.github.mzmine.modules.io.import_rawdata_bruker_tsf.TSFImportTask;
import io.github.mzmine.modules.io.import_rawdata_bruker_tsf.TSFUtils;
import io.github.mzmine.modules.io.import_rawdata_icpms_csv.IcpMsCVSImportTask;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImzMLImportTask;
import io.github.mzmine.modules.io.import_rawdata_mzdata.MzDataImportTask;
import io.github.mzmine.modules.io.import_rawdata_mzml.MSDKmzMLImportTask;
import io.github.mzmine.modules.io.import_rawdata_mzxml.MzXMLImportTask;
import io.github.mzmine.modules.io.import_rawdata_netcdf.NetCDFImportTask;
import io.github.mzmine.modules.io.import_rawdata_thermo_raw.ThermoRawImportTask;
import io.github.mzmine.modules.io.import_rawdata_waters_raw.WatersRawImportTask;
import io.github.mzmine.modules.io.import_rawdata_zip.ZipImportTask;
import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportParameters;
import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.CollectionUtils;
import io.github.mzmine.util.DialogLoggerUtil;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RawDataFileType;
import io.github.mzmine.util.RawDataFileTypeDetector;
import io.github.mzmine.util.RawDataFileUtils;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Raw data import module
 */
public class AllSpectralDataImportModule implements MZmineProcessingModule {

  private static final Logger logger = Logger.getLogger(
      AllSpectralDataImportModule.class.getName());

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
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {

    File[] selectedFiles = parameters.getValue(AllSpectralDataImportParameters.fileNames);

    // for bruker files path might point to D:\datafile.d\datafile.d  where the first is a folder
    // change to the folder
    Stream<File> fileStream = Arrays.stream(selectedFiles).map(this::validateBrukerPath);

    // check if files were already loaded
    if (parameters.getValue(AllSpectralDataImportParameters.skipExistingFiles)) {
      Set<String> currentFileNames = project.getCurrentRawDataFiles().stream()
          .map(RawDataFile::getName).collect(Collectors.toSet());
      fileStream = fileStream.filter(f -> !currentFileNames.contains(f.getName()));
    }

    File[] fileNames = fileStream.toArray(File[]::new);

    // check for duplicates in the input files
    List<String> duplicates = CollectionUtils.streamDuplicates(
        Arrays.stream(fileNames).map(File::getName)).toList();
    if (!duplicates.isEmpty()) {
      String msg = "Stopped import as there were duplicate file names in the import list. "
          + "Make sure to use unique names as MZmine and many downstream tools depend on this. Duplicates are: "
          + String.join("; ", duplicates);
      logger.warning(msg);
      if (!MZmineCore.isHeadLessMode()) {
        DialogLoggerUtil.showErrorDialog("Duplicate file names selected", msg);
      }
      return ExitCode.ERROR;
    }

    boolean useAdvancedOptions = parameters.getParameter(
        AllSpectralDataImportParameters.advancedImport).getValue();
    AdvancedSpectraImportParameters advancedParam =
        useAdvancedOptions ? parameters.getParameter(AllSpectralDataImportParameters.advancedImport)
            .getEmbeddedParameters() : null;

    if (Arrays.stream(fileNames).anyMatch(Objects::isNull)) {
      logger.warning("List of filenames contains null");
      return ExitCode.ERROR;
    }

    // start importing spectral libraries first
    final File[] libraryFiles = parameters.getValue(SpectralLibraryImportParameters.dataBaseFiles);
    if (libraryFiles != null) {
      // check for duplicates in the input files
      duplicates = CollectionUtils.streamDuplicates(Arrays.stream(libraryFiles).map(File::getName))
          .toList();
      if (!duplicates.isEmpty()) {
        String msg =
            "Stopped import as there were duplicate library file names in the import list. Duplicates are: "
                + String.join("; ", duplicates);
        logger.warning(msg);
        if (!MZmineCore.isHeadLessMode()) {
          DialogLoggerUtil.showErrorDialog("Duplicate library file names selected", msg);
        }
        return ExitCode.ERROR;
      }

      Set<String> currentLibraries = project.getCurrentSpectralLibraries().stream()
          .map(lib -> lib.getPath().getName()).collect(Collectors.toSet());

      // remove duplicates
      Arrays.stream(libraryFiles).filter(f -> !currentLibraries.contains(f.getName()))
          .forEach(f -> {
            Task newTask = new SpectralLibraryImportTask(project, f, moduleCallDate);
            tasks.add(newTask);
          });
    }

    // Find common prefix in raw file names if in GUI mode
    String commonPrefix = RawDataFileUtils.askToRemoveCommonPrefix(fileNames);

    // one storage for all files imported in the same task as they are typically analyzed together
    final MemoryMapStorage storage = MemoryMapStorage.forRawDataFile();

    final List<RawDataFileType> fileTypes = Arrays.stream(fileNames).<RawDataFileType>mapMulti(
        (filename, consumer) -> consumer.accept(
            RawDataFileTypeDetector.detectDataFileType(filename))).toList();

    // if any is null the data type was not detected then error out
    if (fileTypes.stream().anyMatch(Objects::isNull)) {
      String files = IntStream.range(0, fileTypes.size()).filter(i -> fileTypes.get(i) == null)
          .mapToObj(i -> fileNames[i].getAbsolutePath()).collect(Collectors.joining("\n"));
      String msg =
          "Could not identify the data type needed for import of n files=" + fileTypes.stream()
              .filter(Objects::isNull).count();
      MZmineCore.getDesktop().displayErrorMessage(msg);
      logger.log(Level.SEVERE, msg + ".  " + files);
      return ExitCode.ERROR;
    }

    final long numTdf = fileTypes.stream().filter(type -> type.equals(RawDataFileType.BRUKER_TDF))
        .count();
    final long numTsf = fileTypes.stream().filter(type -> type.equals(RawDataFileType.BRUKER_TSF))
        .count();
    if (numTdf > 0) {
      TDFUtils.setDefaultNumThreads((int) (MZmineCore.getConfiguration().getPreferences()
          .getParameter(MZminePreferences.numOfThreads).getValue() / numTdf));
    }
    if (numTsf > 0) {
      TSFUtils.setDefaultNumThreads((int) (MZmineCore.getConfiguration().getPreferences()
          .getParameter(MZminePreferences.numOfThreads).getValue() / numTsf));
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
        RawDataFile newMZmineFile = createDataFile(fileType, fileName.getAbsolutePath(), newName,
            storage);

        final AbstractTask newTask =
            useAdvancedOptions && advancedParam != null ? createAdvancedTask(fileType, project,
                fileName, newMZmineFile, advancedParam, AllSpectralDataImportModule.class,
                parameters, moduleCallDate, storage)
                : createTask(fileType, project, fileName, newMZmineFile,
                    AllSpectralDataImportModule.class, parameters, moduleCallDate, storage);

        // add task to list
        if (newTask != null) {
          tasks.add(newTask);
        }

        if (i == fileName.length() - 1) {
          newTask.addTaskStatusListener((task, newStatus, oldStatus) -> {
            if (newStatus == TaskStatus.CANCELED || newStatus == TaskStatus.FINISHED
                || newStatus == TaskStatus.ERROR) {
              final Integer threads = MZmineCore.getConfiguration().getPreferences()
                  .getParameter(MZminePreferences.numOfThreads).getValue();
              TDFUtils.setDefaultNumThreads(threads);
              TSFUtils.setDefaultNumThreads(threads);
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

  /**
   * Checks if the file and its parent both start with .d
   *
   * @param f file to validate
   * @return the valid bruker file path for bruker .d files or the input file
   */
  private File validateBrukerPath(File f) {
    if (f.getName().endsWith(".d") && f.getParent().endsWith(".d")) {
      return f.getParentFile();
    } else {
      return f;
    }
  }

  /**
   * @param newMZmineFile null for mzml files, can be ims or non ims. must be determined in import
   *                      task.
   */
  private AbstractTask createTask(RawDataFileType fileType, MZmineProject project, File file,
      @Nullable RawDataFile newMZmineFile, Class<? extends MZmineModule> module,
      ParameterSet parameters, @NotNull Instant moduleCallDate,
      @Nullable final MemoryMapStorage storage) {
    return switch (fileType) {
      // imaging
      case IMZML ->
          new ImzMLImportTask(project, file, (ImagingRawDataFile) newMZmineFile, module, parameters,
              moduleCallDate);
      // imaging, maldi, or LC-MS
      case BRUKER_TSF ->
          new TSFImportTask(project, file, MemoryMapStorage.forRawDataFile(), module, parameters,
              moduleCallDate);
      // IMS
      case BRUKER_TDF ->
          new TDFImportTask(project, file, (IMSRawDataFile) newMZmineFile, module, parameters,
              moduleCallDate);
      // MS
      case MZML, MZML_IMS ->
          new MSDKmzMLImportTask(project, file, module, parameters, moduleCallDate, storage);
      case MZXML ->
          new MzXMLImportTask(project, file, newMZmineFile, module, parameters, moduleCallDate);
      case MZDATA ->
          new MzDataImportTask(project, file, newMZmineFile, module, parameters, moduleCallDate);
      case NETCDF ->
          new NetCDFImportTask(project, file, newMZmineFile, module, parameters, moduleCallDate);
      case WATERS_RAW ->
          new WatersRawImportTask(project, file, newMZmineFile, module, parameters, moduleCallDate);
      case THERMO_RAW ->
          new ThermoRawImportTask(project, file, newMZmineFile, module, parameters, moduleCallDate);
      case ICPMSMS_CSV ->
          new IcpMsCVSImportTask(project, file, newMZmineFile, module, parameters, moduleCallDate);
      case MZML_GZIP, MZML_ZIP ->
          new ZipImportTask(project, file, module, parameters, moduleCallDate, storage);
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
      File file, @Nullable RawDataFile newMZmineFile,
      @NotNull AdvancedSpectraImportParameters advancedParam, Class<? extends MZmineModule> module,
      ParameterSet parameters, @NotNull Instant moduleCallDate,
      @Nullable final MemoryMapStorage storage) {
    return switch (fileType) {
      // MS
      case MZML, MZML_IMS ->
          new MSDKmzMLImportTask(project, file, null, advancedParam, module, parameters,
              moduleCallDate, storage);
      case MZXML ->
          new MzXMLImportTask(project, file, newMZmineFile, advancedParam, module, parameters,
              moduleCallDate);
      case BRUKER_TDF ->
          new TDFImportTask(project, file, (IMSRawDataFile) newMZmineFile, advancedParam, module,
              parameters, moduleCallDate);
      case AIRD ->
          new AirdImportTask(project, file, newMZmineFile, module, parameters, moduleCallDate);
      // all unsupported tasks are wrapped to apply import and mass detection separately
      case MZDATA, THERMO_RAW, WATERS_RAW, NETCDF, MZML_ZIP, MZML_GZIP, ICPMSMS_CSV, IMZML ->
          createWrappedAdvancedTask(fileType, project, file, newMZmineFile, advancedParam, module,
              parameters, moduleCallDate, storage);
      default -> throw new IllegalStateException("Unexpected data type: " + fileType);
    };
  }

  private AbstractTask createWrappedAdvancedTask(RawDataFileType fileType, MZmineProject project,
      File file, RawDataFile newMZmineFile, @NotNull AdvancedSpectraImportParameters advancedParam,
      Class<? extends MZmineModule> module, ParameterSet parameters,
      @NotNull Instant moduleCallDate, @Nullable final MemoryMapStorage storage) {
    // log
    logger.warning("Advanced processing is not available for MS data type: " + fileType.toString()
        + " and file " + file.getAbsolutePath());
    // create wrapped task to apply import and mass detection
    return new MsDataImportAndMassDetectWrapperTask(getMassListStorage(), newMZmineFile,
        createTask(fileType, project, file, newMZmineFile, module, parameters, moduleCallDate,
            storage), advancedParam, moduleCallDate);
  }

  @Nullable
  private RawDataFile createDataFile(RawDataFileType fileType, String absPath, String newName,
      MemoryMapStorage storage) throws IOException {
    return switch (fileType) {
      case MZXML, MZDATA, THERMO_RAW, WATERS_RAW, NETCDF, ICPMSMS_CSV, AIRD ->
          MZmineCore.createNewFile(newName, absPath, storage);
      case MZML, MZML_IMS, MZML_ZIP, MZML_GZIP -> null; // created in Mzml import task
      case IMZML -> MZmineCore.createNewImagingFile(newName, absPath, storage);
      case BRUKER_TDF -> MZmineCore.createNewIMSFile(newName, absPath, storage);
      case BRUKER_TSF ->
          null; // TSF can be anything: Single shot maldi, imaging, or LC-MS (non ims)
    };
  }

  public MemoryMapStorage getMassListStorage() {
    if (storageMassLists == null) {
      this.storageMassLists = MemoryMapStorage.forRawDataFile();
    }
    return storageMassLists;
  }

}
