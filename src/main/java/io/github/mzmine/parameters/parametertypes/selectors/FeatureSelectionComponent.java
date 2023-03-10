/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.parameters.parametertypes.selectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.ranges.IntRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.RangeUtils;
import java.awt.event.ActionEvent;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import org.jetbrains.annotations.Nullable;

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

  void setValue(@Nullable List<FeatureSelection> newValue) {
    selection.clear();
    if (newValue == null) {
      return;
    }
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
