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

package io.github.mzmine.modules.visualization.intensitymap.plot;

import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapLabel;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapPeak;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Labels of peaks, e.g. from a feature list (user request). They never overlap each other, axis
 * labels, or tile titles: after every view change the strongest labels are placed first, and labels
 * without free space are hidden, so zooming in shows more of them.
 */
final class IntensityMapFeatureLabels {

  // assumption: more labels are not readable on a typical screen, and placing them stays cheap
  private static final int MAX_LABELS = 150;
  // decision: at most one label per area of this size, so the labels do not bury the data
  private static final double AREA_PER_LABEL = 100 * 80;
  // labels tried per placement, strongest first
  private static final int MAX_CANDIDATES = 3000;
  private static final double MAX_WIDTH = 180;
  private static final double MARKER_RADIUS = 2.5;
  private static final double GAP = 3;
  // clicks this close to a marker hit its label
  private static final double CLICK_RADIUS = MARKER_RADIUS + 3;
  private static final double GROUP_RADIUS = 6;
  private static final double ISOTOPE_RADIUS = 4.5;

  private static final String ACCENT_CSS = String.format("#%02x%02x%02x",
      Math.round(IntensityMapPlot.ACCENT.getRed() * 255),
      Math.round(IntensityMapPlot.ACCENT.getGreen() * 255),
      Math.round(IntensityMapPlot.ACCENT.getBlue() * 255));

  private final Pane layer = new Pane();
  private final List<Label> labels = new ArrayList<>();
  private final List<Circle> markers = new ArrayList<>();
  private final List<IntensityMapPlacedLabel> placed = new ArrayList<>();
  // rings around the grouped features and isotopes of the hovered label
  private final List<Circle> rings = new ArrayList<>();
  private @Nullable IntensityMapLabel highlighted;
  private List<IntensityMapLabel> entries = List.of();
  // distinct names of the labels, suggestions of the search
  private List<String> names = List.of();
  // lower case search text, empty shows all labels
  private String filter = "";
  private boolean annotatedOnly;
  private boolean dark;
  // view of the current placement, to skip placing again for an unchanged view
  private @Nullable Object placedView;

  IntensityMapFeatureLabels() {
    layer.setMouseTransparent(true);
    layer.setPickOnBounds(false);
  }

  /**
   * @return layer of labels and markers above the plot, in viewport coordinates
   */
  @NotNull Pane layer() {
    return layer;
  }

  boolean isEmpty() {
    return entries.isEmpty();
  }

  void setLabels(@NotNull final List<IntensityMapLabel> values) {
    // primary labels first, then by intensity
    entries = values.stream().sorted(Comparator.comparing(IntensityMapLabel::secondary)
            .thenComparing(Comparator.comparingDouble(IntensityMapLabel::intensity).reversed()))
        .toList();
    clearHighlight();
    names = values.stream().map(IntensityMapLabel::name)
        .filter(name -> name != null && !name.isBlank()).distinct()
        .sorted(String.CASE_INSENSITIVE_ORDER).toList();
    placedView = null;
  }

  /**
   * @return distinct annotation and compound names of the labels, sorted
   */
  @NotNull List<String> names() {
    return names;
  }

  /**
   * @param text only labels whose name, text, or description contain the text (ignoring case) are
   *             shown; null or blank shows all labels
   */
  void setFilter(@Nullable final String text) {
    final String value = text == null ? "" : text.trim().toLowerCase();
    if (!value.equals(filter)) {
      filter = value;
      placedView = null;
    }
  }

  boolean isFiltered() {
    return !filter.isEmpty();
  }

  boolean matches(@NotNull final IntensityMapLabel entry) {
    if (filter.isEmpty()) {
      return true;
    }
    return (entry.name() != null && entry.name().toLowerCase().contains(filter)) || entry.text()
        .toLowerCase().contains(filter) || entry.description().toLowerCase().contains(filter);
  }

  void setAnnotatedOnly(final boolean annotatedOnly) {
    this.annotatedOnly = annotatedOnly;
    placedView = null;
  }

  void setDark(final boolean dark) {
    this.dark = dark;
    labels.forEach(label -> style(label, false));
    placedView = null;
  }

  /**
   * Hides all labels.
   */
  void clear() {
    clearHighlight();
    placed.clear();
    placedView = null;
    hideFrom(0);
  }

