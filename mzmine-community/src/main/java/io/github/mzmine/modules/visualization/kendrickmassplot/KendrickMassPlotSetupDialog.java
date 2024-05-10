/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.kendrickmassplot;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.util.ExitCode;
import java.util.Map;
import java.util.Objects;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class KendrickMassPlotSetupDialog extends ParameterSetupDialog {

  private final VBox vbox;
  private Timeline autoUpdateTimeline;

  public KendrickMassPlotSetupDialog(boolean valueCheckRequired, ParameterSet parameters,
      Region message) {
    super(valueCheckRequired, parameters, message);
    vbox = new VBox();
    vbox.getChildren()
        .add(new ListView<>(FXCollections.observableArrayList("Calculating repeating units")));
    RepeatingUnitSuggester repeatingUnitSuggester = new RepeatingUnitSuggester(
        parameterSet.getParameter(KendrickMassPlotParameters.featureList).getValue()
            .getMatchingFeatureLists()[0]);
    vbox.getChildren().clear();
    vbox.getChildren().add(repeatingUnitSuggester.getListView());
    this.getParamsPane().addColumn(2);
    this.getParamsPane().add(new Label("Suggested repeating units:"), 2, 0);
    this.getParamsPane().add(vbox, 2, 1, 1, 8);
    setupAutoUpdate();
  }

  private void setupAutoUpdate() {
    autoUpdateTimeline = new Timeline(
        new KeyFrame(Duration.seconds(0.5), e -> parametersChanged()));
    autoUpdateTimeline.setCycleCount(Timeline.INDEFINITE); // Make it run continuously
    autoUpdateTimeline.play(); // Start the timeline
  }

  @Override
  public void closeDialog(ExitCode exitCode) {
    if (autoUpdateTimeline != null) {
      autoUpdateTimeline.stop();  // Stop the timeline to avoid memory leaks
      autoUpdateTimeline = null;
    }
    vbox.getChildren().clear();
    super.closeDialog(exitCode);
  }

  @Override
  protected void parametersChanged() {
    ParameterSet oldParameterSet = parameterSet.cloneParameterSet();
    ParameterUtils.copyParameters(parameterSet, oldParameterSet);
    super.parametersChanged();
    updateParameterSetFromComponents();
    if (!ParameterUtils.equalValues(oldParameterSet, parameterSet)) {
      addSuggestedRepeatingUnits();
    }
  }

  private void addSuggestedRepeatingUnits() {
    RepeatingUnitSuggester repeatingUnitSuggester = new RepeatingUnitSuggester(
        parameterSet.getParameter(KendrickMassPlotParameters.featureList).getValue()
            .getMatchingFeatureLists()[0]);
    Task<ObservableList<String>> loadTask = repeatingUnitSuggester.getLoadItemsTask();
    loadTask.setOnSucceeded(e -> {
      ListView<String> newListView = repeatingUnitSuggester.getListView();
      if (!vbox.getChildren().isEmpty() && vbox.getChildren().get(0) instanceof ListView) {
        ListView<String> oldListView = (ListView<String>) vbox.getChildren().get(0);
        if (!areListViewsEqual(oldListView, newListView)) {
          vbox.getChildren().setAll(newListView);
        }
      } else {
        vbox.getChildren().setAll(newListView);
      }
    });

    if (!loadTask.isRunning()) {
      repeatingUnitSuggester.loadItems(); // Load items if not already loading/loaded
    }
  }


  private boolean areListViewsEqual(ListView<String> oldListView, ListView<String> newListView) {
    ObservableList<String> oldItems = oldListView.getItems();
    ObservableList<String> newItems = newListView.getItems();

    // Check if both lists are of the same size
    if (oldItems.size() != newItems.size()) {
      return false;
    }

    // Compare each element
    for (int i = 0; i < oldItems.size(); i++) {
      if (!oldItems.get(i).equals(newItems.get(i))) {
        return false; // Return false at the first instance of non-equal elements
      }
    }

    return true; // If all elements are the same
  }

  /**
   * Compares two ParameterSets for equality.
   *
   * @param ps1 the first ParameterSet
   * @param ps2 the second ParameterSet
   * @return true if both ParameterSets contain the same parameters with the same values.
   */
  private boolean areParameterSetsEqual(ParameterSet ps1, ParameterSet ps2) {
    // Early exit if both references are the same
    if (ps1 == ps2) {
      return true;
    }
    // Check if either is null
    if (ps1 == null || ps2 == null) {
      return false;
    }

    // Retrieve parameters from both sets
    Map<String, Parameter<?>> params1 = ps1.getNameParameterMap();
    Map<String, Parameter<?>> params2 = ps2.getNameParameterMap();

    // Check if they have the same number of parameters
    if (params1.size() != params2.size()) {
      return false;
    }

    // Compare each parameter
    for (Map.Entry<String, Parameter<?>> entry : params1.entrySet()) {
      Parameter<?> param1 = entry.getValue();
      Parameter<?> param2 = params2.get(entry.getKey());

      // Check if parameter exists in both sets
      if (param2 == null) {
        return false;
      }

      // Check if values of the parameters are the same
      if (!Objects.equals(param1.getValue(), param2.getValue())) {
        return false;
      }
    }

    // All checks passed, the ParameterSets are equal
    return true;
  }
}
