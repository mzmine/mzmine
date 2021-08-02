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

package io.github.mzmine.parameters.parametertypes.selectors;

import io.github.mzmine.util.RangeUtils;
import java.awt.event.ActionEvent;
import java.util.List;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.ranges.IntRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import io.github.mzmine.util.ExitCode;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

public class FeatureSelectionComponent extends BorderPane {

  private final ObservableList<FeatureSelection> selection = FXCollections.observableArrayList();
  private final ListView<FeatureSelection> selectionList;
  private final FlowPane buttonPanel;
  private final Button addButton, removeButton, allButton, clearButton;

  public FeatureSelectionComponent() {

    selectionList = new ListView<>(selection);
    selectionList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    selectionList.setPrefWidth(200);
    selectionList.setPrefHeight(50);
    setCenter(selectionList);

    buttonPanel = new FlowPane(Orientation.VERTICAL);

    addButton = new Button("Add");
    addButton.setOnAction(e -> {
      final IntRangeParameter idParameter =
          new IntRangeParameter("ID", "Range of included peak IDs", false, null);
      final MZRangeParameter mzParameter = new MZRangeParameter(false);
      final RTRangeParameter rtParameter = new RTRangeParameter(false);
      final StringParameter nameParameter =
          new StringParameter("Name", "Peak identity name", null, false);
      SimpleParameterSet paramSet = new SimpleParameterSet(
          new Parameter[] {idParameter, mzParameter, rtParameter, nameParameter});
      ExitCode exitCode = paramSet.showSetupDialog(true);
      if (exitCode == ExitCode.OK) {
        Range<Integer> idRange = paramSet.getParameter(idParameter).getValue();
        Range<Double> mzRange = paramSet.getParameter(mzParameter).getValue();
        // TODO: FloatRangeParameter
        Range<Float> rtRange = RangeUtils.toFloatRange(paramSet.getParameter(rtParameter).getValue());
        String name = paramSet.getParameter(nameParameter).getValue();
        FeatureSelection ps = new FeatureSelection(idRange, mzRange, rtRange, name);
        selection.add(ps);
      }
    });

    removeButton = new Button("Remove");
    removeButton.setOnAction(e -> {
      selection.removeAll(selectionList.getSelectionModel().getSelectedItems());
    });

    allButton = new Button("Set to all");
    allButton.setOnAction(e -> {
      FeatureSelection ps = new FeatureSelection(null, null, null, null);
      selection.clear();
      selection.add(ps);
    });

    clearButton = new Button("Clear");
    clearButton.setOnAction(e -> {
      selection.clear();
    });

    buttonPanel.getChildren().addAll(addButton, removeButton, allButton, clearButton);

    setRight(buttonPanel);
  }

  void setValue(List<FeatureSelection> newValue) {
    selection.clear();
    selection.addAll(newValue);
  }

  public List<FeatureSelection> getValue() {
    return ImmutableList.copyOf(selection);
  }

  public void actionPerformed(ActionEvent event) {

    Object src = event.getSource();

    if (src == addButton) {
    }

    if (src == allButton) {
    }

    if (src == removeButton) {
    }

    if (src == clearButton) {

    }

  }

  public void setToolTipText(String toolTip) {
    selectionList.setTooltip(new Tooltip(toolTip));
  }

}
