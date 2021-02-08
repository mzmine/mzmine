/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.gui.chartbasics.simplechart;

import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.listener.RegionSelectionListener;
import io.github.mzmine.main.MZmineCore;
import java.awt.BasicStroke;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYShapeAnnotation;


/**
 * Wraps around a {@link AllowsRegionSelection} and adds controls to select regions of interest.
 *
 * @param <T>
 * @author https://github.com/SteffenHeu
 */
public class RegionSelectionWrapper<T extends EChartViewer & AllowsRegionSelection> extends
    BorderPane {

  final GridPane selectionControls;
  private final T node;
  private final ObservableList<RegionSelectionListener> finishedRegionSelectionListeners;
  private final Stroke roiStroke = new BasicStroke(1f);
  private final Paint roiPaint = MZmineCore.getConfiguration().getDefaultColorPalette()
      .getNegativeColorAWT();

  public RegionSelectionWrapper(T node) {
    this.node = node;
    setCenter(node);
    selectionControls = new GridPane();
    finishedRegionSelectionListeners = FXCollections.observableArrayList();
    initSelectionPane();
  }

  public ObservableList<RegionSelectionListener> getFinishedRegionListeners() {
    return finishedRegionSelectionListeners;
  }

  public List<List<Point2D>> getFinishedRegionsAsListOfPointLists() {
    return finishedRegionSelectionListeners.stream().map(l -> l.buildingPointsProperty().get())
        .collect(Collectors.toList());
  }

  public List<Path2D> getFinishedRegionsAsListOfPaths() {
    return finishedRegionSelectionListeners.stream().map(l -> l.buildingPathProperty().get())
        .collect(Collectors.toList());
  }

  private void initSelectionPane() {
    Button btnStartRegion = new Button("Start region");
    Button btnFinishRegion = new Button("Finish region");
    Button btnCancelRegion = new Button("Cancel region");
    Button btnClearRegions = new Button("Clear regions");

    btnStartRegion.setOnAction(e -> node.startRegion());

    btnFinishRegion.setOnAction(e -> {
      RegionSelectionListener selection = node.finishPath();
      finishedRegionSelectionListeners.add(selection);
    });

    btnCancelRegion.setOnAction(e -> node.finishPath());

    btnClearRegions.setDisable(true);
    btnClearRegions.setOnAction(e -> {
      List<XYAnnotation> annotations = node.getChart().getXYPlot().getAnnotations();
      new ArrayList<>(annotations)
          .forEach(a -> node.getChart().getXYPlot().removeAnnotation(a, true));
      finishedRegionSelectionListeners.clear();
    });

    finishedRegionSelectionListeners
        .addListener((ListChangeListener<RegionSelectionListener>) c -> {
          c.next();
          if (c.wasRemoved()) {
            boolean disable = c.getList().isEmpty();
            btnClearRegions.setDisable(disable);
          }
          if (c.wasAdded()) {
            node.getChart().getXYPlot()
                .addAnnotation(
                    new XYShapeAnnotation(c.getAddedSubList().get(0).buildingPathProperty()
                        .get(), roiStroke, roiPaint));
            btnClearRegions.setDisable(false);
          }
        });

    selectionControls.add(btnStartRegion, 0, 0);
    selectionControls.add(btnFinishRegion, 1, 0);
    selectionControls.add(btnCancelRegion, 2, 0);
    selectionControls.add(btnClearRegions, 3, 0);
    selectionControls.setHgap(5);
    selectionControls.getStyleClass().add(".region-match-chart-bg");
    selectionControls.setAlignment(Pos.TOP_CENTER);
    setBottom(selectionControls);
  }

  public GridPane getSelectionControls() {
    return selectionControls;
  }
}
