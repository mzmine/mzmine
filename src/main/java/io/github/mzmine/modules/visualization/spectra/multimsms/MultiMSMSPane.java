/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */
package io.github.mzmine.modules.visualization.spectra.multimsms;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.identities.ms2.interf.AbstractMSMSIdentity;
import io.github.mzmine.gui.chartbasics.chartgroups.ChartGroup;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.gui.wrapper.ChartViewWrapper;
import io.github.mzmine.modules.visualization.spectra.multimsms.pseudospectra.PseudoSpectrum;
import io.github.mzmine.modules.visualization.spectra.multimsms.pseudospectra.PseudoSpectrumDataSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.MirrorChartFactory;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;

/**
 * Holds more charts for data reviewing
 *
 * @author Robin Schmid
 */
public class MultiMSMSPane extends BorderPane {

  // annotations for MSMS
  private List<AbstractMSMSIdentity> msmsAnnotations;
  // to flag annotations in spectra

  private boolean exchangeTolerance = true;
  private MZTolerance mzTolerance = new MZTolerance(0.0015, 2.5d);

  // MS 1
  private ChartViewWrapper msone;

  // MS 2
  private ChartGroup group;
  //
  private BorderPane contentPane;
  private GridPane pnCharts;
  private int col = 4;
  private int realCol = col;
  private boolean autoCol = true;
  private boolean alwaysShowBest = false;
  private boolean showTitle = false;
  private boolean showLegend = false;
  // only the last doamin axis
  private boolean onlyShowOneAxis = true;
  // click marker in all of the group
  private boolean showCrosshair = true;

  private Label lbRawIndex;
  private Pane pnTopMenu;
  private Label lbRawName;
  private Button nextRaw, prevRaw;
  private CheckBox cbBestRaw;
  private CheckBox cbUseBestForMissingRaw;

  private FeatureListRow[] rows;
  private RawDataFile raw;
  private RawDataFile[] allRaw;
  private boolean createMS1;
  private int rawIndex;
  private boolean useBestForMissingRaw;

  /**
   * Create the frame.
   */
  public MultiMSMSPane() {
//    setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
//    setBounds(100, 100, 853, 586);
    contentPane = new BorderPane();
    contentPane.setPadding(new Insets(5));
//    contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
//    contentPane.setLayout(new BorderLayout(0, 0));
//    setContentPane(contentPane);
    setCenter(contentPane);

    pnTopMenu = new FlowPane();
    contentPane.setTop(pnTopMenu);

    prevRaw = new Button("<");
    pnTopMenu.getChildren().add(prevRaw);
    prevRaw.setOnAction(e -> {
      prevRaw();
    });

    nextRaw = new Button(">");
    pnTopMenu.getChildren().add(nextRaw);
    nextRaw.setOnAction(e -> {
      nextRaw();
    });

    lbRawIndex = new Label("");
    pnTopMenu.getChildren().add(lbRawIndex);
    lbRawName = new Label("");
    pnTopMenu.getChildren().add(lbRawName);

    cbBestRaw = new CheckBox("use best for each");
    pnTopMenu.getChildren().add(cbBestRaw);
    cbBestRaw.selectedProperty().addListener((observable, oldValue, newValue) -> {
      setAlwaysShowBest(newValue);
    });

    cbUseBestForMissingRaw = new CheckBox("use best missing raw");
    pnTopMenu.getChildren().add(cbUseBestForMissingRaw);
    cbUseBestForMissingRaw.selectedProperty().addListener((observable, oldValue, newValue) -> {
      setUseBestForMissing(newValue);
    });

    pnCharts = new GridPane();
    contentPane.setCenter(pnCharts);

    var colCon = new ColumnConstraints();
    colCon.setFillWidth(true);
    colCon.setPercentWidth(100);
    pnCharts.getColumnConstraints().add(colCon);
//    pnCharts.setLayout(new GridLayout(0, 4));

    addMenu();
  }

