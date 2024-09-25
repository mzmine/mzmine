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

package io.github.mzmine.gui.mainwindow.workspaces;

import static io.github.mzmine.util.javafx.FxMenuUtil.addMenuItem;
import static io.github.mzmine.util.javafx.FxMenuUtil.addModuleMenuItems;
import static io.github.mzmine.util.javafx.FxMenuUtil.addSeparator;

import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.batchmode.BatchModeModule;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.noiseamplitude.NoiseAmplitudeResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.savitzkygolay.SavitzkyGolayResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.ImageBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_imsexpander.ImsExpanderModule;
import io.github.mzmine.modules.dataprocessing.featdet_maldispotfeaturedetection.MaldiSpotFeatureDetectionModule;
import io.github.mzmine.modules.dataprocessing.featdet_msn.MsnFeatureDetectionModule;
import io.github.mzmine.modules.dataprocessing.featdet_msn_tree.MsnTreeFeatureDetectionModule;
import io.github.mzmine.modules.dataprocessing.featdet_targeted.TargetedFeatureDetectionModule;
import io.github.mzmine.modules.dataprocessing.filter_clearannotations.ClearFeatureAnnotationsModule;
import io.github.mzmine.modules.dataprocessing.filter_interestingfeaturefinder.AnnotateIsomersModule;
import io.github.mzmine.modules.dataprocessing.group_imagecorrelate.ImageCorrelateGroupingModule;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.corrgrouping.CorrelateGroupingModule;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.export.ExportCorrAnnotationModule;
import io.github.mzmine.modules.dataprocessing.id_biotransformer.BioTransformerModule;
import io.github.mzmine.modules.dataprocessing.id_ecmscalcpotential.CalcEcmsPotentialModule;
import io.github.mzmine.modules.dataprocessing.id_formulapredictionfeaturelist.FormulaPredictionFeatureListModule;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.addionannotations.AddIonNetworkingModule;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.checkmsms.IonNetworkMSMSCheckModule;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.clearionids.ClearIonIdentitiesModule;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.formula.createavgformulas.CreateAvgNetworkFormulasModule;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.formula.prediction.FormulaPredictionIonNetworkModule;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkingModule;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.refinement.IonNetworkRefinementModule;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.relations.IonNetRelationsModule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationModule;
import io.github.mzmine.modules.dataprocessing.id_localcsvsearch.LocalCSVDatabaseSearchModule;
import io.github.mzmine.modules.dataprocessing.id_ms2search.Ms2SearchModule;
import io.github.mzmine.modules.dataprocessing.id_nist.NistMsSearchModule;
import io.github.mzmine.modules.dataprocessing.id_online_reactivity.OnlineLcReactivityModule;
import io.github.mzmine.modules.dataprocessing.id_spectral_library_match.SpectralLibrarySearchModule;
import io.github.mzmine.modules.io.export_ccsbase.CcsBaseExportModule;
import io.github.mzmine.modules.io.export_compoundAnnotations_csv.CompoundAnnotationsCSVExportModule;
import io.github.mzmine.modules.io.export_features_all_speclib_matches.ExportAllIdsGraphicalModule;
import io.github.mzmine.modules.io.export_features_csv.CSVExportModularModule;
import io.github.mzmine.modules.io.export_features_csv_legacy.LegacyCSVExportModule;
import io.github.mzmine.modules.io.export_features_featureML.FeatureMLExportModularModule;
import io.github.mzmine.modules.io.export_features_gnps.fbmn.GnpsFbmnExportAndSubmitModule;
import io.github.mzmine.modules.io.export_features_gnps.gc.GnpsGcExportAndSubmitModule;
import io.github.mzmine.modules.io.export_features_metaboanalyst.MetaboAnalystExportModule;
import io.github.mzmine.modules.io.export_features_mgf.AdapMgfExportModule;
import io.github.mzmine.modules.io.export_features_msp.AdapMspExportModule;
import io.github.mzmine.modules.io.export_features_mztabm.MZTabmExportModule;
import io.github.mzmine.modules.io.export_features_sirius.SiriusExportModule;
import io.github.mzmine.modules.io.export_features_sql.SQLExportModule;
import io.github.mzmine.modules.io.export_features_venn.VennExportModule;
import io.github.mzmine.modules.io.export_library_gnps_batch.GNPSLibraryBatchExportModule;
import io.github.mzmine.modules.io.export_network_graphml.NetworkGraphMlExportModule;
import io.github.mzmine.modules.io.import_feature_networks.ImportFeatureNetworksSimpleModule;
import io.github.mzmine.modules.io.projectload.ProjectLoadModule;
import io.github.mzmine.modules.io.projectsave.ProjectSaveAsModule;
import io.github.mzmine.modules.io.projectsave.ProjectSaveModule;
import io.github.mzmine.modules.io.spectraldbsubmit.batch.LibraryBatchGenerationModule;
import io.github.mzmine.modules.visualization.projectmetadata.ProjectMetadataTab;
import io.github.mzmine.util.javafx.FxMenuUtil;
import java.util.EnumSet;
import java.util.List;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;

