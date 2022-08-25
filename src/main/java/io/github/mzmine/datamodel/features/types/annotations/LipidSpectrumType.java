/*
 *  Copyright 2006-2022 The MZmine Development Team
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
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.LinkedGraphicalType;
import io.github.mzmine.datamodel.features.types.graphicalnodes.LipidSpectrumChart;
import io.github.mzmine.datamodel.features.types.tasks.FeaturesGraphicalNodeTask;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.MatchedLipid;
import io.github.mzmine.modules.visualization.spectra.matchedlipid.MatchedLipidSpectrumTab;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.layout.StackPane;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

public class LipidSpectrumType extends LinkedGraphicalType {

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "lipid_matched_signals";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Matched Lipid Signals";
  }

  @Override
  public Node getCellNode(TreeTableCell<ModularFeatureListRow, Boolean> cell,
      TreeTableColumn<ModularFeatureListRow, Boolean> coll, Boolean value, RawDataFile raw) {
    ModularFeatureListRow row = cell.getTableRow().getItem();

    if (row == null || !value) {
      return null;
    }

    Node node = row.getBufferedColChart(coll.getText());
    if (node != null) {
      return node;
    }

    StackPane pane = new StackPane();

    List<MatchedLipid> matchedLipids = row.get(LipidMatchListType.class);
    if (matchedLipids != null && !matchedLipids.isEmpty()) {
      Task task = new FeaturesGraphicalNodeTask(LipidSpectrumChart.class, pane, row,
          coll.getText());
      MZmineCore.getTaskController().addTask(task, TaskPriority.NORMAL);
    }
    return pane;
  }

  @Override
  public double getColumnWidth() {
    return DEFAULT_GRAPHICAL_CELL_WIDTH + 50;
  }

  @Nullable
  @Override
  public Runnable getDoubleClickAction(@Nonnull ModularFeatureListRow row,
      @Nonnull List<RawDataFile> file, DataType<?> superType,
      @org.jetbrains.annotations.Nullable final Object value) {
    List<MatchedLipid> matchedLipids = row.get(LipidMatchListType.class);
    if (matchedLipids != null) {
      MatchedLipidSpectrumTab matchedLipidSpectrumTab = new MatchedLipidSpectrumTab(
          matchedLipids.get(0).getLipidAnnotation().getAnnotation() + " Matched Signals",
          new LipidSpectrumChart(row, null));
      return () -> MZmineCore.getDesktop().addTab(matchedLipidSpectrumTab);
    } else {
      return null;
    }
  }
}
