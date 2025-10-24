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

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.SummedMobilogramXYProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineConfiguration;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.RangeUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableRow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;

/**
 * Generates a chart at creation with a preferred size set. updateItem sets the datasets in one call
 * so that there is only one call to {@link JFreeChart#fireChartChanged()} and drawing the chart.
 * The first cell seems to be the measurement cell and is never updated here as there are many calls
 * to update item on cell 0 and just single calls on all other cells.
 */
public class MobilogramFeatureShapeCell extends TreeTableCell<ModularFeatureListRow, Object> {

  private static final Logger logger = Logger.getLogger(MobilogramFeatureShapeCell.class.getName());

  private final SimpleXYChart<SummedMobilogramXYProvider> plot;
  private final Region view;
  private final int id;

  public MobilogramFeatureShapeCell(int id) {
    super();
    this.id = id;
    setMinWidth(GraphicalColumType.LARGE_GRAPHICAL_CELL_WIDTH);
    setMinHeight(GraphicalColumType.DEFAULT_GRAPHICAL_CELL_HEIGHT);
    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

    plot = createChart();
    view = new BorderPane(plot);

    graphicProperty().bind(emptyProperty().map(empty -> empty ? null : view));
  }

  private String getIdentifier(String action) {
    final TreeTableRow<ModularFeatureListRow> row = getTableRow();
    return "Chromatogram Cell %d action: %s (%s %s row %s)".formatted(id, action, isEmpty(),
        getItem(), row == null ? "no table row" : row.getItem());
  }

  @Override
  protected void updateItem(Object o, boolean b) {
    // always need to call super.updateItem
    super.updateItem(o, b);

    final ModularFeatureListRow row = getTableRow().getItem();
    // first cell with id 0 seems to be just the measuring cell - no need to put content.
    // the chart itself already helps measurements
    if (row == null || isEmpty() || id == 0) {
      // no need to remove datasets because we will replace once we have a new dataset with setAll
      return;
    }

    final IMSRawDataFile imsFile = (IMSRawDataFile) getTableRow().getItem().getRawDataFiles()
        .stream().filter(file -> file instanceof IMSRawDataFile).findAny().orElse(null);
    if (imsFile == null) {
      return;
    }
    final MobilityType mt = imsFile.getMobilityType();
    final String axisLabel = mt.getAxisLabel();
    plot.setDomainAxisLabel(axisLabel);

    // debugging of updates
//    logger.info(getIdentifier("updateItem"));

    // for selecting a range
    com.google.common.collect.Range<Float> featureMobRange = null;
    Double maxHeight = null;

    //
    int size = row.getFilesFeatures().size();
    ArrayList<ColoredXYDataset> datasets = new ArrayList<>(size);
    for (ModularFeature f : row.getFeatures()) {
      IonTimeSeries<? extends Scan> series = f.getFeatureData();
      if (series instanceof IonMobilogramTimeSeries) {
        // IonTimeSeriesToXYProvider is already precomputed and can be set on FXThread directly
        // therefore no need for caching and other thread
        final SummedMobilogramXYProvider provider = new SummedMobilogramXYProvider(f);
        datasets.add(new ColoredXYDataset(provider, RunOption.THIS_THREAD));

        maxHeight = MathUtils.max(provider.getMaxIntensity(), maxHeight);
        featureMobRange = RangeUtils.span(featureMobRange, f.getMobilityRange());
      }
    }

    if (datasets.isEmpty()) {
      plot.removeAllDatasets();
      return;
    }

    datasets.trimToSize();

    featureMobRange = RangeUtils.multiplyGrow(featureMobRange, 1.25f);
    featureMobRange = RangeUtils.withinBounds(featureMobRange, 0f, null);
    final Range mobRange = RangeUtils.guavaToJFree(featureMobRange);

    final double finalMaxHeight = maxHeight;
    plot.applyWithNotifyChanges(false, true, () -> {
      plot.setDatasets(datasets);
      try {
        final XYPlot xyplot = plot.getXYPlot();
        xyplot.getRangeAxis().setRange(new Range(0, finalMaxHeight), true, false);
        xyplot.getRangeAxis().setUpperMargin(0.025);
        xyplot.getDomainAxis().setRange(mobRange, true, false);
        xyplot.getDomainAxis().setDefaultAutoRange(mobRange);
      } catch (NullPointerException | NoSuchElementException ex) {
        // error in jfreechart draw method
      }
    });
  }

  private SimpleXYChart<SummedMobilogramXYProvider> createChart() {
    final MZmineConfiguration config = ConfigService.getConfiguration();
    UnitFormat uf = config.getUnitFormat();

    SimpleXYChart<SummedMobilogramXYProvider> chart = new SimpleXYChart<>("",
        uf.format("Intensity", "a.u."));
    chart.setRangeAxisNumberFormatOverride(config.getIntensityFormat());
    chart.setDomainAxisNumberFormatOverride(config.getMobilityFormat());
    chart.setLegendItemsVisible(false);
    chart.setPrefWidth(GraphicalColumType.LARGE_GRAPHICAL_CELL_WIDTH);

    chart.getChart().setBackgroundPaint((new Color(0, 0, 0, 0)));
    chart.getXYPlot().setBackgroundPaint((new Color(0, 0, 0, 0)));

//    chart.addChartDrawDebugListener(() -> logger.info(getIdentifier("CHART DRAW")));
    return chart;
  }

}