  /**
   * Places the labels that fit, strongest first.
   *
   * @param view       state of the view; an unchanged view keeps the current placement
   * @param position   screen position of a signal of an overlay, null if it is not shown, e.g. of a
   *                   hidden overlay, outside the data, or below the noise cut-off
   * @param unoccluded true if nothing in the view covers the marker at the screen position
   * @param occupied   screen bounds of axis labels and titles
   * @param area       screen area for labels, e.g. the plot area of the 2D view
   */
  void place(@NotNull final Object view,
      @NotNull final BiFunction<String, IntensityMapPeak, @Nullable Point2D> position,
      @NotNull final BiPredicate<IntensityMapLabel, Point2D> unoccluded,
      @NotNull final List<Bounds> occupied, @NotNull final Bounds area) {
    if (Objects.equals(view, placedView)) {
      return;
    }
    placedView = view;
    placed.clear();
    final List<Bounds> taken = new ArrayList<>(occupied);
    // decision: searched labels are what the user asked for, they are only limited by overlaps
    final int maximum = isFiltered() ? MAX_LABELS
        : (int) Math.clamp(area.getWidth() * area.getHeight() / AREA_PER_LABEL, 1, MAX_LABELS);
    int used = 0;
    int tried = 0;
    for (final IntensityMapLabel entry : entries) {
      if (used == maximum || tried == MAX_CANDIDATES) {
        break;
      }
      if ((annotatedOnly && !entry.annotated()) || !matches(entry)) {
        continue;
      }
      final Point2D point = position.apply(entry.seriesId(), entry.apex());
      if (point == null || !area.contains(point)) {
        continue;
      }
      tried++;
      final Bounds marker = new BoundingBox(point.getX() - MARKER_RADIUS,
          point.getY() - MARKER_RADIUS, 2 * MARKER_RADIUS, 2 * MARKER_RADIUS);
      if (intersects(marker, taken)) {
        continue;
      }
      final Label label = label(used);
      label.setText(entry.text());
      label.applyCss();
      final double width = Math.min(label.prefWidth(-1), MAX_WIDTH);
      final double height = label.prefHeight(width);
      final Bounds bounds = free(point, width, height, taken, area);
      if (bounds == null || !unoccluded.test(entry, point)) {
        continue;
      }
      label.resizeRelocate(bounds.getMinX(), bounds.getMinY(), width, height);
      label.setVisible(true);
      final Circle circle = marker(used);
      circle.setCenterX(point.getX());
      circle.setCenterY(point.getY());
      circle.setVisible(true);
      taken.add(bounds);
      taken.add(marker);
      placed.add(new IntensityMapPlacedLabel(entry, point.getX(), point.getY(), bounds));
      used++;
    }
    hideFrom(used);
    // the highlight follows the view
    final IntensityMapLabel hovered = highlighted;
    highlighted = null;
    highlight(hovered, position);
  }

  /**
   * Highlights the features grouped with a label, e.g. the other ions of its compound, and the
   * isotope signals of its feature (user request). Grouped features are marked even if their own
   * labels do not fit.
   *
   * @param label    hovered label, null removes the highlight
   * @param position screen position of a signal of an overlay, null if not shown
   */
  void highlight(@Nullable final IntensityMapLabel label,
      @NotNull final BiFunction<String, IntensityMapPeak, @Nullable Point2D> position) {
    if (label == highlighted) {
      return;
    }
    highlighted = label;
    int used = 0;
    if (label != null) {
      for (final IntensityMapLabel entry : entries) {
        if (isGrouped(label, entry)) {
          used = ring(used, position.apply(entry.seriesId(), entry.apex()), false);
        }
      }
      for (final IntensityMapPeak isotope : label.isotopes()) {
        used = ring(used, position.apply(label.seriesId(), isotope), true);
      }
    }
    for (int i = used; i < rings.size(); i++) {
      rings.get(i).setVisible(false);
    }
    for (int i = 0; i < placed.size(); i++) {
      style(labels.get(i), label != null && isGrouped(label, placed.get(i).label()));
    }
  }

  void clearHighlight() {
    if (highlighted == null) {
      return;
    }
    highlighted = null;
    rings.forEach(ring -> ring.setVisible(false));
    for (int i = 0; i < placed.size() && i < labels.size(); i++) {
      style(labels.get(i), false);
    }
  }

