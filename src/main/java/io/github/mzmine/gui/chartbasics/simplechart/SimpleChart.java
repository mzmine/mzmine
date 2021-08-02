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

package io.github.mzmine.gui.chartbasics.simplechart;

import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.xy.XYDataset;

/**
 * Defines methods supported by all simple charts.
 */
public interface SimpleChart<T extends PlotXYDataProvider> {

  /**
   * @return Mapping of datasetIndex -> Dataset
   */
  public LinkedHashMap<Integer, XYDataset> getAllDatasets();

  public void setDomainAxisLabel(String label);

  public void setRangeAxisLabel(String label);

  public void setDomainAxisNumberFormatOverride(NumberFormat format);

  public void setRangeAxisNumberFormatOverride(NumberFormat format);

  public void setLegendItemsVisible(boolean visible);

  public XYPlot getXYPlot();

  public int addDataset(T datasetProvider);

  public void removeAllDatasets();

  public XYItemRenderer getDefaultRenderer();

  public void setDefaultRenderer(XYItemRenderer defaultRenderer);

  public ObjectProperty<XYItemRenderer> defaultRendererProperty();

  public void setShowCrosshair(boolean show);

  public void setItemLabelsVisible(boolean visible);

  /**
   * @return current cursor position or null
   */
  public PlotCursorPosition getCursorPosition();

  public void setCursorPosition(PlotCursorPosition cursorPosition);

  public ObjectProperty<PlotCursorPosition> cursorPositionProperty();

  public void addContextMenuItem(String title, EventHandler<ActionEvent> ai);

  public void addDatasetChangeListener(DatasetChangeListener listener);

  public void removeDatasetChangeListener(DatasetChangeListener listener);

  public void clearDatasetChangeListeners();

  public void notifyDatasetChangeListeners(DatasetChangeEvent event);
}
