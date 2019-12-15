/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.main;

import io.github.mzmine.modules.batchmode.BatchModeModule;
import io.github.mzmine.modules.dataanalysis.clustering.ClusteringModule;
import io.github.mzmine.modules.dataanalysis.heatmaps.HeatMapModule;
import io.github.mzmine.modules.dataanalysis.projectionplots.CDAPlotModule;
import io.github.mzmine.modules.dataanalysis.projectionplots.PCAPlotModule;
import io.github.mzmine.modules.dataanalysis.projectionplots.SammonsPlotModule;
import io.github.mzmine.modules.dataanalysis.rtmzplots.cvplot.CVPlotModule;
import io.github.mzmine.modules.dataanalysis.rtmzplots.logratioplot.LogratioPlotModule;
import io.github.mzmine.modules.dataanalysis.significance.SignificanceModule;
import io.github.mzmine.modules.dataprocessing.align_adap3.ADAP3AlignerModule;
import io.github.mzmine.modules.dataprocessing.align_hierarchical.HierarAlignerGcModule;
import io.github.mzmine.modules.dataprocessing.align_join.JoinAlignerModule;
import io.github.mzmine.modules.dataprocessing.align_ransac.RansacAlignerModule;
import io.github.mzmine.modules.dataprocessing.featdet_ADAPchromatogrambuilder.ADAPChromatogramBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_adap3decompositionV1_5.ADAP3DecompositionV1_5Module;
import io.github.mzmine.modules.dataprocessing.featdet_adap3decompositionV2.ADAP3DecompositionV2Module;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogrambuilder.ChromatogramBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.DeconvolutionModule;
import io.github.mzmine.modules.dataprocessing.featdet_gridmass.GridMassModule;
import io.github.mzmine.modules.dataprocessing.featdet_manual.ManualPeakPickerModule;
import io.github.mzmine.modules.dataprocessing.featdet_manual.XICManualPickerModule;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionModule;
import io.github.mzmine.modules.dataprocessing.featdet_msms.MsMsPeakPickerModule;
import io.github.mzmine.modules.dataprocessing.featdet_peakextender.PeakExtenderModule;
import io.github.mzmine.modules.dataprocessing.featdet_shapemodeler.ShapeModelerModule;
import io.github.mzmine.modules.dataprocessing.featdet_shoulderpeaksfilter.ShoulderPeaksFilterModule;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingModule;
import io.github.mzmine.modules.dataprocessing.featdet_targeted.TargetedPeakDetectionModule;
import io.github.mzmine.modules.dataprocessing.filter_alignscans.AlignScansModule;
import io.github.mzmine.modules.dataprocessing.filter_baselinecorrection.BaselineCorrectionModule;
import io.github.mzmine.modules.dataprocessing.filter_clearannotations.PeaklistClearAnnotationsModule;
import io.github.mzmine.modules.dataprocessing.filter_cropfilter.CropFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_deisotoper.IsotopeGrouperModule;
import io.github.mzmine.modules.dataprocessing.filter_duplicatefilter.DuplicateFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_extractscans.ExtractScansModule;
import io.github.mzmine.modules.dataprocessing.filter_groupms2.GroupMS2Module;
import io.github.mzmine.modules.dataprocessing.filter_merge.RawFileMergeModule;
import io.github.mzmine.modules.dataprocessing.filter_neutralloss.NeutralLossFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_peakcomparisonrowfilter.PeakComparisonRowFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_peakfilter.PeakFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_scanfilters.ScanFiltersModule;
import io.github.mzmine.modules.dataprocessing.filter_scansmoothing.ScanSmoothingModule;
import io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.PeakFinderModule;
import io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.multithreaded.MultiThreadPeakFinderModule;
import io.github.mzmine.modules.dataprocessing.gapfill_samerange.SameRangeGapFillerModule;
import io.github.mzmine.modules.dataprocessing.id_adductsearch.AdductSearchModule;
import io.github.mzmine.modules.dataprocessing.id_camera.CameraSearchModule;
import io.github.mzmine.modules.dataprocessing.id_complexsearch.ComplexSearchModule;
import io.github.mzmine.modules.dataprocessing.id_customdbsearch.CustomDBSearchModule;
import io.github.mzmine.modules.dataprocessing.id_formula_sort.FormulaSortModule;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.FormulaPredictionModule;
import io.github.mzmine.modules.dataprocessing.id_formulapredictionpeaklist.FormulaPredictionPeakListModule;
import io.github.mzmine.modules.dataprocessing.id_fragmentsearch.FragmentSearchModule;
import io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport.GNPSResultsImportModule;
import io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner.IsotopePeakScannerModule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.LipidSearchModule;
import io.github.mzmine.modules.dataprocessing.id_ms2search.Ms2SearchModule;
import io.github.mzmine.modules.dataprocessing.id_nist.NistMsSearchModule;
import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.OnlineDBSearchModule;
import io.github.mzmine.modules.dataprocessing.id_precursordbsearch.PrecursorDBSearchModule;
import io.github.mzmine.modules.dataprocessing.id_sirius.SiriusProcessingModule;
import io.github.mzmine.modules.dataprocessing.id_spectraldbsearch.LocalSpectralDBSearchModule;
import io.github.mzmine.modules.dataprocessing.id_spectraldbsearch.sort.SortSpectralDBIdentitiesModule;
import io.github.mzmine.modules.dataprocessing.norm_linear.LinearNormalizerModule;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration.RTCalibrationModule;
import io.github.mzmine.modules.dataprocessing.norm_standardcompound.StandardCompoundNormalizerModule;
import io.github.mzmine.modules.io.adapmgfexport.AdapMgfExportModule;
import io.github.mzmine.modules.io.adapmspexport.AdapMspExportModule;
import io.github.mzmine.modules.io.csvexport.CSVExportModule;
import io.github.mzmine.modules.io.exportscans.ExportScansFromRawFilesModule;
import io.github.mzmine.modules.io.exportscans.ExportScansModule;
import io.github.mzmine.modules.io.gnpsexport.fbmn.GnpsFbmnExportAndSubmitModule;
import io.github.mzmine.modules.io.gnpsexport.gc.GnpsGcExportAndSubmitModule;
import io.github.mzmine.modules.io.metaboanalystexport.MetaboAnalystExportModule;
import io.github.mzmine.modules.io.mztabexport.MzTabExportModule;
import io.github.mzmine.modules.io.mztabimport.MzTabImportModule;
import io.github.mzmine.modules.io.projectclose.ProjectCloseModule;
import io.github.mzmine.modules.io.projectload.ProjectLoadModule;
import io.github.mzmine.modules.io.projectsave.ProjectSaveAsModule;
import io.github.mzmine.modules.io.projectsave.ProjectSaveModule;
import io.github.mzmine.modules.io.rawdataexport.RawDataExportModule;
import io.github.mzmine.modules.io.rawdataimport.RawDataImportModule;
import io.github.mzmine.modules.io.siriusexport.SiriusExportModule;
import io.github.mzmine.modules.io.spectraldbsubmit.LibrarySubmitModule;
import io.github.mzmine.modules.io.sqlexport.SQLExportModule;
import io.github.mzmine.modules.io.xmlexport.XMLExportModule;
import io.github.mzmine.modules.io.xmlimport.XMLImportModule;
import io.github.mzmine.modules.tools.isotopepatternpreview.IsotopePatternPreviewModule;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.modules.tools.kovats.KovatsIndexExtractionModule;
import io.github.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeModule;
import io.github.mzmine.modules.tools.mzrangecalculator.MzRangeFormulaCalculatorModule;
import io.github.mzmine.modules.tools.mzrangecalculator.MzRangeMassCalculatorModule;
import io.github.mzmine.modules.tools.sortdatafiles.SortDataFilesModule;
import io.github.mzmine.modules.tools.sortpeaklists.SortPeakListsModule;
import io.github.mzmine.modules.visualization.featurelisttable.PeakListTableModule;
import io.github.mzmine.modules.visualization.featurelisttable.export.IsotopePatternExportModule;
import io.github.mzmine.modules.visualization.featurelisttable.export.MSMSExportModule;
import io.github.mzmine.modules.visualization.fx3d.Fx3DVisualizerModule;
import io.github.mzmine.modules.visualization.histogram.HistogramVisualizerModule;
import io.github.mzmine.modules.visualization.infovisualizer.InfoVisualizerModule;
import io.github.mzmine.modules.visualization.intensityplot.IntensityPlotModule;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotModule;
import io.github.mzmine.modules.visualization.mzhistogram.MZDistributionHistoModule;
import io.github.mzmine.modules.visualization.neutralloss.NeutralLossVisualizerModule;
import io.github.mzmine.modules.visualization.productionfilter.ProductIonFilterVisualizerModule;
import io.github.mzmine.modules.visualization.scatterplot.ScatterPlotVisualizerModule;
import io.github.mzmine.modules.visualization.spectra.msms.MsMsVisualizerModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingManager;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.identification.sumformulaprediction.DPPSumFormulaPredictionModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.deisotoper.DPPIsotopeGrouperModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.massdetection.DPPMassDetectionModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.customdatabase.CustomDBSpectraSearchModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.lipidsearch.LipidSpectraSearchModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.onlinedatabase.OnlineDBSpectraSearchModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.spectraldatabase.SpectraIdentificationSpectralDatabaseModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.sumformula.SumFormulaSpectraSearchModule;
import io.github.mzmine.modules.visualization.spectra.spectralmatchresults.SpectraIdentificationResultsModule;
import io.github.mzmine.modules.visualization.tic.TICVisualizerModule;
import io.github.mzmine.modules.visualization.twod.TwoDVisualizerModule;
import io.github.mzmine.modules.visualization.vankrevelendiagram.VanKrevelenDiagramModule;

