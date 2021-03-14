/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
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
import io.github.mzmine.modules.dataprocessing.featdet_gridmass.GridMassModule;
import io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.ImageBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.IonMobilityTraceBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.MassCalibrationModule;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionModule;
import io.github.mzmine.modules.dataprocessing.featdet_mobilityscanmerger.MobilityScanMergerModule;
import io.github.mzmine.modules.dataprocessing.featdet_msn.MsnFeatureDetectionModule;
import io.github.mzmine.modules.dataprocessing.featdet_shoulderpeaksfilter.ShoulderPeaksFilterModule;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingModule;
import io.github.mzmine.modules.dataprocessing.featdet_targeted.TargetedFeatureDetectionModule;
import io.github.mzmine.modules.dataprocessing.filter_alignscans.AlignScansModule;
import io.github.mzmine.modules.dataprocessing.filter_baselinecorrection.BaselineCorrectionModule;
import io.github.mzmine.modules.dataprocessing.filter_blanksubtraction.PeakListBlankSubtractionModule;
import io.github.mzmine.modules.dataprocessing.filter_clearannotations.FeatureListClearAnnotationsModule;
import io.github.mzmine.modules.dataprocessing.filter_cropfilter.CropFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_duplicatefilter.DuplicateFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_extractscans.ExtractScansModule;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.FeatureFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_groupms2.GroupMS2Module;
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
import io.github.mzmine.modules.dataprocessing.id_adductsearch.AdductSearchModule;
import io.github.mzmine.modules.dataprocessing.id_camera.CameraSearchModule;
import io.github.mzmine.modules.dataprocessing.id_ccscalc.CCSCalcModule;
import io.github.mzmine.modules.dataprocessing.id_cliquems.CliqueMSModule;
import io.github.mzmine.modules.dataprocessing.id_complexsearch.ComplexSearchModule;
import io.github.mzmine.modules.dataprocessing.id_formulapredictionfeaturelist.FormulaPredictionFeatureListModule;
import io.github.mzmine.modules.dataprocessing.id_fragmentsearch.FragmentSearchModule;
import io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport.GNPSResultsImportModule;
import io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner.IsotopePeakScannerModule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.LipidSearchModule;
import io.github.mzmine.modules.dataprocessing.id_localcsvsearch.LocalCSVDatabaseSearchModule;
import io.github.mzmine.modules.dataprocessing.id_ms2search.Ms2SearchModule;
import io.github.mzmine.modules.dataprocessing.id_nist.NistMsSearchModule;
import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.OnlineDBSearchModule;
import io.github.mzmine.modules.dataprocessing.id_precursordbsearch.PrecursorDBSearchModule;
import io.github.mzmine.modules.dataprocessing.id_sirius.SiriusIdentificationModule;
import io.github.mzmine.modules.dataprocessing.id_spectraldbsearch.LocalSpectralDBSearchModule;
import io.github.mzmine.modules.dataprocessing.norm_linear.LinearNormalizerModule;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration.RTCalibrationModule;
import io.github.mzmine.modules.dataprocessing.norm_standardcompound.StandardCompoundNormalizerModule;
import io.github.mzmine.modules.io.export_features_csv.CSVExportModularModule;
import io.github.mzmine.modules.io.export_features_csv_legacy.LegacyCSVExportModule;
import io.github.mzmine.modules.io.export_gnps.fbmn.GnpsFbmnExportAndSubmitModule;
import io.github.mzmine.modules.io.export_gnps.gc.GnpsGcExportAndSubmitModule;
import io.github.mzmine.modules.io.export_metaboanalyst.MetaboAnalystExportModule;
import io.github.mzmine.modules.io.export_mgf.AdapMgfExportModule;
import io.github.mzmine.modules.io.export_msp.AdapMspExportModule;
import io.github.mzmine.modules.io.export_mzml.MzMLExportModule;
import io.github.mzmine.modules.io.export_mztab.MzTabExportModule;
import io.github.mzmine.modules.io.export_mztabm.MZTabmExportModule;
import io.github.mzmine.modules.io.export_netcdf.NetCDFExportModule;
import io.github.mzmine.modules.io.export_scans.ExportScansFromRawFilesModule;
import io.github.mzmine.modules.io.export_sirius.SiriusExportModule;
import io.github.mzmine.modules.io.import_all_data_files.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_bruker_tdf.TDFImportModule;
import io.github.mzmine.modules.io.import_imzml.ImzMLImportModule;
import io.github.mzmine.modules.io.import_mzdata.MzDataImportModule;
import io.github.mzmine.modules.io.import_mzml_jmzml.MzMLImportModule;
import io.github.mzmine.modules.io.import_mzml_msdk.MSDKmzMLImportModule;
import io.github.mzmine.modules.io.import_mztab.MzTabImportModule;
import io.github.mzmine.modules.io.import_mztabm.MZTabmImportModule;
import io.github.mzmine.modules.io.import_mzxml.MzXMLImportModule;
import io.github.mzmine.modules.io.import_netcdf.NetCDFImportModule;
import io.github.mzmine.modules.io.import_thermo_raw.ThermoRawImportModule;
import io.github.mzmine.modules.io.import_waters_raw.WatersRawImportModule;
import io.github.mzmine.modules.io.import_zip.ZipImportModule;
import io.github.mzmine.modules.io.projectload.ProjectLoadModule;
import io.github.mzmine.modules.io.projectsave.ProjectSaveAsModule;
import io.github.mzmine.modules.io.projectsave.ProjectSaveModule;
import java.util.Collections;
import java.util.List;

