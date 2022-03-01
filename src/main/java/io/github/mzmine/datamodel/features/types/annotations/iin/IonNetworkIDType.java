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

package io.github.mzmine.datamodel.features.types.annotations.iin;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.numbers.abstr.IntegerType;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.modules.visualization.chromatogram.TICVisualizerTab;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The ID from Ion Identity Networking (IIN), which searches for different ions describing the same
 * molecule. See {@link IonIdentity} and {@link IonNetwork}
 */
public class IonNetworkIDType extends IntegerType {

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "iin_id";
  }

  @NotNull
  @Override
  public String getHeaderString() {
    return "IIN ID";
  }

  @Nullable
  @Override
  public Runnable getDoubleClickAction(@NotNull ModularFeatureListRow row,
      @NotNull List<RawDataFile> file) {

    var ionIdentity = row.getBestIonIdentity();
    if (ionIdentity == null) {
      return null;
    }

    return () -> {
      final IonNetwork network = ionIdentity.getNetwork();
      ModularFeature bestFeature = row.getBestFeature();
      final RawDataFile bestRaw = bestFeature.getRawDataFile();

      Map<Feature, String> labels = new HashMap<>();
      List<Feature> features = network.keySet().stream().filter(r -> r.getFeature(bestRaw) != null)
          .<Feature>mapMulti((r, c) -> {
            Feature feature = r.getFeature(bestRaw);
            labels.put(feature, network.get(r).getAdduct());
            c.accept(feature);
          }).toList();

      TICVisualizerTab tab = new TICVisualizerTab(new RawDataFile[]{bestRaw}, TICPlotType.BASEPEAK,
          new ScanSelection(1), bestFeature.getRawDataPointsMZRange(), features, labels);
      MZmineCore.getDesktop().addTab(tab);
    };
  }
}