/**
 * List of modules included in MZmine 2
 */
public class MZmineModulesList {

  public static final Class<?> MODULES[] = new Class<?>[] {

      // Project methods
      ProjectLoadModule.class, //
      ProjectSaveModule.class, //
      ProjectSaveAsModule.class, //
      ProjectCloseModule.class, //

      // Batch mode
      BatchModeModule.class, //

      // Raw data methods
      RawDataImportModule.class, //
      RawDataExportModule.class, //
      ExportScansFromRawFilesModule.class, //
      RawFileMergeModule.class, //
      ExtractScansModule.class, //
      MassDetectionModule.class, //
      ShoulderPeaksFilterModule.class, //
      ChromatogramBuilderModule.class, //
      ADAPChromatogramBuilderModule.class, //
      // Not ready for prime time: ADAP3DModule.class,
      GridMassModule.class, //
      ManualPeakPickerModule.class, //
      MsMsPeakPickerModule.class, //
      ScanFiltersModule.class, //
      CropFilterModule.class, //
      BaselineCorrectionModule.class, //
      AlignScansModule.class, //
      ScanSmoothingModule.class, //
      SortDataFilesModule.class, //
      XICManualPickerModule.class, //

      // Alignment
      SortPeakListsModule.class, //
      JoinAlignerModule.class, //
      HierarAlignerGcModule.class, //

      RansacAlignerModule.class, //
      ADAP3AlignerModule.class, //
      // PathAlignerModule.class, //

      // I/O
      CSVExportModule.class, //
      MetaboAnalystExportModule.class, //
      MzTabExportModule.class, //
      SQLExportModule.class, //
      XMLExportModule.class, //
      MzTabImportModule.class, //
      XMLImportModule.class, //
      AdapMspExportModule.class, //
      AdapMgfExportModule.class, //
      GnpsFbmnExportAndSubmitModule.class, //
      GnpsGcExportAndSubmitModule.class, //
      SiriusExportModule.class, //

      // Gap filling
      PeakFinderModule.class, //
      MultiThreadPeakFinderModule.class, //
      SameRangeGapFillerModule.class, //

      // Isotopes
      IsotopeGrouperModule.class, //
      IsotopePatternCalculator.class, //
      IsotopePeakScannerModule.class, //

      // Feature detection
      SmoothingModule.class, //
      DeconvolutionModule.class, //
      ShapeModelerModule.class, //
      PeakExtenderModule.class, //
      TargetedPeakDetectionModule.class, //
      ADAP3DecompositionV1_5Module.class, //
      ADAP3DecompositionV2Module.class, //

      // Feature list filtering
      GroupMS2Module.class, //
      DuplicateFilterModule.class, //
      RowsFilterModule.class, //
      PeakComparisonRowFilterModule.class, //
      PeakFilterModule.class, //
      PeaklistClearAnnotationsModule.class, //
      NeutralLossFilterModule.class, //

      // Normalization
      RTCalibrationModule.class, //
      LinearNormalizerModule.class, //
      StandardCompoundNormalizerModule.class, //

      // Data analysis
      CVPlotModule.class, //
      LogratioPlotModule.class, //
      PCAPlotModule.class, //
      CDAPlotModule.class, //
      SammonsPlotModule.class, //
      ClusteringModule.class, //
      HeatMapModule.class, //
      SignificanceModule.class, //

      // Identification
      LocalSpectralDBSearchModule.class, //
      PrecursorDBSearchModule.class, //
      SortSpectralDBIdentitiesModule.class, //
      CustomDBSearchModule.class, //
      FormulaPredictionModule.class, //
      FragmentSearchModule.class, //
      AdductSearchModule.class, //
      ComplexSearchModule.class, //
      OnlineDBSearchModule.class, //
      LipidSearchModule.class, //
      CameraSearchModule.class, //
      NistMsSearchModule.class, //
      FormulaPredictionPeakListModule.class, //
      FormulaSortModule.class, // sort formulas
      Ms2SearchModule.class, //
      SiriusProcessingModule.class, //
      GNPSResultsImportModule.class, //

      // Visualizers
      TICVisualizerModule.class, //
      SpectraVisualizerModule.class, //
      TwoDVisualizerModule.class, //
      Fx3DVisualizerModule.class, //
      MsMsVisualizerModule.class, //
      NeutralLossVisualizerModule.class, //
      MZDistributionHistoModule.class, //
      PeakListTableModule.class, //
      IsotopePatternExportModule.class, //
      MSMSExportModule.class, //
      ScatterPlotVisualizerModule.class, //
      HistogramVisualizerModule.class, //
      InfoVisualizerModule.class, //
      IntensityPlotModule.class, //
      KendrickMassPlotModule.class, //
      VanKrevelenDiagramModule.class, //
      ProductIonFilterVisualizerModule.class, //

      // Tools
      MzRangeMassCalculatorModule.class, //
      MzRangeFormulaCalculatorModule.class, //
      IsotopePatternPreviewModule.class, //
      KovatsIndexExtractionModule.class, //

      // all other regular MZmineModule (not MZmineRunnableModule) NOT
      // LISTED IN MENU
      SpectraIdentificationSpectralDatabaseModule.class, //
      LibrarySubmitModule.class, //
      CustomDBSpectraSearchModule.class, //
      LipidSpectraSearchModule.class, //
      OnlineDBSpectraSearchModule.class, //
      SumFormulaSpectraSearchModule.class, //
      ExportScansModule.class, //
      SpectraIdentificationResultsModule.class, //
      MsMsSpectraMergeModule.class, //

      // Data point processing, implement DataPointProcessingModule
      DataPointProcessingManager.class, //
      DPPMassDetectionModule.class, //
      DPPSumFormulaPredictionModule.class, //
      DPPIsotopeGrouperModule.class,//

      // not ready for prime time:
      // DPPAnyElementIsotopeGrouperModule.class basically working, but
      // only for specific elements
      // at the moment
      // PeakListBlankSubtractionModule.class
  };
}
