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
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.noiseamplitude.NoiseAmplitudeResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.savitzkygolay.SavitzkyGolayResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.ImageBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_imsexpander.ImsExpanderModule;
import io.github.mzmine.modules.dataprocessing.featdet_maldispotfeaturedetection.MaldiSpotFeatureDetectionModule;
import io.github.mzmine.modules.dataprocessing.featdet_msn.MsnFeatureDetectionModule;
import io.github.mzmine.modules.dataprocessing.featdet_msn_tree.MsnTreeFeatureDetectionModule;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingModule;
import io.github.mzmine.modules.dataprocessing.featdet_targeted.TargetedFeatureDetectionModule;
import java.util.Collection;
import java.util.List;
import javafx.scene.control.Menu;

public class FeatureDetectionMenuBuilder extends MenuBuilder {

  public FeatureDetectionMenuBuilder() {

  }

  @Override
  public Menu build(Collection<Workspace> workspaces) {
    final Menu menu = new Menu("Feature detection");

    addLcMsMenu(menu);
    addGcMsMenu(menu);
    addImsMsMenu(menu);
    addMsImagingMenu(menu);

    addSeparator(menu);

    addSmoothingAddResolving(menu);

    return filterMenu(menu, workspaces);
  }

  private void addLcMsMenu(Menu menu) {
    final Menu lcms = new Menu("LC-MS");
    menu.getItems().add(lcms);

    addMenuItem(lcms, "Chromatogram builder", ModularADAPChromatogramBuilderModule.class,
        List.of(Workspace.ACADEMIC, Workspace.PRO_FULL, Workspace.LC_MS, Workspace.LIBRARY), null);
    addMenuItem(lcms, "Targeted feature detection", TargetedFeatureDetectionModule.class,
        List.of(Workspace.ACADEMIC, Workspace.PRO_FULL, Workspace.LC_MS, Workspace.LIBRARY), null);
  }

  private void addGcMsMenu(Menu menu) {
    final Menu gcms = new Menu("GC-MS");
    menu.getItems().add(gcms);

    addMenuItem(gcms, "Chromatogram builder", ModularADAPChromatogramBuilderModule.class,
        List.of(Workspace.ACADEMIC, Workspace.PRO_FULL, Workspace.GC_MS, Workspace.LIBRARY), null);
  }

  private void addImsMsMenu(Menu menu) {
    final Menu ims = new Menu("LC-IMS-MS");
    menu.getItems().add(ims);

    addMenuItem(ims, "Chromatogram builder", ModularADAPChromatogramBuilderModule.class,
        List.of(Workspace.ACADEMIC, Workspace.PRO_FULL, Workspace.IMS), null);
    addMenuItem(ims, "IMS expander", ImsExpanderModule.class,
        List.of(Workspace.ACADEMIC, Workspace.PRO_FULL, Workspace.IMS), null);
    addMenuItem(ims, "Mobilogram binning (optional)", ModularADAPChromatogramBuilderModule.class,
        List.of(Workspace.ACADEMIC, Workspace.PRO_FULL, Workspace.IMS), null);
  }

  private void addMsImagingMenu(Menu menu) {
    final Menu msImaging = new Menu("MS imaging");
    menu.getItems().add(msImaging);

    addMenuItem(msImaging, "Image builder", ImageBuilderModule.class,
        List.of(Workspace.ACADEMIC, Workspace.PRO_FULL, Workspace.MALDI_MS), null);
    addMenuItem(msImaging, "Dried droplet MALDI", MaldiSpotFeatureDetectionModule.class,
        List.of(Workspace.ACADEMIC, Workspace.PRO_FULL, Workspace.MALDI_MS), null);
  }

  private void addMSnMenu(Menu menu) {
    final Menu msn = new Menu("MSn");
    menu.getItems().add(msn);

    addMenuItem(msn, "MSn tree feature list builder", MsnTreeFeatureDetectionModule.class,
        List.of(Workspace.ACADEMIC, Workspace.PRO_FULL, Workspace.LIBRARY), null);
    addMenuItem(msn, "MSn feature list builder", MsnFeatureDetectionModule.class,
        List.of(Workspace.ACADEMIC, Workspace.PRO_FULL, Workspace.LIBRARY), null);
  }

  private void addSmoothingAddResolving(Menu menu) {
    addMenuItem(menu, "Smoothing", SmoothingModule.class,
        List.of(Workspace.ACADEMIC, Workspace.PRO_FULL, Workspace.LC_MS, Workspace.IMS,
            Workspace.GC_MS, Workspace.LIBRARY), null);

    final Menu resolving = new Menu("Resolving");
    menu.getItems().add(resolving);
    addMenuItem(resolving, "Local minimum resolver", MinimumSearchFeatureResolverModule.class,
        List.of(Workspace.ACADEMIC, Workspace.PRO_FULL, Workspace.LC_MS, Workspace.IMS,
            Workspace.GC_MS, Workspace.LIBRARY), null);
    addMenuItem(resolving, "Noise amplitude resolver", NoiseAmplitudeResolverModule.class,
        List.of(Workspace.ACADEMIC, Workspace.PRO_FULL), null);
    addMenuItem(resolving, "Savitzgy Golay resolver", SavitzkyGolayResolverModule.class,
        List.of(Workspace.ACADEMIC, Workspace.PRO_FULL), null);
  }
}
