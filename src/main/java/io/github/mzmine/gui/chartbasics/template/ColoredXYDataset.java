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

package io.github.mzmine.gui.chartbasics.template;

import io.github.mzmine.gui.chartbasics.template.providers.ColorPropertyProvider;
import io.github.mzmine.gui.chartbasics.template.providers.ColorProvider;
import io.github.mzmine.gui.chartbasics.template.providers.LabelTextProvider;
import io.github.mzmine.gui.chartbasics.template.providers.PlotXYDatasetProvider;
import io.github.mzmine.gui.chartbasics.template.providers.SeriesKeyProvider;
import io.github.mzmine.gui.chartbasics.template.providers.ToolTipTextProvider;
import io.github.mzmine.gui.chartbasics.template.providers.XYValueProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.javafx.FxColorUtil;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javax.annotation.Nullable;
import org.jfree.data.xy.AbstractXYDataset;

/**
 * Default dataset class for {@link SimpleXYLineChart}. Any class implementing {@link
 * PlotXYDatasetProvider} can be used to construct this dataset. The dataset implements the
 * interfaces, too, because the default renderers can then generate labels and tooltips based on the
 * interface methods and therefore be more reusable.
 *
 * @author https://github.com/SteffenHeu
 */
public class ColoredXYDataset extends AbstractXYDataset implements Task, SeriesKeyProvider,
    LabelTextProvider, ToolTipTextProvider, ColorPropertyProvider {

  private static Logger logger = Logger.getLogger(ColoredXYDataset.class.getName());
  // dataset stuff
  private final int seriesCount = 1;
  private final XYValueProvider xyValueProvider;
  private final SeriesKeyProvider<Comparable<?>> seriesKeyProvider;
  private final LabelTextProvider labelTextProvider;
  private final ToolTipTextProvider toolTipTextProvider;
  private ObjectProperty<javafx.scene.paint.Color> fxColor;
  private List<Double> domainValues;
  private List<Double> rangeValues;
  private Double minRangeValue;

  // task stuff
  private TaskStatus status;
  private String errorMessage;
  private boolean computed;
  private int computedItemCount;

  public ColoredXYDataset(XYValueProvider xyValueProvider,
      SeriesKeyProvider<Comparable<?>> seriesKeyProvider, LabelTextProvider labelTextProvider,
      ToolTipTextProvider toolTipTextProvider, ColorProvider colorProvider) {

    // Task stuff
    this.computed = false;
    status = TaskStatus.WAITING;
    errorMessage = "";

    // dataset stuff
    this.xyValueProvider = xyValueProvider;
    this.seriesKeyProvider = seriesKeyProvider;
    this.labelTextProvider = labelTextProvider;
    this.toolTipTextProvider = toolTipTextProvider;
    this.fxColor = new SimpleObjectProperty<>(colorProvider.getFXColor());

    minRangeValue = Double.MAX_VALUE;
    domainValues = Collections.emptyList();
    rangeValues = Collections.emptyList();
    this.computedItemCount = 0;

    fxColorProperty().addListener(((observable, oldValue, newValue) -> {
      fireDatasetChanged();
    }));

    MZmineCore.getTaskController().addTask(this);
  }

  public ColoredXYDataset(PlotXYDatasetProvider datasetProvider) {
    this(datasetProvider, datasetProvider, datasetProvider, datasetProvider,
        datasetProvider);
  }

  public java.awt.Color getAWTColor() {
    return FxColorUtil.fxColorToAWT(fxColor.getValue());
  }

  public void setAWTColor(java.awt.Color color) {
    this.fxColor.set(FxColorUtil.awtColorToFX(color));
  }

  public javafx.scene.paint.Color getFXColor() {
    return fxColor.getValue();
  }

  @Override
  public ObjectProperty<javafx.scene.paint.Color> fxColorProperty() {
    return fxColor;
  }

  public void setFxColor(javafx.scene.paint.Color colorfx) {
    this.fxColor.set(colorfx);
  }

  @Override
  public int getSeriesCount() {
    return seriesCount;
  }

  @Override
  public Comparable<?> getSeriesKey() {
    return seriesKeyProvider.getSeriesKey();
  }

  @Override
  public Comparable<?> getSeriesKey(int series) {
    return seriesKeyProvider.getSeriesKey();
  }

  @Override
  public int getItemCount(int series) {
    return computedItemCount;
  }

  @Override
  public Number getX(int series, int item) {
    if (!computed) {
      return 0.d;
    }
    return domainValues.get(item);
  }

  @Override
  public Number getY(int series, int item) {
    if (!computed) {
      return 0.d;
    }
    return rangeValues.get(item);
  }

  public List<Double> getXValues() {
    return Collections.unmodifiableList(domainValues);
  }

  public List<Double> getYValues() {
    return Collections.unmodifiableList(rangeValues);
  }

  public int getValueIndex(final double domainValue, final double rangeValue) {
    // todo binary search somehow here
    for (int i = 0; i < computedItemCount; i++) {
      if (Double.compare(domainValue, getX(0, i).doubleValue()) == 0
          && Double.compare(rangeValue, getY(0, i).doubleValue()) == 0) {
        return i;
      }
    }
    return -1;
  }

  public XYValueProvider getValueProvider() {
    return xyValueProvider;
  }

  public SeriesKeyProvider<Comparable<?>> getSeriesKeyProvider() {
    return seriesKeyProvider;
  }

  @Override
  @Nullable
  public String getLabel(final int itemIndex) {
    if (itemIndex > getItemCount(1)) {
      return null;
    }
    if (labelTextProvider != null) {
      return labelTextProvider.getLabel(itemIndex);
    }
    return String.valueOf(getYValue(1, itemIndex));
  }

  @Override
  @Nullable
  public String getToolTipText(final int itemIndex) {
    if (itemIndex > getItemCount(1) || toolTipTextProvider == null) {
      return null;
    }
    return toolTipTextProvider.getToolTipText(itemIndex);
  }

  public Double getMinimumRangeValue() {
    return minRangeValue;
  }

  /**
   * When an object implementing interface {@code Runnable} is used to create a thread, starting the
   * thread causes the object's {@code run} method to be called in that separately executing
   * thread.
   * <p>
   * The general contract of the method {@code run} is that it may take any action whatsoever.
   *
   * @see Thread#run()
   */
  @Override
  public void run() {

    status = TaskStatus.PROCESSING;

    xyValueProvider.computeValues();

    if (status == TaskStatus.CANCELED) {
      return;
    }

    if (xyValueProvider.getDomainValues().size() != xyValueProvider.getRangeValues().size()) {
      throw new IllegalArgumentException(
          "Number of domain values does not match number of range values.");
    }

    rangeValues = xyValueProvider.getRangeValues();
    domainValues = xyValueProvider.getDomainValues();

    for (Double rangeValue : rangeValues) {
      if (rangeValue.doubleValue() < minRangeValue.doubleValue()) {
        minRangeValue = rangeValue;
      }
    }

    computedItemCount = domainValues.size();

    computed = true;
    status = TaskStatus.FINISHED;
    Platform.runLater(this::fireDatasetChanged);
  }

  @Override
  public String getTaskDescription() {
    return "Computing values for dataset " + seriesKeyProvider.getSeriesKey();
  }

  @Override
  public double getFinishedPercentage() {
    return xyValueProvider.getComputationFinishedPercentage();
  }

  @Override
  public TaskStatus getStatus() {
    return status;
  }

  @Override
  public String getErrorMessage() {
    return errorMessage;
  }

  /**
   * The standard TaskPriority assign to this task
   *
   * @return
   */
  @Override
  public TaskPriority getTaskPriority() {
    return TaskPriority.NORMAL;
  }

  /**
   * Cancel a running task by user request.
   */
  @Override
  public void cancel() {
    status = TaskStatus.CANCELED;
  }
}
