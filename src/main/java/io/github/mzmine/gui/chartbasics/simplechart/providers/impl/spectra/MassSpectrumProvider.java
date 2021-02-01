package io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra;

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Color;
import java.text.NumberFormat;
import javafx.beans.property.SimpleObjectProperty;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MassSpectrumProvider implements PlotXYDataProvider {

  private final NumberFormat mzFormat;
  private final String seriesKey;
  private final MassSpectrum spectrum;

  public MassSpectrumProvider(MassSpectrum spectrum, String seriesKey) {
    this.spectrum = spectrum;
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    this.seriesKey = seriesKey;
  }

  @Nonnull
  @Override
  public Color getAWTColor() {
    return Color.BLACK;
  }

  @Nonnull
  @Override
  public javafx.scene.paint.Color getFXColor() {
    return javafx.scene.paint.Color.BLACK;
  }

  @Nullable
  @Override
  public String getLabel(int index) {
    return mzFormat.format(spectrum.getMzValue(index));
  }

  @Nonnull
  @Override
  public Comparable<?> getSeriesKey() {
    return seriesKey;
  }

  @Nullable
  @Override
  public String getToolTipText(int itemIndex) {
    return null;
  }

  @Override
  public void computeValues(SimpleObjectProperty<TaskStatus> status) {

  }

  @Override
  public double getDomainValue(int index) {
    return spectrum.getMzValue(index);
  }

  @Override
  public double getRangeValue(int index) {
    return spectrum.getIntensityValue(index);
  }

  @Override
  public int getValueCount() {
    return spectrum.getNumberOfDataPoints();
  }

  @Override
  public double getComputationFinishedPercentage() {
    return 1d;
  }
}
