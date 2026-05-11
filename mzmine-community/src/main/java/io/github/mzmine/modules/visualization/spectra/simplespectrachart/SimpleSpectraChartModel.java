package io.github.mzmine.modules.visualization.spectra.simplespectrachart;

import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.gui.chartbasics.gui.javafx.model.FxXYPlot;
import io.github.mzmine.gui.chartbasics.simplechart.PlotCursorPosition;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.main.ConfigService;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;

/**
 * State for {@link SimpleSpectraChartController}. Holds the wrapped {@link SimpleXYChart} together
 * with all observable properties. Mutations of the dataset map are reflected in the underlying
 * chart by {@link SimpleSpectraChartViewBuilder}.
 */
public class SimpleSpectraChartModel {

  // single observable map -> setAll style mutations only.
  private final MapProperty<@NotNull XYDataset, @NotNull XYItemRenderer> datasetRenderers = new SimpleMapProperty<>(
      FXCollections.observableHashMap());

  // chart instance is created once and kept as a final-equivalent property so
  // that view builder can subscribe and dispose of it cleanly.
  private final @NotNull SimpleXYChart<PlotXYDataProvider> chart = new SimpleXYChart<>("m/z",
      "Intensity");

  // decision: defaultSpectrumType drives the renderer choice when a caller adds
  // a raw provider without specifying a renderer. CENTROIDED is the more common
  // case for processed mzmine spectra.
  private final ObjectProperty<@NotNull MassSpectrumType> defaultSpectrumType = new SimpleObjectProperty<>(
      MassSpectrumType.CENTROIDED);

  private final StringProperty title = chart.getChartModel().titleProperty();
  private final StringProperty domainLabel = new SimpleStringProperty("m/z");
  private final StringProperty rangeLabel = new SimpleStringProperty("Intensity");

  private final ObjectProperty<@Nullable NumberFormat> domainAxisFormat = new SimpleObjectProperty<>(
      ConfigService.getGuiFormats().mzFormat());
  private final ObjectProperty<@Nullable NumberFormat> rangeAxisFormat = new SimpleObjectProperty<>(
      ConfigService.getGuiFormats().intensityFormat());

  // Screen-space bounds of all labels drawn during the current render pass, shared across every
  // dataset on the plot so labels from different datasets cannot overlap each other. Written by
  // SimpleSpectraItemLabelGenerator during JFreeChart's drawItem pass and cleared on
  // PlotChangeEvent by SimpleSpectraChartViewBuilder. Plain ArrayList is fine because mutation is
  // confined to the FX/render thread and no JavaFX binding observes this cache.
  private final @NotNull List<@NotNull Rectangle2D> drawnLabelBounds = new ArrayList<>();

  // Pixels to exclude from the bottom of the dataArea when deciding where labels may appear.
  // Shrinks the label-allowed rectangle so that low-intensity peaks near the baseline do not
  // receive labels (their labels would either crowd the axis or sit on the baseline noise).
  // Default 0 keeps current behaviour — every label inside the dataArea is allowed.
  private final DoubleProperty bottomLabelMargin = new SimpleDoubleProperty(25);

  public SimpleSpectraChartModel() {
  }

  public @NotNull List<@NotNull Rectangle2D> getDrawnLabelBounds() {
    return drawnLabelBounds;
  }

  public void clearDrawnLabelBounds() {
    drawnLabelBounds.clear();
  }

  public MapProperty<@NotNull XYDataset, @NotNull XYItemRenderer> datasetRenderersProperty() {
    return datasetRenderers;
  }

  public ObservableMap<@NotNull XYDataset, @NotNull XYItemRenderer> getDatasetRenderers() {
    return datasetRenderers.get();
  }

  public ObjectProperty<@Nullable PlotCursorPosition> cursorPositionProperty() {
    return chart.cursorPositionProperty();
  }

  public @Nullable PlotCursorPosition getCursorPosition() {
    return cursorPositionProperty().get();
  }

  public void setCursorPosition(@Nullable PlotCursorPosition value) {
    cursorPositionProperty().set(value);
  }

  public ObjectProperty<@NotNull MassSpectrumType> defaultSpectrumTypeProperty() {
    return defaultSpectrumType;
  }

  public @NotNull MassSpectrumType getDefaultSpectrumType() {
    return defaultSpectrumType.get();
  }

  public void setDefaultSpectrumType(@NotNull MassSpectrumType type) {
    defaultSpectrumType.set(type);
  }

  public StringProperty titleProperty() {
    return title;
  }

  public @Nullable String getTitle() {
    return title.get();
  }

  public void setTitle(@Nullable String value) {
    title.set(value);
  }

  public StringProperty domainLabelProperty() {
    return domainLabel;
  }

  public StringProperty rangeLabelProperty() {
    return rangeLabel;
  }

  public ObjectProperty<@Nullable NumberFormat> domainAxisFormatProperty() {
    return domainAxisFormat;
  }

  public ObjectProperty<@Nullable NumberFormat> rangeAxisFormatProperty() {
    return rangeAxisFormat;
  }

  public DoubleProperty bottomLabelMarginProperty() {
    return bottomLabelMargin;
  }

  public double getBottomLabelMargin() {
    return bottomLabelMargin.get();
  }

  public void setBottomLabelMargin(double pixels) {
    bottomLabelMargin.set(pixels);
  }

  /**
   * Package private. Only the controller and view builder need direct access to the chart. External
   * users should go through the controller.
   */
  @NotNull SimpleXYChart<PlotXYDataProvider> getChart() {
    return chart;
  }

  FxXYPlot getXYPlot() {
    return chart.getXYPlot();
  }

}
