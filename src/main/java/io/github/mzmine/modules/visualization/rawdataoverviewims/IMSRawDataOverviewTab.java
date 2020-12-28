/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.visualization.rawdataoverviewims;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nonnull;

public class IMSRawDataOverviewTab extends MZmineTab {

  private final IMSRawDataOverviewPane pane;

  public IMSRawDataOverviewTab() {
    super("Ion mobility raw data overview", true, false);
    pane = new IMSRawDataOverviewPane();
    setContent(pane);
  }

  @Nonnull
  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    return Collections.emptySet();
  }

  @Nonnull
  @Override
  public Collection<? extends FeatureList> getFeatureLists() {
    return Collections.emptySet();
  }

  @Nonnull
  @Override
  public Collection<? extends FeatureList> getAlignedFeatureLists() {
    return Collections.emptySet();
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {
    pane.setRawDataFile(rawDataFiles.stream().findFirst().get());
  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(Collection<? extends FeatureList> featurelists) {

  }
}
