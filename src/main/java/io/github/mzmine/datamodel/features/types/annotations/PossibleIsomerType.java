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

package io.github.mzmine.datamodel.features.types.annotations;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ListDataType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.filter_interestingfeaturefinder.visualization.MultiImsTraceVisualizerTab;
import io.github.mzmine.util.ParsingUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PossibleIsomerType extends ListDataType<Integer> implements AnnotationType {

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "possible_isomers";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Possible Isomers";
  }

  @Override
  public @NotNull String getFormattedString(List<Integer> list) {
    return list == null || list.isEmpty() ? "" : list.size() + ": " + list.toString();
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
      isomerRows.addAll(isomerIds.stream()
          .<ModularFeatureListRow>map(id -> (ModularFeatureListRow) flist.findRowByID(id))
          .filter(r -> r != null).distinct().toList());
      isomerRows.add(row);
//      isomerRows.addAll(flist.modularStream().filter(r -> isomerIds.contains(r.getID())).toList());
//      isomerRows.addAll(flist.modularStream().filter(r -> isomerIds.contains(r.getID())).toList());

      // recursively add new isomer ids
      /*boolean isomerFound = true;
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
      }*/

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

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if (!(value instanceof List<?> list)) {
      return;
    }
    int[] objects = list.stream().filter(i -> i instanceof Integer).mapToInt(i -> (Integer) i)
        .toArray();
    String str = ParsingUtils.intArrayToString(objects, objects.length);
    writer.writeCharacters(str);
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull ModularFeatureList flist,
      @NotNull ModularFeatureListRow row, @Nullable ModularFeature feature,
      @Nullable RawDataFile file) throws XMLStreamException {
    final String text = reader.getElementText();
    if (text.isEmpty()) {
      return null;
    }
    int[] array = ParsingUtils.stringToIntArray(text);
    return Arrays.stream(array).boxed().toList();
  }
}
