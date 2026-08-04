/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.CheckComboParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.ImportType;
import io.github.mzmine.parameters.parametertypes.ImportTypeParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameWithExampleExportParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityToleranceParameter;
import io.github.mzmine.util.files.ExtensionFilters;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.WriterOptions;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class StandardCompoundNormalizationTypeParameters extends SimpleParameterSet {

  public static final CheckComboParameter<SampleType> sampleTypes = new CheckComboParameter<>(
      "Reference samples", """
      Select all sample types that shall be used to calculate the recalibration from.
      The recalibration of all other samples will be based on the acquisition order, which is
      determined by the acquisition type column in the metadata (CTRL/CMD + M).
      """, SampleType.values(), List.of(SampleType.values()));

  public static final ComboParameter<StandardUsageType> standardUsageType = new ComboParameter<>(
      "Normalization type", "Normalize intensities using", StandardUsageType.values());

  public static final DoubleParameter mzVsRtBalance = new DoubleParameter("m/z vs RT balance",
      "Used in distance measuring as multiplier of m/z difference");

  public static final FileNameWithExampleExportParameter standardCompoundsFile = new FileNameWithExampleExportParameter(
      "Standard compounds file",
      "CSV or TSV file containing the internal standard compounds to match in the feature list.",
      ExtensionFilters.CSV_TSV_IMPORT,
      StandardCompoundNormalizationTypeParameters::exportExampleFile);

  private static void exportExampleFile(File file) {
    FileAndPathUtil.createDirectory(file.getParentFile());
    try (var w = Files.newBufferedWriter(file.toPath(), WriterOptions.REPLACE.toOpenOption())) {
      String example = """
          mz,rt,mobility,name
          200.1234,6.5,1.75,optional name""";

      w.write(example);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static final StringParameter fieldSeparator = new StringParameter("Field separator",
      "Character(s) used to separate fields in the standard compounds file. Use '\\t' for tab separated files.",
      ",");

  private static final List<ImportType<?>> importTypes = List.of(
      new ImportType<>(true, "mz", new PrecursorMZType()), //
      new ImportType<>(true, "rt", new RTType()), //
      new ImportType<>(false, "mobility", new MobilityType()), //
      new ImportType<>(true, "name", new CompoundNameType()));

  public static final ImportTypeParameter standardCompounds = new ImportTypeParameter(
      "Standard compounds",
      "Select the columns that contain the internal standard compound properties.", importTypes);

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter("m/z tolerance",
      "Maximum allowed m/z difference when matching imported standards to feature list rows.",
      0.005, 5);

  public static final RTToleranceParameter rtTolerance = new RTToleranceParameter("RT tolerance",
      "Maximum allowed retention time difference when matching imported standards to feature list rows.",
      new RTTolerance(0.03f, Unit.MINUTES));

  public static final MobilityToleranceParameter mobilityTolerance = new MobilityToleranceParameter(
      "Mobility tolerance",
      "Maximum allowed mobility difference when matching imported standards to feature list rows.",
      new MobilityTolerance(0.01f));

  public static final ComboParameter<StandardCompoundNormalizationMode> mode = new ComboParameter<>(
      "Mode", """
      Defines how samples without all standards are handled.
      %s: all selected standards must be detected in each raw file, otherwise normalization fails.
      %s: raw files need at least one detected standard, missing standards are skipped.
      %s: raw files without any detected standard are skipped and normalized by interpolation \
      between the neighboring reference samples.""".formatted(
      StandardCompoundNormalizationMode.REQUIRE_ALL_IN_ALL_SAMPLES,
      StandardCompoundNormalizationMode.REQUIRE_ONE_PER_SAMPLE,
      StandardCompoundNormalizationMode.SKIP_FILES_WITHOUT_STANDARD),
      StandardCompoundNormalizationMode.values(), StandardCompoundNormalizationMode.getDefault());

  /**
   * Only used to read the value of the legacy boolean parameter that {@link #mode} replaced. Never
   * added to {@link #getParameters()}, only registered in {@link #getNameParameterMap()} so that
   * old batch files still load. Instance field on purpose: the static parameter instances are
   * shared between all parameter sets.
   */
  private static final BooleanParameter legacyRequireAllStandards = new BooleanParameter(
      "Require all standards",
      "If enabled, all selected standards must be present in each raw file for normalization",
      true);

  public StandardCompoundNormalizationTypeParameters() {
    super(sampleTypes, standardUsageType, mzVsRtBalance, standardCompoundsFile, fieldSeparator,
        standardCompounds, mzTolerance, rtTolerance, mobilityTolerance, mode);
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    final Map<String, Parameter<?>> map = super.getNameParameterMap();
    // the boolean "Require all standards" parameter was replaced by the mode combo parameter.
    // types differ, so load the old value into the legacy parameter and map it in
    // handleLoadedParameters
    map.put(legacyRequireAllStandards.getName(), legacyRequireAllStandards.cloneParameter());
    return map;
  }

  @Override
  public void handleLoadedParameters(final Map<String, Parameter<?>> loadedParams,
      final int loadedVersion) {
    super.handleLoadedParameters(loadedParams, loadedVersion);

    final Parameter<?> oldRequireAllParam = loadedParams.get(legacyRequireAllStandards.getName());
    if (oldRequireAllParam != null) {
      // old parameter set: true was "all standards in all samples", false only required one standard
      setParameter(mode, oldRequireAllParam.getValue() == Boolean.FALSE
          ? StandardCompoundNormalizationMode.REQUIRE_ONE_PER_SAMPLE
          : StandardCompoundNormalizationMode.REQUIRE_ALL_IN_ALL_SAMPLES);
    }
  }

  public static @NotNull StandardCompoundNormalizationTypeParameters create(
      final @NotNull List<SampleType> selectedSampleTypes,
      final @NotNull StandardUsageType selectedStandardUsageType,
      final double selectedMzVsRtBalance, final @NotNull File selectedStandardCompoundsFile,
      final @NotNull String selectedFieldSeparator, final @NotNull MZTolerance selectedMzTolerance,
      final @NotNull RTTolerance selectedRtTolerance,
      final @NotNull MobilityTolerance selectedMobilityTolerance,
      final @NotNull StandardCompoundNormalizationMode selectedMode) {
    return create(selectedSampleTypes, selectedStandardUsageType, selectedMzVsRtBalance,
        selectedStandardCompoundsFile, selectedFieldSeparator, copyImportTypes(importTypes),
        selectedMzTolerance, selectedRtTolerance, selectedMobilityTolerance, selectedMode);
  }

  public static @NotNull StandardCompoundNormalizationTypeParameters create(
      final @NotNull List<SampleType> selectedSampleTypes,
      final @NotNull StandardUsageType selectedStandardUsageType,
      final double selectedMzVsRtBalance, final @NotNull File selectedStandardCompoundsFile,
      final @NotNull String selectedFieldSeparator,
      final @NotNull List<ImportType<?>> selectedStandardCompounds,
      final @NotNull MZTolerance selectedMzTolerance,
      final @NotNull RTTolerance selectedRtTolerance,
      final @NotNull MobilityTolerance selectedMobilityTolerance,
      final @NotNull StandardCompoundNormalizationMode selectedMode) {
    final StandardCompoundNormalizationTypeParameters parameters = (StandardCompoundNormalizationTypeParameters) new StandardCompoundNormalizationTypeParameters().cloneParameterSet();
    parameters.setParameter(StandardCompoundNormalizationTypeParameters.sampleTypes,
        selectedSampleTypes);
    parameters.setParameter(StandardCompoundNormalizationTypeParameters.standardUsageType,
        selectedStandardUsageType);
    parameters.setParameter(StandardCompoundNormalizationTypeParameters.mzVsRtBalance,
        selectedMzVsRtBalance);
    parameters.setParameter(StandardCompoundNormalizationTypeParameters.standardCompoundsFile,
        selectedStandardCompoundsFile);
    parameters.setParameter(StandardCompoundNormalizationTypeParameters.fieldSeparator,
        selectedFieldSeparator);
    parameters.setParameter(StandardCompoundNormalizationTypeParameters.standardCompounds,
        copyImportTypes(selectedStandardCompounds));
    parameters.setParameter(StandardCompoundNormalizationTypeParameters.mzTolerance,
        selectedMzTolerance);
    parameters.setParameter(StandardCompoundNormalizationTypeParameters.rtTolerance,
        selectedRtTolerance);
    parameters.setParameter(StandardCompoundNormalizationTypeParameters.mobilityTolerance,
        selectedMobilityTolerance);
    parameters.setParameter(StandardCompoundNormalizationTypeParameters.mode, selectedMode);
    return parameters;
  }

  @Override
  public boolean checkParameterValues(final Collection<String> errorMessages,
      final boolean skipRawDataAndFeatureListParameters) {
    final boolean superCheck = super.checkParameterValues(errorMessages,
        skipRawDataAndFeatureListParameters);

    final List<ImportType<?>> selectedTypes = getValue(standardCompounds).stream()
        .filter(ImportType::isSelected).toList();
    final boolean mzSelected = importTypeListContainsType(selectedTypes, PrecursorMZType.class);
    final boolean rtSelected = importTypeListContainsType(selectedTypes, RTType.class);

    if (!mzSelected) {
      errorMessages.add("Standard compounds must import the \"%s\" column.".formatted(
          new PrecursorMZType().getHeaderString()));
    }
    if (!rtSelected) {
      errorMessages.add("Standard compounds must import the \"%s\" column.".formatted(
          new RTType().getHeaderString()));
    }

    return superCheck && mzSelected && rtSelected;
  }

  private boolean importTypeListContainsType(final @NotNull List<ImportType<?>> importTypes,
      final @NotNull Class<? extends DataType<?>> typeClass) {
    return importTypes.stream()
        .anyMatch(importType -> typeClass.isInstance(importType.getDataType()));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static @NotNull List<ImportType<?>> copyImportTypes(
      final @NotNull List<ImportType<?>> source) {
    return source.stream().<ImportType<?>>map(
        importType -> new ImportType(importType.isSelected(), importType.getCsvColumnName(),
            importType.getDataType(), importType.getMapper())).toList();
  }
}