  /**
   * Show best for missing MSMS in raw (if not selected none is shown)
   *
   * @param selected
   */
  public void setUseBestForMissing(boolean selected) {
    useBestForMissingRaw = selected;
    updateAllCharts();
  }

  private void nextRaw() {
    if (allRaw == null) {
      return;
    }
    while (rawIndex + 1 < allRaw.length) {
      rawIndex++;
      if (rawContainsFragmentation(allRaw[rawIndex])) {
        setRaw(allRaw[rawIndex]);
        break;
      }
    }
  }

  private void prevRaw() {
    if (allRaw == null) {
      return;
    }
    if (rawIndex > 0) {
      rawIndex--;
      setRaw(allRaw[rawIndex]);
    }
    while (rawIndex - 1 >= 0) {
      rawIndex--;
      if (rawContainsFragmentation(allRaw[rawIndex])) {
        setRaw(allRaw[rawIndex]);
        break;
      }
    }
  }

  /**
   * any row contains fragment scan in raw data file
   *
   * @param raw
   * @return
   */
  private boolean rawContainsFragmentation(RawDataFile raw) {
    for (FeatureListRow row : rows) {
      Feature peak = row.getFeature(raw);
      if (peak != null && peak.getMostIntenseFragmentScan() != null) {
        return true;
      }
    }
    return false;
  }

  /**
   * set raw data file and update
   *
   * @param raw
   */
  public void setRaw(RawDataFile raw) {
    this.raw = raw;

    this.rawIndex = 0;
    if (raw != null) {
      for (int i = 0; i < allRaw.length; i++) {
        if (raw.equals(allRaw[i])) {
          rawIndex = i;
          break;
        }
      }
    }

    lbRawName.setText(raw == null ? "" : raw.getName());
    lbRawIndex.setText("(" + rawIndex + ") ");
    updateAllCharts();
  }

  public void setAlwaysShowBest(boolean alwaysShowBest) {
    this.alwaysShowBest = alwaysShowBest;
    updateAllCharts();
  }

  private void addMenu() {
    MenuBar menu = new MenuBar();
    Menu settings = new Menu("Settings");
    menu.getMenus().add(settings);

    // set columns
    Menu setCol = new Menu("set columns");
    menu.getMenus().add(setCol);
    setCol.setOnAction(e -> {
      try {
        TextInputDialog inputDialog = new TextInputDialog(String.valueOf(col));
        inputDialog.setContentText("Columns");
        var result = inputDialog.showAndWait();
        if (result.isPresent()) {
          col = Integer.parseInt(result.get());
          setAutoColumns(false);
          setColumns(col);
        }
      } catch (Exception e2) {
        e2.printStackTrace();
      }
    });

    // reset zoom
    Menu resetZoom = new Menu("reset zoom");
    menu.getMenus().add(resetZoom);
    resetZoom.setOnAction(e -> {
      if (group != null) {
        group.resetZoom();
      }
    });

    //
    CheckMenuItem autoColumns = new CheckMenuItem("auto columns");
    autoColumns.setSelected(autoCol);
    autoColumns.setOnAction(e -> setAutoColumns(autoColumns.isSelected()));

    CheckMenuItem oneAxisOnly = new CheckMenuItem("show one axis only");
    oneAxisOnly.setSelected(onlyShowOneAxis);
    oneAxisOnly.setOnAction(e -> setOnlyShowOneAxis(oneAxisOnly.isSelected()));

    CheckMenuItem toggleLegend = new CheckMenuItem("show legend");
    toggleLegend.setSelected(showLegend);
    toggleLegend.setOnAction(e -> setShowLegend(toggleLegend.isSelected()));

    CheckMenuItem toggleTitle = new CheckMenuItem("show title");
    toggleTitle.setSelected(showTitle);
    toggleTitle.setOnAction(e -> setShowTitle(toggleTitle.isSelected()));

    CheckMenuItem toggleCrosshair = new CheckMenuItem("show crosshair");
    oneAxisOnly.setSelected(showCrosshair);
    oneAxisOnly.setOnAction(e -> setShowCrosshair(oneAxisOnly.isSelected()));

    settings.getItems().addAll(autoColumns, oneAxisOnly, toggleLegend, toggleTitle, toggleCrosshair);
    setTop(menu);
  }

