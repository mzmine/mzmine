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
