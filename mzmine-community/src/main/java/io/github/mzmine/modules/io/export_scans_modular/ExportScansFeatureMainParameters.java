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

package io.github.mzmine.modules.io.export_scans_modular;

import io.github.mzmine.modules.dataanalysis.spec_chimeric_precursor.HandleChimericMsMsParameters;
import io.github.mzmine.modules.dataanalysis.spec_chimeric_precursor.HandleChimericMsMsParameters.ChimericMsOption;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.PresetSimpleSpectraMergeSelectParameters;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.SpectraMergeSelectModuleOptions;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.SpectraMergeSelectPresets;
import io.github.mzmine.modules.io.export_features_sirius.SiriusExportTask;
import io.github.mzmine.modules.io.spectraldbsubmit.batch.LibraryBatchGenerationModule;
import io.github.mzmine.modules.io.spectraldbsubmit.batch.LibraryBatchMetadataParameters;
import io.github.mzmine.modules.io.spectraldbsubmit.batch.SpectralLibraryExportFormats;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.AdvancedParametersParameter;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntensityNormalizer;
import io.github.mzmine.parameters.parametertypes.IntensityNormalizerComboParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameSuffixExportParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ParameterSetParameter;
import io.github.mzmine.parameters.parametertypes.submodules.SubModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import org.jetbrains.annotations.NotNull;

