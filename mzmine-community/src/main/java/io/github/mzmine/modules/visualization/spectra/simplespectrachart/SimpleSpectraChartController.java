/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.spectra.simplespectrachart;

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.MSnInfoImpl;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.gui.chartbasics.simplechart.PlotCursorPosition;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.XYDatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.MassSpectrumProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYBarRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.main.ConfigService;
import java.text.NumberFormat;
import java.util.Map;
import java.util.SequencedCollection;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;

/**
 * MVCI controller for a slim spectra chart. Replaces the responsibilities of the legacy
 * {@code SpectraPlot} with a small, composable surface:
 * <ul>
 *   <li>add multiple datasets, each with its own renderer;</li>
 *   <li>auto-pick a renderer based on {@link MassSpectrumType} (profile vs
 *   centroid) when only a data provider is supplied;</li>
 *   <li>change colors of datasets through their {@link ColoredXYDataset}.</li>
 * </ul>
 */
public class SimpleSpectraChartController extends FxController<SimpleSpectraChartModel> {

  private final SimpleSpectraChartViewBuilder viewBuilder;

  public SimpleSpectraChartController() {
    super(new SimpleSpectraChartModel());
    this.viewBuilder = new SimpleSpectraChartViewBuilder(model);
  }

  @Override
  protected @NotNull FxViewBuilder<SimpleSpectraChartModel> getViewBuilder() {
    return viewBuilder;
  }

  // --- adding datasets -------------------------------------------------------

  /**
   * Adds a spectrum, choosing the renderer based on the spectrum type.
   *
   * @return the wrapped {@link ColoredXYDataset} so callers can later mutate its color through
   * {@link #setDatasetColor(ColoredXYDataset, Color)} or by calling
   * {@link ColoredXYDataset#fxColorProperty()} directly.
   */
  public @NotNull ColoredXYDataset addSpectrum(@NotNull PlotXYDataProvider provider,
      @NotNull MassSpectrumType type) {
    final ColoredXYDataset dataset = new ColoredXYDataset(provider);
    addDataset(dataset, defaultRendererFor(type));
    return dataset;
  }

  /**
   * Adds a spectrum using {@link SimpleSpectraChartModel#getDefaultSpectrumType()} to choose the
   * renderer.
   */
  public @NotNull ColoredXYDataset addSpectrum(@NotNull PlotXYDataProvider provider) {
    return addSpectrum(provider, model.getDefaultSpectrumType());
  }

  /**
   * Generic add. Use this when callers need full control over the renderer.
   */
  public void addDataset(@NotNull XYDataset dataset, @NotNull XYItemRenderer renderer) {
    onGuiThread(() -> {
      if (dataset instanceof ColoredXYDataset data
          && data.getValueProvider() instanceof MassSpectrumProvider provider) {
        final MassSpectrum spectrum = provider.getSpectrum();
        if (spectrum instanceof Scan scan) {
          addPrecursorMarkers(scan);
        }
      }
      model.datasetRenderersProperty().put(dataset, renderer);
    });
  }

  /**
   * Convenience overload that wraps a raw provider in a {@link ColoredXYDataset}.
   */
  public @NotNull ColoredXYDataset addDataset(@NotNull PlotXYDataProvider provider,
      @NotNull XYItemRenderer renderer) {
    final ColoredXYDataset dataset = new ColoredXYDataset(provider);
    addDataset(dataset, renderer);
    return dataset;
  }

  // --- removal & replacement -------------------------------------------------

  public void clearDatasets() {
    onGuiThread(() -> {
      clearPrecursorMarkers();
      model.getChart()
          .applyWithNotifyChanges(false, () -> model.datasetRenderersProperty().clear());
    });
  }

  /**
   * Replaces all current datasets with a single new one in one notification batch.
   */
  public void setDataset(@NotNull XYDataset dataset, @NotNull XYItemRenderer renderer) {
    onGuiThread(() -> model.getChart().applyWithNotifyChanges(false, () -> {
      model.datasetRenderersProperty().clear();
      clearPrecursorMarkers();
      model.datasetRenderersProperty().put(dataset, renderer);
    }));
  }

  public void addDatasets(
      @NotNull Map<? extends @NotNull XYDataset, ? extends @NotNull XYItemRenderer> datasets) {
    onGuiThread(() -> model.getChart().applyWithNotifyChanges(false,
        () -> datasets.forEach(model.datasetRenderersProperty()::put)));
  }

  // --- color mutation --------------------------------------------------------

  /**
   * Sets the color of an already-added {@link ColoredXYDataset}. The color propagates to the chart
   * automatically because the {@code Colored*} renderers read from the dataset's color provider on
   * each repaint.
   */
  public void setDatasetColor(@NotNull ColoredXYDataset dataset, @NotNull Color color) {
    onGuiThread(() -> dataset.fxColorProperty().set(color));
  }

  // --- property accessors ----------------------------------------------------

