package io.github.mzmine.util.reporting.jasper;

import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataanalysis.rowsboxplot.RowBoxPlotDataset;
import io.github.mzmine.util.color.SimpleColorPalette;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;

public class ExportBoxplot extends EChartViewer {

  private final BoxAndWhiskerRenderer boxAndWhiskerRenderer;

  public ExportBoxplot() {
    final NumberFormats formats = MZmineCore.getConfiguration().getGuiFormats();
    final JFreeChart barChart = ChartFactory.createBarChart("Rows box plot", "Metadata", "Height",
        null);
    ((NumberAxis) barChart.getCategoryPlot().getRangeAxis()).setNumberFormatOverride(
        formats.intensityFormat());

    super(barChart);

    setMinWidth(200);
    boxAndWhiskerRenderer = new BoxAndWhiskerRenderer();
    boxAndWhiskerRenderer.setMeanVisible(false);

    barChart.getCategoryPlot().setDataset(0, null);
    barChart.getCategoryPlot().setRenderer(0, boxAndWhiskerRenderer);

    barChart.getCategoryPlot().getRangeAxis().setLabel("Area");
    barChart.setTitle((String) null);

    barChart.getCategoryPlot().getDomainAxis().setTickLabelsVisible(false);
    barChart.getCategoryPlot().getDomainAxis().setTickMarksVisible(false);

    ((NumberAxis) getChart().getCategoryPlot()
        .getRangeAxis()).setNumberFormatOverride(ConfigService.getGuiFormats().intensityFormat());
  }

  public void setDataset(RowBoxPlotDataset ds) {
    applyWithNotifyChanges(false, () -> {
      getChart().getCategoryPlot().setDataset(0, ds);
      final SimpleColorPalette colors = ds.getColorPalette();
      colors.applyToChart(getChart());
      // need to reset the renderer otherwise the old will keep the colors
      boxAndWhiskerRenderer.clearSeriesPaints(false);
      boxAndWhiskerRenderer.clearSeriesStrokes(false);
    });
  }
}
