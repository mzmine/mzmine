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
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
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
import io.github.mzmine.modules.io.import_rawdata_thermo_raw.ThermoRawImportTask;
import io.github.mzmine.modules.io.import_rawdata_zip.ZipImportTask;
import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportParameters;
import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RawDataFileType;
import io.github.mzmine.util.RawDataFileTypeDetector;
import io.github.mzmine.util.collections.CollectionUtils;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    if (f.getParent().endsWith(".d") && (f.getName().endsWith(".d") || f.getName().endsWith(".tdf")
        || f.getName().endsWith(".tsf") || f.getName().endsWith(".baf"))) {
      return f.getParentFile();
    } else {
      return f;
    }
  }

  /**
   * @return true if duplciates found in import list and already loaded files
   */
  private static boolean checkDuplicateFilesInImportListAndProject(
      final @NotNull MZmineProject project, final File[] fileNames) {
    // check that files were not loaded before
    File[] currentAndLoadFiles = Stream.concat(
        project.getCurrentRawDataFiles().stream().map(RawDataFile::getFileName).map(File::new),
        Arrays.stream(fileNames)).toArray(File[]::new);
    return containsDuplicateFiles(currentAndLoadFiles,
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
          Make sure to use unique names as MZmine and many downstream tools depend on this. Duplicates are:
          %s""".formatted(context, String.join("\n", duplicates));
      logger.warning(msg);
      MZmineCore.getDesktop().displayErrorMessage(msg);
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

    // precheck first, make sure all
    File[] selectedFiles = Arrays.stream(
            parameters.getValue(AllSpectralDataImportParameters.fileNames))
        .map(AllSpectralDataImportModule::validateBrukerPath).toArray(File[]::new);
    // check for duplicates in the input files
    if (containsDuplicateFiles(selectedFiles, "raw data file names in the import list.")) {
      return ExitCode.ERROR;
    }
    if (Arrays.stream(selectedFiles).anyMatch(Objects::isNull)) {
      logger.warning("List of filenames contains null");
      return ExitCode.ERROR;
    }

    // for bruker files path might point to D:\datafile.d\datafile.d  where the first is a folder
    // change to the folder
    // skip files that are already loaded
    final File[] fileNames = AllSpectralDataImportParameters.skipAlreadyLoadedFiles(project,
        parameters);

    // after skipping already loaded
    if (checkDuplicateFilesInImportListAndProject(project, fileNames)) {
      return ExitCode.ERROR;
    }

    AdvancedSpectraImportParameters advancedParam = parameters.getEmbeddedParametersIfSelectedOrElse(
        AllSpectralDataImportParameters.advancedImport, null);

    ScanImportProcessorConfig scanProcessorConfig = createSpectralProcessors(advancedParam);

    // start importing spectral libraries first
    final File[] libraryFiles = parameters.getValue(SpectralLibraryImportParameters.dataBaseFiles);

    if (libraryFiles != null) {
      // no duplicate names
      if (containsDuplicateFiles(libraryFiles, "spectral libraries in the import list")) {
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

    // one storage for all files imported in the same task as they are typically analyzed together
    final MemoryMapStorage storage = MemoryMapStorage.forRawDataFile();

    final List<RawDataFileType> fileTypes = Arrays.stream(fileNames).<RawDataFileType>mapMulti(
        (filename, consumer) -> consumer.accept(
            RawDataFileTypeDetector.detectDataFileType(filename))).toList();

    // if any is null the data type was not detected then error out
    if (fileTypes.stream().anyMatch(Objects::isNull)) {
      String files = IntStream.range(0, fileTypes.size()).filter(i -> fileTypes.get(i) == null)
          .mapToObj(i -> fileNames[i].getAbsolutePath()).collect(Collectors.joining(",\n"));
      String msg = STR."Could not identify the data type needed for import of n files=\{fileTypes.stream()
          .filter(Objects::isNull).count()}. The file/path might not exist.\n\{files}";
      MZmineCore.getDesktop().displayErrorMessage(msg);
      logger.log(Level.SEVERE, STR."\{msg}.  \{files}");
      return ExitCode.ERROR;
    }

    for (int i = 0; i < fileNames.length; i++) {
      final File fileName = fileNames[i];

      if ((!fileName.exists()) || (!fileName.canRead())) {
        MZmineCore.getDesktop().displayErrorMessage(
            STR."Cannot read file \{fileName}. The file/path might not exist.");
        logger.warning(STR."Cannot read file \{fileName}. The file/path might not exist.");
        return ExitCode.ERROR;
      }

      final RawDataFileType fileType = fileTypes.get(i);
      logger.finest(STR."File \{fileName} type detected as \{fileType}");

      try {
        RawDataFile newMZmineFile = createDataFile(fileType, fileName.getAbsolutePath(),
            fileName.getName(), storage);

        final AbstractTask newTask;//
        if (advancedParam != null) {
          newTask = createAdvancedTask(fileType, project, fileName, newMZmineFile,
              scanProcessorConfig, AllSpectralDataImportModule.class, parameters, moduleCallDate,
              storage);
        } else {
          newTask = createTask(fileType, project, fileName, newMZmineFile, scanProcessorConfig,
              AllSpectralDataImportModule.class, parameters, moduleCallDate, storage);
        }

        // add task to list
        if (newTask != null) {
          tasks.add(newTask);
          dataImportTasks.add(newTask);
        }

      } catch (IOException e) {
        MZmineCore.getDesktop().displayErrorMessage(
            STR."Could not create a new temporary file \{e}. Does the dirve of the temporary storage have enough space?");
        logger.log(Level.SEVERE, "Could not create a new temporary file ", e);
        return ExitCode.ERROR;
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

  /**
   * @param newMZmineFile       null for mzml files, can be ims or non ims. must be determined in
   *                            import task.
   * @param scanProcessorConfig
   */
  private AbstractTask createTask(RawDataFileType fileType, MZmineProject project, File file,
      @Nullable RawDataFile newMZmineFile,
      @NotNull final ScanImportProcessorConfig scanProcessorConfig,
      Class<? extends MZmineModule> module, ParameterSet parameters,
      @NotNull Instant moduleCallDate, @Nullable final MemoryMapStorage storage) {
    return switch (fileType) {
      // imaging
      case IMZML -> new ImzMLImportTask(project, file, scanProcessorConfig,
          (ImagingRawDataFile) newMZmineFile, module, parameters, moduleCallDate);
      // imaging, maldi, or LC-MS
      case BRUKER_TSF ->
          new TSFImportTask(project, file, storage, module, parameters, moduleCallDate,
              scanProcessorConfig);
      // IMS
      case BRUKER_TDF -> // ims files are big, use own storage
          new TDFImportTask(project, file, MemoryMapStorage.forRawDataFile(), module, parameters,
              moduleCallDate);
      // MS
      case MZML, MZML_IMS ->
          new MSDKmzMLImportTask(project, file, scanProcessorConfig, module, parameters,
              moduleCallDate, storage);
      case MZXML ->
          new MzXMLImportTask(project, file, newMZmineFile, scanProcessorConfig, module, parameters,
              moduleCallDate);
      case MZDATA ->
          new MzDataImportTask(project, file, newMZmineFile, module, parameters, moduleCallDate);
      case NETCDF ->
          new NetCDFImportTask(project, file, newMZmineFile, module, parameters, moduleCallDate);
      case THERMO_RAW ->
          new ThermoImportTaskDelegator(storage, moduleCallDate, file, scanProcessorConfig, project,
              parameters, module);
      case ICPMSMS_CSV ->
          new IcpMsCVSImportTask(project, file, newMZmineFile, module, parameters, moduleCallDate);
      case MZML_GZIP, MZML_ZIP ->
          new ZipImportTask(project, file, scanProcessorConfig, module, parameters, moduleCallDate,
              storage);
      case BRUKER_BAF ->
          new BafImportTask(storage, moduleCallDate, file, module, parameters, project,
              scanProcessorConfig);
//      case AIRD -> throw new IllegalStateException("Unexpected value: " + fileType);
      case WATERS_RAW, SCIEX_WIFF, SCIEX_WIFF2, AGILENT_D ->
          new MSConvertImportTask(moduleCallDate, file, scanProcessorConfig, project, module,
              parameters);
    };
  }

  /**
   * Create a task that imports an directly applies mass detection. Not supported by all imports
   * yet
   *
   * @param scanProcessorConfig the advanced parameters
   * @return the task or null if the data format is not supported for direct mass detection
   */
  private AbstractTask createAdvancedTask(RawDataFileType fileType, MZmineProject project,
      File file, @Nullable RawDataFile newMZmineFile,
      @NotNull ScanImportProcessorConfig scanProcessorConfig, Class<? extends MZmineModule> module,
      ParameterSet parameters, @NotNull Instant moduleCallDate,
      @Nullable final MemoryMapStorage storage) {
    return switch (fileType) {
      // imaging
      case IMZML -> new ImzMLImportTask(project, file, scanProcessorConfig,
          (ImagingRawDataFile) newMZmineFile, module, parameters, moduleCallDate);
      // MS
      case MZML, MZML_IMS ->
          new MSDKmzMLImportTask(project, file, null, scanProcessorConfig, module, parameters,
              moduleCallDate, storage);
      case MZXML ->
          new MzXMLImportTask(project, file, newMZmineFile, scanProcessorConfig, module, parameters,
              moduleCallDate);
      case BRUKER_TDF ->
          new TDFImportTask(project, file, storage, scanProcessorConfig, module, parameters,
              moduleCallDate);
      case THERMO_RAW ->
          new ThermoImportTaskDelegator(storage, moduleCallDate, file, scanProcessorConfig, project,
              parameters, module);
//          new ThermoRawImportTask(project, file, module, parameters, moduleCallDate,
//              scanProcessorConfig);
      case BRUKER_TSF ->
          new TSFImportTask(project, file, storage, module, parameters, moduleCallDate,
              scanProcessorConfig);
      case BRUKER_BAF ->
          new BafImportTask(storage, moduleCallDate, file, module, parameters, project,
              scanProcessorConfig);
      case AGILENT_D, SCIEX_WIFF, SCIEX_WIFF2, WATERS_RAW ->
          new MSConvertImportTask(moduleCallDate, file, scanProcessorConfig, project, module,
              parameters);
      // all unsupported tasks are wrapped to apply import and mass detection separately
      case MZDATA, NETCDF, MZML_ZIP, MZML_GZIP, ICPMSMS_CSV ->
          createWrappedAdvancedTask(fileType, project, file, newMZmineFile, scanProcessorConfig,
              module, parameters, moduleCallDate, storage);
    };
  }

  private AbstractTask createWrappedAdvancedTask(RawDataFileType fileType, MZmineProject project,
      File file, RawDataFile newMZmineFile, @NotNull ScanImportProcessorConfig scanProcessorConfig,
      Class<? extends MZmineModule> module, ParameterSet parameters,
      @NotNull Instant moduleCallDate, @Nullable final MemoryMapStorage storage) {
    // log
    logger.warning("Advanced processing is not available for MS data type: " + fileType.toString()
        + " and file " + file.getAbsolutePath());
    // create wrapped task to apply import and mass detection
    return new MsDataImportAndMassDetectWrapperTask(getMassListStorage(), newMZmineFile,
        createTask(fileType, project, file, newMZmineFile, scanProcessorConfig, module, parameters,
            moduleCallDate, storage), scanProcessorConfig, moduleCallDate);
  }

  @Nullable
  private RawDataFile createDataFile(RawDataFileType fileType, String absPath, String newName,
      MemoryMapStorage storage) throws IOException {
    return switch (fileType) {
      case MZXML, MZDATA, /*WATERS_RAW,*/ NETCDF, ICPMSMS_CSV ->
          MZmineCore.createNewFile(newName, absPath, storage);
      case MZML, MZML_IMS, MZML_ZIP, MZML_GZIP -> null; // created in Mzml import task
      case IMZML -> MZmineCore.createNewImagingFile(newName, absPath, storage);
      case BRUKER_TSF, BRUKER_BAF, BRUKER_TDF ->
          null; // TSF can be anything: Single shot maldi, imaging, or LC-MS (non ims)
      case WATERS_RAW, SCIEX_WIFF, SCIEX_WIFF2, AGILENT_D, THERMO_RAW -> null;
    };
  }

  public MemoryMapStorage getMassListStorage() {
    if (storageMassLists == null) {
      this.storageMassLists = MemoryMapStorage.forMassList();
    }
    return storageMassLists;
  }

}
