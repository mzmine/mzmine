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

package io.github.mzmine.modules.visualization.ims.imsvisualizer;
/*
import java.awt.BasicStroke;
import java.awt.Color;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.ims.ImsVisualizerTask;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerTab;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.MassListDataSet;
import io.github.mzmine.util.color.SimpleColorPalette;

public class IntensityMobilityPlot extends EChartViewer {

  private Scan selectedMobilityScan;
  private final double selectedRetentionTime;
  private final RawDataFile[] dataFiles;

  public IntensityMobilityPlot(XYDataset dataset, ImsVisualizerTask imsTask) {
    super(ChartFactory.createXYLineChart("", "intensity", "", dataset, PlotOrientation.VERTICAL,
        false, true, false));
    JFreeChart chart = getChart();
    XYPlot plot = chart.getXYPlot();
    EStandardChartTheme theme = MZmineCore.getConfiguration().getDefaultChartTheme();
    theme.apply(chart);
    this.selectedRetentionTime = imsTask.getSelectedRetentionTime();
    dataFiles = imsTask.getDataFiles();
    var renderer = new XYLineAndShapeRenderer(true, true);
    renderer.setSeriesPaint(0, Color.BLACK);
    renderer.setSeriesStroke(0, new BasicStroke(1.0f));
    renderer.setSeriesShapesVisible(0, false);

    plot.setRenderer(renderer);
    plot.setBackgroundPaint(Color.WHITE);
    plot.setRangeGridlinePaint(Color.WHITE);
    plot.setDomainGridlinePaint(Color.WHITE);
    plot.getDomainAxis().setInverted(true);
    plot.getRangeAxis().setVisible(false);
    plot.getDomainAxis().setAutoRange(false);
    plot.getDomainAxis().setAutoRange(true);
    plot.getDomainAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());

    // mouse listener.
    addChartMouseListener(new ChartMouseListenerFX() {
      @Override
      public void chartMouseClicked(ChartMouseEventFX event) {
        ChartEntity chartEntity = event.getEntity();
        if (chartEntity instanceof XYItemEntity) {
          XYItemEntity entity = (XYItemEntity) chartEntity;
          int serindex = entity.getSeriesIndex();
          int itemindex = entity.getItem();
          double mobility = dataset.getYValue(serindex, itemindex);
          List<Scan> selectedScan = imsTask.getSelectedScans();
          for (int i = 0; i < selectedScan.size(); i++) {
            if (selectedScan.get(i).getMobility() == mobility
                && selectedScan.get(i).getRetentionTime() == selectedRetentionTime) {
              selectedMobilityScan = selectedScan.get(i);
              break;
            }
          }
          showSelectedScan();
        }
      }

      @Override
      public void chartMouseMoved(ChartMouseEventFX event) {}
    });
  }

  public void showSelectedScan() {
    SpectraVisualizerTab spectraTab = new SpectraVisualizerTab(dataFiles[0]);
    spectraTab.loadRawData(selectedMobilityScan);

    // set colors depending on vision
    SimpleColorPalette palette = MZmineCore.getConfiguration().getDefaultColorPalette();
    Color posColor = palette.getPositiveColorAWT();
    Color negColor = palette.getNegativeColorAWT();

    // set color
    XYPlot plotSpectra = (XYPlot) spectraTab.getSpectrumPlot().getChart().getPlot();

    // set color
    plotSpectra.getRenderer().setSeriesPaint(0, posColor);

    // add mass list
    MassList[] massLists = selectedMobilityScan.getMassLists();
    for (MassList massList : massLists) {
      MassListDataSet dataset = new MassListDataSet(massList);
      spectraTab.getSpectrumPlot().addDataSet(dataset, negColor, true);
    }
    MZmineCore.getDesktop().addTab(spectraTab);
  }
}*/
