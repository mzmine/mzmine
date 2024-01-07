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

package io.github.mzmine.modules.dataprocessing.featdet_manual;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.chromatogram.TICDataSet;
import io.github.mzmine.modules.visualization.chromatogram.TICPlot;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeComponent;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.javafx.FxColorUtil;
import io.github.mzmine.util.javafx.FxIconUtil;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;

public class XICManualPickerDialog extends ParameterSetupDialog {

  private static final long serialVersionUID = 1L;

  private static final Logger logger = Logger.getLogger(XICManualPickerDialog.class.getName());

  private static final Image icoLower = FxIconUtil.loadImageFromResources(
      "icons/integration_lowerboundary.png");
  private static final Image icoUpper = FxIconUtil.loadImageFromResources(
      "icons/integration_upperboundary.png");
  private final ModularFeatureList flist;

  protected Range<Double> mzRange, rtRange;
  protected RawDataFile rawDataFile;
  protected ParameterSet parameters;
  protected DoubleRangeComponent rtRangeComp;
  protected DoubleRangeComponent mzRangeComp;
  // protected JComponent origRangeComp;

  protected double lower, upper;

  protected TICDataSet dataSet;

  protected Button setLower, setUpper;
  protected TextField txtArea;

  protected NumberFormat intensityFormat, mzFormat;
  protected InputSource inputSource;

  protected NextBorder nextBorder;
  // XYPlot
  private TICPlot ticPlot;

