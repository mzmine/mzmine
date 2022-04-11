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

package io.github.mzmine.modules.visualization.spectra.matchedlipid;

import java.util.Collection;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.types.graphicalnodes.LipidSpectrumChart;
import io.github.mzmine.gui.mainwindow.MZmineTab;

public class MatchedLipidSpectrumTab extends MZmineTab {

  public MatchedLipidSpectrumTab(String matchedLipids, LipidSpectrumChart lipidSpectrumChart) {
    super(matchedLipids);
    setContent(lipidSpectrumChart);
  }

  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Collection<? extends FeatureList> getFeatureLists() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Collection<? extends FeatureList> getAlignedFeatureLists() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {
    // TODO Auto-generated method stub

  }


}
