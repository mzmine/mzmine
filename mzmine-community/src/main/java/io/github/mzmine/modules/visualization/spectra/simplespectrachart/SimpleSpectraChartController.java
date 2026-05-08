package io.github.mzmine.modules.visualization.spectra.simplespectrachart;

import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.gui.chartbasics.simplechart.PlotCursorPosition;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYBarRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import java.text.NumberFormat;
import java.util.Map;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;

/**
 * MVCI controller for a slim spectra chart. Replaces the responsibilities of
 * the legacy {@code SpectraPlot} with a small, composable surface:
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
   * @return the wrapped {@link ColoredXYDataset} so callers can later mutate
   * its color through {@link #setDatasetColor(ColoredXYDataset, Color)} or by
   * calling {@link ColoredXYDataset#fxColorProperty()} directly.
   */
  public @NotNull ColoredXYDataset addSpectrum(@NotNull PlotXYDataProvider provider,
      @NotNull MassSpectrumType type) {
    final ColoredXYDataset dataset = new ColoredXYDataset(provider);
    addDataset(dataset, defaultRendererFor(type));
    return dataset;
  }

  /**
   * Adds a spectrum using {@link SimpleSpectraChartModel#getDefaultSpectrumType()}
   * to choose the renderer.
   */
  public @NotNull ColoredXYDataset addSpectrum(@NotNull PlotXYDataProvider provider) {
    return addSpectrum(provider, model.getDefaultSpectrumType());
  }

  /**
   * Generic add. Use this when callers need full control over the renderer.
   */
  public void addDataset(@NotNull XYDataset dataset, @NotNull XYItemRenderer renderer) {
    onGuiThread(() -> model.datasetRenderersProperty().put(dataset, renderer));
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

  public void removeDataset(@NotNull XYDataset dataset) {
    onGuiThread(() -> model.datasetRenderersProperty().remove(dataset));
  }

  public void clearDatasets() {
    onGuiThread(() -> model.getChart().applyWithNotifyChanges(false,
        () -> model.datasetRenderersProperty().clear()));
  }

  /**
   * Replaces all current datasets with a single new one in one notification batch.
   */
  public void setDataset(@NotNull XYDataset dataset, @NotNull XYItemRenderer renderer) {
    onGuiThread(() -> model.getChart().applyWithNotifyChanges(false, () -> {
      model.datasetRenderersProperty().clear();
      model.datasetRenderersProperty().put(dataset, renderer);
    }));
  }

  public void addDatasets(@NotNull Map<? extends @NotNull XYDataset, ? extends @NotNull XYItemRenderer> datasets) {
    onGuiThread(() -> model.getChart().applyWithNotifyChanges(false,
        () -> datasets.forEach(model.datasetRenderersProperty()::put)));
  }

  // --- color mutation --------------------------------------------------------

  /**
   * Sets the color of an already-added {@link ColoredXYDataset}. The color
   * propagates to the chart automatically because the {@code Colored*}
   * renderers read from the dataset's color provider on each repaint.
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

  public void setDefaultSpectrumType(@NotNull MassSpectrumType type) {
    model.setDefaultSpectrumType(type);
  }

  // --- renderer factory ------------------------------------------------------

  /**
   * Returns the default renderer for a given spectrum type. PROFILE spectra
   * are drawn as continuous lines, every other type as vertical bars.
   * <p>
   * The {@code Colored*} renderers are intentionally chosen over the legacy
   * {@code ContinuousRenderer} / {@code PeakRenderer}: only the colored
   * variants pick up the color from the {@link ColoredXYDataset}, which is
   * what makes runtime color changes work without re-creating the renderer.
   */
  public static @NotNull XYItemRenderer defaultRendererFor(@NotNull MassSpectrumType type) {
    return switch (type) {
      case PROFILE -> new ColoredXYLineRenderer();
      case CENTROIDED, THRESHOLDED, MIXED, ANY -> new ColoredXYBarRenderer(false);
    };
  }
}