public class ExportScansFeatureMainParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final FileNameSuffixExportParameter file = new FileNameSuffixExportParameter(
      "Export file", """
      Local scans file. Use pattern "%s" in the file name to substitute with feature list name.
      (i.e. "prefix_%s_suffix.mgf" would become "prefix_SourceFeatureListName_suffix.mgf").
      If the file already exists, it will be overwritten.
      Filename without this pattern means all selected feature lists are exported to the same file.""".formatted(
      SiriusExportTask.MULTI_NAME_PATTERN, SiriusExportTask.MULTI_NAME_PATTERN), "scans");

  public static final ComboParameter<SpectralLibraryExportFormats> exportFormat = new ComboParameter<>(
      "Export format", "format to export", SpectralLibraryExportFormats.values(),
      SpectralLibraryExportFormats.json_mzmine);

  public static final ParameterSetParameter<LibraryBatchMetadataParameters> metadata = new ParameterSetParameter<>(
      "Metadata", "Metadata for all entries", new LibraryBatchMetadataParameters());

  public static final OptionalModuleParameter<ProjectMetadataToLibraryMapperParameters> projectMetadataMapper = new OptionalModuleParameter<>(
      "Sample wide metadata", """
      Transfer sample wide metadata from the project metadata table columns to all entries of those samples.
      Allows to define 'compound names' and 'descriptions' via project metadata.
      This allows to measure blank samples and describe their specific properties to create libraries of spectra from blanks
      , without knowing the compound identity.""", new ProjectMetadataToLibraryMapperParameters());

  public static final BooleanParameter skipAnnotatedFeatures = new BooleanParameter(
      "Skip annotated features", """
      Skip annotated features. This is useful if they are already exported with the
      %s module that takes annotated features and creates spectral libraries.""".formatted(
      LibraryBatchGenerationModule.MODULE_NAME), false);

  public static final AdvancedParametersParameter<AdvancedExportScansFeatureParameters> advanced = new AdvancedParametersParameter<>(
      new AdvancedExportScansFeatureParameters(), true);

  public static final OptionalModuleParameter<ExportMs1ScansFeatureParameters> exportMs1 = new OptionalModuleParameter<>(
      "Export MS1", "Option to also export MS1 scans.", new ExportMs1ScansFeatureParameters());

  public static final SubModuleParameter<ExportFragmentScansFeatureParameters> exportFragmentScans = new SubModuleParameter<>(
      "Export fragment scans", "Control the selection, merging, and export of fragment scans.",
      new ExportFragmentScansFeatureParameters());

  public static final IntensityNormalizerComboParameter normalizer = IntensityNormalizerComboParameter.createDefaultScientific();

  /*
   * additional requirements:
   * Export MS1
   */
  public ExportScansFeatureMainParameters() {
    super(flists, file, exportFormat, metadata, projectMetadataMapper, skipAnnotatedFeatures,
        exportMs1, exportFragmentScans, normalizer, advanced);
  }

  /**
   * Adds some default settings apart from the arguments. USI are always set to compact.
   *
   * @param param                           the parameter set to be changed in place
   * @param exportPath                      base file path and name
   * @param fileSuffix                      file suffix to define what kind of scans are exported
   *                                        like unknown_scans
   * @param libGenMetadata                  metadata for instrument etc
   * @param mzTolScans                      for merging and for handling of chimerics
   * @param isolationToleranceForInstrument wider isolation window
   * @param skipAnnotatedFeatures           useful if annotated features were already exported by
   *                                        batch library generation module
   * @param exportMs1                       option to export MS1 as well
   */
  public static void setAll(final ParameterSet param, final File exportPath,
      final String fileSuffix, final LibraryBatchMetadataParameters libGenMetadata,
      final MZTolerance mzTolScans, final MZTolerance isolationToleranceForInstrument,
      final boolean skipAnnotatedFeatures, final boolean exportMs1) {

    var exportFormat = SpectralLibraryExportFormats.json_mzmine;

    File fileName = FileAndPathUtil.getRealFilePathWithSuffix(exportPath, fileSuffix,
        exportFormat.getExtension());

    param.setParameter(ExportScansFeatureMainParameters.file, fileName);

    param.setParameter(ExportScansFeatureMainParameters.flists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(ExportScansFeatureMainParameters.exportFormat, exportFormat);
    param.setParameter(ExportScansFeatureMainParameters.skipAnnotatedFeatures,
        skipAnnotatedFeatures);
    param.setParameter(ExportScansFeatureMainParameters.normalizer,
        IntensityNormalizer.createScientific());

    // metadata is user defined
    param.getParameter(ExportScansFeatureMainParameters.metadata)
        .setEmbeddedParameters((LibraryBatchMetadataParameters) libGenMetadata.cloneParameterSet());

    // MS1
    {
      param.setParameter(ExportScansFeatureMainParameters.exportMs1, exportMs1);

      var ms1Params = param.getParameter(ExportScansFeatureMainParameters.exportMs1)
          .getEmbeddedParameters();
      ms1Params.setParameter(ExportMs1ScansFeatureParameters.separateMs1File, false);
      ms1Params.setParameter(ExportMs1ScansFeatureParameters.ms1RequiresFragmentScan, true);
      ms1Params.setParameter(ExportMs1ScansFeatureParameters.ms1Selection,
          Ms1ScanSelection.MS1_AND_CORRELATED);
    }

    // MS2 MSn
    {
      var ms2Params = param.getParameter(ExportScansFeatureMainParameters.exportFragmentScans)
          .getEmbeddedParameters();
      ms2Params.setParameter(ExportFragmentScansFeatureParameters.minSignals, 1);

      // set merging
      var mergingParameters = PresetSimpleSpectraMergeSelectParameters.create(
          SpectraMergeSelectPresets.REPRESENTATIVE_MSn_TREE, mzTolScans);
      ms2Params.getParameter(ExportFragmentScansFeatureParameters.merging)
          .setValue(SpectraMergeSelectModuleOptions.SIMPLE_MERGED, mergingParameters);

      // chimerics
      ms2Params.setParameter(ExportFragmentScansFeatureParameters.handleChimerics, true);
      var chimerics = ms2Params.getParameter(ExportFragmentScansFeatureParameters.handleChimerics)
          .getEmbeddedParameters();
      chimerics.setParameter(HandleChimericMsMsParameters.option, ChimericMsOption.FLAG);
      chimerics.setParameter(HandleChimericMsMsParameters.mainMassWindow, mzTolScans);

      chimerics.setParameter(HandleChimericMsMsParameters.isolationWindow,
          isolationToleranceForInstrument);
      chimerics.setParameter(HandleChimericMsMsParameters.minimumPrecursorPurity, 0.75);
    }

    // project sample wide metadata
    {
      // active if skipping annotated features this means that we are exporting blanks or similar
      param.setParameter(ExportScansFeatureMainParameters.projectMetadataMapper,
          skipAnnotatedFeatures);
      var projectMetaParams = param.getParameter(
          ExportScansFeatureMainParameters.projectMetadataMapper).getEmbeddedParameters();
      projectMetaParams.setParameter(
          ProjectMetadataToLibraryMapperParameters.compoundNameFromSampleMetadata, true,
          MetadataColumn.FILENAME_HEADER); // use file name as compound name for now
      projectMetaParams.setParameter(
          ProjectMetadataToLibraryMapperParameters.descriptionFromSampleMetadata, false, "");
    }

    // advanced
    {
      param.setParameter(ExportScansFeatureMainParameters.advanced, true);
      var advanced = param.getParameter(ExportScansFeatureMainParameters.advanced)
          .getEmbeddedParameters();
      advanced.setParameter(AdvancedExportScansFeatureParameters.compactUSI, true);
    }
  }


  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
