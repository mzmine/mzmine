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

package io.github.mzmine.modules.visualization.chromatogram;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.File;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.LegendItemEntity;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import com.google.common.base.Joiner;
import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.Desktop;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.SimpleSorter;
import io.github.mzmine.util.dialogs.AxesSetupDialog;
import io.github.mzmine.util.javafx.FxIconUtil;
import io.github.mzmine.util.javafx.WindowsMenu;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

/**
 * Total ion chromatogram visualizer using JFreeChart library
 */
public class TICVisualizerWindow extends Stage {

  // Icons.
  private static final Image SHOW_SPECTRUM_ICON =
      FxIconUtil.loadImageFromResources("icons/spectrumicon.png");
  private static final Image DATA_POINTS_ICON =
      FxIconUtil.loadImageFromResources("icons/datapointsicon.png");
  private static final Image ANNOTATIONS_ICON =
      FxIconUtil.loadImageFromResources("icons/annotationsicon.png");
  private static final Image AXES_ICON = FxIconUtil.loadImageFromResources("icons/axesicon.png");
  private static final Image LEGEND_ICON = FxIconUtil.loadImageFromResources("icons/legendkey.png");
  private static final Image BACKGROUND_ICON =
      FxIconUtil.loadImageFromResources("icons/bgicon.png");

  // CSV extension.
  private static final String CSV_EXTENSION = "*.csv";

  private final Scene mainScene;
  private final BorderPane mainPane;
  private final ToolBar toolBar;
  private final TICPlot ticPlot;

  // Data sets
  private Hashtable<RawDataFile, TICDataSet> ticDataSets;

  private TICPlotType plotType;
  private ScanSelection scanSelection;
  private Range<Double> mzRange;

  private Desktop desktop;

  // Export file chooser.
  private static FileChooser exportChooser = null;


