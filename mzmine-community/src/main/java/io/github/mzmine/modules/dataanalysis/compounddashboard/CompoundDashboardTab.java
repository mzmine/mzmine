/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataanalysis.compounddashboard;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.mainwindow.SimpleTab;
import java.util.Collection;
import java.util.Collections;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tab wrapping a {@link CompoundDashboardController}. Opened from the main window for any feature
 * list that has an associated {@code CompoundList}.
 */
public class CompoundDashboardTab extends SimpleTab {

  private final CompoundDashboardController controller;

  public CompoundDashboardTab(@Nullable final FeatureList flist) {
    super(CompoundDashboardModule.NAME, true, false);
    setSubTitle(flist == null ? null : flist.getName());

    controller = new CompoundDashboardController();
    final Region region = controller.buildView();
    setContent(region);

    if (flist instanceof ModularFeatureList mfl) {
      controller.setFeatureList(mfl);
    }

    setOnClosed(_ -> {
      controller.close();
      setOnClosed(null);
    });
  }

  public CompoundDashboardController getController() {
    return controller;
  }

  public @Nullable FeatureList getFeatureList() {
    return controller.getFeatureList();
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getFeatureLists() {
    final FeatureList fl = controller.getFeatureList();
    return fl == null || fl.isAligned() ? Collections.emptyList() : Collections.singletonList(fl);
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getAlignedFeatureLists() {
    final FeatureList fl = controller.getFeatureList();
    return fl != null && fl.isAligned() ? Collections.singletonList(fl) : Collections.emptyList();
  }

  @Override
  public void onFeatureListSelectionChanged(final Collection<? extends FeatureList> featureLists) {
    if (featureLists == null || featureLists.isEmpty()) {
      controller.setFeatureList(null);
      setSubTitle(null);
      return;
    }
    final FeatureList first = featureLists.iterator().next();
    if (first instanceof ModularFeatureList mfl) {
      controller.setFeatureList(mfl);
      setSubTitle(mfl.getName());
    }
  }

  @Override
  public void onAlignedFeatureListSelectionChanged(
      final Collection<? extends FeatureList> featureLists) {
    onFeatureListSelectionChanged(featureLists);
  }

}
