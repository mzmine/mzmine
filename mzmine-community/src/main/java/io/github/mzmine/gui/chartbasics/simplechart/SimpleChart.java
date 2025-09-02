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

package io.github.mzmine.gui.chartbasics.simplechart;

import io.github.mzmine.gui.chartbasics.simplechart.datasets.ProviderAndRenderer;
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

  int addDataset(T datasetProvider, XYItemRenderer renderer);

  default int addDataset(ProviderAndRenderer data) {
    return addDataset((T) data.provider(), data.renderer());
  }

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
