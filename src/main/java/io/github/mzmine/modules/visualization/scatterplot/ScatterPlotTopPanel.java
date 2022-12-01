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

package io.github.mzmine.modules.visualization.scatterplot;

import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.visualization.scatterplot.scatterplotchart.ScatterPlotDataSet;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class ScatterPlotTopPanel extends HBox {

  private Label itemNameLabel, numOfDisplayedItems;

  public ScatterPlotTopPanel() {

    itemNameLabel = new Label();
    itemNameLabel.setStyle("-fx-text-fill: blue; -fx-font-weight: bold; -fx-font-size: 15px");

    numOfDisplayedItems = new Label();
    // numOfDisplayedItems.setFont(new Font("SansSerif", Font.PLAIN, 10));

    getChildren().addAll(numOfDisplayedItems, itemNameLabel);
    // add(Box.createHorizontalGlue());


  }

  public void updateNumOfItemsText(FeatureList featureList, ScatterPlotDataSet dataSet,
      ScatterPlotAxisSelection axisX, ScatterPlotAxisSelection axisY, int fold) {

    int percentage = 100;

    int totalItems = dataSet.getItemCount(0);

    int itemsWithinFold = 0;
    double x, y, ratio;
    final double thresholdMax = fold, thresholdMin = (1.0 / fold);

    for (int i = 0; i < totalItems; i++) {
      x = dataSet.getXValue(0, i);
      y = dataSet.getYValue(0, i);
      ratio = x / y;
      if ((ratio >= thresholdMin) && (ratio <= thresholdMax))
        itemsWithinFold++;
    }

    ratio = ((double) itemsWithinFold / totalItems);

    percentage = (int) Math.round(ratio * 100);

    String display = "<html><b>" + dataSet.getItemCount(0) + "</b> peaks displayed, <b>"
        + percentage + "%</b> within <b>" + fold + "-fold</b> margin";

    // If we have a selection, show it
    if (dataSet.getSeriesCount() > 1) {

      itemsWithinFold = 0;
      totalItems = dataSet.getItemCount(1);

      for (int i = 0; i < totalItems; i++) {
        x = dataSet.getXValue(1, i);
        y = dataSet.getYValue(1, i);
        ratio = x / y;
        if ((ratio >= thresholdMin) && (ratio <= thresholdMax))
          itemsWithinFold++;
      }

      ratio = ((double) itemsWithinFold / totalItems);

      percentage = (int) Math.round(ratio * 100);

      display += " (" + percentage + "% of selected)";
    }

    display += "</html>";

    numOfDisplayedItems.setText(display);
  }

  public void updateItemNameText(FeatureListRow selectedRow) {

    if (selectedRow == null) {
      itemNameLabel.setText("");
      return;
    }

    FeatureIdentity identity = selectedRow.getPreferredFeatureIdentity();
    String itemName;
    if (identity != null) {
      itemName = identity.getName();
    } else {
      itemName = selectedRow.toString();
    }

    itemNameLabel.setText(itemName);
  }

}
