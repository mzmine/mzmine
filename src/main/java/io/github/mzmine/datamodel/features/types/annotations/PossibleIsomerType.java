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

package io.github.mzmine.datamodel.features.types.annotations;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ListDataType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.filter_interestingfeaturefinder.visualization.MultiImsTraceVisualizerTab;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ListProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PossibleIsomerType extends ListDataType<Integer> implements AnnotationType {

  @Override
  public @NotNull String getHeaderString() {
    return "Possible Isomers";
  }

  @Override
  public @Nullable Runnable getDoubleClickAction(@NotNull ModularFeatureListRow row,
      @NotNull List<RawDataFile> file) {

    return () -> {
      if (row.get(PossibleIsomerType.class) == null || row.get(PossibleIsomerType.class)
          .isEmpty()) {
        return;
      }
      var isomerIds = new ArrayList<>(row.get(PossibleIsomerType.class));

      final ModularFeatureList flist = row.getFeatureList();
      final List<ModularFeatureListRow> isomerRows = new ArrayList<>();
      isomerRows.add(row);
      isomerRows.addAll(flist.modularStream().filter(r -> isomerIds.contains(r.getID())).toList());

      // recursively add new isomer ids
      boolean isomerFound = true;
      while (isomerFound) {
        isomerFound = false;

        // add new isomer ids here, the old lists keeps track of all the ones we added
        final List<Integer> newIsomerIds = new ArrayList<>();
        for (final ModularFeatureListRow isomerRow : isomerRows) {
          final ListProperty<Integer> moreIsomers = isomerRow.get(PossibleIsomerType.class);
          if (moreIsomers == null || moreIsomers.isEmpty()) {
            continue;
          }
          for (Integer isoId : moreIsomers) {
            if (!isomerIds.contains(isoId) && !newIsomerIds.contains(isoId)) {
              isomerFound = true;
              newIsomerIds.add(isoId);
            }
          }
        }
        isomerIds.addAll(newIsomerIds);
        isomerRows
            .addAll(flist.modularStream().filter(r -> newIsomerIds.contains(r.getID())).toList());
      }

      final MultiImsTraceVisualizerTab tab = new MultiImsTraceVisualizerTab();

      var bestFile = row.getBestFeature().getRawDataFile();
      final List<ModularFeature> features = new ArrayList<>();
      for (final ModularFeatureListRow isomerRow : isomerRows) {
        if (isomerRow.getFeature(bestFile) != null) {
          features.add(isomerRow.getFeature(bestFile));
        } else {
          features.add(isomerRow.getBestFeature());
        }
      }

      MZmineCore.runLater(() -> {
        tab.setFeatures(features);
        MZmineCore.getDesktop().addTab(tab);
      });
    };
  }
}
