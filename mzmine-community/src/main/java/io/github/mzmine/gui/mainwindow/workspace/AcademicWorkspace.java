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

package io.github.mzmine.gui.mainwindow.workspace;

import static io.github.mzmine.util.javafx.FxMenuUtil.addModuleMenuItem;
import static io.github.mzmine.util.javafx.FxMenuUtil.addModuleMenuItems;
import static io.github.mzmine.util.javafx.FxMenuUtil.addSeparator;

import io.github.mzmine.modules.dataanalysis.feat_ms2_similarity_intra.IntraFeatureRowMs2SimilarityModule;
import io.github.mzmine.modules.dataanalysis.pca_new.PCAModule;
import io.github.mzmine.modules.dataanalysis.significance.anova.AnovaModule;
import io.github.mzmine.modules.dataanalysis.spec_chimeric_precursor.PrecursorPurityCheckerModule;
import io.github.mzmine.modules.dataanalysis.statsdashboard.StatsDasboardModule;
import io.github.mzmine.modules.dataanalysis.volcanoplot.VolcanoPlotModule;
import io.github.mzmine.modules.dataprocessing.align_append_rows.MergeAlignerModule;
import io.github.mzmine.modules.dataprocessing.align_gc.GCAlignerModule;
import io.github.mzmine.modules.dataprocessing.align_join.JoinAlignerModule;
import io.github.mzmine.modules.dataprocessing.align_lcimage.LcImageAlignerModule;
import io.github.mzmine.modules.dataprocessing.align_ransac.RansacAlignerModule;
import io.github.mzmine.modules.dataprocessing.featdet_denormalize_by_inject_time.DenormalizeScansMultiplyByInjectTimeModule;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.MassCalibrationModule;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionModule;
import io.github.mzmine.modules.dataprocessing.featdet_mobilityscanmerger.MobilityScanMergerModule;
import io.github.mzmine.modules.dataprocessing.featdet_shoulderpeaksfilter.ShoulderPeaksFilterModule;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingModule;
import io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.SpectralDeconvolutionGCModule;
import io.github.mzmine.modules.dataprocessing.filter_alignscans.AlignScansModule;
import io.github.mzmine.modules.dataprocessing.filter_blanksubtraction.FeatureListBlankSubtractionModule;
import io.github.mzmine.modules.dataprocessing.filter_blanksubtraction_chromatograms.ChromatogramBlankSubtractionModule;
import io.github.mzmine.modules.dataprocessing.filter_cropfilter.CropFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_diams2.DiaMs2CorrModule;
import io.github.mzmine.modules.dataprocessing.filter_duplicatefilter.DuplicateFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.FeatureFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_groupms2.GroupMS2Module;
import io.github.mzmine.modules.dataprocessing.filter_groupms2_refine.GroupedMs2RefinementModule;
import io.github.mzmine.modules.dataprocessing.filter_ims_msms_refinement.ImsMs2RefinementModule;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.IsotopeFinderModule;
import io.github.mzmine.modules.dataprocessing.filter_isotopegrouper.IsotopeGrouperModule;
import io.github.mzmine.modules.dataprocessing.filter_maldigroupms2.MaldiGroupMS2Module;
import io.github.mzmine.modules.dataprocessing.filter_maldipseudofilegenerator.MaldiPseudoFileGeneratorModule;
import io.github.mzmine.modules.dataprocessing.filter_merge.RawFileMergeModule;
import io.github.mzmine.modules.dataprocessing.filter_mobilitymzregionextraction.MobilityMzRegionExtractionModule;
import io.github.mzmine.modules.dataprocessing.filter_neutralloss.NeutralLossFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_scan_signals.ScanSignalRemovalModule;
import io.github.mzmine.modules.dataprocessing.filter_scanfilters.ScanFiltersModule;
import io.github.mzmine.modules.dataprocessing.filter_scansmoothing.ScanSmoothingModule;
import io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.multithreaded.MultiThreadPeakFinderModule;
import io.github.mzmine.modules.dataprocessing.gapfill_samerange.SameRangeGapFillerModule;
import io.github.mzmine.modules.dataprocessing.id_ccscalc.CCSCalcModule;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.external.ExternalCCSCalibrationModule;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.reference.ReferenceCCSCalibrationModule;
import io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner.IsotopePeakScannerModule;
import io.github.mzmine.modules.dataprocessing.id_spectral_library_match.library_to_featurelist.SpectralLibraryToFeatureListModule;
import io.github.mzmine.modules.dataprocessing.norm_linear.LinearNormalizerModule;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration.RTCorrectionModule;
import io.github.mzmine.modules.dataprocessing.norm_standardcompound.StandardCompoundNormalizerModule;
import io.github.mzmine.modules.io.export_msn_tree.MSnTreeExportModule;
import io.github.mzmine.modules.io.export_scans.ExportScansFromRawFilesModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportModule;
import io.github.mzmine.util.javafx.ModuleMenuItem;
import io.mzio.mzmine.gui.workspace.WorkspaceMenuHelper;
import io.mzio.mzmine.gui.workspace.WorkspaceTags;
import io.mzio.users.user.MZmineUser;
import java.util.EnumSet;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import org.jetbrains.annotations.Nullable;

public final class AcademicWorkspace extends AbstractWorkspace {

  public static final String uniqueId = "academic";

