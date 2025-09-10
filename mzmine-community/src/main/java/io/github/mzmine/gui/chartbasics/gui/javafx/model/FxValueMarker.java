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

package io.github.mzmine.gui.chartbasics.gui.javafx.model;

import io.github.mzmine.javafx.properties.LastUpdateProperty;
import io.github.mzmine.main.ConfigService;
import java.awt.BasicStroke;
import java.awt.Paint;
import java.awt.Stroke;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyFloatProperty;
import javafx.beans.property.ReadOnlyFloatWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.plot.ValueMarker;

/**
 * A {@link ValueMarker} that can be configured in JavaFX. Is invisible if visible false, value
 * null, or alpha 0. If the value or alpha is changed, there is an automatic update event triggered
 * in the plot.
 */
public class FxValueMarker extends ValueMarker {

  private final LastUpdateProperty lastVisibleChange;
  private final ObjectProperty<@Nullable Double> actualValue = new SimpleObjectProperty<>(null);

  // is the base alpha that is applied when the marker is visible
  private final FloatProperty alpha = new SimpleFloatProperty(1f);
  // the actual alpha is used to hide the marker if value is null or invisible
  private final ReadOnlyFloatWrapper actualAlpha = new ReadOnlyFloatWrapper(1f);

  private final BooleanProperty visible = new SimpleBooleanProperty();
  private final ObjectProperty<@NotNull Stroke> stroke = new SimpleObjectProperty<>(
      new BasicStroke(1.0f));
  private final ObjectProperty<@NotNull Paint> paint = new SimpleObjectProperty<>(
      ConfigService.getConfiguration().getDefaultColorPalette().getPositiveColorAWT());


  public FxValueMarker() {
    super(0);

    // set alpha
    actualAlpha.bind(
        Bindings.createFloatBinding(this::calcActualAlpha, actualValue, visible, alpha));

    visible.subscribe((nv) -> super.setAlpha(nv ? 1.0f : 0.0f));
    stroke.subscribe((nv) -> super.setStroke(nv));
    paint.subscribe((nv) -> super.setPaint(nv));
    actualValue.subscribe((nv) -> {
      if (nv == null) {
        return; // no need to set null as marker only accepts primitives and default would trigger update
      }
      super.setValue(nv);
    });

    lastVisibleChange = new LastUpdateProperty(actualValue, actualAlpha);
  }

  @Override
  public void setValue(double value) {
    actualValue.set(value);
  }


  public void setValue(@Nullable Double value) {
    actualValue.set(value);
  }

  /**
   * Is used to listen for updates that should trigger a chart redraw
   *
   */
  public LastUpdateProperty lastVisibleChangeProperty() {
    return lastVisibleChange;
  }

  private float calcActualAlpha() {
    if (!isVisible() || getActualValue() == null) {
      return 0;
    }

    return getAlpha();
  }

  /**
   * The actual alpha currently used due to visibility and value status.
   */
  public float getActualAlpha() {
    return actualAlpha.get();
  }

  /**
   * The actual alpha currently used due to visibility and value status.
   */
  public ReadOnlyFloatProperty actualAlphaProperty() {
    return actualAlpha.getReadOnlyProperty();
  }

  /**
   * The base alpha value that should be applied if the value is visible and not null
   */
  @Override
  public float getAlpha() {
    return alpha.get();
  }

  /**
   * The base alpha value that should be applied if the value is visible and not null
   */
  public FloatProperty alphaProperty() {
    return alpha;
  }

  /**
   * The base alpha value that should be applied if the value is visible and not null
   */
  @Override
  public void setAlpha(float alpha) {
    this.alpha.set(alpha);
  }

  public @Nullable Double getActualValue() {
    return actualValue.get();
  }

  public ObjectProperty<@Nullable Double> actualValueProperty() {
    return actualValue;
  }

  public boolean isVisible() {
    return visible.get();
  }

  public BooleanProperty visibleProperty() {
    return visible;
  }

  public void setVisible(boolean visible) {
    this.visible.set(visible);
  }

  @Override
  public @NotNull Stroke getStroke() {
    return stroke.get();
  }

  public ObjectProperty<@NotNull Stroke> strokeProperty() {
    return stroke;
  }

  @Override
  public void setStroke(@NotNull Stroke stroke) {
    this.stroke.set(stroke);
  }

  @Override
  public @NotNull Paint getPaint() {
    return paint.get();
  }

  public ObjectProperty<@NotNull Paint> paintProperty() {
    return paint;
  }

  @Override
  public void setPaint(@NotNull Paint paint) {
    this.paint.set(paint);
  }
}