  public XICManualPickerDialog(boolean valueCheckRequired, ParameterSet parameters) {
    super(valueCheckRequired, parameters);

    Color l = MZmineCore.getConfiguration().getDefaultColorPalette().get(1);
    Color u = MZmineCore.getConfiguration().getDefaultColorPalette().get(2);
    Stroke stroke = new BasicStroke(1.0f);

    // make new panel, put tic into the middle of a border layout.
    // remove(this.paramsPane);

    rtRangeComp = new DoubleRangeComponent(MZmineCore.getConfiguration().getRTFormat());
    rtRangeComp.getMinTxtField().setOnKeyTyped(e -> updatePlot());
    rtRangeComp.getMaxTxtField().setOnKeyTyped(e -> updatePlot());
    mzRangeComp = new DoubleRangeComponent(MZmineCore.getConfiguration().getMZFormat());
    mzRangeComp.getMinTxtField().setOnAction(e -> onMzRangeChanged());
    mzRangeComp.getMaxTxtField().setOnAction(e -> onMzRangeChanged());

    Label rtLabel = new Label("Retention time range");
    paramsPane.add(rtLabel, 0, getNumberOfParameters() + 1);
    paramsPane.add(rtRangeComp, 1, getNumberOfParameters() + 1);
    paramsPane.add(new Label("m/z range"), 0, getNumberOfParameters() + 2);
    paramsPane.add(mzRangeComp, 1, getNumberOfParameters() + 2);

    BorderPane pnlNewMain = new BorderPane();

    // put another border layout for south of the new main panel, so we can place controls and
    // integration specific stuff there
    BorderPane pnlControlsAndParameters = new BorderPane();
    pnlControlsAndParameters.setCenter(this.paramsPane);
    pnlNewMain.setBottom(pnlControlsAndParameters);

    // now make another panel to put the integration specific stuff, like
    // the buttons and the current area
    FlowPane pnlIntegration = new FlowPane();

    ImageView startImage = new ImageView(icoLower);
    startImage.setPreserveRatio(true);
    startImage.setFitHeight(30);
    ImageView endImage = new ImageView(icoUpper);
    endImage.setPreserveRatio(true);
    endImage.setFitHeight(30);

    setLower = new Button(null, startImage);
    setLower.setTooltip(new Tooltip("Set the lower integration boundary."));
    setLower.setOnAction(e -> {
      nextBorder = NextBorder.LOWER;
      setButtonBackground();
    });

    setUpper = new Button(null, endImage);
    setUpper.setTooltip(new Tooltip("Set the upper integration boundary."));
    setUpper.setOnAction(e -> {
      nextBorder = NextBorder.UPPER;
      setButtonBackground();
    });

    txtArea = new TextField();
    txtArea.setPrefColumnCount(10);
    txtArea.setEditable(false);
    pnlIntegration.getChildren()
        .addAll(setLower, setUpper, new Separator(), new Label("Area: "), txtArea);

    pnlControlsAndParameters.setTop(pnlIntegration);

    ticPlot = new TICPlot();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    pnlNewMain.setCenter(ticPlot);

    // add a mouse listener to place the boundaries
    getTicPlot().addChartMouseListener(new ChartMouseListenerFX() {

      // https://stackoverflow.com/questions/1512112/jfreechart-get-mouse-coordinates
      @Override // draw a marker at the current position
      public void chartMouseMoved(ChartMouseEventFX event) {
        Point2D screenPoint = new Point2D(event.getTrigger().getScreenX(),
            event.getTrigger().getScreenY());
        Point2D p = ticPlot.screenToLocal(screenPoint);
        Rectangle2D plotArea = getTicPlot().getCanvas().getRenderingInfo().getPlotInfo()
            .getDataArea();
        XYPlot plot = ticPlot.getXYPlot();
        double rtValue = plot.getDomainAxis()
            .java2DToValue(p.getX(), plotArea, plot.getDomainAxisEdge());

        java.awt.Color clr = FxColorUtil.fxColorToAWT((nextBorder == NextBorder.LOWER) ? l : u);
        addMarkers();
        plot.addDomainMarker(new ValueMarker(rtValue, clr, stroke));
      }

      @Override
      public void chartMouseClicked(ChartMouseEventFX event) {
        Point2D screenPoint = new Point2D(event.getTrigger().getScreenX(),
            event.getTrigger().getScreenY());
        Point2D p = ticPlot.screenToLocal(screenPoint);
        Rectangle2D plotArea = getTicPlot().getCanvas().getRenderingInfo().getPlotInfo()
            .getDataArea();

        XYPlot plot = ticPlot.getXYPlot();
        double rtValue = plot.getDomainAxis()
            .java2DToValue(p.getX(), plotArea, plot.getDomainAxisEdge());

        inputSource = InputSource.GRAPH;
        setRTBoundary(rtValue);
        inputSource = InputSource.OTHER;
      }
    });

    mainPane.setRight(pnlNewMain);

    nextBorder = NextBorder.LOWER;
    inputSource = InputSource.OTHER;

    this.parameters = parameters;
    mzRange = parameters.getParameter(XICManualPickerParameters.mzRange).getValue();
    rtRange = parameters.getParameter(XICManualPickerParameters.rtRange).getValue();
    flist = parameters.getParameter(XICManualPickerParameters.flists).getValue()
        .getMatchingFeatureLists()[0];
    rawDataFile = parameters.getParameter(XICManualPickerParameters.rawDataFiles).getValue()
        .getMatchingRawDataFiles()[0];

    ScanSelection sel = new ScanSelection(1, rawDataFile.getDataRTRange());
    Scan[] scans = sel.getMatchingScans(rawDataFile);
    dataSet = new TICDataSet(rawDataFile, scans, mzRange, null);

    getTicPlot().addTICDataSet(dataSet);
    getTicPlot().setPlotType(TICPlotType.BASEPEAK);

    lower = rtRange.lowerEndpoint();
    upper = rtRange.upperEndpoint();
    getTicPlot().getXYPlot()
        .addDomainMarker(new ValueMarker(lower, java.awt.Color.GREEN, new BasicStroke(1.0f)));
    getTicPlot().getXYPlot()
        .addDomainMarker(new ValueMarker(upper, java.awt.Color.RED, new BasicStroke(1.0f)));

    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();

    rtRangeComp.setValue(rtRange);
    mzRangeComp.setValue(mzRange);
    setButtonBackground();
    calcArea();
  }

  private void onMzRangeChanged() {
    mzRange = mzRangeComp.getValue();
    ticPlot.removeAllDataSets();
    parameters.getParameter(XICManualPickerParameters.mzRange).setValue(mzRange);

    ScanSelection sel = new ScanSelection(1, rawDataFile.getDataRTRange());
    Scan[] scans = sel.getMatchingScans(rawDataFile);
    dataSet = new TICDataSet(rawDataFile, scans, mzRange, null);
    ticPlot.addTICDataSet(dataSet);
  }