  /**
   * @return true for the label itself and features of its group in the same overlay
   */
  private static boolean isGrouped(@NotNull final IntensityMapLabel label,
      @NotNull final IntensityMapLabel other) {
    return label == other || (label.group() != null && label.seriesId().equals(other.seriesId())
        && label.group().equals(other.group()));
  }

  /**
   * @return the number of used rings, one more if the point is shown
   */
  private int ring(final int used, @Nullable final Point2D point, final boolean isotope) {
    if (point == null) {
      return used;
    }
    while (rings.size() <= used) {
      final Circle ring = new Circle();
      ring.setFill(null);
      ring.setStroke(IntensityMapPlot.ACCENT);
      ring.setManaged(false);
      rings.add(ring);
      layer.getChildren().add(ring);
    }
    final Circle ring = rings.get(used);
    ring.setRadius(isotope ? ISOTOPE_RADIUS : GROUP_RADIUS);
    ring.setStrokeWidth(isotope ? 1.2 : 1.8);
    // dashed rings are isotopes, solid rings grouped features
    ring.getStrokeDashArray().setAll(isotope ? List.of(2.5, 2.0) : List.of());
    ring.setCenterX(point.getX());
    ring.setCenterY(point.getY());
    ring.setVisible(true);
    return used + 1;
  }

  /**
   * @return label bounds next to the marker that are free, trying above right, above left, and
   * below right; null if none is free
   */
  static @Nullable Bounds free(@NotNull final Point2D point, final double width,
      final double height, @NotNull final List<Bounds> taken, @NotNull final Bounds area) {
    final double[][] offsets = {{GAP, -height - GAP}, {-width - GAP, -height - GAP}, {GAP, GAP}};
    for (final double[] offset : offsets) {
      final Bounds candidate = new BoundingBox(point.getX() + offset[0], point.getY() + offset[1],
          width, height);
      if (area.contains(candidate) && !intersects(candidate, taken)) {
        return candidate;
      }
    }
    return null;
  }

  private static boolean intersects(@NotNull final Bounds bounds,
      @NotNull final List<Bounds> taken) {
    for (final Bounds other : taken) {
      if (bounds.intersects(other)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return the shown label whose text or marker is at the screen position, null if none
   */
  @Nullable IntensityMapLabel at(final double x, final double y) {
    if (!layer.isVisible()) {
      return null;
    }
    for (final IntensityMapPlacedLabel label : placed) {
      if (label.bounds().contains(x, y)
          || Math.hypot(label.x() - x, label.y() - y) <= CLICK_RADIUS) {
        return label.label();
      }
    }
    return null;
  }

  private @NotNull Label label(final int index) {
    while (labels.size() <= index) {
      final Label label = new Label();
      label.setManaged(false);
      label.setMaxWidth(MAX_WIDTH);
      style(label, false);
      labels.add(label);
      layer.getChildren().add(label);
    }
    return labels.get(index);
  }

  private @NotNull Circle marker(final int index) {
    while (markers.size() <= index) {
      final Circle circle = new Circle(MARKER_RADIUS, IntensityMapPlot.ACCENT);
      circle.setStroke(Color.WHITE);
      circle.setStrokeWidth(0.8);
      circle.setManaged(false);
      markers.add(circle);
      layer.getChildren().add(circle);
    }
    return markers.get(index);
  }

  /**
   * @param grouped framed in the accent color, e.g. grouped with the hovered label
   */
  private void style(@NotNull final Label label, final boolean grouped) {
    final String background = dark ? "rgba(30,35,42,0.85)" : "rgba(255,255,255,0.85)";
    // an outline by a second background keeps the label size, unlike a border
    label.setStyle((grouped ? "-fx-background-color: " + ACCENT_CSS + ", " + background
        + "; -fx-background-insets: 0, 1;" : "-fx-background-color: " + background + ";") + (dark
        ? "-fx-text-fill: #e2e8f0;" : "-fx-text-fill: #1e293b;")
        + "-fx-font-size: 11; -fx-padding: 0 3 0 3; -fx-background-radius: 3;");
  }

  private void hideFrom(final int index) {
    for (int i = index; i < labels.size(); i++) {
      labels.get(i).setVisible(false);
    }
    for (int i = index; i < markers.size(); i++) {
      markers.get(i).setVisible(false);
    }
  }
}