  /**
   * Constructor for total ion chromatogram visualizer
   */
  public TICVisualizerWindow(RawDataFile dataFiles[], TICPlotType plotType,
      ScanSelection scanSelection, Range<Double> mzRange, List<Feature> peaks,
      Map<Feature, String> peakLabels) {

    assert mzRange != null;

    setTitle("Chromatogram loading...");

    this.desktop = MZmineCore.getDesktop();
    this.plotType = plotType;
    this.ticDataSets = new Hashtable<RawDataFile, TICDataSet>();
    this.scanSelection = scanSelection;
    this.mzRange = mzRange;

    mainPane = new BorderPane();
    mainScene = new Scene(mainPane);

    // Use main CSS
    mainScene.getStylesheets()
        .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    setScene(mainScene);

    setMinWidth(400.0);
    setMinHeight(300.0);

    // sizeToScene();
    // setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    // setBackground(Color.white);

    ticPlot = new TICPlot();
    mainPane.setCenter(ticPlot);

    toolBar = new ToolBar();
    toolBar.setOrientation(Orientation.VERTICAL);

    Button showSpectrumBtn = new Button(null, new ImageView(SHOW_SPECTRUM_ICON));
    showSpectrumBtn.setTooltip(new Tooltip("Show spectrum of selected scan"));
    showSpectrumBtn.setOnAction(e -> {
      CursorPosition pos = getCursorPosition();
      if (pos != null) {
        SpectraVisualizerModule.showNewSpectrumWindow(pos.getDataFile(), pos.getScanNumber());
      }
    });


    Button datapointsBtn = new Button(null, new ImageView(DATA_POINTS_ICON));
    datapointsBtn.setTooltip(new Tooltip("Toggle displaying of data points"));
    datapointsBtn.setOnAction(e -> {
      ticPlot.switchDataPointsVisible();
    });


    Button annotationsBtn = new Button(null, new ImageView(ANNOTATIONS_ICON));
    annotationsBtn.setTooltip(new Tooltip("Toggle displaying of peak labels"));
    annotationsBtn.setOnAction(e -> {
      ticPlot.switchItemLabelsVisible();
    });


    Button axesBtn = new Button(null, new ImageView(AXES_ICON));
    axesBtn.setTooltip(new Tooltip("Setup ranges for axes"));
    axesBtn.setOnAction(e -> {
      AxesSetupDialog dialog = new AxesSetupDialog(this, ticPlot.getXYPlot());
      dialog.show();
    });

    Button legendBtn = new Button(null, new ImageView(LEGEND_ICON));
    legendBtn.setTooltip(new Tooltip("Toggle display of the legend"));
    legendBtn.setOnAction(e -> {
      ticPlot.switchLegendVisible();
    });

    Button backgroundBtn = new Button(null, new ImageView(BACKGROUND_ICON));
    backgroundBtn.setTooltip(new Tooltip("Toggle between white or gray background color"));
    backgroundBtn.setOnAction(e -> {
      ticPlot.switchBackground();
    });

    toolBar.getItems().addAll(showSpectrumBtn, datapointsBtn, annotationsBtn, axesBtn, legendBtn,
        backgroundBtn);
    mainPane.setRight(toolBar);

    // add all peaks
    if (peaks != null) {

      for (Feature peak : peaks) {
        if (peakLabels != null && peakLabels.containsKey(peak)) {

          final String label = peakLabels.get(peak);
          ticPlot.addLabelledPeakDataset(new PeakDataSet(peak, label), label);

        } else {

          ticPlot.addPeakDataset(new PeakDataSet(peak));
        }
      }
    }

    // add all data files
    for (RawDataFile dataFile : dataFiles) {
      addRawDataFile(dataFile);
    }

    // Add the Windows menu
    WindowsMenu.addWindowsMenu(mainScene);

    // pack();

    // get the window settings parameter
    ParameterSet paramSet =
        MZmineCore.getConfiguration().getModuleParameters(ChromatogramVisualizerModule.class);
    WindowSettingsParameter settings =
        paramSet.getParameter(TICVisualizerParameters.WINDOWSETTINGSPARAMETER);

    // update the window and listen for changes
    // settings.applySettingsToWindow(this);
    // this.addComponentListener(settings);

    // Listen for clicks on legend items
    ticPlot.addChartMouseListener(new ChartMouseListenerFX() {
      @Override
      public void chartMouseClicked(ChartMouseEventFX event) {
        ChartEntity entity = event.getEntity();
        XYPlot plot = (XYPlot) ticPlot.getChart().getPlot();

        if ((entity != null) && entity instanceof LegendItemEntity
            && plot.getRenderer().getClass().getName().indexOf(".TICPlotRenderer") > -1) {
          LegendItemEntity itemEntity = (LegendItemEntity) entity;
          XYLineAndShapeRenderer rendererAll = (XYLineAndShapeRenderer) plot.getRenderer();

          // Find index value
          int index = -1;
          for (int i = 0; i < plot.getDatasetCount(); i++) {
            if (rendererAll.getLegendItem(i, 1) != null && rendererAll.getLegendItem(i, 1)
                .getDescription().equals(itemEntity.getSeriesKey())) {
              index = i;
              break;
            }
          }
          XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer(index);

          // Select or deselect dataset
          Font font = new Font("Helvetica", Font.BOLD, 11);
          BasicStroke stroke = new BasicStroke(4);
          if (renderer.getDefaultLegendTextFont() != null
              && renderer.getDefaultLegendTextFont().isBold()) {
            font = new Font("Helvetica", Font.PLAIN, 11);
            stroke = new BasicStroke(1);
          }
          renderer.setDefaultLegendTextFont(font);
          renderer.setSeriesStroke(0, stroke);
        }
      }

      @Override
      public void chartMouseMoved(ChartMouseEventFX event) {
        ChartEntity entity = event.getEntity();
        XYPlot plot = (XYPlot) ticPlot.getChart().getPlot();
        if ((entity != null) && entity instanceof LegendItemEntity
            && plot.getRenderer().getClass().getName().indexOf(".TICPlotRenderer") > -1) {
          // ticPlot.setCursor(new Cursor(Cursor.HAND_CURSOR));
        } else {
          // ticPlot.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        }
      }
    });


    setOnHiding(e -> {
      for (Task task : ticDataSets.values()) {
        TaskStatus status = task.getStatus();
        if ((status == TaskStatus.WAITING) || (status == TaskStatus.PROCESSING)) {
          task.cancel();
        }

      }
    });

  }

  void updateTitle() {

    NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
    NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();

    StringBuffer mainTitle = new StringBuffer();
    StringBuffer subTitle = new StringBuffer();

    // If all data files have m/z range less than or equal to range of
    // the plot (mzMin, mzMax), then call this TIC, otherwise XIC
    Set<RawDataFile> fileSet = ticDataSets.keySet();
    String ticOrXIC = "TIC";

    // Enlarge range a bit to avoid rounding errors
    Range<Double> mzRange2 = Range.range(mzRange.lowerEndpoint() - 1, BoundType.CLOSED,
        mzRange.upperEndpoint() + 1, BoundType.CLOSED);
    for (RawDataFile df : fileSet) {
      if (!mzRange2.encloses(df.getDataMZRange())) {
        ticOrXIC = "XIC";
        break;
      }
    }

    if (plotType == TICPlotType.BASEPEAK) {
      if (ticOrXIC.equals("TIC")) {
        mainTitle.append("Base peak chromatogram");
      } else {
        mainTitle.append("XIC (base peak)");
      }
    } else {
      if (ticOrXIC.equals("TIC")) {
        mainTitle.append("TIC");
      } else {
        mainTitle.append("XIC");
      }
    }

    mainTitle.append(", m/z: " + mzFormat.format(mzRange.lowerEndpoint()) + " - "
        + mzFormat.format(mzRange.upperEndpoint()));

    CursorPosition pos = getCursorPosition();

    if (pos != null) {
      subTitle.append("Selected scan #");
      subTitle.append(pos.getScanNumber());
      if (ticDataSets.size() > 1) {
        subTitle.append(" (" + pos.getDataFile() + ")");
      }
      subTitle.append(", RT: " + rtFormat.format(pos.getRetentionTime()));
      if (plotType == TICPlotType.BASEPEAK) {
        subTitle.append(", base peak: " + mzFormat.format(pos.getMzValue()) + " m/z");
      }
      subTitle.append(", IC: " + intensityFormat.format(pos.getIntensityValue()));
    }

    // update window title
    RawDataFile files[] = ticDataSets.keySet().toArray(new RawDataFile[0]);
    Arrays.sort(files, new SimpleSorter());
    String dataFileNames = Joiner.on(",").join(files);
    setTitle("Chromatogram: [" + dataFileNames + "; " + mzFormat.format(mzRange.lowerEndpoint())
        + " - " + mzFormat.format(mzRange.upperEndpoint()) + " m/z" + "]");

    // update plot title
    ticPlot.setTitle(mainTitle.toString(), subTitle.toString());

  }

  /**
   * @return Returns the plotType.
   */
  TICPlotType getPlotType() {
    return plotType;
  }

  TICDataSet[] getAllDataSets() {
    return ticDataSets.values().toArray(new TICDataSet[0]);
  }

  /**
   */
  public void setRTRange(Range<Double> rtRange) {
    ticPlot.getXYPlot().getDomainAxis().setRange(rtRange.lowerEndpoint(), rtRange.upperEndpoint());
  }

  public void setAxesRange(double xMin, double xMax, double xTickSize, double yMin, double yMax,
      double yTickSize) {
    NumberAxis xAxis = (NumberAxis) ticPlot.getXYPlot().getDomainAxis();
    NumberAxis yAxis = (NumberAxis) ticPlot.getXYPlot().getRangeAxis();
    xAxis.setRange(xMin, xMax);
    xAxis.setTickUnit(new NumberTickUnit(xTickSize));
    yAxis.setRange(yMin, yMax);
    yAxis.setTickUnit(new NumberTickUnit(yTickSize));
  }

  /**
   * @see io.github.mzmine.modules.RawDataVisualizer#setIntensityRange(double, double)
   */
  public void setIntensityRange(double intensityMin, double intensityMax) {
    ticPlot.getXYPlot().getRangeAxis().setRange(intensityMin, intensityMax);
  }

  /**
   * @see io.github.mzmine.modules.RawDataVisualizer#getRawDataFiles()
   */
  public RawDataFile[] getRawDataFiles() {
    return ticDataSets.keySet().toArray(new RawDataFile[0]);
  }

  public void addRawDataFile(RawDataFile newFile) {

    final Scan scans[] = scanSelection.getMatchingScans(newFile);
    if (scans.length == 0) {
      desktop.displayErrorMessage("No scans found.");
      return;
    }

    TICDataSet ticDataset = new TICDataSet(newFile, scans, mzRange, this);
    ticDataSets.put(newFile, ticDataset);
    ticPlot.addTICDataset(ticDataset);

  }

  public void removeRawDataFile(RawDataFile file) {
    TICDataSet dataset = ticDataSets.get(file);
    ticPlot.getXYPlot().setDataset(ticPlot.getXYPlot().indexOf(dataset), null);
    ticDataSets.remove(file);
  }

  /**
   * Export a file's chromatogram.
   *
   * @param file the file.
   */
  public void exportChromatogram(RawDataFile file) {

    // Get the data set.
    final TICDataSet dataSet = ticDataSets.get(file);
    if (dataSet != null) {

      // Create the chooser if necessary.
      if (exportChooser == null) {

        exportChooser = new FileChooser();
        exportChooser.setTitle("Select Chromatogram File");
        exportChooser.getExtensionFilters()
            .add(new ExtensionFilter("Comma-separated values files", CSV_EXTENSION));
      }

      exportChooser.setInitialFileName(file.getName());
      // Choose an export file.
      final File exportFile = exportChooser.showSaveDialog(this);
      if (exportFile != null) {

        MZmineCore.getTaskController().addTask(new ExportChromatogramTask(dataSet, exportFile));
      }
    }
  }

  /**
   * @return current cursor position
   */
  public CursorPosition getCursorPosition() {
    double selectedRT = ticPlot.getXYPlot().getDomainCrosshairValue();
    double selectedIT = ticPlot.getXYPlot().getRangeCrosshairValue();
    Enumeration<TICDataSet> e = ticDataSets.elements();
    while (e.hasMoreElements()) {
      TICDataSet dataSet = e.nextElement();
      int index = dataSet.getIndex(selectedRT, selectedIT);
      if (index >= 0) {
        double mz = 0;
        if (plotType == TICPlotType.BASEPEAK) {
          mz = dataSet.getZValue(0, index);
        }
        CursorPosition pos = new CursorPosition(selectedRT, mz, selectedIT, dataSet.getDataFile(),
            dataSet.getScanNumber(index));
        return pos;
      }
    }
    return null;
  }

  /**
   * @return current cursor position
   */
  public void setCursorPosition(CursorPosition newPosition) {
    ticPlot.getXYPlot().setDomainCrosshairValue(newPosition.getRetentionTime(), false);
    ticPlot.getXYPlot().setRangeCrosshairValue(newPosition.getIntensityValue());
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  // @Override
  public void actionPerformed(ActionEvent event) {

    String command = event.getActionCommand();

    if (command.equals("SHOW_SPECTRUM")) {
    }

    if (command.equals("MOVE_CURSOR_LEFT")) {
      CursorPosition pos = getCursorPosition();
      if (pos != null) {
        TICDataSet dataSet = ticDataSets.get(pos.getDataFile());
        int index = dataSet.getIndex(pos.getRetentionTime(), pos.getIntensityValue());
        if (index > 0) {
          index--;
          pos.setRetentionTime(dataSet.getXValue(0, index));
          pos.setIntensityValue(dataSet.getYValue(0, index));
          setCursorPosition(pos);

        }
      }
    }

    if (command.equals("MOVE_CURSOR_RIGHT")) {
      CursorPosition pos = getCursorPosition();
      if (pos != null) {
        TICDataSet dataSet = ticDataSets.get(pos.getDataFile());
        int index = dataSet.getIndex(pos.getRetentionTime(), pos.getIntensityValue());
        if (index >= 0) {
          index++;
          if (index < dataSet.getItemCount(0)) {
            pos.setRetentionTime(dataSet.getXValue(0, index));
            pos.setIntensityValue(dataSet.getYValue(0, index));
            setCursorPosition(pos);
          }
        }
      }
    }

  }



  public TICPlot getTICPlot() {
    return ticPlot;
  }

  public ToolBar getToolBar() {
    return toolBar;
  }


}