  private void setRTBoundary(double rt) {
    if (rt <= rtRange.lowerEndpoint() || nextBorder == NextBorder.LOWER) {
      lower = rt;
      nextBorder = NextBorder.UPPER;
    } else if (nextBorder == NextBorder.UPPER && rt >= lower) {
      upper = rt;
      nextBorder = NextBorder.LOWER;
    }
    setButtonBackground();
    // use addMarkers() once here, to visually accept invalid inputs, they might be corrected later on.
    addMarkers();
    checkRanges();
    rtRangeComp.setValue(rtRange);
    calcArea();
    setValuesToRangeParameter();
  }

  private boolean checkRanges() {
    if (upper > lower) {
      rtRange = Range.closed(lower, upper);
      return true;
    }
    return false;
  }

  private void setValuesToRangeParameter() {
    if (!checkRanges() || rtRange == null) {
      return;
    }
    parameters.getParameter(XICManualPickerParameters.rtRange).setValue(rtRange);
    parameters.getParameter(XICManualPickerParameters.mzRange).setValue(mzRange);
  }

  @Override
  public void parametersChanged() {
  }

  private void addMarkers() {
    getTicPlot().getXYPlot().clearDomainMarkers();
    getTicPlot().getXYPlot()
        .addDomainMarker(new ValueMarker(lower, java.awt.Color.GREEN, new BasicStroke(1.0f)));
    getTicPlot().getXYPlot()
        .addDomainMarker(new ValueMarker(upper, java.awt.Color.RED, new BasicStroke(1.0f)));
  }

  private void calcArea() {
    if (!checkRanges()) {
      return;
    }

    Task integration = new AbstractTask(null, Instant.now()) {

      @Override
      public void run() {
        setStatus(TaskStatus.PROCESSING);
        double area = FeatureUtils.integrateOverMzRtRange(rawDataFile,
            (List<Scan>) flist.getSeletedScans(rawDataFile), RangeUtils.toFloatRange(rtRange),
            mzRange);
        Platform.runLater(() -> txtArea.setText(intensityFormat.format(area)));
        setStatus(TaskStatus.FINISHED);
      }

      @Override
      public String getTaskDescription() {
        return "Manual integration of m/z " + mzFormat.format(
            (mzRange.lowerEndpoint() + mzRange.upperEndpoint()) / 2);
      }

      @Override
      public double getFinishedPercentage() {
        return 0;
      }
    };

    MZmineCore.getTaskController().addTask(integration);
  }

  public TICPlot getTicPlot() {
    return ticPlot;
  }

  public void setTicPlot(TICPlot ticPlot) {
    this.ticPlot = ticPlot;
  }

  private void setButtonBackground() {
    if (nextBorder == NextBorder.UPPER) {
      setLower.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
      setUpper.setBackground(new Background(new BackgroundFill(Color.RED, null, null)));
    } else {
      setLower.setBackground(new Background(new BackgroundFill(Color.GREEN, null, null)));
      setUpper.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
    }
  }

  public void updatePlot() {
    // logger.info(event.getType().toString() + " source: " +
    // inputSource.toString());
    parametersChanged();
    if (inputSource == InputSource.OTHER && checkRtComponentValue()) {
      rtRange = rtRangeComp.getValue();
      lower = rtRange.lowerEndpoint();
      upper = rtRange.upperEndpoint();
      addMarkers();
      setValuesToRangeParameter();
      calcArea();
    }
  }

  private boolean checkRtComponentValue() {
    Range<Double> value = rtRangeComp.getValue();
    if (value == null) {
      return false;
    }
    if (!value.hasLowerBound() || !value.hasUpperBound()) {
      return false;
    }

    if (value != null) {
      if (value.lowerEndpoint() > value.upperEndpoint()) {
        return false;
      }
      return value.lowerEndpoint() < value.upperEndpoint();
    }
    return true;
  }


  public enum NextBorder {
    LOWER, UPPER
  }


  private enum InputSource {
    OTHER, GRAPH
  }


}
