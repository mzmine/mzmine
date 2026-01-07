/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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


package io.github.mzmine.modules.visualization.vankrevelendiagram;

import io.github.mzmine.main.MZmineCore;
import java.text.NumberFormat;
import java.util.Arrays;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

public class VanKrevelenDiagramBubbleLegend extends BorderPane {

  private final VanKrevelenDiagramXYZDataset vanKrevelenDiagramXYZDataset;

  public VanKrevelenDiagramBubbleLegend(VanKrevelenDiagramXYZDataset vanKrevelenDiagramXYZDataset) {
    this.vanKrevelenDiagramXYZDataset = vanKrevelenDiagramXYZDataset;
    init();
  }

  private void init() {
    double min = Arrays.stream(vanKrevelenDiagramXYZDataset.getBubbleSizeValues()).min().orElse(0.0);
    double max = Arrays.stream(vanKrevelenDiagramXYZDataset.getBubbleSizeValues()).max().orElse(0.0);
    GridPane gridPane = new GridPane();
    gridPane.setHgap(10);
    gridPane.setVgap(10);
    gridPane.setPadding(new Insets(10));
    Text valueType = new Text(vanKrevelenDiagramXYZDataset.getBubbleVanKrevelenDataType().getName());
    gridPane.add(valueType, 0, 0);
    GridPane.setHalignment(valueType, HPos.CENTER);
    GridPane.setValignment(valueType, VPos.CENTER);
    Circle smallBubble = createBubble(3);
    gridPane.add(smallBubble, 0, 1);
    GridPane.setHalignment(smallBubble, HPos.CENTER);
    GridPane.setValignment(smallBubble, VPos.CENTER);
    Text smaller = new Text("<");
    gridPane.add(smaller, 1, 1);
    GridPane.setHalignment(smaller, HPos.CENTER);
    GridPane.setValignment(smaller, VPos.CENTER);
    Circle bigBubble = createBubble(15);
    gridPane.add(bigBubble, 2, 1);
    GridPane.setHalignment(bigBubble, HPos.CENTER);
    GridPane.setValignment(bigBubble, VPos.CENTER);
    NumberFormat numberFormat = identifyNumberFormat(vanKrevelenDiagramXYZDataset.getBubbleVanKrevelenDataType());
    Text smallValue = new Text(numberFormat.format(min));
    gridPane.add(smallValue, 0, 2);
    GridPane.setHalignment(smallValue, HPos.CENTER);
    GridPane.setValignment(smallValue, VPos.CENTER);
    Text bigValue = new Text(numberFormat.format(max));
    gridPane.add(bigValue, 2, 2);
    GridPane.setHalignment(bigValue, HPos.CENTER);
    GridPane.setValignment(bigValue, VPos.CENTER);
    this.setCenter(gridPane);
  }

  private NumberFormat identifyNumberFormat(VanKrevelenDiagramDataTypes bubbleVanKrevelenDiagramDataTypes) {
    return switch (bubbleVanKrevelenDiagramDataTypes) {
      case MZ -> MZmineCore.getConfiguration().getMZFormat();
      case RETENTION_TIME, TAILING_FACTOR, ASYMMETRY_FACTOR, FWHM ->
          MZmineCore.getConfiguration().getRTFormat();
      case MOBILITY -> MZmineCore.getConfiguration().getMobilityFormat();
      case INTENSITY, AREA -> MZmineCore.getConfiguration().getIntensityFormat();
    };
  }

  private Circle createBubble(int size) {
    Circle circle = new Circle(size);
    circle.setFill(MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColor());
    return circle;
  }
}
