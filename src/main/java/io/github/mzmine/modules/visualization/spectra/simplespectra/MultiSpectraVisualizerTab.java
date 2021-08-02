/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.visualization.spectra.simplespectra;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.main.MZmineCore;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Collections;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

public class MultiSpectraVisualizerTab extends MZmineTab {

  private final NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();

  public MultiSpectraVisualizerTab(FeatureListRow row) {
    super("All MS/MS of ", false, false);
    setText("All MS/MS of " + mzFormat.format(row.getAverageMZ()));
    MultiSpectraVisualizerPane content = new MultiSpectraVisualizerPane(row);
    setContent(content);
  }

  /**
   * Adds a new multi spectra visualizer tab. May be called from within or outside the fx
   * application thread.
   *
   * @param row
   */
  public static void addNewMultiSpectraVisualizerTab(FeatureListRow row) {
    if (Platform.isFxApplicationThread()) {
      MZmineCore.getDesktop().addTab(new MultiSpectraVisualizerTab(row));
    } else {
      Platform.runLater(() -> MZmineCore.getDesktop().addTab(new MultiSpectraVisualizerTab(row)));
    }
  }

  @NotNull
  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getFeatureLists() {
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getAlignedFeatureLists() {
    return Collections.emptyList();
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {

  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }
}
