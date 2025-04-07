/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.RawDataImportTask;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.MsProcessor;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.MsProcessorList;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.processors.CropMzMsProcessor;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.processors.DenormalizeInjectTimeMsProcessor;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.processors.MassDetectorMsProcessor;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.processors.SortByMzMsProcessor;
import io.github.mzmine.modules.io.import_rawdata_bruker_baf.library.BafImportTask;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.TDFImportTask;
import io.github.mzmine.modules.io.import_rawdata_bruker_tsf.TSFImportTask;
import io.github.mzmine.modules.io.import_rawdata_icpms_csv.IcpMsCVSImportTask;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImzMLImportTask;
import io.github.mzmine.modules.io.import_rawdata_msconvert.MSConvertImportTask;
import io.github.mzmine.modules.io.import_rawdata_mzdata.MzDataImportTask;
import io.github.mzmine.modules.io.import_rawdata_mzml.MSDKmzMLImportTask;
import io.github.mzmine.modules.io.import_rawdata_mzxml.MzXMLImportTask;
import io.github.mzmine.modules.io.import_rawdata_netcdf.NetCDFImportTask;
import io.github.mzmine.modules.io.import_rawdata_thermo_raw.ThermoImportTaskDelegator;
import io.github.mzmine.modules.io.import_rawdata_zip.ZipImportTask;
import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportParameters;
import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RawDataFileType;
import static io.github.mzmine.util.RawDataFileTypeDetector.BAF_SUFFIX;
import static io.github.mzmine.util.RawDataFileTypeDetector.BRUKER_FOLDER_SUFFIX;
import static io.github.mzmine.util.RawDataFileTypeDetector.TDF_SUFFIX;
import static io.github.mzmine.util.RawDataFileTypeDetector.TSF_SUFFIX;
import io.github.mzmine.util.collections.CollectionUtils;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Raw data import module
 */
public class AllSpectralDataImportModule implements MZmineProcessingModule {

  private static final Logger logger = Logger.getLogger(
      AllSpectralDataImportModule.class.getName());

  public static final String MODULE_NAME = "Import MS data";
  private static final String MODULE_DESCRIPTION = "This module combines the import of different MS data formats and provides advanced options";

  /**
   * Define filters and processors for scans
   */
  public static @NotNull ScanImportProcessorConfig createSpectralProcessors(
      @Nullable final AdvancedSpectraImportParameters advanced) {
    if (advanced == null) {
      return ScanImportProcessorConfig.createDefault();
    }

    List<MsProcessor> processors = new ArrayList<>();
    processors.add(new SortByMzMsProcessor());

    // read parameters
    boolean ms1Detector = advanced.getValue(AdvancedSpectraImportParameters.msMassDetection);
    boolean ms2Detector = advanced.getValue(AdvancedSpectraImportParameters.ms2MassDetection);

    boolean applyMassDetection = ms1Detector || ms2Detector;

    boolean denormalizeMsn = advanced.getValue(AdvancedSpectraImportParameters.denormalizeMSnScans);
    Range<Double> cropMzRange = advanced.getEmbeddedParameterValueIfSelectedOrElse(
        AdvancedSpectraImportParameters.mzRange, null);

    // create more steps
    if (cropMzRange != null) {
      processors.add(
          new CropMzMsProcessor(cropMzRange.lowerEndpoint(), cropMzRange.upperEndpoint()));
    }
    if (applyMassDetection) {
      processors.add(new MassDetectorMsProcessor(advanced));
    }
    if (denormalizeMsn) {
      processors.add(new DenormalizeInjectTimeMsProcessor());
    }

    var scanFilter = advanced.getValue(AdvancedSpectraImportParameters.scanFilter);
    var conf = new ScanImportProcessorConfig(scanFilter, new MsProcessorList(processors));
    logger.info("Data import uses advanced direct data processing with these settings:\n" + conf);
    return conf;
  }

  /**
   * Checks if the file and its parent both start with .d
   *
   * @param f file to validate
   * @return the valid bruker file path for bruker .d files or the input file
   */
  public static File validateBrukerPath(File f) {
    final File parent = f.getParentFile();
    if (parent == null || !parent.getName().endsWith(BRUKER_FOLDER_SUFFIX)) {
      return f;
    } else if (f.getName().endsWith(TDF_SUFFIX) || f.getName().endsWith(TSF_SUFFIX) || f.getName()
        .endsWith(BAF_SUFFIX)) {
      // refer to the .d folder by default
      return f.getParentFile();
    } else if (f.getName().endsWith(BRUKER_FOLDER_SUFFIX) && f.isFile()) {
      // there also exists a .d file in the .d folder
      return parent;
    } else if (parent.exists() && parent.isDirectory() && Objects.requireNonNullElse(
        parent.listFiles(p -> p != null && p.getName().startsWith("analysis.")), new File[0]).length
        > 0) {
      return parent;
    } else {
      return f;
    }
  }

