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

import com.google.common.collect.Range;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapLabel;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapPeak;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Labels of peaks, e.g. from a feature list. They never overlap each other, axis labels, or tile
 * titles: after every view change the strongest labels are placed first, and labels without free
 * space are hidden, so zooming in shows more of them.
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
  // the veil over the data around a highlighted group, in the plot background color
  private static final double VEIL_ALPHA = 0.7;
  private static final double HOLE_PADDING = 5;
  private static final double MIN_HOLE = 12;
  // opacity of the labels outside a highlighted group
  private static final double DIMMED = 0.3;
  private static final double ISOTOPE_RADIUS = 4.5;

  private final Pane layer = new Pane();
  private final List<Label> labels = new ArrayList<>();
  private final List<Circle> markers = new ArrayList<>();
  private final List<IntensityMapPlacedLabel> placed = new ArrayList<>();
  // rings around the grouped features and isotopes of the hovered label
  private final List<Circle> rings = new ArrayList<>();
  private @Nullable IntensityMapLabel highlighted;
  private @Nullable Shape veil;
  private @NotNull Color veilColor = Color.WHITE;
  // screen area of the last placement, where the veil and the highlight rings are drawn
  private @Nullable Bounds area;
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

  private boolean isFiltered() {
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

  /**
   * @param color background of the data, the veil around a highlighted group takes its color
   */
  void setVeilColor(@NotNull final Color color) {
    veilColor = color;
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
   * @param view     state of the view; an unchanged view keeps the current placement
   * @param peaks    screen positions of peaks in the current view
   * @param occupied screen bounds of axis labels and titles
   * @param area     screen area for labels, e.g. the plot area of the 2D view
   */
  void place(@NotNull final Object view, @NotNull final IntensityMapPeakProjector peaks,
      @NotNull final List<Bounds> occupied, @NotNull final Bounds area) {
    if (Objects.equals(view, placedView)) {
      return;
    }
    placedView = view;
    this.area = area;
    placed.clear();
    final List<Bounds> taken = new ArrayList<>(occupied);
    // decision: search results are limited only by overlaps, not by the label density
    final int maximum = isFiltered() ? MAX_LABELS
        : (int) Math.clamp(area.getWidth() * area.getHeight() / AREA_PER_LABEL, 1, MAX_LABELS);
    int used = 0;
    int tried = 0;
    // groups whose main label is shown, by overlay
    final Set<String> shownGroups = new HashSet<>();
    for (final IntensityMapLabel entry : candidates()) {
      if (used == maximum || tried == MAX_CANDIDATES) {
        break;
      }
      if ((annotatedOnly && !entry.annotated()) || !matches(entry)) {
        continue;
      }
      // decision: further ions of a compound are labeled only next to its shown main label
      if (entry.secondary() && !shownGroups.contains(groupKey(entry))) {
        continue;
      }
      final Point2D point = peaks.position(entry.seriesId(), entry.apex());
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
      if (bounds == null || !peaks.unoccluded(entry, point)) {
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
      if (!entry.secondary() && entry.group() != null) {
        shownGroups.add(groupKey(entry));
      }
      used++;
    }
    hideFrom(used);
    // the highlight follows the view
    final IntensityMapLabel hovered = highlighted;
    highlighted = null;
    highlight(hovered, peaks);
  }

  /**
   * @return main labels first, strongest first, then the further ions of compounds
   */
  private @NotNull List<IntensityMapLabel> candidates() {
    final List<IntensityMapLabel> ordered = new ArrayList<>(entries.size());
    entries.stream().filter(entry -> !entry.secondary()).forEach(ordered::add);
    entries.stream().filter(IntensityMapLabel::secondary).forEach(ordered::add);
    return ordered;
  }

  private static @NotNull String groupKey(@NotNull final IntensityMapLabel entry) {
    return entry.seriesId() + " " + entry.group();
  }

  /**
   * Highlights the features grouped with a label, e.g. the other ions of its compound or of its
   * GC-EI deconvolution, and the isotope signals of all these features. Grouped features are marked
   * even if their own labels do not fit. A veil in the background color covers the rest of the data
   * and the other labels fade, so the peaks of the group stand out.
   *
   * @param label hovered label, null removes the highlight
   * @param peaks screen positions of peaks in the current view
   */
  void highlight(@Nullable final IntensityMapLabel label,
      @NotNull final IntensityMapPeakProjector peaks) {
    if (label == highlighted) {
      return;
    }
    highlighted = label;
    int used = 0;
    final List<Bounds> holes = new ArrayList<>();
    if (label != null) {
      // the label itself belongs to its group
      for (final IntensityMapLabel entry : entries) {
        if (!isGrouped(label, entry)) {
          continue;
        }
        used = ring(used, peaks.position(entry.seriesId(), entry.apex()), false);
        addHole(holes,
            peaks.extent(entry.seriesId(), entry.apex(), entry.xRange(), entry.yRange()));
        // ions grouped by deconvolution have no labels of their own
        for (final IntensityMapPeak ion : entry.grouped()) {
          used = ring(used, peaks.position(entry.seriesId(), ion), false);
          addHole(holes, extentLike(peaks, entry, ion));
        }
        for (final IntensityMapPeak isotope : entry.isotopes()) {
          used = ring(used, peaks.position(entry.seriesId(), isotope), true);
          addHole(holes, extentLike(peaks, entry, isotope));
        }
      }
    }
    for (int i = used; i < rings.size(); i++) {
      rings.get(i).setVisible(false);
    }
    for (int i = 0; i < placed.size(); i++) {
      final boolean grouped = label != null && isGrouped(label, placed.get(i).label());
      style(labels.get(i), grouped);
      final double opacity = label == null || grouped ? 1 : DIMMED;
      labels.get(i).setOpacity(opacity);
      markers.get(i).setOpacity(opacity);
    }
    updateVeil(label == null ? null : holes);
  }

  /**
   * @return screen bounds of a signal without an extent of its own, e.g. an isotope, with the
   * extent of the labeled feature
   */
  private static @Nullable Bounds extentLike(@NotNull final IntensityMapPeakProjector peaks,
      @NotNull final IntensityMapLabel label, @NotNull final IntensityMapPeak peak) {
    final double dx = peak.x() - label.x();
    final double dy = peak.y() - label.y();
    return peaks.extent(label.seriesId(), peak,
        Range.closed(label.xRange().lowerEndpoint() + dx, label.xRange().upperEndpoint() + dx),
        Range.closed(label.yRange().lowerEndpoint() + dy, label.yRange().upperEndpoint() + dy));
  }

  private static void addHole(@NotNull final List<Bounds> holes, @Nullable final Bounds extent) {
    if (extent == null) {
      return;
    }
    // narrow peaks, e.g. of a single m/z bin, still get a visible opening
    final double width = Math.max(MIN_HOLE, extent.getWidth()) + 2 * HOLE_PADDING;
    final double height = Math.max(MIN_HOLE, extent.getHeight()) + 2 * HOLE_PADDING;
    holes.add(
        new BoundingBox(extent.getCenterX() - width / 2, extent.getCenterY() - height / 2, width,
            height));
  }

  /**
   * @param holes openings of the highlighted peaks, null removes the veil
   */
  private void updateVeil(@Nullable final List<Bounds> holes) {
    if (veil != null) {
      layer.getChildren().remove(veil);
      veil = null;
    }
    final Bounds covered = area;
    if (holes == null || covered == null) {
      return;
    }
    Shape shape = new Rectangle(covered.getMinX(), covered.getMinY(), covered.getWidth(),
        covered.getHeight());
    for (final Bounds hole : holes) {
      final Rectangle opening = new Rectangle(hole.getMinX(), hole.getMinY(), hole.getWidth(),
          hole.getHeight());
      opening.setArcWidth(8);
      opening.setArcHeight(8);
      shape = Shape.subtract(shape, opening);
    }
    shape.setFill(veilColor.deriveColor(0, 1, 1, VEIL_ALPHA));
    shape.setStroke(null);
    shape.setManaged(false);
    shape.setMouseTransparent(true);
    veil = shape;
    // below the labels, markers, and rings
    layer.getChildren().addFirst(shape);
  }

  void clearHighlight() {
    if (highlighted == null) {
      return;
    }
    highlighted = null;
    rings.forEach(ring -> ring.setVisible(false));
    for (int i = 0; i < placed.size() && i < labels.size(); i++) {
      style(labels.get(i), false);
      labels.get(i).setOpacity(1);
      markers.get(i).setOpacity(1);
    }
    updateVeil(null);
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
    // grouped peaks outside the zoomed range have no ring, like labels outside the plot area
    final Bounds covered = area;
    if (point == null || covered == null || !covered.contains(point)) {
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
    final String background = IntensityMapTheme.labelBackground(dark);
    // an outline by a second background keeps the label size, unlike a border
    label.setStyle(
        (grouped ? "-fx-background-color: " + IntensityMapPlot.ACCENT_HEX + ", " + background
            + "; -fx-background-insets: 0, 1;" : "-fx-background-color: " + background + ";")
            + IntensityMapTheme.text(dark)
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
