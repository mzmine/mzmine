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

package io.github.mzmine.datamodel.features.types.graphicalnodes;

import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Entity;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Event;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.GestureButton;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureHandler;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonTimeSeriesToXYProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.chromatogram.ChromatogramVisualizerModule;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.RangeUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableRow;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;

/**
 * Generates a chart at creation with a preferred size set. updateItem sets the datasets in one call
 * so that there is only one call to {@link JFreeChart#fireChartChanged()} and drawing the chart.
 * The first cell seems to be the measurement cell and is never updated here as there are many calls
 * to update item on cell 0 and just single calls on all other cells.
 */
public class ChromatogramFeatureShapeCell extends TreeTableCell<ModularFeatureListRow, Object> {

  private static final Logger logger = Logger.getLogger(
      ChromatogramFeatureShapeCell.class.getName());

  private final SimpleXYChart<IonTimeSeriesToXYProvider> plot;
  private final Region view;
  private final int id;

  public ChromatogramFeatureShapeCell(int id) {
    super();
    this.id = id;
    setMinWidth(GraphicalColumType.LARGE_GRAPHICAL_CELL_WIDTH);
    setMinHeight(GraphicalColumType.DEFAULT_GRAPHICAL_CELL_HEIGHT);
    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

    plot = createChart();
    // use stackpane as it is transparent / borderpane is not
    view = new StackPane(plot);

    graphicProperty().bind(emptyProperty().map(empty -> empty ? null : view));
  }

  private String getIdentifier(String action) {
    final TreeTableRow<ModularFeatureListRow> row = getTableRow();
    return "Chromatogram Cell %d action: %s (%s %s row %s)".formatted(id, action, isEmpty(),
        getItem(), row == null ? "no table row" : row.getItem());
  }

  @Override
  protected void updateItem(Object o, boolean visible) {
    // always need to call super.updateItem
    super.updateItem(o, visible);

    final ModularFeatureListRow row = getTableRow().getItem();
    // first cell with id 0 seems to be just the measuring cell - no need to put content.
    // the chart itself already helps measurements
    if (id == 0) {
      return;
    }
    if (row == null || isEmpty()) {
      plot.removeAllDatasets();
      return;
    }

    // clear zoom history because it comes from old data
    plot.getZoomHistory().clear();

    // debugging of updates
//    logger.info(getIdentifier("updateItem"));

    // for selecting a range
    com.google.common.collect.Range<Float> featureRTRange = null;
    Float maxHeight = null;

    //
    int size = row.getFilesFeatures().size();
    ArrayList<ColoredXYDataset> datasets = new ArrayList<>(size);
    for (ModularFeature f : row.getFeatures()) {
      if (f.getRawDataFile() instanceof ImagingRawDataFile) {
        continue;
      }
      maxHeight = MathUtils.max(f.getHeight(), maxHeight);

//        fwhm = MathUtils.max(fwhm, f.getFWHM());
      featureRTRange = RangeUtils.span(featureRTRange, f.getRawDataPointsRTRange());

      IonTimeSeries<? extends Scan> dpSeries = f.getFeatureData();
      if (dpSeries != null) {
        // IonTimeSeriesToXYProvider is already precomputed and can be set on FXThread directly
        // therefore no need for caching and other thread
        ColoredXYDataset dataset = new ColoredXYDataset(new IonTimeSeriesToXYProvider(f),
            RunOption.THIS_THREAD);
        datasets.add(dataset);
      }
    }

    if (datasets.isEmpty()) {
      plot.removeAllDatasets();
      return;
    }

    datasets.trimToSize();

    featureRTRange = RangeUtils.multiplyGrow(featureRTRange, 1.25f);
    featureRTRange = RangeUtils.withinBounds(featureRTRange, 0f, null);
    final Range rtRange = RangeUtils.guavaToJFree(featureRTRange);

    final float finalMaxHeight = maxHeight;
    plot.applyWithNotifyChanges(false, true, () -> {
      plot.setDatasets(datasets);
      try {
        final XYPlot xyplot = plot.getXYPlot();
        xyplot.getRangeAxis().setRange(new Range(0, finalMaxHeight), true, false);
        xyplot.getRangeAxis().setUpperMargin(0.025);
        xyplot.getDomainAxis().setRange(rtRange, true, false);
        xyplot.getDomainAxis().setDefaultAutoRange(rtRange);
      } catch (NullPointerException | NoSuchElementException ex) {
        // error in jfreechart draw method
      }
    });
  }

  private SimpleXYChart<IonTimeSeriesToXYProvider> createChart() {
    UnitFormat uf = MZmineCore.getConfiguration().getUnitFormat();

    SimpleXYChart<IonTimeSeriesToXYProvider> chart = new SimpleXYChart<>(
        uf.format("Retention time", "min"), uf.format("Intensity", "a.u."));

    chart.setRangeAxisNumberFormatOverride(MZmineCore.getConfiguration().getIntensityFormat());
    chart.setDomainAxisNumberFormatOverride(MZmineCore.getConfiguration().getRTFormat());
    chart.setLegendItemsVisible(false);
    chart.setPrefWidth(GraphicalColumType.LARGE_GRAPHICAL_CELL_WIDTH);

    chart.getMouseAdapter().addGestureHandler(new ChartGestureHandler(
        new ChartGesture(Entity.ALL, Event.DOUBLE_CLICK, GestureButton.BUTTON1), _ -> {
      final ModularFeatureListRow row = getTableRow().getItem();
      FxThread.runLater(() -> ChromatogramVisualizerModule.visualizeFeatureListRows(List.of(row)));
    }));

    return chart;
  }

}
