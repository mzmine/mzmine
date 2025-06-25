/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;

public class KendrickMassPlotSetupDialog extends ParameterSetupDialog {

  private static final Logger logger = Logger.getLogger(
      KendrickMassPlotSetupDialog.class.getName());

  private final ListView<RepeatingUnit> listView;

  public KendrickMassPlotSetupDialog(boolean valueCheckRequired, ParameterSet parameters,
      Region message) {
    super(valueCheckRequired, parameters, message);
    listView = new ListView<>();
    listView.setPrefHeight(300);
    this.getParamsPane().addColumn(2);
    this.getParamsPane().add(new Label("Suggested repeating units:"), 2, 0);
    this.getParamsPane().add(new BorderPane(listView), 2, 1, 1, 8);
    addSuggestedRepeatingUnits();
  }

  private void addSuggestedRepeatingUnits() {
    ModularFeatureList[] matchingFeatureLists = parameterSet.getParameter(
        KendrickMassPlotParameters.featureList).getValue().getMatchingFeatureLists();
    ModularFeatureList matchingFeatureList;
    if (matchingFeatureLists.length > 0) {
      matchingFeatureList = parameterSet.getParameter(KendrickMassPlotParameters.featureList)
          .getValue().getMatchingFeatureLists()[0];
    } else {
      logger.log(Level.WARNING, "No feature list selected");
      return;
    }

    // run prediction and add results to view on fx thread
    RepeatingUnitSuggester.createOnNewThread(matchingFeatureList,
        result -> listView.setItems(FXCollections.observableList(result)));
  }

}