public sealed interface Workspace permits Academic {

  String getName();

  MenuBar buildMainMenu(EnumSet<WorkspaceTags> tags);

  //--------------------------------------------------------
  // Default methods below here
  //--------------------------------------------------------

  /**
   * @return A duplicate-free list of all modules covered by the main menu of this workspace.
   */
  default List<Class<? extends MZmineRunnableModule>> getAllModules(EnumSet<WorkspaceTags> tags) {
    return buildMainMenu(tags).getMenus().stream().map(WorkspaceMenuUtils::extractModules)
        .flatMap(List::stream).distinct().toList();
  }

  default Menu buildDefaultProjectMenu() {
    final Menu menu = new Menu(MainMenuEntries.PROJECT.toString());
    final Menu recentProjects = new Menu("Recent projects");

    menu.setOnShowing(_ -> WorkspaceMenuUtils.fillRecentProjects(recentProjects));

    FxMenuUtil.addModuleMenuItem(menu, ProjectLoadModule.class, KeyCode.O,
        KeyCombination.SHORTCUT_DOWN);
    FxMenuUtil.addModuleMenuItem(menu, ProjectSaveModule.class, KeyCode.S,
        KeyCombination.SHORTCUT_DOWN);
    FxMenuUtil.addModuleMenuItem(menu, ProjectSaveAsModule.class, KeyCode.S,
        KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN);
    addMenuItem(menu, "Close project", MZmineGUI::requestCloseProject, KeyCode.Q,
        KeyCombination.SHORTCUT_DOWN);
    addSeparator(menu);
    FxMenuUtil.addModuleMenuItem(menu, BatchModeModule.class, KeyCode.B,
        KeyCombination.SHORTCUT_DOWN);
    addSeparator(menu);
    addMenuItem(menu, "Sample metadata",
        () -> MZmineCore.getDesktop().addTab(new ProjectMetadataTab()), KeyCode.M,
        KeyCombination.SHORTCUT_DOWN);
    addSeparator(menu);
    addMenuItem(menu, "Set preferences",
        () -> MZmineCore.getConfiguration().getPreferences().showSetupDialog(true), KeyCode.P,
        KeyCombination.SHORTCUT_DOWN);
    addMenuItem(menu, "Save configuration", WorkspaceMenuUtils::saveConfiguration, null);
    addMenuItem(menu, "Load configuration", WorkspaceMenuUtils::loadConfiguration, null);

    return menu;
  }

  default Menu buildDefaultLcMsSubMenu() {
    return addModuleMenuItems("LC-MS", ModularADAPChromatogramBuilderModule.class,
        TargetedFeatureDetectionModule.class);
  }

