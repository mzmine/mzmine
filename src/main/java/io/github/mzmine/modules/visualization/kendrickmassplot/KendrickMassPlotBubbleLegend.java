package io.github.mzmine.modules.visualization.kendrickmassplot;

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

public class KendrickMassPlotBubbleLegend extends BorderPane {

  private final KendrickMassPlotXYZDataset kendrickMassPlotXYZDataset;

  public KendrickMassPlotBubbleLegend(KendrickMassPlotXYZDataset kendrickMassPlotXYZDataset) {
    this.kendrickMassPlotXYZDataset = kendrickMassPlotXYZDataset;
    init();
  }

  private void init() {
    double min = Arrays.stream(kendrickMassPlotXYZDataset.getBubbleSizeValues()).min().orElse(0.0);
    double max = Arrays.stream(kendrickMassPlotXYZDataset.getBubbleSizeValues()).max().orElse(0.0);
    GridPane gridPane = new GridPane();
    gridPane.setHgap(10);
    gridPane.setVgap(10);
    gridPane.setPadding(new Insets(10));
    Text valueType = new Text(
        kendrickMassPlotXYZDataset.getBubbleKendrickDataType().getName());
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
    NumberFormat numberFormat = identifyNumberFormat(
        kendrickMassPlotXYZDataset.getBubbleKendrickDataType());
    Text smallValue = new Text(
        numberFormat.format(min));
    gridPane.add(smallValue, 0, 2);
    GridPane.setHalignment(smallValue, HPos.CENTER);
    GridPane.setValignment(smallValue, VPos.CENTER);
    Text bigValue = new Text(
        numberFormat.format(max));
    gridPane.add(bigValue, 2, 2);
    GridPane.setHalignment(bigValue, HPos.CENTER);
    GridPane.setValignment(bigValue, VPos.CENTER);
    this.setCenter(gridPane);
  }

  private NumberFormat identifyNumberFormat(KendrickPlotDataTypes bubbleKendrickDataType) {
    switch (bubbleKendrickDataType) {
      case M_OVER_Z, KENDRICK_MASS -> {
        return MZmineCore.getConfiguration().getMZFormat();
      }
      case KENDRICK_MASS_DEFECT, REMAINDER_OF_KENDRICK_MASS, RETENTION_TIME, TAILING_FACTOR, ASYMMETRY_FACTOR, FWHM -> {
        return MZmineCore.getConfiguration().getRTFormat();
      }
      case MOBILITY -> {
        return MZmineCore.getConfiguration().getMobilityFormat();
      }
      case INTENSITY, AREA -> {
        return MZmineCore.getConfiguration().getIntensityFormat();
      }
    }
    return MZmineCore.getConfiguration().getRTFormat();
  }

  private Circle createBubble(int size) {
    Circle circle = new Circle(size);
    circle.setFill(MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColor());
    return circle;
  }
}
