/*
 * Copyright 2006-2012 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.main;

import net.sf.mzmine.modules.batchmode.BatchModeModule;
import net.sf.mzmine.modules.masslistmethods.chromatogrambuilder.ChromatogramBuilderModule;
import net.sf.mzmine.modules.masslistmethods.shoulderpeaksfilter.ShoulderPeaksFilterModule;
import net.sf.mzmine.modules.peaklistmethods.alignment.join.JoinAlignerModule;
import net.sf.mzmine.modules.peaklistmethods.alignment.ransac.RansacAlignerModule;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.ClusteringModule;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.heatmaps.HeatMapModule;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.projectionplots.CDAPlotModule;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.projectionplots.PCAPlotModule;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.projectionplots.SammonsPlotModule;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.rtmzplots.cvplot.CVPlotModule;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.rtmzplots.logratioplot.LogratioPlotModule;
import net.sf.mzmine.modules.peaklistmethods.filtering.duplicatefilter.DuplicateFilterModule;
import net.sf.mzmine.modules.peaklistmethods.filtering.rowsfilter.RowsFilterModule;
import net.sf.mzmine.modules.peaklistmethods.gapfilling.peakfinder.PeakFinderModule;
import net.sf.mzmine.modules.peaklistmethods.gapfilling.samerange.SameRangeGapFillerModule;
import net.sf.mzmine.modules.peaklistmethods.identification.adductsearch.AdductSearchModule;
import net.sf.mzmine.modules.peaklistmethods.identification.camera.CameraSearchModule;
import net.sf.mzmine.modules.peaklistmethods.identification.complexsearch.ComplexSearchModule;
import net.sf.mzmine.modules.peaklistmethods.identification.custom.CustomDBSearchModule;
import net.sf.mzmine.modules.peaklistmethods.identification.dbsearch.OnlineDBSearchModule;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.FormulaPredictionModule;
import net.sf.mzmine.modules.peaklistmethods.identification.fragmentsearch.FragmentSearchModule;
import net.sf.mzmine.modules.peaklistmethods.identification.glycerophospholipidsearch.GPLipidSearchModule;
import net.sf.mzmine.modules.peaklistmethods.identification.nist.NistMsSearchModule;
import net.sf.mzmine.modules.peaklistmethods.io.csvexport.CSVExportModule;
import net.sf.mzmine.modules.peaklistmethods.io.sqlexport.SQLExportModule;
import net.sf.mzmine.modules.peaklistmethods.io.xmlexport.XMLExportModule;
import net.sf.mzmine.modules.peaklistmethods.io.xmlimport.XMLImportModule;
import net.sf.mzmine.modules.peaklistmethods.isotopes.deisotoper.IsotopeGrouperModule;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.modules.peaklistmethods.normalization.linear.LinearNormalizerModule;
import net.sf.mzmine.modules.peaklistmethods.normalization.rtnormalizer.RTNormalizerModule;
import net.sf.mzmine.modules.peaklistmethods.normalization.standardcompound.StandardCompoundNormalizerModule;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.DeconvolutionModule;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.peakextender.PeakExtenderModule;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.shapemodeler.ShapeModelerModule;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.smoothing.SmoothingModule;
import net.sf.mzmine.modules.projectmethods.projectclose.ProjectCloseModule;
import net.sf.mzmine.modules.projectmethods.projectload.ProjectLoadModule;
import net.sf.mzmine.modules.projectmethods.projectsave.ProjectSaveAsModule;
import net.sf.mzmine.modules.projectmethods.projectsave.ProjectSaveModule;
import net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection.BaselineCorrectionModule;
import net.sf.mzmine.modules.rawdatamethods.filtering.datasetfilters.DataSetFiltersModule;
import net.sf.mzmine.modules.rawdatamethods.filtering.scanfilters.ScanFiltersModule;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.manual.ManualPeakPickerModule;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.MassDetectionModule;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.msms.MsMsPeakPickerModule;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.RawDataImportModule;
import net.sf.mzmine.modules.rawdatamethods.targetedpeakdetection.TargetedPeakDetectionModule;
import net.sf.mzmine.modules.tools.mzrangecalculator.MzRangeCalculatorModule;
import net.sf.mzmine.modules.visualization.histogram.HistogramVisualizerModule;
import net.sf.mzmine.modules.visualization.infovisualizer.InfoVisualizerModule;
import net.sf.mzmine.modules.visualization.intensityplot.IntensityPlotModule;
import net.sf.mzmine.modules.visualization.neutralloss.NeutralLossVisualizerModule;
import net.sf.mzmine.modules.visualization.peaklist.PeakListTableModule;
import net.sf.mzmine.modules.visualization.peaklist.export.IsotopePatternExportModule;
import net.sf.mzmine.modules.visualization.peaklist.export.MSMSExportModule;
import net.sf.mzmine.modules.visualization.scatterplot.ScatterPlotVisualizerModule;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizerModule;
import net.sf.mzmine.modules.visualization.threed.ThreeDVisualizerModule;
import net.sf.mzmine.modules.visualization.tic.TICVisualizerModule;
import net.sf.mzmine.modules.visualization.twod.TwoDVisualizerModule;

/**
 * List of modules included in MZmine
 */