  public void setColumns(int col2) {
    col = col2;
    renewCharts(group);
  }

  public void setAutoColumns(boolean selected) {
    this.autoCol = selected;
  }

  public void setShowCrosshair(boolean showCrosshair) {
    this.showCrosshair = showCrosshair;
    if (group != null) {
      group.setShowCrosshair(showCrosshair, showCrosshair);
    }
  }

  public void setShowLegend(boolean showLegend) {
    this.showLegend = showLegend;
    forAllCharts(c -> c.getLegend().setVisible(showLegend));
  }

  public void setShowTitle(boolean showTitle) {
    this.showTitle = showTitle;
    forAllCharts(c -> c.getTitle().setVisible(showTitle));
  }

  public void setOnlyShowOneAxis(boolean onlyShowOneAxis) {
    this.onlyShowOneAxis = onlyShowOneAxis;
    int i = 0;
    forAllCharts(c -> {
      // show only the last domain axes
      ValueAxis axis = c.getXYPlot().getDomainAxis();
      axis.setVisible(!onlyShowOneAxis || i >= group.size() - realCol);
    });
  }

  /**
   * Sort rows
   *
   * @param rows
   * @param raw
   * @param sorting
   * @param direction
   */
  public void setData(FeatureListRow[] rows, RawDataFile[] allRaw, RawDataFile raw,
      boolean createMS1,
      SortingProperty sorting, SortingDirection direction) {
    Arrays.sort(rows, new FeatureListRowSorter(sorting, direction));
    setData(rows, allRaw, raw, createMS1);
  }

  /**
   * Create charts and show
   *
   * @param rows
   * @param raw
   */
  public void setData(FeatureListRow[] rows, RawDataFile[] allRaw, RawDataFile raw,
      boolean createMS1) {
    this.rows = rows;
    this.allRaw = allRaw;
    this.createMS1 = createMS1;

    // check raw
    if (raw != null && !rawContainsFragmentation(raw)) {
      // change to best of highest row
      raw = Arrays.stream(rows).map(FeatureListRow::getMostIntenseFragmentScan).filter(Objects::nonNull)
          .findFirst().get().getDataFile();
    }
    // set raw and update
    setRaw(raw);
  }

  /**
   * Create new charts
   */
  public void updateAllCharts() {
    msone = null;
    group = new ChartGroup(showCrosshair, showCrosshair, true, false);
    // MS1
    if (createMS1) {
      Scan scan = null;
      Feature best = null;
      for (FeatureListRow r : rows) {
        Feature f = raw == null ? r.getBestFeature() : r.getFeature(raw);
        if (f != null && (best == null || f.getHeight() > best.getHeight())) {
          best = f;
        }
      }
      if (best != null) {
        scan = best.getRepresentativeScan();
        EChartViewer cp = SpectrumChartFactory.createScanChartViewer(scan, showTitle, showLegend);
        if (cp != null) {
          cp.minHeightProperty().bind(pnCharts.heightProperty().divide(rows.length+1));
          msone = new ChartViewWrapper(cp);
        }
      }
    } else {
      // pseudo MS1 from all rows and isotope pattern
      EChartViewer cp = PseudoSpectrum.createChartViewer(rows, raw, false, "pseudo");
      if (cp != null) {
        cp.setMinHeight(200);
        cp.minHeightProperty().bind(pnCharts.heightProperty().divide(rows.length+1));
        cp.getChart().getLegend().setVisible(showLegend);
        cp.getChart().getTitle().setVisible(showTitle);
        msone = new ChartViewWrapper(cp);
      }
    }

    if (msone != null) {
      group.add(msone);
    }

    // COMMON
    // MS2 of all rows
    for (FeatureListRow row : rows) {
      EChartViewer c = MirrorChartFactory.createMSMSChartViewer(row, raw, showTitle, showLegend,
          alwaysShowBest, useBestForMissingRaw);
      if (c != null) {
        c.minHeightProperty().bind(pnCharts.heightProperty().divide(rows.length+1));
        group.add(new ChartViewWrapper(c));
      }
    }

    renewCharts(group);
  }