  @Override
  public String getName() {
    return "Academic";
  }

  @Override
  public String getUniqueId() {
    return uniqueId;
  }

  @Override
  public MenuBar buildMainMenu(EnumSet<WorkspaceTags> tags) {
    final MenuBar menuBar = new MenuBar();

    menuBar.getMenus().add(buildDefaultProjectMenu());
    menuBar.getMenus().add(buildRawDataMenu());
    menuBar.getMenus().add(buildFeatureDetectionMenu());
    menuBar.getMenus().add(buildFeatureListMethodsMenu());
    menuBar.getMenus().add(buildDefaultVisualizationMenu());
    menuBar.getMenus().add(buildDefaultWizardMenu());
    menuBar.getMenus().add(buildDefaultToolsMenu());
    menuBar.getMenus().add(buildDefaultWindowsMenu());
    menuBar.getMenus().add(buildDefaultUsersMenu());
    final Menu workspaces = buildDefaultWorkspacesMenu();
    if (WorkspaceMenuHelper.getWorkspaces().size() > 1) {
      menuBar.getMenus().add(workspaces);
    }
    menuBar.getMenus().add(buildDefaultHelpMenu());
    return menuBar;
  }

  private Menu buildRawDataMenu() {
    final Menu menu = new Menu("Raw data methods");

    addModuleMenuItem(menu, AllSpectralDataImportModule.class, KeyCode.I,
        KeyCombination.SHORTCUT_DOWN);

    addModuleMenuItems(menu, "Spectra processing", MassDetectionModule.class,
        MobilityScanMergerModule.class, ShoulderPeaksFilterModule.class,
        ScanSignalRemovalModule.class, MassCalibrationModule.class);

    addModuleMenuItems(menu, "Raw data filtering", ScanFiltersModule.class, CropFilterModule.class,
        AlignScansModule.class, ScanSmoothingModule.class,
        DenormalizeScansMultiplyByInjectTimeModule.class, MaldiPseudoFileGeneratorModule.class);

    addSeparator(menu);

    addModuleMenuItem(menu, RawFileMergeModule.class, null);

    addSeparator(menu);

    addModuleMenuItems(menu, "Raw data export", ExportScansFromRawFilesModule.class,
        MSnTreeExportModule.class);

    addSeparator(menu);

    addModuleMenuItems(menu, "Spectral libraries", SpectralLibraryImportModule.class,
        SpectralLibraryToFeatureListModule.class);

    return menu;
  }

  private Menu buildFeatureDetectionMenu() {
    final Menu menu = new Menu("Feature detection");
    menu.getItems()
        .addAll(buildDefaultLcMsSubMenu(), buildDefaultGcMsSubMenu(), buildDefaultImsMsSubMenu(),
            buildDefaultImagingSubMenu(), buildDefaultMsnSubMenu(), new SeparatorMenuItem(),
            new ModuleMenuItem(SmoothingModule.class),
            new ModuleMenuItem(ChromatogramBlankSubtractionModule.class),
            buildDefaultResolvingSubMenu());

    return menu;
  }

  private Menu buildFeatureListMethodsMenu() {
    final Menu menu = new Menu("Feature list methods", null,
        buildDefaultFeatureListExportSubMenu());

    addModuleMenuItems(menu, "Processing", GroupMS2Module.class, DiaMs2CorrModule.class,
        MaldiGroupMS2Module.class, ImsMs2RefinementModule.class, GroupedMs2RefinementModule.class,
        PrecursorPurityCheckerModule.class, IntraFeatureRowMs2SimilarityModule.class,
        ReferenceCCSCalibrationModule.class, ExternalCCSCalibrationModule.class,
        CCSCalcModule.class);

    addModuleMenuItems(menu, "Isotopes", IsotopeGrouperModule.class, IsotopeFinderModule.class,
        IsotopePeakScannerModule.class);

    menu.getItems().add(buildDefaultFeatureGroupingSubMenu());

    addModuleMenuItems(menu, "Spectral deconvolution (GC)", SpectralDeconvolutionGCModule.class);
    addModuleMenuItems(menu, "Feature list filtering", DuplicateFilterModule.class,
        RowsFilterModule.class, FeatureFilterModule.class, FeatureListBlankSubtractionModule.class,
        ChromatogramBlankSubtractionModule.class, MobilityMzRegionExtractionModule.class,
        NeutralLossFilterModule.class);
    addModuleMenuItems(menu, "Alignment", JoinAlignerModule.class, MergeAlignerModule.class,
        RansacAlignerModule.class, GCAlignerModule.class,
        LcImageAlignerModule.class); // HierarAlignerGcModule, ADAP3AlignerModule (not mit compatible)
    addModuleMenuItems(menu, "Gap filling/Recursive feature finding",
        MultiThreadPeakFinderModule.class, SameRangeGapFillerModule.class);
    addModuleMenuItems(menu, "Normalization", RTCorrectionModule.class,
        LinearNormalizerModule.class, StandardCompoundNormalizerModule.class);

    menu.getItems().add(buildDefaultAnnotationSubMenu());

    addModuleMenuItems(menu, "Statistics", StatsDasboardModule.class, VolcanoPlotModule.class,
        PCAModule.class, AnovaModule.class);
    return menu;
  }

  @Override
  public boolean isAllowedWithLicense(@Nullable MZmineUser user) {
    return true;
  }
}
