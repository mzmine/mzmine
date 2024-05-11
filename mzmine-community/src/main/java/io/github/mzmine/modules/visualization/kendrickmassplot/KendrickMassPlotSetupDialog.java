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

import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class KendrickMassPlotSetupDialog extends ParameterSetupDialog {

  private final VBox vbox;
  private final Button upatedBotton;

  public KendrickMassPlotSetupDialog(boolean valueCheckRequired, ParameterSet parameters,
      Region message) {
    super(valueCheckRequired, parameters, message);
    upatedBotton = new Button("Calculate");
    //upatedBotton.setOnAction(_ -> addSuggestedRepeatingUnits());
    vbox = new VBox();
    vbox.getChildren()
        .add(new ListView<>(FXCollections.observableArrayList("Calculating repeating units")));
    this.getParamsPane().addColumn(2);
    this.getParamsPane().add(new Label("Suggested repeating units:"), 2, 0);
    this.getParamsPane().add(upatedBotton, 2, 1);
    this.getParamsPane().add(vbox, 2, 2, 1, 7);
    addSuggestedRepeatingUnits();
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

}