  /**
   * @param group
   */
  public void renewCharts(ChartGroup group) {
    pnCharts.getChildren().clear();
    if (group != null && group.size() > 0) {
      /*realCol = autoCol ? (int) Math.floor(Math.sqrt(group.size())) - 1 : col;
      if (realCol < 1) {
        realCol = 1;
      }
      GridLayout layout = new GridLayout(0, realCol);
      pnCharts.setLayout(layout);*/
      // add to layout
      int i = 0;
      for (ChartViewWrapper cp : group.getList()) {
        // show only the last domain axes
        ValueAxis axis = cp.getChart().getXYPlot().getDomainAxis();
        axis.setVisible(!onlyShowOneAxis || i >= group.size() - realCol);

        pnCharts.add(new BorderPane(cp.getChartFX()), 0, i);
        i++;
      }
    }
//    pnCharts.revalidate();
//    pnCharts.repaint();
  }

  // ANNOTATIONS
  public void addMSMSAnnotation(AbstractMSMSIdentity ann) {
    if (msmsAnnotations == null) {
      msmsAnnotations = new ArrayList<>();
    }
    msmsAnnotations.add(ann);

    // extract mz tolerance
    if (mzTolerance == null || exchangeTolerance) {
      setMzTolerance(ann.getMzTolerance());
    }

    // add to charts
    addAnnotationToCharts(ann);
  }

  public void addMSMSAnnotations(List<? extends AbstractMSMSIdentity> ann) {
    if (ann == null) {
      return;
    }
    // extract mz tolerance
    if (mzTolerance == null || exchangeTolerance) {
      for (AbstractMSMSIdentity a : ann) {
        if (a.getMzTolerance() != null) {
          setMzTolerance(a.getMzTolerance());
          break;
        }
      }
    }

    // add all
    for (AbstractMSMSIdentity a : ann) {
      addMSMSAnnotation(a);
    }
  }

  /**
   * To flag annotations in spectra
   *
   * @param mzTolerance
   */
  public void setMzTolerance(MZTolerance mzTolerance) {
    if (mzTolerance == null && this.mzTolerance == null) {
      return;
    }

    boolean changed =
        mzTolerance != this.mzTolerance || (this.mzTolerance == null && mzTolerance != null)
            || !this.mzTolerance.equals(mzTolerance);
    this.mzTolerance = mzTolerance;
    exchangeTolerance = false;

    if (changed) {
      addAllAnnotationsToCharts();
    }
  }

  private void addAllAnnotationsToCharts() {
    if (msmsAnnotations == null) {
      return;
    }

    removeAllAnnotationsFromCharts();

    for (AbstractMSMSIdentity a : msmsAnnotations) {
      addAnnotationToCharts(a);
    }
  }

  private void removeAllAnnotationsFromCharts() {
    forAllCharts(c -> {

    });
  }

  private void addAnnotationToCharts(AbstractMSMSIdentity ann) {
    if (mzTolerance != null) {
      forAllCharts(c -> {
        PseudoSpectrumDataSet data = (PseudoSpectrumDataSet) c.getXYPlot().getDataset(0);
        data.addIdentity(mzTolerance, ann);
      });
    }
  }

  public MZTolerance getMzTolerance() {
    return mzTolerance;
  }

  /**
   * all charts (ms1 and MS2)
   *
   * @param op
   */
  public void forAllCharts(Consumer<JFreeChart> op) {
    if (group != null) {
      group.forAllCharts(op);
    }
  }

  /**
   * only ms2 charts
   *
   * @param op
   */
  public void forAllMSMSCharts(Consumer<JFreeChart> op) {
    if (group == null || group.getList() == null) {
      return;
    }

    int start = msone == null ? 0 : 1;
    for (int i = start; i < group.getList().size(); i++) {
      op.accept(group.getList().get(i).getChart());
    }
  }
}
