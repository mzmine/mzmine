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

package io.github.mzmine.modules.batchmode;

import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.dataprocessing.adap_hierarchicalclustering.ADAPHierarchicalClusteringModule;
import io.github.mzmine.modules.dataprocessing.adap_mcr.ADAPMultivariateCurveResolutionModule;
import io.github.mzmine.modules.dataprocessing.align_adap3.ADAP3AlignerModule;
import io.github.mzmine.modules.dataprocessing.align_hierarchical.HierarAlignerGcModule;
import io.github.mzmine.modules.dataprocessing.align_join.JoinAlignerModule;
import io.github.mzmine.modules.dataprocessing.align_path.PathAlignerModule;
import io.github.mzmine.modules.dataprocessing.align_ransac.RansacAlignerModule;
import io.github.mzmine.modules.dataprocessing.featdet_adap3d.ADAP3DModule;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ADAPpeakpicking.AdapResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.baseline.BaselineFeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.centwave.CentWaveResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.noiseamplitude.NoiseAmplitudeResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.savitzkygolay.SavitzkyGolayResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_denormalize_by_inject_time.DenormalizeScansMultiplyByInjectTimeModule;
import io.github.mzmine.modules.dataprocessing.featdet_gridmass.GridMassModule;
import io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.ImageBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_imsexpander.ImsExpanderModule;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.IonMobilityTraceBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.MassCalibrationModule;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionModule;
import io.github.mzmine.modules.dataprocessing.featdet_mobilityscanmerger.MobilityScanMergerModule;
import io.github.mzmine.modules.dataprocessing.featdet_msn.MsnFeatureDetectionModule;
import io.github.mzmine.modules.dataprocessing.featdet_msn_tree.MsnTreeFeatureDetectionModule;
import io.github.mzmine.modules.dataprocessing.featdet_recursiveimsbuilder.RecursiveIMSBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_shoulderpeaksfilter.ShoulderPeaksFilterModule;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingModule;
import io.github.mzmine.modules.dataprocessing.featdet_targeted.TargetedFeatureDetectionModule;
import io.github.mzmine.modules.dataprocessing.filter_alignscans.AlignScansModule;
import io.github.mzmine.modules.dataprocessing.filter_baselinecorrection.BaselineCorrectionModule;
import io.github.mzmine.modules.dataprocessing.filter_blanksubtraction.FeatureListBlankSubtractionModule;
import io.github.mzmine.modules.dataprocessing.filter_clearannotations.ClearFeatureAnnotationsModule;
import io.github.mzmine.modules.dataprocessing.filter_cropfilter.CropFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_diams2.DiaMs2CorrModule;
import io.github.mzmine.modules.dataprocessing.filter_duplicatefilter.DuplicateFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_extractscans.ExtractScansModule;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.FeatureFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_groupms2.GroupMS2Module;
import io.github.mzmine.modules.dataprocessing.filter_interestingfeaturefinder.AnnotateIsomersModule;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.IsotopeFinderModule;
import io.github.mzmine.modules.dataprocessing.filter_isotopegrouper.IsotopeGrouperModule;
import io.github.mzmine.modules.dataprocessing.filter_merge.RawFileMergeModule;
import io.github.mzmine.modules.dataprocessing.filter_mobilitymzregionextraction.MobilityMzRegionExtractionModule;
import io.github.mzmine.modules.dataprocessing.filter_neutralloss.NeutralLossFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_peakcomparisonrowfilter.PeakComparisonRowFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_scanfilters.ScanFiltersModule;
import io.github.mzmine.modules.dataprocessing.filter_scansmoothing.ScanSmoothingModule;
import io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.PeakFinderModule;
import io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.multithreaded.MultiThreadPeakFinderModule;
import io.github.mzmine.modules.dataprocessing.gapfill_samerange.SameRangeGapFillerModule;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.corrgrouping.CorrelateGroupingModule;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.export.ExportCorrAnnotationModule;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.msms.similarity.MS2SimilarityModule;
import io.github.mzmine.modules.dataprocessing.id_camera.CameraSearchModule;
import io.github.mzmine.modules.dataprocessing.id_ccscalc.CCSCalcModule;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.external.ExternalCCSCalibrationModule;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.reference.ReferenceCCSCalibrationModule;
import io.github.mzmine.modules.dataprocessing.id_cliquems.CliqueMSModule;
import io.github.mzmine.modules.dataprocessing.id_formulapredictionfeaturelist.FormulaPredictionFeatureListModule;
import io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport.GNPSResultsImportModule;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.addionannotations.AddIonNetworkingModule;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.checkmsms.IonNetworkMSMSCheckModule;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.clearionids.ClearIonIdentitiesModule;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.formula.createavgformulas.CreateAvgNetworkFormulasModule;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.formula.prediction.FormulaPredictionIonNetworkModule;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkingModule;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.refinement.IonNetworkRefinementModule;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.relations.IonNetRelationsModule;
import io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner.IsotopePeakScannerModule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.LipidSearchModule;
import io.github.mzmine.modules.dataprocessing.id_localcsvsearch.LocalCSVDatabaseSearchModule;
import io.github.mzmine.modules.dataprocessing.id_ms2search.Ms2SearchModule;
import io.github.mzmine.modules.dataprocessing.id_nist.NistMsSearchModule;
import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.OnlineDBSearchModule;
import io.github.mzmine.modules.dataprocessing.id_precursordbsearch.PrecursorDBSearchModule;
import io.github.mzmine.modules.dataprocessing.id_spectral_library_match.SpectralLibrarySearchModule;
import io.github.mzmine.modules.dataprocessing.norm_linear.LinearNormalizerModule;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration.RTCalibrationModule;
import io.github.mzmine.modules.dataprocessing.norm_standardcompound.StandardCompoundNormalizerModule;
import io.github.mzmine.modules.io.deprecated_jmzml.MzMLImportModule;
import io.github.mzmine.modules.io.export_features_csv.CSVExportModularModule;
import io.github.mzmine.modules.io.export_features_csv_legacy.LegacyCSVExportModule;
import io.github.mzmine.modules.io.export_features_gnps.fbmn.GnpsFbmnExportAndSubmitModule;
import io.github.mzmine.modules.io.export_features_gnps.gc.GnpsGcExportAndSubmitModule;
import io.github.mzmine.modules.io.export_features_metaboanalyst.MetaboAnalystExportModule;
import io.github.mzmine.modules.io.export_features_mgf.AdapMgfExportModule;
import io.github.mzmine.modules.io.export_features_msp.AdapMspExportModule;
import io.github.mzmine.modules.io.export_features_mztab.MzTabExportModule;
import io.github.mzmine.modules.io.export_features_mztabm.MZTabmExportModule;
import io.github.mzmine.modules.io.export_features_sirius.SiriusExportModule;
import io.github.mzmine.modules.io.export_library_analysis_csv.LibraryAnalysisCSVExportModule;
import io.github.mzmine.modules.io.export_msn_tree.MSnTreeExportModule;
import io.github.mzmine.modules.io.export_rawdata_mzml.MzMLExportModule;
import io.github.mzmine.modules.io.export_rawdata_netcdf.NetCDFExportModule;
import io.github.mzmine.modules.io.export_scans.ExportScansFromRawFilesModule;
import io.github.mzmine.modules.io.import_features_mztab.MzTabImportModule;
import io.github.mzmine.modules.io.import_features_mztabm.MZTabmImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.TDFImportModule;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImzMLImportModule;
import io.github.mzmine.modules.io.import_rawdata_mzdata.MzDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_mzml.MSDKmzMLImportModule;
import io.github.mzmine.modules.io.import_rawdata_mzxml.MzXMLImportModule;
import io.github.mzmine.modules.io.import_rawdata_netcdf.NetCDFImportModule;
import io.github.mzmine.modules.io.import_rawdata_thermo_raw.ThermoRawImportModule;
import io.github.mzmine.modules.io.import_rawdata_waters_raw.WatersRawImportModule;
import io.github.mzmine.modules.io.import_rawdata_zip.ZipImportModule;
import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportModule;
import io.github.mzmine.modules.io.projectload.ProjectLoadModule;
import io.github.mzmine.modules.io.projectsave.ProjectSaveAsModule;
import io.github.mzmine.modules.io.projectsave.ProjectSaveModule;
import io.github.mzmine.modules.io.spectraldbsubmit.batch.LibraryBatchGenerationModule;
import io.github.mzmine.modules.tools.clear_project.ClearProjectModule;
import io.github.mzmine.modules.visualization.projectmetadata.ProjectMetadataImportModule;
import java.util.List;

