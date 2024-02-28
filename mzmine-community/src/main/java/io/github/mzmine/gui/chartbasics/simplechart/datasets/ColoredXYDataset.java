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

package io.github.mzmine.gui.chartbasics.simplechart.datasets;

import com.google.common.collect.Range;
import com.google.errorprone.annotations.ForOverride;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleChartUtility;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.providers.ColorPropertyProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.ColorProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.IntervalWidthProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.LabelTextProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.SeriesKeyProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.ToolTipTextProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYItemObjectProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYValueProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.javafx.FxColorUtil;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.data.xy.IntervalXYDataset;

/**
 * Default dataset class for {@link SimpleXYChart}. Any class implementing
 * {@link PlotXYDataProvider} can be used to construct this dataset. The dataset implements the
 * interfaces, too, because the default renderers can then generate labels and tooltips based on the
 * interface methods and therefore be more reusable.
 *
 * @author https://github.com/SteffenHeu
 */
public class ColoredXYDataset extends AbstractTaskXYDataset implements IntervalXYDataset,
    SeriesKeyProvider, LabelTextProvider, ToolTipTextProvider, ColorPropertyProvider {

  private static final Logger logger = Logger.getLogger(ColoredXYDataset.class.getName());
  protected final XYValueProvider xyValueProvider;
  protected final SeriesKeyProvider<Comparable<?>> seriesKeyProvider;
  protected final LabelTextProvider labelTextProvider;
  protected final ToolTipTextProvider toolTipTextProvider;
  protected final IntervalWidthProvider intervalWidthProvider;

  protected final XYItemObjectProvider xyItemObjectProvider;
  private final RunOption runOption;
  // dataset stuff
  private final int seriesCount = 1;
  protected ObjectProperty<javafx.scene.paint.Color> fxColor;

  // task stuff
  protected boolean computed;
  protected int computedItemCount;
  protected boolean[] isLocalMaximum;
  protected boolean valuesComputed;

  protected Range<Double> domainRange;
  protected Range<Double> rangeRange;

  private ColoredXYDataset(XYValueProvider xyValueProvider,
      SeriesKeyProvider<Comparable<?>> seriesKeyProvider, LabelTextProvider labelTextProvider,
      ToolTipTextProvider toolTipTextProvider, ColorProvider colorProvider,
      XYItemObjectProvider xyItemObjectProvider, @NotNull final RunOption runOption) {

    // Task stuff
    this.computed = false;
    this.valuesComputed = false;

    // dataset stuff
    this.xyValueProvider = xyValueProvider;
    this.seriesKeyProvider = seriesKeyProvider;
    this.labelTextProvider = labelTextProvider;
    this.toolTipTextProvider = toolTipTextProvider;
    this.xyItemObjectProvider = xyItemObjectProvider;
    if (xyValueProvider instanceof IntervalWidthProvider) {
      this.intervalWidthProvider = (IntervalWidthProvider) xyValueProvider;
    } else {
      intervalWidthProvider = null;
    }

    this.fxColor = new SimpleObjectProperty<>(colorProvider.getFXColor());
    this.computedItemCount = 0;
    fxColorProperty().addListener(((observable, oldValue, newValue) -> fireDatasetChanged()));

    this.runOption = checkRunOption(runOption);
    handleRunOption(runOption);
  }

  private ColoredXYDataset(XYValueProvider xyValueProvider,
      SeriesKeyProvider<Comparable<?>> seriesKeyProvider, LabelTextProvider labelTextProvider,
      ToolTipTextProvider toolTipTextProvider, ColorProvider colorProvider,
      @NotNull final RunOption runOption) {
    this(xyValueProvider, seriesKeyProvider, labelTextProvider, toolTipTextProvider, colorProvider,
        xyValueProvider instanceof XYItemObjectProvider objProv ? objProv : null, runOption);
  }

  /**
   * Can be called by extending classes to not start the computation thread before their constructor
   * finished.
   * <p></p>
   * Note: Computation task has to be started by the respective extending class.
   */
  public ColoredXYDataset(PlotXYDataProvider datasetProvider, @NotNull final RunOption runOption) {
    this(datasetProvider, datasetProvider, datasetProvider, datasetProvider, datasetProvider,
        datasetProvider instanceof XYItemObjectProvider objProv ? objProv : null, runOption);
  }

  public ColoredXYDataset(@NotNull PlotXYDataProvider datasetProvider) {
    this(datasetProvider, RunOption.NEW_THREAD);
  }

  /**
   *
   */
  protected void handleRunOption(@NotNull final RunOption runOption) {
    switch (runOption) {
      case THIS_THREAD -> run();
      case NEW_THREAD -> MZmineCore.getTaskController().addTask(this);
      case DO_NOT_RUN -> {
      }
    }
  }

  /**
   * Checks if the thread and given run option are valid. Running on the FX thread is not allowed.
   *
   * @param runOption The requested run option.
   * @return a valid run option.
   */
  protected final RunOption checkRunOption(final RunOption runOption) {
    if (runOption == RunOption.THIS_THREAD && Platform.isFxApplicationThread()) {
      logger.warning(() -> "Calculation of data set values was started on the JavaFX thread."
          + " Creating a new thread instead. Provider: " + xyValueProvider.getClass().getName());
      return RunOption.NEW_THREAD;
    } else {
      return runOption;
    }
  }

  public ToolTipTextProvider getToolTipTextProvider() {
    return toolTipTextProvider;
  }

  public java.awt.Color getAWTColor() {
    return FxColorUtil.fxColorToAWT(fxColor.getValue());
  }

  private void setAWTColor(java.awt.Color color) {
    this.fxColor.set(FxColorUtil.awtColorToFX(color));
  }

  public javafx.scene.paint.Color getFXColor() {
    return fxColor.getValue();
  }

  private void setFXColor(javafx.scene.paint.Color colorfx) {
    this.fxColor.set(colorfx);
  }

  @Override
  public ObjectProperty<javafx.scene.paint.Color> fxColorProperty() {
    return fxColor;
  }

  public void setColor(java.awt.Color color) {
    setFXColor(FxColorUtil.awtColorToFX(color));
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
    if (!valuesComputed) {
      return 0.d;
    }
    return xyValueProvider.getDomainValue(item);
  }

  @Override
  public Number getY(int series, int item) {
    if (!valuesComputed) {
      return 0.d;
    }
    return xyValueProvider.getRangeValue(item);
  }

  @Override
  public double getXValue(int series, int item) {
    if (!valuesComputed) {
      return 0.0d;
    }
    return xyValueProvider.getDomainValue(item);
  }

  @Override
  public double getYValue(int series, int item) {
    if (!valuesComputed) {
      return 0.0d;
    }
    return xyValueProvider.getRangeValue(item);
  }

  public int getValueIndex(final double domainValue, final double rangeValue) {
    // todo binary search somehow here
    for (int i = 0; i < computedItemCount; i++) {
      if (Double.compare(domainValue, getXValue(0, i)) == 0
          && Double.compare(rangeValue, getYValue(0, i)) == 0) {
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
    return rangeRange != null ? rangeRange.lowerEndpoint() : 0;
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
    setStatus(TaskStatus.PROCESSING);
    xyValueProvider.computeValues(statusProperty());
    if (getStatus() != TaskStatus.PROCESSING) {
      return;
    }

    computedItemCount = xyValueProvider.getValueCount();
    isLocalMaximum = new boolean[computedItemCount];
    valuesComputed = true;

    double minDomain = Double.POSITIVE_INFINITY;
    double maxDomain = Double.NEGATIVE_INFINITY;
    double minRange = Double.POSITIVE_INFINITY;
    double maxRange = Double.NEGATIVE_INFINITY;

    for (int i = 0; i < computedItemCount; i++) {
      final double rangeValue = xyValueProvider.getRangeValue(i);
      final double domainValue = xyValueProvider.getDomainValue(i);

      minDomain = Math.min(domainValue, minDomain);
      maxDomain = Math.max(domainValue, maxDomain);
      minRange = Math.min(rangeValue, minRange);
      maxRange = Math.max(rangeValue, maxRange);

      isLocalMaximum[i] = SimpleChartUtility.isLocalMaximum(this, 0, i);
    }

    domainRange = computedItemCount > 0 ? Range.closed(minDomain, maxDomain) : Range.closed(0d, 1d);
    rangeRange = computedItemCount > 0 ? Range.closed(minRange, maxRange) : Range.closed(0d, 1d);

//    if (setToFinished) {
    onCalculationsFinished();
//    }
  }

  /**
   * Sets the {@link Task#getStatus()} property to finished, fires a dataset changed event and sets
   * {@link this#computed} to true.
   */
  protected void onCalculationsFinished() {
    computed = true;
    setStatus(TaskStatus.FINISHED);
    if (getRunOption()
        != RunOption.THIS_THREAD) {  // no need to notify then, dataset will be up to date
      MZmineCore.runLater(this::fireDatasetChanged);
    }
  }

  @Override
  public String getTaskDescription() {
    return "Computing values for dataset " + seriesKeyProvider.getSeriesKey();
  }

  @Override
  public double getFinishedPercentage() {
    return xyValueProvider.getComputationFinishedPercentage();
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

  @Override
  public Number getStartX(int series, int item) {
    return intervalWidthProvider != null ? getXValue(series, item)
        - intervalWidthProvider.getIntervalWidth() / 2 : getX(series, item);
  }

  @Override
  public double getStartXValue(int series, int item) {
    return intervalWidthProvider != null ? getXValue(series, item)
        - intervalWidthProvider.getIntervalWidth() / 2 : getXValue(series, item);
  }

  @Override
  public Number getEndX(int series, int item) {
    return intervalWidthProvider != null ? getXValue(series, item)
        + intervalWidthProvider.getIntervalWidth() / 2 : getXValue(series, item);
  }

  @Override
  public double getEndXValue(int series, int item) {
    return intervalWidthProvider != null ? getXValue(series, item)
        + intervalWidthProvider.getIntervalWidth() / 2 : getXValue(series, item);
  }

  @Override
  public Number getStartY(int series, int item) {
    return 0;
  }

  @Override
  public double getStartYValue(int series, int item) {
    return 0;
  }

  @Override
  public Number getEndY(int series, int item) {
    return getY(series, item);
  }

  @Override
  public double getEndYValue(int series, int item) {
    return getY(series, item).doubleValue();
  }

  public boolean isLocalMaximum(int item) {
    if (item > getItemCount(0)) {
      return false;
    }
    return isLocalMaximum[item];
  }

  /**
   * @return The domain value range. Null if called before the computation has been finished.
   */
  public Range<Double> getDomainValueRange() {
    return domainRange;
  }

  /**
   * @return The range value range. Null if called before the computation has been finished.
   */
  public Range<Double> getRangeValueRange() {
    return rangeRange;
  }

  /**
   * Returns the {@link RunOption} this data set was created with. Extending classes need to
   * override this method in case they need to do additional assignments in the constructor and
   * therefore pass {{@link RunOption#DO_NOT_RUN} in the constructor, as it is a protected variable
   * in this class. Alternatively, the extending class can override
   * {@link #onCalculationsFinished()}.
   *
   * @return The {@link RunOption} this data set was created with.
   */
  @ForOverride
  protected RunOption getRunOption() {
    return runOption;
  }
}
