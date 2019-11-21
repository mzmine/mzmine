/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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
import io.github.mzmine.modules.masslistmethods.ADAPchromatogrambuilder.ADAPChromatogramBuilderModule;
import io.github.mzmine.modules.masslistmethods.chromatogrambuilder.ChromatogramBuilderModule;
import io.github.mzmine.modules.masslistmethods.shoulderpeaksfilter.ShoulderPeaksFilterModule;
import io.github.mzmine.modules.peaklistmethods.alignment.adap3.ADAP3AlignerModule;
import io.github.mzmine.modules.peaklistmethods.alignment.hierarchical.HierarAlignerGcModule;
import io.github.mzmine.modules.peaklistmethods.alignment.join.JoinAlignerModule;
import io.github.mzmine.modules.peaklistmethods.alignment.ransac.RansacAlignerModule;
import io.github.mzmine.modules.peaklistmethods.dataanalysis.clustering.ClusteringModule;
import io.github.mzmine.modules.peaklistmethods.dataanalysis.heatmaps.HeatMapModule;
import io.github.mzmine.modules.peaklistmethods.dataanalysis.projectionplots.CDAPlotModule;
import io.github.mzmine.modules.peaklistmethods.dataanalysis.projectionplots.PCAPlotModule;
import io.github.mzmine.modules.peaklistmethods.dataanalysis.projectionplots.SammonsPlotModule;
import io.github.mzmine.modules.peaklistmethods.dataanalysis.rtmzplots.cvplot.CVPlotModule;
import io.github.mzmine.modules.peaklistmethods.dataanalysis.rtmzplots.logratioplot.LogratioPlotModule;
import io.github.mzmine.modules.peaklistmethods.dataanalysis.significance.SignificanceModule;
import io.github.mzmine.modules.peaklistmethods.filtering.clearannotations.PeaklistClearAnnotationsModule;
import io.github.mzmine.modules.peaklistmethods.filtering.duplicatefilter.DuplicateFilterModule;
import io.github.mzmine.modules.peaklistmethods.filtering.groupms2.GroupMS2Module;
import io.github.mzmine.modules.peaklistmethods.filtering.neutralloss.NeutralLossFilterModule;
import io.github.mzmine.modules.peaklistmethods.filtering.peakcomparisonrowfilter.PeakComparisonRowFilterModule;
import io.github.mzmine.modules.peaklistmethods.filtering.peakfilter.PeakFilterModule;
import io.github.mzmine.modules.peaklistmethods.filtering.rowsfilter.RowsFilterModule;
import io.github.mzmine.modules.peaklistmethods.gapfilling.peakfinder.PeakFinderModule;
import io.github.mzmine.modules.peaklistmethods.gapfilling.peakfinder.multithreaded.MultiThreadPeakFinderModule;
import io.github.mzmine.modules.peaklistmethods.gapfilling.samerange.SameRangeGapFillerModule;
import io.github.mzmine.modules.peaklistmethods.identification.adductsearch.AdductSearchModule;
import io.github.mzmine.modules.peaklistmethods.identification.camera.CameraSearchModule;
import io.github.mzmine.modules.peaklistmethods.identification.complexsearch.ComplexSearchModule;
import io.github.mzmine.modules.peaklistmethods.identification.customdbsearch.CustomDBSearchModule;
import io.github.mzmine.modules.peaklistmethods.identification.formulaprediction.FormulaPredictionModule;
import io.github.mzmine.modules.peaklistmethods.identification.formulapredictionpeaklist.FormulaPredictionPeakListModule;
import io.github.mzmine.modules.peaklistmethods.identification.fragmentsearch.FragmentSearchModule;
import io.github.mzmine.modules.peaklistmethods.identification.gnpsresultsimport.GNPSResultsImportModule;
import io.github.mzmine.modules.peaklistmethods.identification.lipididentification.LipidSearchModule;
import io.github.mzmine.modules.peaklistmethods.identification.ms2search.Ms2SearchModule;
import io.github.mzmine.modules.peaklistmethods.identification.nist.NistMsSearchModule;
import io.github.mzmine.modules.peaklistmethods.identification.onlinedbsearch.OnlineDBSearchModule;
import io.github.mzmine.modules.peaklistmethods.identification.precursordbsearch.PrecursorDBSearchModule;
import io.github.mzmine.modules.peaklistmethods.identification.sirius.SiriusProcessingModule;
import io.github.mzmine.modules.peaklistmethods.identification.spectraldbsearch.LocalSpectralDBSearchModule;
import io.github.mzmine.modules.peaklistmethods.identification.spectraldbsearch.sort.SortSpectralDBIdentitiesModule;
import io.github.mzmine.modules.peaklistmethods.io.adap.mgfexport.AdapMgfExportModule;
import io.github.mzmine.modules.peaklistmethods.io.adap.mspexport.AdapMspExportModule;
import io.github.mzmine.modules.peaklistmethods.io.csvexport.CSVExportModule;
import io.github.mzmine.modules.peaklistmethods.io.gnpsexport.fbmn.GnpsFbmnExportAndSubmitModule;
import io.github.mzmine.modules.peaklistmethods.io.gnpsexport.gc.GnpsGcExportAndSubmitModule;
import io.github.mzmine.modules.peaklistmethods.io.metaboanalystexport.MetaboAnalystExportModule;
import io.github.mzmine.modules.peaklistmethods.io.mztabexport.MzTabExportModule;
import io.github.mzmine.modules.peaklistmethods.io.mztabimport.MzTabImportModule;
import io.github.mzmine.modules.peaklistmethods.io.siriusexport.SiriusExportModule;
import io.github.mzmine.modules.peaklistmethods.io.spectraldbsubmit.LibrarySubmitModule;
import io.github.mzmine.modules.peaklistmethods.io.sqlexport.SQLExportModule;
import io.github.mzmine.modules.peaklistmethods.io.xmlexport.XMLExportModule;
import io.github.mzmine.modules.peaklistmethods.io.xmlimport.XMLImportModule;
import io.github.mzmine.modules.peaklistmethods.isotopes.deisotoper.IsotopeGrouperModule;
import io.github.mzmine.modules.peaklistmethods.isotopes.isotopepeakscanner.IsotopePeakScannerModule;
import io.github.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.modules.peaklistmethods.normalization.linear.LinearNormalizerModule;
import io.github.mzmine.modules.peaklistmethods.normalization.rtcalibration.RTCalibrationModule;
import io.github.mzmine.modules.peaklistmethods.normalization.standardcompound.StandardCompoundNormalizerModule;
import io.github.mzmine.modules.peaklistmethods.peakpicking.adap3decompositionV1_5.ADAP3DecompositionV1_5Module;
import io.github.mzmine.modules.peaklistmethods.peakpicking.adap3decompositionV2.ADAP3DecompositionV2Module;
import io.github.mzmine.modules.peaklistmethods.peakpicking.deconvolution.DeconvolutionModule;
import io.github.mzmine.modules.peaklistmethods.peakpicking.peakextender.PeakExtenderModule;
import io.github.mzmine.modules.peaklistmethods.peakpicking.shapemodeler.ShapeModelerModule;
import io.github.mzmine.modules.peaklistmethods.peakpicking.smoothing.SmoothingModule;
import io.github.mzmine.modules.peaklistmethods.sortpeaklists.SortPeakListsModule;
import io.github.mzmine.modules.projectmethods.projectclose.ProjectCloseModule;
import io.github.mzmine.modules.projectmethods.projectload.ProjectLoadModule;
import io.github.mzmine.modules.projectmethods.projectsave.ProjectSaveAsModule;
import io.github.mzmine.modules.projectmethods.projectsave.ProjectSaveModule;
import io.github.mzmine.modules.rawdatamethods.exportscans.ExportScansFromRawFilesModule;
import io.github.mzmine.modules.rawdatamethods.exportscans.ExportScansModule;
import io.github.mzmine.modules.rawdatamethods.extractscans.ExtractScansModule;
import io.github.mzmine.modules.rawdatamethods.filtering.alignscans.AlignScansModule;
import io.github.mzmine.modules.rawdatamethods.filtering.baselinecorrection.BaselineCorrectionModule;
import io.github.mzmine.modules.rawdatamethods.filtering.cropper.CropFilterModule;
import io.github.mzmine.modules.rawdatamethods.filtering.scanfilters.ScanFiltersModule;
import io.github.mzmine.modules.rawdatamethods.filtering.scansmoothing.ScanSmoothingModule;
import io.github.mzmine.modules.rawdatamethods.merge.RawFileMergeModule;
import io.github.mzmine.modules.rawdatamethods.peakpicking.gridmass.GridMassModule;
import io.github.mzmine.modules.rawdatamethods.peakpicking.manual.ManualPeakPickerModule;
import io.github.mzmine.modules.rawdatamethods.peakpicking.manual.XICManualPickerModule;
import io.github.mzmine.modules.rawdatamethods.peakpicking.massdetection.MassDetectionModule;
import io.github.mzmine.modules.rawdatamethods.peakpicking.msms.MsMsPeakPickerModule;
import io.github.mzmine.modules.rawdatamethods.peakpicking.targetedpeakdetection.TargetedPeakDetectionModule;
import io.github.mzmine.modules.rawdatamethods.rawdataexport.RawDataExportModule;
import io.github.mzmine.modules.rawdatamethods.rawdataimport.RawDataImportModule;
import io.github.mzmine.modules.rawdatamethods.sortdatafiles.SortDataFilesModule;
import io.github.mzmine.modules.tools.isotopepatternpreview.IsotopePatternPreviewModule;
import io.github.mzmine.modules.tools.kovats.KovatsIndexExtractionModule;
import io.github.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeModule;
import io.github.mzmine.modules.tools.mzrangecalculator.MzRangeFormulaCalculatorModule;
import io.github.mzmine.modules.tools.mzrangecalculator.MzRangeMassCalculatorModule;
import io.github.mzmine.modules.visualization.fx3d.Fx3DVisualizerModule;
import io.github.mzmine.modules.visualization.histogram.HistogramVisualizerModule;
import io.github.mzmine.modules.visualization.infovisualizer.InfoVisualizerModule;
import io.github.mzmine.modules.visualization.intensityplot.IntensityPlotModule;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotModule;
import io.github.mzmine.modules.visualization.mzhistogram.MZDistributionHistoModule;
import io.github.mzmine.modules.visualization.neutralloss.NeutralLossVisualizerModule;
import io.github.mzmine.modules.visualization.peaklisttable.PeakListTableModule;
import io.github.mzmine.modules.visualization.peaklisttable.export.IsotopePatternExportModule;
import io.github.mzmine.modules.visualization.peaklisttable.export.MSMSExportModule;
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

      // all other regular MZmineModule (not MZmineRunnableModule) NOT LISTED IN MENU
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
      // DPPAnyElementIsotopeGrouperModule.class basically working, but only for specific elements
      // at the moment
//       PeakListBlankSubtractionModule.class
  };
}
