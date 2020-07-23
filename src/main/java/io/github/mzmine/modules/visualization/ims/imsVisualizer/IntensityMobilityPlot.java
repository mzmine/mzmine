/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.ims.imsVisualizer;

import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.ims.ImsVisualizerParameters;
import io.github.mzmine.modules.visualization.ims.ImsVisualizerTask;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerWindow;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.MassListDataSet;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.color.SimpleColorPalette;
import javafx.application.Platform;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;

import java.awt.*;
import java.util.logging.Logger;

public class IntensityMobilityPlot extends EChartViewer {

  private XYPlot plot;
  private JFreeChart chart;
  private Logger logger = Logger.getLogger(this.getClass().getName());
  static final Font legendFont = new Font("SansSerif", Font.PLAIN, 12);
  private EStandardChartTheme theme;
  private Scan selectedMobilityScan;
  private double selectedRetentionTime;
  private Scan scans[];
  private RawDataFile dataFiles[];

  public IntensityMobilityPlot(XYDataset dataset, ParameterSet parameters, ImsVisualizerTask imsVisualizerTask) {
    super(
        ChartFactory.createXYLineChart(
            "", "intensity", "", dataset, PlotOrientation.VERTICAL, false, true, false));
    chart = getChart();
    plot = chart.getXYPlot();
    theme = MZmineCore.getConfiguration().getDefaultChartTheme();
    theme.apply(chart);
    this.selectedRetentionTime = imsVisualizerTask.getSelectedRetentionTime();
    dataFiles =
        parameters
            .getParameter(ImsVisualizerParameters.dataFiles)
            .getValue()
            .getMatchingRawDataFiles();
    scans =
        parameters
            .getParameter(ImsVisualizerParameters.scanSelection)
            .getValue()
            .getMatchingScans(dataFiles[0]);

    var renderer = new XYLineAndShapeRenderer(true, true);
    renderer.setSeriesPaint(0, Color.GREEN);
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
    // mouse listener.
    addChartMouseListener(
        new ChartMouseListenerFX() {
          @Override
          public void chartMouseClicked(ChartMouseEventFX event) {
            ChartEntity chartEntity = event.getEntity();
            if (chartEntity instanceof XYItemEntity) {
              XYItemEntity entity = (XYItemEntity) chartEntity;
              int serindex = entity.getSeriesIndex();
              int itemindex = entity.getItem();
              double mobility = dataset.getYValue(serindex, itemindex);
              for (int i = 0; i < scans.length; i++) {
                if (scans[i].getMobility() == mobility && scans[i].getRetentionTime() == selectedRetentionTime) {
                  selectedMobilityScan = scans[i];
                  break;
                }
              }
              updateChart();
            }
          }

          @Override
          public void chartMouseMoved(ChartMouseEventFX event) {}
        });
  }

  void updateChart() {
    SpectraVisualizerWindow spectraWindow = new SpectraVisualizerWindow(dataFiles[0]);
    spectraWindow.loadRawData(selectedMobilityScan);

    // set colors depending on vision
    SimpleColorPalette palette = MZmineCore.getConfiguration().getDefaultColorPalette();
    Color posColor = palette.getPositiveColorAWT();
    Color negColor = palette.getNegativeColorAWT();

    // set color
    XYPlot plotSpectra = (XYPlot) spectraWindow.getSpectrumPlot().getChart().getPlot();

    // set color
    plotSpectra.getRenderer().setSeriesPaint(0, posColor);

    // add mass list
    MassList[] massLists = selectedMobilityScan.getMassLists();
    for (MassList massList : massLists) {
      MassListDataSet dataset = new MassListDataSet(massList);
      spectraWindow.getSpectrumPlot().addDataSet(dataset, negColor, true);
    }
    Platform.runLater(() -> spectraWindow.show());
  }
}
