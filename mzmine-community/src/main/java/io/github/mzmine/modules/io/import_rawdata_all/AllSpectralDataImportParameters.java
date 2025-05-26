/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.io.import_rawdata_msconvert.MSConvertImportTask;
import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportParameters;
import io.github.mzmine.modules.visualization.projectmetadata.color.ColorByMetadataModule;
import io.github.mzmine.modules.visualization.projectmetadata.io.ProjectMetadataImportParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComponentWrapperParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.util.RawDataFileTypeDetector;
import io.github.mzmine.util.files.ExtensionFilters;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AllSpectralDataImportParameters extends SimpleParameterSet {

  private static final Logger logger = Logger.getLogger(
      AllSpectralDataImportParameters.class.getName());

  public static final ComponentWrapperParameter<Boolean, BooleanParameter> applyVendorCentroiding = new ComponentWrapperParameter<>(
      new BooleanParameter("Try vendor centroiding",
          "Vendor centroiding will be applied to the imported raw data if this option is selected and cetroiding is supported.",
          true), () -> FxButtons.createButton(null, FxIcons.GEAR_PREFERENCES,
      "Open the preference dialog, which controls this parameter for the drag & drop import and the mzwizard.",
      () -> ConfigService.getPreferences()
          .showSetupDialog(true, MZminePreferences.applyVendorCentroiding.getName())));

  /**
   * This parameter adds a different validation step to files that may map files selected in the All
   * .d or similar buttons to a validated list of distinct files
   */
  public static final FileNamesParameter fileNames = new FileNamesParameter("File names", "",
      ExtensionFilters.MS_RAW_DATA, "Drag & drop your MS data files here.",
      AllSpectralDataImportParameters::validateDistinctPaths);

  public static final OptionalModuleParameter<AdvancedSpectraImportParameters> advancedImport = new OptionalModuleParameter<>(
      "Advanced import",
      "Caution: Advanced option that applies mass detection (centroiding+thresholding) directly to imported scans (see help).\nAdvantage: Lower memory consumption\nCaution: All processing steps will directly change the underlying data, with no way of retrieving raw data or initial results.",
      new AdvancedSpectraImportParameters(), false);

  public static final OptionalParameter<FileNameParameter> metadataFile = new OptionalParameter<>(
      ProjectMetadataImportParameters.fileName);

  public static final BooleanParameter sortAndRecolor = new BooleanParameter("Sort and color", """
      Apply default sorting and coloring by sample type.
      To color by metadata, apply the "%s" module in batch, quick access, or via right click in the MS data files list.""".formatted(
      ColorByMetadataModule.MODULE_NAME), true);

  public AllSpectralDataImportParameters() {
    super(new Parameter[]{fileNames, //
            applyVendorCentroiding, //
            advancedImport, // directly process masslists
            metadataFile, // metadata import
            sortAndRecolor, // sort and recolor
            // allow import of spectral libraries
            SpectralLibraryImportParameters.dataBaseFiles},
        "https://mzmine.github.io/mzmine_documentation/module_docs/io/data-import.html");
  }


  public static ParameterSet create(final boolean applyVendorCentroiding,
      @NotNull final File[] allDataFiles, @Nullable final File metadata,
      @Nullable final File[] allLibraryFiles) {
    return create(applyVendorCentroiding, allDataFiles, metadata, allLibraryFiles, null);
  }

  public static ParameterSet create(final boolean applyVendorCentroiding,
      @NotNull final File[] allDataFiles, @Nullable final File metadata,
      @Nullable final File[] allLibraryFiles,
      @Nullable final AdvancedSpectraImportParameters advanced) {
    var params = new AllSpectralDataImportParameters().cloneParameterSet();
    params.setParameter(AllSpectralDataImportParameters.applyVendorCentroiding,
        applyVendorCentroiding);
    params.setParameter(fileNames, allDataFiles);
    params.setParameter(metadataFile, metadata != null, metadata);
    params.setParameter(SpectralLibraryImportParameters.dataBaseFiles, allLibraryFiles);
    params.setParameter(advancedImport, advanced != null);
    if (advanced != null) {
      params.getParameter(advancedImport).setEmbeddedParameters(advanced);
    }
    return params;
  }

  /**
   * @return true if parameterset is of this class or contains at least the parameters
   */
  public static boolean isParameterSetClass(final ParameterSet parameters) {
    return parameters != null && (
        parameters.getClass().equals(AllSpectralDataImportParameters.class) || (
            parameters.hasParameter(advancedImport) && parameters.hasParameter(fileNames)));
  }

  /**
   * Get all files in the project that match the file path
   *
   * @return a list of already loaded raw data files
   */
  public static List<RawDataFile> getLoadedRawDataFiles(MZmineProject project,
      final ParameterSet parameters) {
    // all files that should be loaded
    // need to validate bruker paths and use absolute file paths as they are used in RawDataFile
    Set<File> loadFileSet = streamValidatedFiles(parameters).map(ImportFile::importedFile)
        .collect(Collectors.toSet());

    // the actual files in the list
    return project.getCurrentRawDataFiles().stream()
        .filter(raw -> loadFileSet.contains(raw.getAbsoluteFilePath())).toList();
  }

  /**
   * Removes already loaded files from the import list
   *
   * @return array of files - files that are already loaded
   */
  public static ImportFile[] skipAlreadyLoadedFiles(MZmineProject project,
      final ParameterSet parameters) {

    Set<File> currentlyLoadedFiles = project.getCurrentRawDataFiles().stream()
        .map(RawDataFile::getAbsoluteFilePath).collect(Collectors.toSet());

    // compare based on absolute files
    // skip all files in import that directly match the abs path of another file.
    // need to validate bruker paths and use absolute file paths as they are used in RawDataFile
    return streamValidatedFiles(parameters).filter(
            file -> !currentlyLoadedFiles.contains(file.importedFile().getAbsoluteFile()))
        .toArray(ImportFile[]::new);
  }

  /**
   * Applies {@link AllSpectralDataImportModule#validateBrukerPath(File)} to get the actual file
   * paths and applies name changes due to the MSconvert import. This is done always before import.
   *
   * @return stream of {@link ImportFile}s
   */
  @NotNull
  public static Stream<ImportFile> streamValidatedFiles(final ParameterSet parameters) {
    final boolean keepConverted = ConfigService.getPreferences()
        .getValue(MZminePreferences.keepConvertedFile);
    return Arrays.stream(parameters.getValue(fileNames))
        .map(AllSpectralDataImportModule::validateBrukerPath).map(
            file -> new ImportFile(file, RawDataFileTypeDetector.detectDataFileType(file),
                MSConvertImportTask.applyMsConvertImportNameChanges(file, keepConverted)));
  }


  /**
   * Validating paths may jump from nested .d folders or similar to the main file this creates
   * duplicates which are filtered
   *
   * @return distinct files after validating paths
   */
  public static @NotNull File[] validateDistinctPaths(@NotNull File[] files) {
    return Arrays.stream(files).map(AllSpectralDataImportModule::validateBrukerPath)
        .filter(Objects::nonNull)
        // needs distinct as bruker files may be duplicated with .d files in multiple layers
        .distinct().toArray(File[]::new);
  }

}