public class MZmineModulesList {

    public static final Class<?> MODULES[] = new Class<?>[] {

	    // Project methods
	    ProjectLoadModule.class,
	    ProjectSaveModule.class,
	    ProjectSaveAsModule.class,
	    ProjectCloseModule.class,

	    // Batch mode
	    BatchModeModule.class,

	    // Raw data methods
	    RawDataImportModule.class,
	    MassDetectionModule.class,
	    ShoulderPeaksFilterModule.class,
	    ChromatogramBuilderModule.class,
	    ManualPeakPickerModule.class,
	    MsMsPeakPickerModule.class,
	    ScanFiltersModule.class,
	    DataSetFiltersModule.class,
	    BaselineCorrectionModule.class,

	    // Alignment
	    JoinAlignerModule.class,
	    RansacAlignerModule.class,
	    // PathAlignerModule.class,

	    // I/O
	    CSVExportModule.class,
	    XMLExportModule.class,
	    XMLImportModule.class,
	    SQLExportModule.class,

	    // Gap filling
	    PeakFinderModule.class,
	    SameRangeGapFillerModule.class,

	    // Isotopes
	    IsotopeGrouperModule.class,
	    IsotopePatternCalculator.class,

	    // Peak detection
	    SmoothingModule.class,
	    DeconvolutionModule.class,
	    ShapeModelerModule.class,
	    PeakExtenderModule.class,
	    TargetedPeakDetectionModule.class,

	    // Peak list filtering
	    DuplicateFilterModule.class,
	    RowsFilterModule.class,

	    // Normalization
	    RTNormalizerModule.class,
	    LinearNormalizerModule.class,
	    StandardCompoundNormalizerModule.class,

	    // Data analysis
	    CVPlotModule.class,
	    LogratioPlotModule.class,
	    PCAPlotModule.class,
	    CDAPlotModule.class,
	    SammonsPlotModule.class,
	    ClusteringModule.class,
	    HeatMapModule.class,

	    // Identification
	    CustomDBSearchModule.class,
	    FormulaPredictionModule.class,
	    FragmentSearchModule.class,
	    AdductSearchModule.class,
	    ComplexSearchModule.class,
	    OnlineDBSearchModule.class,
	    GPLipidSearchModule.class,
        CameraSearchModule.class,
	    NistMsSearchModule.class,

	    // Visualizers
	    TICVisualizerModule.class, SpectraVisualizerModule.class,
	    TwoDVisualizerModule.class, ThreeDVisualizerModule.class,
	    NeutralLossVisualizerModule.class, PeakListTableModule.class,
	    IsotopePatternExportModule.class, MSMSExportModule.class,
	    ScatterPlotVisualizerModule.class, HistogramVisualizerModule.class,
	    InfoVisualizerModule.class, IntensityPlotModule.class,

	    // Tools
	    MzRangeCalculatorModule.class };

}
