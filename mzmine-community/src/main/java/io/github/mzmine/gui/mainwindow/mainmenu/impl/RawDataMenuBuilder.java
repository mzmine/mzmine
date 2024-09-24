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

package io.github.mzmine.gui.mainwindow.mainmenu.impl;

import static io.github.mzmine.util.javafx.FxMenuUtil.addMenuItem;
import static io.github.mzmine.util.javafx.FxMenuUtil.addSeparator;

import io.github.mzmine.gui.mainwindow.mainmenu.MenuBuilder;
import io.github.mzmine.gui.mainwindow.mainmenu.Workspace;
import io.github.mzmine.modules.dataprocessing.featdet_denormalize_by_inject_time.DenormalizeScansMultiplyByInjectTimeModule;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.MassCalibrationModule;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionModule;
import io.github.mzmine.modules.dataprocessing.featdet_mobilityscanmerger.MobilityScanMergerModule;
import io.github.mzmine.modules.dataprocessing.featdet_shoulderpeaksfilter.ShoulderPeaksFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_alignscans.AlignScansModule;
import io.github.mzmine.modules.dataprocessing.filter_cropfilter.CropFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_maldipseudofilegenerator.MaldiPseudoFileGeneratorModule;
import io.github.mzmine.modules.dataprocessing.filter_merge.RawFileMergeModule;
import io.github.mzmine.modules.dataprocessing.filter_scan_signals.ScanSignalRemovalModule;
import io.github.mzmine.modules.dataprocessing.filter_scanfilters.ScanFiltersModule;
import io.github.mzmine.modules.dataprocessing.filter_scansmoothing.ScanSmoothingModule;
import io.github.mzmine.modules.dataprocessing.id_spectral_library_match.library_to_featurelist.SpectralLibraryToFeatureListModule;
import io.github.mzmine.modules.io.export_msn_tree.MSnTreeExportModule;
import io.github.mzmine.modules.io.export_scans.ExportScansFromRawFilesModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportModule;
import java.util.Collection;
import java.util.List;
import javafx.scene.control.Menu;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;

public class RawDataMenuBuilder extends MenuBuilder {

  public RawDataMenuBuilder() {
  }

  @Override
  public Menu build(Collection<Workspace> workspaces) {

    final Menu menu = new Menu("Raw data methods");

    addMenuItem(menu, "Import MS data", AllSpectralDataImportModule.class, KeyCode.I,
        KeyCombination.SHORTCUT_DOWN);

    final Menu spectraProcessing = new Menu("Spectra processing");
    menu.getItems().add(spectraProcessing);
    addMenuItem(spectraProcessing, "Mass detection", MassDetectionModule.class, null);
    addMenuItem(spectraProcessing, "Mobility scan merging", MobilityScanMergerModule.class, null);
    addMenuItem(spectraProcessing, "FTMS shoulder peak filter", ShoulderPeaksFilterModule.class,
        null);
    addMenuItem(spectraProcessing, "Scan signal removal", ScanSignalRemovalModule.class, null);
    addMenuItem(spectraProcessing, "Mass calibration", MassCalibrationModule.class, null);

    final Menu filtering = new Menu("Raw data filtering");
    menu.getItems().add(filtering);
    addMenuItem(filtering, "Scan by scan filtering", ScanFiltersModule.class,
        List.of(Workspace.ACADEMIC, Workspace.PRO_FULL), null);
    addMenuItem(filtering, "Crop filter", CropFilterModule.class,
        List.of(Workspace.ACADEMIC, Workspace.PRO_FULL), null);
    addMenuItem(filtering, "Align scans (MS1)", AlignScansModule.class,
        List.of(Workspace.ACADEMIC, Workspace.PRO_FULL), null);
    addMenuItem(filtering, "Scan smoothing (MS1)", ScanSmoothingModule.class,
        List.of(Workspace.ACADEMIC, Workspace.PRO_FULL), null);
    addMenuItem(filtering, "Denormalize scans (multiply by inject time)",
        DenormalizeScansMultiplyByInjectTimeModule.class,
        List.of(Workspace.ACADEMIC, Workspace.PRO_FULL), null);
    addMenuItem(filtering, "Split MALDI spots to raw data files",
        MaldiPseudoFileGeneratorModule.class,
        List.of(Workspace.ACADEMIC, Workspace.PRO_FULL, Workspace.MALDI_MS), null);

    addSeparator(menu);

    addMenuItem(menu, "Raw data file merging", RawFileMergeModule.class,
        List.of(Workspace.ACADEMIC, Workspace.PRO_FULL), null);

    addSeparator(menu);

    final Menu export = new Menu("Raw data export");
    menu.getItems().add(export);
    addMenuItem(export, "Export scans", ExportScansFromRawFilesModule.class,
        List.of(Workspace.ACADEMIC, Workspace.PRO_FULL), null);
    addMenuItem(export, "Export MSn trees", MSnTreeExportModule.class,
        List.of(Workspace.ACADEMIC, Workspace.PRO_FULL), null);

    addSeparator(menu);

    final Menu specLibs = new Menu("Spectral libraries");
    menu.getItems().add(specLibs);
    addMenuItem(specLibs, "Spectral library import", SpectralLibraryImportModule.class, null);
    addMenuItem(specLibs, "Spectral library to feature list",
        SpectralLibraryToFeatureListModule.class, null);

    return filterMenu(menu, workspaces);
  }
}
