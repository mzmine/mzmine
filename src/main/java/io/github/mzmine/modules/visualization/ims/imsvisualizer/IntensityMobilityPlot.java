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

package io.github.mzmine.modules.visualization.ims.imsvisualizer;

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
}