public class BatchModeModulesList {

  public static final List<Class<? extends MZmineProcessingModule>> MODULES = Collections
      .unmodifiableList(List.of(

          /**
           * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#PROJECT}
           */
          ProjectLoadModule.class, //
          ProjectSaveModule.class, //
          ProjectSaveAsModule.class, //

          /**
           * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#SPECTRAL_DATA}
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

          MassDetectionModule.class, //
          MassCalibrationModule.class, //
          MobilityScanMergerModule.class, //
          ShoulderPeaksFilterModule.class, //
          AlignScansModule.class, //
          BaselineCorrectionModule.class, //
          CropFilterModule.class, //
          ExtractScansModule.class, //
          RawFileMergeModule.class, //
          ScanFiltersModule.class, //
          ScanSmoothingModule.class, //

          /**
           * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#FEATURE_DETECTION}
           */
          ModularADAPChromatogramBuilderModule.class, //
          IonMobilityTraceBuilderModule.class, //
          GridMassModule.class, //
          ImageBuilderModule.class, //
          MsnFeatureDetectionModule.class, //
          TargetedFeatureDetectionModule.class, //
          SmoothingModule.class, //
          MinimumSearchFeatureResolverModule.class, //
          AdapResolverModule.class, //
          ADAPHierarchicalClusteringModule.class, //
          ADAPMultivariateCurveResolutionModule.class, //
          ADAP3DModule.class, //
          BaselineFeatureResolverModule.class, //
          CentWaveResolverModule.class, //
          NoiseAmplitudeResolverModule.class, //
          SavitzkyGolayResolverModule.class, //
          PeakFinderModule.class, //
          MultiThreadPeakFinderModule.class, //
          SameRangeGapFillerModule.class, //

          /**
           * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#FEATURE_FILTERING}
           */
          FeatureFilterModule.class, //
          RowsFilterModule.class, //
          IsotopeGrouperModule.class, //
          JoinAlignerModule.class, //
          ADAP3AlignerModule.class, //
          HierarAlignerGcModule.class, //
          PathAlignerModule.class, //
          RansacAlignerModule.class, //
          PeakListBlankSubtractionModule.class, //
          DuplicateFilterModule.class, //
          MobilityMzRegionExtractionModule.class, //
          NeutralLossFilterModule.class, //
          PeakComparisonRowFilterModule.class, //

          /**
           * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#FEATURE_PROCESSING}
           */
          FeatureListClearAnnotationsModule.class, //
          LinearNormalizerModule.class, //
          RTCalibrationModule.class, //
          StandardCompoundNormalizerModule.class, //

          /**
           * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#FEATURE_ANNOTATION}
           */
          AdductSearchModule.class, //
          CameraSearchModule.class, //
          CCSCalcModule.class, //
          CliqueMSModule.class, //
          GroupMS2Module.class, //
          ComplexSearchModule.class, //
          FormulaPredictionFeatureListModule.class, //
          FragmentSearchModule.class, //
          IsotopePeakScannerModule.class, //
          LipidSearchModule.class, //
          LocalCSVDatabaseSearchModule.class, //
          Ms2SearchModule.class, //
          NistMsSearchModule.class, //
          OnlineDBSearchModule.class, //
          PrecursorDBSearchModule.class, //
          SiriusIdentificationModule.class, //
          LocalSpectralDBSearchModule.class, //

          /**
           * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#FEATURE_IO}
           */
          GnpsFbmnExportAndSubmitModule.class, //
          GnpsGcExportAndSubmitModule.class, //
          MetaboAnalystExportModule.class, //
          AdapMgfExportModule.class, //
          GNPSResultsImportModule.class, //
          AdapMspExportModule.class, //
          MzMLExportModule.class, //
          MzTabExportModule.class, //
          MZTabmExportModule.class, //
          NetCDFExportModule.class, //
          ExportScansFromRawFilesModule.class, //
          SiriusExportModule.class, //
          MZTabmImportModule.class, //
          MzTabImportModule.class, //
          CSVExportModularModule.class, //
          LegacyCSVExportModule.class //

          /**
           * needed in batch mode?
           * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#VISUALIZATION}
           */

          /**
           * needed in batch mode?
           * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#OTHER}
           */

      ));

  private BatchModeModulesList() {
  }
}
