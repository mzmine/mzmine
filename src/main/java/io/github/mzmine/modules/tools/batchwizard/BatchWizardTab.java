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

package io.github.mzmine.modules.tools.batchwizard;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import org.jetbrains.annotations.NotNull;

public class BatchWizardTab extends MZmineTab {

  public BatchWizardTab() {
    super("Processing wizard");

    final FXMLLoader loader = new FXMLLoader(getClass().getResource("BatchWizard.fxml"));

    try {
      Node mainPane = loader.load();
      setContent(mainPane);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public @NotNull Collection<? extends RawDataFile> getRawDataFiles() {
    return Collections.emptyList();
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getFeatureLists() {
    return Collections.emptyList();
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getAlignedFeatureLists() {
    return Collections.emptyList();
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {
    // do nothing
  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {
    // do nothing
  }

  @Override
  public void onAlignedFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {
    // do nothing
  }
}