public class BatchModeModulesList {

  public static final List<Class<? extends MZmineProcessingModule>> MODULES = List.of(
      /*
       * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#PROJECT}
       */
      ProjectLoadModule.class, //
      ProjectSaveModule.class, //
      ProjectSaveAsModule.class, //
      ClearProjectModule.class, //

      /*
       * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#SPECTRAL_DATA}
       * {@link io.github.mzmine.modules.MZmineModuleCategory#RAWDATAIMPORT}
       */
      AllSpectralDataImportModule.class, //
      TDFImportModule.class, //
      MzMLImportModule.class, //
      ImzMLImportModule.class, //
      MzDataImportModule.class, //
      MSDKmzMLImportModule.class, //
      MzXMLImportModule.class, //
      NetCDFImportModule.class, //
      ThermoRawImportModule.class, //
      WatersRawImportModule.class, //
      ZipImportModule.class, //
      SpectralLibraryImportModule.class, //

      /*
       * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#PROJECT}
       * {@link io.github.mzmine.modules.MZmineModuleCategory#PROJECTMETADATA}
       */
      ProjectMetadataImportModule.class, //

      /*
       * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#SPECTRAL_DATA}
       * {@link io.github.mzmine.modules.MZmineModuleCategory#RAWDATA}
       */
      MassDetectionModule.class, //
      MassCalibrationModule.class, //
      MobilityScanMergerModule.class, //
      RawFileMergeModule.class, //

      /*
       * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#SPECTRAL_DATA}
       * {@link io.github.mzmine.modules.MZmineModuleCategory#RAWDATAFILTERING}
       */
      AlignScansModule.class, //
      BaselineCorrectionModule.class, //
      CropFilterModule.class, //
      ShoulderPeaksFilterModule.class, //
      ScanFiltersModule.class, //
      ScanSmoothingModule.class, //
      DenormalizeScansMultiplyByInjectTimeModule.class, //

      /*
       * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#SPECTRAL_DATA}
       * {@link io.github.mzmine.modules.MZmineModuleCategory#RAWDATAEXPORT}
       */
      ExtractScansModule.class, //
      ExportScansFromRawFilesModule.class, //
      MzMLExportModule.class, //
      MSnTreeExportModule.class, //

      /*
       * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#FEATURE_DETECTION}
       * {@link io.github.mzmine.modules.MZmineModuleCategory#EIC_BUILDING}
       */
      ModularADAPChromatogramBuilderModule.class, //
      MsnTreeFeatureDetectionModule.class, //
      GridMassModule.class, //
      IonMobilityTraceBuilderModule.class, //
      RecursiveIMSBuilderModule.class, //
      ImageBuilderModule.class, //
      MsnFeatureDetectionModule.class, //
      TargetedFeatureDetectionModule.class, //
      ADAPHierarchicalClusteringModule.class, //
      ADAPMultivariateCurveResolutionModule.class, //
      ADAP3DModule.class, //
      ImsExpanderModule.class, //

      /*
       * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#FEATURE_DETECTION}
       * {@link io.github.mzmine.modules.MZmineModuleCategory#FEATURE_RESOLVING}
       */
      SmoothingModule.class, //
      AdapResolverModule.class, //
      BaselineFeatureResolverModule.class, //
      CentWaveResolverModule.class, //
      MinimumSearchFeatureResolverModule.class, //
      NoiseAmplitudeResolverModule.class, //
      SavitzkyGolayResolverModule.class, //

      /*
       * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#FEATURE_DETECTION}
       * {@link io.github.mzmine.modules.MZmineModuleCategory#ALIGNMENT}
       */
      JoinAlignerModule.class, //
      ADAP3AlignerModule.class, //
      HierarAlignerGcModule.class, //
      PathAlignerModule.class, //
      RansacAlignerModule.class, //

      /*
       * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#FEATURE_DETECTION}
       * {@link io.github.mzmine.modules.MZmineModuleCategory#GAPFILLING}
       */
      PeakFinderModule.class, //
      MultiThreadPeakFinderModule.class, //
      SameRangeGapFillerModule.class, //

      /*
       * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#FEATURE_FILTERING}
       */
      FeatureFilterModule.class, //
      RowsFilterModule.class, //
      IsotopeGrouperModule.class, //
      IsotopeFinderModule.class, //
      FeatureListBlankSubtractionModule.class, //
      DuplicateFilterModule.class, //
      MobilityMzRegionExtractionModule.class, //
      NeutralLossFilterModule.class, //
      PeakComparisonRowFilterModule.class, //

      /*
       * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#FEATURE_PROCESSING}
       */
      ClearFeatureAnnotationsModule.class, //
      LinearNormalizerModule.class, //
      RTCalibrationModule.class, //
      StandardCompoundNormalizerModule.class, //

      /*
       * {@link io.github.mzmine.modules.MZmineModuleCategory#FEATURE_GROUPING}
       */
      CorrelateGroupingModule.class, //
      MS2SimilarityModule.class, //
      AnnotateIsomersModule.class, //

      /*
       * {@link io.github.mzmine.modules.MZmineModuleCategory#ION_IDENTITY_NETWORKS}
       */
      IonNetworkingModule.class, //
      AddIonNetworkingModule.class, //
      IonNetworkRefinementModule.class, //
      IonNetworkMSMSCheckModule.class, //
      FormulaPredictionIonNetworkModule.class, //
      CreateAvgNetworkFormulasModule.class, //
      IonNetRelationsModule.class, //
      ClearIonIdentitiesModule.class, //

      /*
        {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#FEATURE_ANNOTATION}
       */
      CameraSearchModule.class, //
      CCSCalcModule.class, //
      ExternalCCSCalibrationModule.class, //
      ReferenceCCSCalibrationModule.class, //
      CliqueMSModule.class, //
      GroupMS2Module.class, //
      DiaMs2CorrModule.class, //
      FormulaPredictionFeatureListModule.class, //
      IsotopePeakScannerModule.class, //
      LipidSearchModule.class, //
      LocalCSVDatabaseSearchModule.class, //
      Ms2SearchModule.class, //
      NistMsSearchModule.class, //
      OnlineDBSearchModule.class, //
      PrecursorDBSearchModule.class, //
      SpectralLibrarySearchModule.class, //

      /*
       * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#FEATURE_IO}
       */
      GnpsFbmnExportAndSubmitModule.class, //
      GnpsGcExportAndSubmitModule.class, //
      ExportCorrAnnotationModule.class, //
      MetaboAnalystExportModule.class, //
      AdapMgfExportModule.class, //
      GNPSResultsImportModule.class, //
      AdapMspExportModule.class, //
      MzTabExportModule.class, //
      MZTabmExportModule.class, //
      NetCDFExportModule.class, //
      SiriusExportModule.class, //
      MZTabmImportModule.class, //
      MzTabImportModule.class, //
      CSVExportModularModule.class, //
      LegacyCSVExportModule.class, //
      LibraryAnalysisCSVExportModule.class, //
      LibraryBatchGenerationModule.class //

      /*
       * needed in batch mode?
       * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#VISUALIZATION}
       */

      /*
       * needed in batch mode?
       * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#OTHER}
       */

  );

  private BatchModeModulesList() {
  }
}
