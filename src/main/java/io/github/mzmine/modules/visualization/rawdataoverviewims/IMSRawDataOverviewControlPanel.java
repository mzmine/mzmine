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

package io.github.mzmine.modules.visualization.rawdataoverviewims;

import com.google.common.collect.Range;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.parametertypes.DoubleComponent;
import io.github.mzmine.parameters.parametertypes.IntegerComponent;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeComponent;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionComponent;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceComponent;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Rectangle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IMSRawDataOverviewControlPanel extends GridPane {

  public static final String TOOLTIP_MOBILITYSCAN_NL = "Noise level for mobility scan processing.\n"
      + "Greatly impacts performance of this overview.\n Influences mobilogram, ion trace building "
      + "and the frame overview heatmap.";

  public static final String TOOLTIP_FRAME_NL =
      "Noise level for frame processing. Influences EIC" + " building and frame chart.";

  public static final String TOOLTIP_MZTOL =
      "m/z tolerance for EIC, ion trace and mobilogram " + "building";

  public static final String TOOLTIP_SCANSEL =
      "Scan selection for EIC, ion trace and mobilogram " + "building";

  public static final String TOOLTIP_RTRANGE =
      "Retention time range around the selected m/z to " + "build EICs and ion traces.";

  public static final String TOOLTIP_BINWIDTH = "Bin width in for mobility dimension to build "
      + "mobilograms.\nAutomatically set to a multiple of the actual acquisition step size.";


  private final IMSRawDataOverviewPane pane;
  private final NumberFormat mzFormat;
  private final NumberFormat intensityFormat;
  private final NumberFormat rtFormat;
  private final NumberFormat mobilityFormat;

  private MZTolerance mzTolerance;
  private ScanSelection scanSelection;
  private Float rtWidth;
  private Integer binWidth;
  private ListView<Range<Double>> mobilogramRangesList;

  private double frameNoiseLevel;
  private double mobilityScanNoiseLevel;
  private DoubleRangeComponent mobilogramRangeComp;

  IMSRawDataOverviewControlPanel(IMSRawDataOverviewPane pane, double frameNoiseLevel,
      double mobilityScanNoiseLevel, MZTolerance mzTolerance, ScanSelection scanSelection,
      Float rtWidth, Integer binWidth) {
    this.pane = pane;
    this.frameNoiseLevel = frameNoiseLevel;
    this.mobilityScanNoiseLevel = mobilityScanNoiseLevel;
    this.mzTolerance = mzTolerance;
    this.scanSelection = scanSelection;
    this.rtWidth = rtWidth;
    this.binWidth = binWidth;
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mobilityFormat = new DecimalFormat("0.0000");
    initControlPanel();
  }

  private void initControlPanel() {
    DoubleComponent frameNoiseLevelComponent = new DoubleComponent(100, 0d, Double.MAX_VALUE,
        intensityFormat, frameNoiseLevel);
    frameNoiseLevelComponent.setText(intensityFormat.format(frameNoiseLevel));
    DoubleComponent mobilityScanNoiseLevelComponent = new DoubleComponent(100, 0d, Double.MAX_VALUE,
        intensityFormat, mobilityScanNoiseLevel);
    mobilityScanNoiseLevelComponent.setText(intensityFormat.format(mobilityScanNoiseLevel));
    MZToleranceComponent mzToleranceComponent = new MZToleranceComponent();
    mzToleranceComponent.setValue(mzTolerance);
    DoubleComponent rtWidthComponent = new DoubleComponent(100, 0d, Double.MAX_VALUE, rtFormat, 2d);
    ScanSelectionComponent scanSelectionComponent = new ScanSelectionComponent();
    scanSelectionComponent.setValue(scanSelection);
    IntegerComponent binWidthComponent = new IntegerComponent(100, 1, 10);
    binWidthComponent.setText(binWidth.toString());

    setPadding(new Insets(5));
    setVgap(5);
    getColumnConstraints().addAll(new ColumnConstraints(150), new ColumnConstraints());
    Label lblMobilityScanNoiseLevel = new Label("Mobility scan noise level");
    lblMobilityScanNoiseLevel.setTooltip(new Tooltip(TOOLTIP_MOBILITYSCAN_NL));
    add(lblMobilityScanNoiseLevel, 0, 0);
    add(mobilityScanNoiseLevelComponent, 1, 0);
    Label lblFrameNoiseLevel = new Label("Frame noise level");
    lblFrameNoiseLevel.setTooltip(new Tooltip(TOOLTIP_FRAME_NL));
    add(lblFrameNoiseLevel, 0, 1);
    add(frameNoiseLevelComponent, 1, 1);
    Label lblMzTol = new Label("m/z tolerance");
    lblMzTol.setTooltip(new Tooltip(TOOLTIP_MZTOL));
    add(lblMzTol, 0, 2);
    add(mzToleranceComponent, 1, 2);
    Label lblScanSel = new Label("Scan selection");
    lblScanSel.setTooltip(new Tooltip(TOOLTIP_SCANSEL));
    add(lblScanSel, 0, 3);
    add(scanSelectionComponent, 1, 3);
    Label lblRtRange = new Label("Retention time width");
    lblRtRange.setTooltip(new Tooltip(TOOLTIP_RTRANGE));
    add(lblRtRange, 0, 4);
    add(rtWidthComponent, 1, 4);
    Label lblBinWidth = new Label("Mobilogram bin width (abs)");
    lblBinWidth.setTooltip(new Tooltip(TOOLTIP_BINWIDTH));
    add(lblBinWidth, 0, 5);
    add(binWidthComponent, 1, 5);

    mobilogramRangeComp = createMobilogramRangeComp(mzToleranceComponent);

    mobilogramRangesList = new ListView<>(FXCollections.observableArrayList());
    mobilogramRangesList.setMaxHeight(150);
    mobilogramRangesList.setMaxWidth(240);
    mobilogramRangesList.setPrefWidth(240);
    mobilogramRangesList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    Button addMzRange = new Button("Add range");
    addMzRange.setOnAction(e -> {
      Range<Double> range = mobilogramRangeComp.getValue();
      if (range == null) {
        return;
      }
      mobilogramRangesList.getItems().add(range);
    });

    Button removeMzRange = new Button("Remove range");
    removeMzRange.setOnAction(e -> mobilogramRangesList.getItems()
        .remove(mobilogramRangesList.getSelectionModel().getSelectedItem()));
    mobilogramRangesList.setCellFactory(param -> new ListCell<>() {
      @Override
      protected void updateItem(Range<Double> item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
          setGraphic(null);
          return;
        }
        setText(
            mzFormat.format(item.lowerEndpoint()) + " - " + mzFormat.format(item.upperEndpoint()));
        SimpleColorPalette colors = MZmineCore.getConfiguration().getDefaultColorPalette().clone();
        colors.remove(pane.getRawDataFile().getColor());
        setGraphic(new Rectangle(10, 10, colors.get(getMobilogramRangesList().indexOf(item))));
      }
    });

    Button update = new Button("Update");
    update.setOnAction(e -> {
      try {
        frameNoiseLevel = Double.parseDouble(frameNoiseLevelComponent.getText());
        mobilityScanNoiseLevel = Double.parseDouble(mobilityScanNoiseLevelComponent.getText());
        frameNoiseLevelComponent.setText(intensityFormat.format(frameNoiseLevel));
        mobilityScanNoiseLevelComponent.setText(intensityFormat.format(mobilityScanNoiseLevel));
        rtWidth = Float.parseFloat(rtWidthComponent.getText());
        rtWidthComponent.setText(rtFormat.format(rtWidth));
        scanSelection = scanSelectionComponent.getValue();
        mzTolerance = mzToleranceComponent.getValue();
        binWidth = Integer.parseInt(binWidthComponent.getText());

        pane.setMzTolerance(mzTolerance);
        pane.setScanSelection(scanSelection);
        pane.setFrameNoiseLevel(frameNoiseLevel);
        pane.setMobilityScanNoiseLevel(mobilityScanNoiseLevel);
        pane.setRtWidth(rtWidth);
        pane.setBinWidth(binWidth);

        pane.updateTicPlot();
        pane.onSelectedFrameChanged();
      } catch (NullPointerException | NumberFormatException ex) {
        ex.printStackTrace();
      }
    });

    add(new Label("EIC/Mobilogram ranges"), 0, 10);
    add(mobilogramRangesList, 1, 10, 1, 1);
    add(new Label("Range:"), 0, 11);
    add(mobilogramRangeComp, 1, 11);
    FlowPane buttons = new FlowPane(addMzRange, removeMzRange, update);
    buttons.setHgap(5);
    buttons.setAlignment(Pos.CENTER);
    add(buttons, 0, 12, 2, 1);
  }

  @NotNull
  private DoubleRangeComponent createMobilogramRangeComp(
      MZToleranceComponent mzToleranceComponent) {
    return new DoubleRangeComponent(mzFormat) {
      @Override
      public Range<Double> getValue() {
        String minString = minTxtField.getText();
        String maxString = maxTxtField.getText();

        Number minValue = null;
        Number maxValue = null;
        try {
          minValue = format.parse(minString.trim());
          maxValue = format.parse(maxString.trim());
        } catch (Exception e) {
          logger.info(e.toString());
        }
        if (minValue != null && maxValue != null) {
          return Range.closed(minValue.doubleValue(), maxValue.doubleValue());
        } else if (minValue != null) {
          return mzToleranceComponent.getValue().getToleranceRange(minValue.doubleValue());
        } else if (maxValue != null) {
          return mzToleranceComponent.getValue().getToleranceRange(maxValue.doubleValue());
        }
        return null;

      }
    };
  }

  public MZTolerance getMzTolerance() {
    return mzTolerance;
  }

  public void setMzTolerance(MZTolerance mzTolerance) {
    this.mzTolerance = mzTolerance;
  }

  public ScanSelection getScanSelection() {
    return scanSelection;
  }

  public void setScanSelection(ScanSelection scanSelection) {
    this.scanSelection = scanSelection;
  }

  public List<Range<Double>> getMobilogramRangesList() {
    return mobilogramRangesList.getItems();
  }

  public void addRanges(List<Range<Double>> rangeList) {
    rangeList.forEach(this::addRange);
  }

  public void addRange(@Nullable Range<Double> range) {
    if (range != null) {
      mobilogramRangesList.getItems().add(range);
    }
  }

  public void addSelectedRangeListener(ChangeListener<Range<Double>> listener) {
    mobilogramRangesList.getSelectionModel().selectedItemProperty().addListener(listener);
  }

  public double getFrameNoiseLevel() {
    return frameNoiseLevel;
  }

  public void setFrameNoiseLevel(double frameNoiseLevel) {
    this.frameNoiseLevel = frameNoiseLevel;
  }

  public double getMobilityScanNoiseLevel() {
    return mobilityScanNoiseLevel;
  }

  public void setMobilityScanNoiseLevel(double mobilityScanNoiseLevel) {
    this.mobilityScanNoiseLevel = mobilityScanNoiseLevel;
  }

  public void setRangeToMobilogramRangeComp(@Nullable Range<Double> range) {
    if (range != null) {
      mobilogramRangeComp.setValue(range);
    }
  }
}