  public MapProperty<@NotNull XYDataset, @NotNull XYItemRenderer> datasetRenderersProperty() {
    return model.datasetRenderersProperty();
  }

  public ObjectProperty<@Nullable PlotCursorPosition> cursorPositionProperty() {
    return model.cursorPositionProperty();
  }

  public StringProperty titleProperty() {
    return model.titleProperty();
  }

  public void setTitle(@Nullable String title) {
    model.setTitle(title);
  }

  public StringProperty domainAxisLabelProperty() {
    return model.domainLabelProperty();
  }

  public StringProperty rangeAxisLabelProperty() {
    return model.rangeLabelProperty();
  }

  public ObjectProperty<@Nullable NumberFormat> domainAxisFormatProperty() {
    return model.domainAxisFormatProperty();
  }

  public ObjectProperty<@Nullable NumberFormat> rangeAxisFormatProperty() {
    return model.rangeAxisFormatProperty();
  }

  /**
   * Pixels excluded from the bottom of the chart's data area when deciding where labels may appear.
   * Increasing this margin hides labels for low-intensity peaks whose label would otherwise sit
   * close to the baseline.
   */
  public DoubleProperty bottomLabelMarginProperty() {
    return model.bottomLabelMarginProperty();
  }

  public void setBottomLabelMargin(double pixels) {
    model.setBottomLabelMargin(pixels);
  }

  public void setDefaultSpectrumType(@NotNull MassSpectrumType type) {
    model.setDefaultSpectrumType(type);
  }

  // --- renderer factory ------------------------------------------------------

  /**
   * Returns the default renderer for a given spectrum type. PROFILE spectra are drawn as continuous
   * lines, every other type as vertical bars.
   * <p>
   * The {@code Colored*} renderers are intentionally chosen over the legacy
   * {@code ContinuousRenderer} / {@code PeakRenderer}: only the colored variants pick up the color
   * from the {@link ColoredXYDataset}, which is what makes runtime color changes work without
   * re-creating the renderer.
   */
  public static @NotNull XYItemRenderer defaultRendererFor(@NotNull MassSpectrumType type) {
    return switch (type) {
      case PROFILE -> new ColoredXYLineRenderer();
      case CENTROIDED, THRESHOLDED, MIXED, ANY -> new ColoredXYBarRenderer(false);
    };
  }

  public void setDatasets(@NotNull SequencedCollection<XYDatasetAndRenderer> list) {
    clearDatasets();
    for (XYDatasetAndRenderer dr : list) {
      addDataset(dr.dataset(), dr.renderer());
    }
  }

  public void setLegendItemsVisible(boolean visible) {
    model.getChart().setLegendItemsVisible(visible);
  }

  // --- precursor / isolation-window markers ----------------------------------

  /**
   * Adds precursor isolation-window annotations for the given scan, using gray at 50 % alpha.
   * Mirrors {@code SpectraPlot.addPrecursorMarkers} so callers can treat both charts uniformly.
   * <p>
   * Uses the global {@link MZminePreferences#showPrecursorWindow} preference to decide whether to
   * draw an {@link org.jfree.chart.plot.IntervalMarker} (window) or a
   * {@link org.jfree.chart.plot.ValueMarker} (single m/z line).
   */
  public void addPrecursorMarkers(@NotNull Scan scan) {
    addPrecursorMarkers(scan, java.awt.Color.GRAY, 0.5f);
  }

  /**
   * Adds precursor isolation-window annotations for the given scan with a custom color and alpha.
   */
  public void addPrecursorMarkers(@NotNull Scan scan, java.awt.Color color, float alpha) {
    boolean showWindow = ConfigService.getPreferences()
        .getValue(MZminePreferences.showPrecursorWindow);
    if (scan.getMSLevel() == 2) {
      final MsMsInfo info = scan.getMsMsInfo();
      final Double prMz = scan.getPrecursorMz();
      if (showWindow && info != null && info.getIsolationWindow() != null) {
        model.getChart().addDomainMarker(info.getIsolationWindow(), color, alpha);
      } else if (prMz != null) {
        model.getChart().addDomainMarker(prMz, color, alpha);
      }
    } else if (scan.getMSLevel() > 2) {
      if (scan.getMsMsInfo() instanceof MSnInfoImpl msn) {
        for (var info : msn.getPrecursors()) {
          if (showWindow && info.getIsolationWindow() != null) {
            model.getChart().addDomainMarker(info.getIsolationWindow(), color, alpha);
          } else {
            model.getChart().addDomainMarker(info.getIsolationMz(), color, alpha);
          }
        }
      }
    }
  }

  /**
   * Removes all domain markers (including precursor markers) from the chart.
   */
  public void clearPrecursorMarkers() {
    model.getXYPlot().clearDomainMarkers();
  }

  @NotNull
  public SimpleXYChart<PlotXYDataProvider> getChart() {
    return model.getChart();
  }
}
