package io.github.mzmine.modules.visualization.featurerow4dplot;

import io.github.mzmine.main.ConfigService;
import io.github.mzmine.util.color.SimpleColorPalette;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import org.jetbrains.annotations.NotNull;

/**
 * Stylised square icon of the 4D feature plot: a handful of bubbles arranged in a scatter, framed
 * by an L-shaped black axis. Bubble fills are sampled from
 * {@link ConfigService#getDefaultColorPalette()} so the icon tracks the user's current palette.
 * <p>
 * Designed in a 24×24 reference coordinate system and scaled to whatever {@code size} the caller
 * passes. Sub-classes {@link Pane} with an enforced square pref/min/max size so the icon — and the
 * button hosting it — render as a true square regardless of children's intrinsic bounds.
 */
public class FeatureRow4DPlotIcon extends Pane {

  // Reference design size — all coordinates below are expressed in this 24×24 box and then scaled
  // uniformly by `size / REFERENCE_SIZE` for the requested output size.
  private static final double REFERENCE_SIZE = 24.0;

  // Alpha applied to palette colours so overlapping bubbles still read as separate bubbles at icon
  // scale instead of merging into a single coloured blob.
  private static final double BUBBLE_ALPHA = 0.95;

  // Selection ring colour — same orange used by ColoredBubbleDatasetRenderer's selection overlay
  // (ConfigService default-palette negative). Hardcoded here so the icon stays stable across theme
  // swaps and palette changes.
  private static final Color C_HIGHLIGHT_STROKE = Color.rgb(222, 110, 48);
  // Axis colour — pure black so the L-frame reads at small sizes against any background.
  private static final Color C_AXIS = Color.BLACK;

  /**
   * Convenience constructor for an 18-px icon — matches
   * {@link io.github.mzmine.javafx.util.FxIconUtil#DEFAULT_ICON_SIZE} so the toggle button lines up
   * height-wise with the neighbouring icon buttons.
   */
  public FeatureRow4DPlotIcon() {
    this(18.0);
  }

  public FeatureRow4DPlotIcon(final double size) {
    // Enforce a square bounding box so callers (e.g. Button#setGraphic) get a quadratic icon.
    setMinSize(size, size);
    setPrefSize(size, size);
    setMaxSize(size, size);

    final double s = size / REFERENCE_SIZE;

    // Sampled at construction time — palette swaps won't repaint an existing icon, but any newly
    // built icon will reflect the user's current default palette.
    final SimpleColorPalette palette = ConfigService.getDefaultColorPalette();

    // L-shaped axis frame. Drawn first so the bubbles sit on top of it.
    addAxisLine(3.0, 2.0, 3.0, 21.0, s);   // y-axis (vertical, left)
    addAxisLine(3.0, 21.0, 22.0, 21.0, s); // x-axis (horizontal, bottom)

    // Bubbles arranged to evoke the screenshot: denser low-RT cluster on the left, sparser
    // high-RT cluster on the right, sizes vary so the "bubble" character reads at a glance. All
    // points stay inside the axis frame (x in [4, 22], y in [3, 20]). Palette indices are cycled
    // by SimpleColorPalette#get so out-of-range indices wrap automatically.
    addBubble(7.0, 16.5, 2.8, bubbleColor(palette, 0), s);
    addBubble(10.0, 11.5, 3.2, bubbleColor(palette, 0), s);
    addBubble(6.0, 10.5, 2.2, bubbleColor(palette, 0), s);
    addBubble(12.5, 8.5, 4.1, bubbleColor(palette, 1), s);
    addBubble(16.0, 14.5, 3.6, bubbleColor(palette, 2), s);
    addBubble(20.0, 5.5, 3.0, bubbleColor(palette, 3), s);
    addBubble(20.5, 12.5, 2.4, bubbleColor(palette, 3), s);

    // The "selected" bubble — filled like its neighbours plus an orange outline ring, mirroring
    // the chart's selection overlay so the icon visually advertises the toggleable feature.
//    addHighlight(14.5, 7.5, 3.0, bubbleColor(palette, 2), s);
  }

  private static @NotNull Color bubbleColor(@NotNull final SimpleColorPalette palette,
      final int index) {
    final Color c = palette.get(index);
    return new Color(c.getRed(), c.getGreen(), c.getBlue(), BUBBLE_ALPHA);
  }

  private void addAxisLine(final double x1, final double y1, final double x2, final double y2,
      final double scale) {
    final Line line = new Line(x1 * scale, y1 * scale, x2 * scale, y2 * scale);
    line.setStroke(C_AXIS);
    // Stroke scales with the icon but clamped at 1 px so the axis is always visible at 16-18 px.
    line.setStrokeWidth(Math.max(1.0, 1.2 * scale));
    line.setStrokeLineCap(StrokeLineCap.BUTT);
    getChildren().add(line);
  }

  private void addBubble(final double cx, final double cy, final double r,
      @NotNull final Color fill, final double scale) {
    final Circle c = new Circle(cx * scale, cy * scale, r * scale);
    c.setFill(fill);
    getChildren().add(c);
  }

  private void addHighlight(final double cx, final double cy, final double r,
      @NotNull final Color fill, final double scale) {
    final Circle c = new Circle(cx * scale, cy * scale, r * scale);
    c.setFill(fill);
    c.setStroke(C_HIGHLIGHT_STROKE);
    // Stroke width scales with the icon so the ring stays proportional at small sizes; clamped at
    // 1 px so it remains visible even on the 16-px toolbar variant.
    c.setStrokeWidth(Math.max(1.0, 1.4 * scale));
    getChildren().add(c);
  }
}