  /**
   * @return true if duplciates found in import list and already loaded files
   */
  private static boolean checkDuplicateFilesInImportListAndProject(
      final @NotNull MZmineProject project, final ImportFile[] filesToImport) {
    // check that files were not loaded before
    File[] alreadyImportedFiles = Stream.concat(
        project.getCurrentRawDataFiles().stream().map(RawDataFile::getAbsoluteFilePath),
        Arrays.stream(filesToImport).map(ImportFile::importedFile)).toArray(File[]::new);
    return containsDuplicateFiles(alreadyImportedFiles,
        "raw data file names in the import list that collide with already loaded data");
  }

  /**
   * @param context libraries or raw data
   * @return true if file names are duplicates
   */
  private static boolean containsDuplicateFiles(final File[] fileNames, String context) {
    List<String> duplicates = CollectionUtils.streamDuplicates(
        Arrays.stream(fileNames).map(File::getName)).toList();
    if (!duplicates.isEmpty()) {
      String msg = """
          Stopped import as there were duplicate %s.
          Make sure to use unique names as mzmine and many downstream tools depend on this. Duplicates are:
          %s""".formatted(context, String.join("\n", duplicates));
      DialogLoggerUtil.showErrorDialog("Duplicate files", msg);
      return true;
    }
    return false;
  }

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
      @NotNull Collection<Task> tasksToAdd, @NotNull Instant moduleCallDate) {

    // collect all tasks and run them on ThreadPoolTask
    List<Task> tasks = new ArrayList<>();
    // collect data import tasks to then load metadata once all data was loaded
    List<Task> dataImportTasks = new ArrayList<>();

    File[] selectedFiles = Arrays.stream(
            parameters.getValue(AllSpectralDataImportParameters.fileNames)).filter(Objects::nonNull)
        .toArray(File[]::new);

    // pre-check first, make sure all files exist
    if (containsMissingFiles(selectedFiles, "MS raw data")) {
      return ExitCode.ERROR;
    }
    // check for duplicates in the input files
    if (containsDuplicateFiles(selectedFiles, "raw data file names in the import list.")) {
      return ExitCode.ERROR;
    }

    // for bruker files path might point to D:\datafile.d\datafile.d  where the first is a folder
    // change to the folder
    // skip files that are already loaded
    final ImportFile[] filesToImport = AllSpectralDataImportParameters.skipAlreadyLoadedFiles(
        project, parameters);

    // after skipping already loaded
    if (checkDuplicateFilesInImportListAndProject(project, filesToImport)) {
      return ExitCode.ERROR;
    }

    AdvancedSpectraImportParameters advancedParam = parameters.getEmbeddedParametersIfSelectedOrElse(
        AllSpectralDataImportParameters.advancedImport, null);

    ScanImportProcessorConfig scanProcessorConfig = createSpectralProcessors(advancedParam);

    // start importing spectral libraries first
    final File[] libraryFiles = parameters.getValue(SpectralLibraryImportParameters.dataBaseFiles);

    if (libraryFiles != null) {
      // not existing files
      if (containsMissingFiles(libraryFiles, "spectral library")) {
        return ExitCode.ERROR;
      }
      // no duplicate names
      if (containsDuplicateFiles(libraryFiles, "spectral library file names in the import list.")) {
        return ExitCode.ERROR;
      }

      Set<File> currentLibraries = project.getCurrentSpectralLibraries().stream()
          .map(SpectralLibrary::getPath).collect(Collectors.toSet());
      for (File f : libraryFiles) {
        // skip libraries that are exactly the same file - there is no BATCH_LAST_LIBRARIES so we can do this here
        if (currentLibraries.contains(f)) {
          continue;
        }

        Task newTask = new SpectralLibraryImportTask(project, f, moduleCallDate);
        tasks.add(newTask);
      }
    }
    // metadata
    final File metadataFile = parameters.getEmbeddedParameterValueIfSelectedOrElse(
        AllSpectralDataImportParameters.metadataFile, null);
    if (metadataFile != null && !metadataFile.isFile()) {
      String msg = "Metadata file in MS data import cannot be found. Make sure to use the full filepath";
      MZmineCore.getDesktop().displayErrorMessage(msg);
      return ExitCode.ERROR;
    }

    // one storage for all files imported in the same task as they are typically analyzed together
    final MemoryMapStorage storage = MemoryMapStorage.forRawDataFile();

    // if any is null, the data type was not detected. then error out
    final List<ImportFile> unknownFileTypes = Arrays.stream(filesToImport)
        .filter(f -> f.type() == null).toList();
    if (!unknownFileTypes.isEmpty()) {
      String files = Arrays.stream(filesToImport).map(f -> f.originalFile().getAbsolutePath())
          .collect(Collectors.joining(",\n"));
      String msg = "Could not identify the data type needed for import of n files=%d. The file/path might not exist.\n%s".formatted(
          unknownFileTypes.size(), files);
      MZmineCore.getDesktop().displayErrorMessage(msg);
      logger.log(Level.SEVERE, "%s.  %s".formatted(msg, files));
      return ExitCode.ERROR;
    }

    for (int i = 0; i < filesToImport.length; i++) {
      final ImportFile fileToImport = filesToImport[i];
      if ((!fileToImport.originalFile().exists()) || (!fileToImport.originalFile().canRead())) {
        DialogLoggerUtil.showErrorDialog("Cannot import file",
            "Cannot read file %s. The file/path might not exist.".formatted(
                fileToImport.originalFile()));
        return ExitCode.ERROR;
      }

      final RawDataFileType fileType = fileToImport.type();
      logger.finest("File " + fileToImport.originalFile() + " type detected as " + fileType);

      final Task newTask;//
      if (advancedParam != null) {
        newTask = createAdvancedTask(fileType, project, fileToImport.originalFile(),
            scanProcessorConfig, AllSpectralDataImportModule.class, parameters, moduleCallDate,
            storage);
      } else {
        newTask = createTask(fileType, project, fileToImport.originalFile(), scanProcessorConfig,
            AllSpectralDataImportModule.class, parameters, moduleCallDate, storage);
      }

      // add task to list
      if (newTask != null) {
        tasks.add(newTask);
        dataImportTasks.add(newTask);
      }

    }

    // create ThreadPool
//    int nThreads = MZmineCore.getConfiguration().getNumOfThreads();
//    var description = STR."Importing \{tasks.size()} data files";
//    var threadPoolTask = new ThreadPoolTask(description , nThreads, tasks);
//    tasksToAdd.add(threadPoolTask);

    AllSpectralDataImportMainTask mainTask = new AllSpectralDataImportMainTask(tasks, parameters);
    tasksToAdd.add(mainTask);

    return ExitCode.OK;
  }

  private boolean containsMissingFiles(File[] selectedFiles, String context) {
    final List<String> missingFiles = Arrays.stream(selectedFiles)
        .filter(Predicate.not(File::exists)).map(File::getAbsolutePath).toList();
    if (!missingFiles.isEmpty()) {
      String msg = """
          Stopped import as there were %s files that cannot be found.
          Make sure to use full file paths of existing files:
          %s""".formatted(context, String.join("\n", missingFiles));
      DialogLoggerUtil.showErrorDialog("Missing files", msg);
      return true;
    }
    return false;
  }

  /**
   * @param scanProcessorConfig
   */
  private RawDataImportTask createTask(RawDataFileType fileType, MZmineProject project, File file,
      @NotNull final ScanImportProcessorConfig scanProcessorConfig,
      Class<? extends MZmineModule> module, ParameterSet parameters,
      @NotNull Instant moduleCallDate, @Nullable final MemoryMapStorage storage) {
    return switch (fileType) {
      // imaging
      case IMZML -> new ImzMLImportTask(project, file, scanProcessorConfig, module, parameters,
          moduleCallDate, storage);
      // imaging, maldi, or LC-MS
      case BRUKER_TSF ->
          new TSFImportTask(project, file, storage, module, parameters, moduleCallDate,
              scanProcessorConfig);
      // IMS
      case BRUKER_TDF -> // ims files are big, use own storage
          new TDFImportTask(project, file, storage, module, parameters, moduleCallDate);
      // MS
      case MZML, MZML_IMS ->
          new MSDKmzMLImportTask(project, file, scanProcessorConfig, module, parameters,
              moduleCallDate, storage);
      case MZXML -> new MzXMLImportTask(project, file, scanProcessorConfig, module, parameters,
          moduleCallDate, storage);
      case MZDATA ->
          new MzDataImportTask(project, file, module, parameters, moduleCallDate, storage);
      case NETCDF ->
          new NetCDFImportTask(project, file, module, parameters, moduleCallDate, storage);
      case THERMO_RAW ->
          new ThermoImportTaskDelegator(storage, moduleCallDate, file, scanProcessorConfig, project,
              parameters, module);
      case ICPMSMS_CSV ->
          new IcpMsCVSImportTask(project, file, module, parameters, moduleCallDate, storage);
      case MZML_GZIP, MZML_ZIP ->
          new ZipImportTask(project, file, scanProcessorConfig, module, parameters, moduleCallDate,
              storage);
      case BRUKER_BAF ->
          new BafImportTask(storage, moduleCallDate, file, module, parameters, project,
              scanProcessorConfig);
//      case AIRD -> throw new IllegalStateException("Unexpected value: " + fileType);
      // When adding a new file type, also add to MSConvertImportTask#getSupportedFileTypes()
      case WATERS_RAW, WATERS_RAW_IMS, SCIEX_WIFF, SCIEX_WIFF2, AGILENT_D, AGILENT_D_IMS ->
          new MSConvertImportTask(storage, moduleCallDate, file, scanProcessorConfig, project,
              module, parameters);
    };
  }

  /**
   * Create a task that imports an directly applies mass detection. Not supported by all imports
   * yet
   *
   * @param scanProcessorConfig the advanced parameters
   * @return the task or null if the data format is not supported for direct mass detection
   */
  private RawDataImportTask createAdvancedTask(RawDataFileType fileType, MZmineProject project,
      File file, @NotNull ScanImportProcessorConfig scanProcessorConfig,
      Class<? extends MZmineModule> module, ParameterSet parameters,
      @NotNull Instant moduleCallDate, @Nullable final MemoryMapStorage storage) {

    // needs a storage for mass lists if advanced import with mass detection was selected but not supported for a MS file type
    MemoryMapStorage storageMassLists = MemoryMapStorage.forMassList();

    return switch (fileType) {
      // imaging
      case IMZML -> new ImzMLImportTask(project, file, scanProcessorConfig, module, parameters,
          moduleCallDate, storage);
      // MS
      case MZML, MZML_IMS ->
          new MSDKmzMLImportTask(project, file, null, scanProcessorConfig, module, parameters,
              moduleCallDate, storage);
      case MZXML -> new MzXMLImportTask(project, file, scanProcessorConfig, module, parameters,
          moduleCallDate, storage);
      case BRUKER_TDF ->
          new TDFImportTask(project, file, storage, scanProcessorConfig, module, parameters,
              moduleCallDate);
      case THERMO_RAW ->
          new ThermoImportTaskDelegator(storage, moduleCallDate, file, scanProcessorConfig, project,
              parameters, module);
      case BRUKER_TSF ->
          new TSFImportTask(project, file, storage, module, parameters, moduleCallDate,
              scanProcessorConfig);
      case BRUKER_BAF ->
          new BafImportTask(storage, moduleCallDate, file, module, parameters, project,
              scanProcessorConfig);
      // When adding a new file type, also add to MSConvertImportTask#getSupportedFileTypes()
      case AGILENT_D, AGILENT_D_IMS, SCIEX_WIFF, SCIEX_WIFF2, WATERS_RAW, WATERS_RAW_IMS ->
          new MSConvertImportTask(storage, moduleCallDate, file, scanProcessorConfig, project,
              module, parameters);
      // all unsupported tasks are wrapped to apply import and mass detection separately
      case MZDATA, NETCDF, MZML_ZIP, MZML_GZIP, ICPMSMS_CSV ->
          createWrappedAdvancedTask(fileType, project, file, scanProcessorConfig, module,
              parameters, moduleCallDate, storage, storageMassLists);
    };
  }

  private RawDataImportTask createWrappedAdvancedTask(RawDataFileType fileType,
      MZmineProject project, File file, @NotNull ScanImportProcessorConfig scanProcessorConfig,
      Class<? extends MZmineModule> module, ParameterSet parameters,
      @NotNull Instant moduleCallDate, @Nullable final MemoryMapStorage storage,
      @Nullable final MemoryMapStorage storageMassLists) {
    // log
    logger.warning("Advanced processing is not available for MS data type: " + fileType.toString()
        + " and file " + file.getAbsolutePath());
    // create wrapped task to apply import and mass detection
    return new MsDataImportAndMassDetectWrapperTask(storageMassLists,
        createTask(fileType, project, file, scanProcessorConfig, module, parameters, moduleCallDate,
            storage), scanProcessorConfig, moduleCallDate);
  }
}
