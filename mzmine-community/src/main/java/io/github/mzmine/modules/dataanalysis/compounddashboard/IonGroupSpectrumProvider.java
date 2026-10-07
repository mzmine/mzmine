package io.github.mzmine.modules.dataanalysis.compounddashboard;

import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Color;
import javafx.beans.property.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * MS1 stick provider for one ion group (parent {@code [M]} stick at index 0, optional isotopes
 * after). The parent stick carries the adduct or m/z label; isotope sticks stay unlabeled to keep
 * the spectrum readable.
 */
public class IonGroupSpectrumProvider implements PlotXYDataProvider {

  private final double[] mzs;
  private final double[] intensities;
  private final @NotNull String parentLabel;
  private final @NotNull String parentTooltip;
  private final @NotNull String seriesKey;
  private final @NotNull Color color;

  public IonGroupSpectrumProvider(@NotNull double[] mzs, @NotNull double[] intensities,
      @NotNull String parentLabel, @NotNull String parentTooltip, @NotNull String seriesKey,
      @NotNull Color color) {
    this.mzs = mzs;
    this.intensities = intensities;
    this.parentLabel = parentLabel;
    this.parentTooltip = parentTooltip;
    this.seriesKey = seriesKey;
    this.color = color;
  }

  @NotNull
  @Override
  public Color getAWTColor() {
    return color;
  }

  @NotNull
  @Override
  public javafx.scene.paint.Color getFXColor() {
    return FxColorUtil.awtColorToFX(color);
  }

  @Nullable
  @Override
  public String getLabel(int index) {
    // decision: only the parent (M) stick at index 0 gets a label; isotopes stay unlabeled.
    return index == 0 ? parentLabel : null;
  }

  @NotNull
  @Override
  public Comparable<?> getSeriesKey() {
    return seriesKey;
  }

  @Nullable
  @Override
  public String getToolTipText(int itemIndex) {
    return itemIndex == 0 ? parentTooltip : null;
  }

  @Override
  public void computeValues(Property<TaskStatus> status) {
    // nothing to do
  }

  @Override
  public double getDomainValue(int index) {
    return mzs[index];
  }

  @Override
  public double getRangeValue(int index) {
    return intensities[index];
  }

  @Override
  public int getValueCount() {
    return mzs.length;
  }

  @Override
  public double getComputationFinishedPercentage() {
    return 1d;
  }

  @Override
  public boolean isComputed() {
    return true;
  }
}