  default Menu buildDefaultGcMsSubMenu() {
    return addModuleMenuItems("GC-MS", ModularADAPChromatogramBuilderModule.class/*,
        SpectralDeconvolutionGCModule.class*/);
  }

  default Menu buildDefaultImsMsSubMenu() {
    return addModuleMenuItems("LC-IMS-MS", ModularADAPChromatogramBuilderModule.class,
        ImsExpanderModule.class, ModularADAPChromatogramBuilderModule.class);
  }

  default Menu buildDefaultImagingSubMenu() {
    return addModuleMenuItems("MS imaging/Spots", ImageBuilderModule.class,
        MaldiSpotFeatureDetectionModule.class);
  }

  default Menu buildDefaultMsnSubMenu() {
    return addModuleMenuItems("MSn", MsnTreeFeatureDetectionModule.class,
        MsnFeatureDetectionModule.class);
  }

  default Menu buildDefaultResolvingSubMenu() {
    return addModuleMenuItems("Resolving", MinimumSearchFeatureResolverModule.class,
        NoiseAmplitudeResolverModule.class, SavitzkyGolayResolverModule.class);
  }

  default Menu buildDefaultFeatureListExportSubMenu() {
    final Menu menu = new Menu("Export feature list");

    addModuleMenuItems(menu, "Graphics", ExportAllIdsGraphicalModule.class);
    addModuleMenuItems(menu, CSVExportModularModule.class, CompoundAnnotationsCSVExportModule.class,
        LegacyCSVExportModule.class, MZTabmExportModule.class, SQLExportModule.class,
        AdapMspExportModule.class, AdapMgfExportModule.class, GnpsFbmnExportAndSubmitModule.class,
        GnpsGcExportAndSubmitModule.class, SiriusExportModule.class,
        ImportFeatureNetworksSimpleModule.class, ExportCorrAnnotationModule.class,
        NetworkGraphMlExportModule.class, FeatureMLExportModularModule.class,
        CcsBaseExportModule.class);
    addModuleMenuItems(menu, "Statistics", VennExportModule.class, MetaboAnalystExportModule.class);
    addModuleMenuItems(menu, "Libraries", LibraryBatchGenerationModule.class,
        GNPSLibraryBatchExportModule.class);

    return menu;
  }

  default Menu buildDefaultFeatureGroupingSubMenu() {
    final Menu groupingMenu = addModuleMenuItems("Feature grouping", CorrelateGroupingModule.class,
        IonNetworkingModule.class, AddIonNetworkingModule.class, IonNetworkRefinementModule.class,
        IonNetRelationsModule.class, FormulaPredictionIonNetworkModule.class,
        CreateAvgNetworkFormulasModule.class, IonNetworkMSMSCheckModule.class,
        ClearIonIdentitiesModule.class);
    groupingMenu.getItems().add(new SeparatorMenuItem());
    addModuleMenuItems(groupingMenu, OnlineLcReactivityModule.class, AnnotateIsomersModule.class);
    groupingMenu.getItems().addAll(new SeparatorMenuItem(),
        new ModuleMenuItem(ImageCorrelateGroupingModule.class, null));
    return groupingMenu;
  }

  default Menu buildDefaultAnnotationSubMenu() {
    final Menu menu = new Menu("Annotation");

    addModuleMenuItems(menu, "Search precursor mass", LocalCSVDatabaseSearchModule.class,
        BioTransformerModule.class);
    addModuleMenuItems(menu, "Search spectra", SpectralLibrarySearchModule.class,
        LipidAnnotationModule.class, NistMsSearchModule.class,
        FormulaPredictionFeatureListModule.class, Ms2SearchModule.class);
    addModuleMenuItems(menu, "EC-MS workflow", CalcEcmsPotentialModule.class);
    menu.getItems().add(new SeparatorMenuItem());
    addModuleMenuItems(menu, ClearFeatureAnnotationsModule.class);
    return menu;
  }
}
