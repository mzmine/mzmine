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

package io.github.mzmine.util;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.beans.property.Property;
import javax.annotation.Nonnull;

public class IonMobilityUtils {

  public static double getSmallestMobilityDelta(Frame frame) {
    double minDelta = Double.MAX_VALUE;

    List<MobilityScan> scans = frame.getMobilityScans();
    double lastMobility = scans.get(0).getMobility();
    for (int i = 1; i < scans.size(); i++) {
      final double currentMobility = scans.get(i).getMobility();
      final double delta = Math.abs(currentMobility - lastMobility);
      if (delta < minDelta) {
        minDelta = delta;
      }
      lastMobility = currentMobility;
    }
    return minDelta;
  }

  public static ModularFeatureList extractRegionFromFeatureList(
      @Nonnull final Collection<Path2D> regions,
      @Nonnull final ModularFeatureList originalFeatureList, @Nonnull final String suffix) {
    ModularFeatureList newFeatureList = originalFeatureList
        .createCopy(originalFeatureList.getName() + suffix);

    Map<ModularFeatureListRow, Set<ModularFeature>> featuresToRemove = new HashMap<>();

    for (FeatureListRow r : newFeatureList.getRows()) {
      ModularFeatureListRow row = (ModularFeatureListRow) r;
      List<RawDataFile> rawDataFiles = row.getRawDataFiles();
      for (RawDataFile file : rawDataFiles) {

        ModularFeature feature = (ModularFeature) row.getFeature(file);
        boolean contained = false;

        if (feature != null) {
          Property<Float> mobility = feature.get(MobilityType.class);

          if (mobility != null) {

            Point2D point = new Point2D.Double(feature.getMZ(), mobility.getValue().doubleValue());
            for (Path2D region : regions) {

              if (region.contains(point)) {
                contained = true;
                break;
              }
            }
          }
        }

        if (!contained) {
//          Set<ModularFeature> set = featuresToRemove
//              .computeIfAbsent((ModularFeatureListRow) row, key -> new HashSet<>());
//          set.add(feature);
          row.removeFeature(file);
        }
      }
    }

    List<FeatureListRow> rowsToRemove = new ArrayList<>();
    for (FeatureListRow r : newFeatureList.getRows()) {
      if (r.getNumberOfFeatures() == 0) {
        rowsToRemove.add(r);
      }
    }
    rowsToRemove.forEach(newFeatureList::removeRow);

    return newFeatureList;
  }
}
