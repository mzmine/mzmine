/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 */

package io.github.mzmine.modules.visualization.spectra.matchedlipid;

import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRuleRating;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.annotations.AbstractXYAnnotation;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.Range;

/**
 * Draws all OAD double-bond evidence groups as one collision-aware chart annotation.
 * <p>
 * Each group label is centered above the m/z span of its visible evidence signals. The layout then
 * searches upward until neither the label nor its horizontal evidence rail overlaps another group
 * label or a conventional matched-fragment label. The rail starts at the lowest-m/z evidence
 * signal and ends at the highest-m/z evidence signal; thin vertical leaders connect it to every
 * signal in between. Keeping all groups in one annotation is essential: independent annotations
 * cannot coordinate their occupied screen space and caused the overlapping labels and diagonal
 * leader fans this class replaces.
 * <p>
 * The tooltip for each group contains one row per signal with its diagnostic role, rule/scaffold,
 * formula, calculated and observed mass, and mass error. Axis conversion happens during every draw
 * call, so the layout remains aligned after zooming.
 */
public final class DoubleBondEvidenceAnnotation extends AbstractXYAnnotation {

  private static final double LABEL_GAP = 8d;
  private static final double LABEL_PADDING = 4d;
  private static final double CANDIDATE_GAP = 7d;
  private static final double RAIL_GAP = 3d;
  private static final int MAX_VERTICAL_CANDIDATES = 24;
  private static final Stroke LABEL_STROKE = new BasicStroke(1.1f);
  private static final Stroke RAIL_STROKE = new BasicStroke(1.5f);
  private static final Stroke LEADER_STROKE = new BasicStroke(0.55f);

  private final @NotNull List<StyledEvidenceGroup> groups;
  private final @NotNull Supplier<List<Rectangle2D>> occupiedBoundsSupplier;

  private DoubleBondEvidenceAnnotation(final @NotNull List<StyledEvidenceGroup> groups,
      final @NotNull Supplier<List<Rectangle2D>> occupiedBoundsSupplier) {
    this.groups = List.copyOf(groups);
    this.occupiedBoundsSupplier = occupiedBoundsSupplier;
  }

  /**
   * Creates one annotation that jointly lays out every complete double-bond evidence group.
   *
   * @param fragments fragments belonging to one matched lipid
   * @param colors color-blind-aware colors used cyclically for group labels and leaders
   * @param occupiedBoundsSupplier current screen bounds of conventional fragment labels; evaluated
   *                               during drawing after the matched-fragment renderer has run
   * @return a combined annotation, or {@code null} when no complete group is present
   */
  public static @Nullable DoubleBondEvidenceAnnotation createAnnotation(
      final @NotNull List<LipidFragment> fragments, final @NotNull List<Color> colors,
      final @NotNull Supplier<List<Rectangle2D>> occupiedBoundsSupplier) {
    final List<EvidenceGroup> evidenceGroups = createGroups(fragments);
    if (evidenceGroups.isEmpty()) {
      return null;
    }
    final List<StyledEvidenceGroup> styledGroups = new ArrayList<>();
    for (int i = 0; i < evidenceGroups.size(); i++) {
      styledGroups.add(new StyledEvidenceGroup(evidenceGroups.get(i), colorAt(colors, i)));
    }
    return new DoubleBondEvidenceAnnotation(styledGroups, occupiedBoundsSupplier);
  }

  private static @NotNull List<EvidenceGroup> createGroups(
      final @NotNull List<LipidFragment> fragments) {
    final Map<EvidenceGroupKey, List<LipidFragment>> fragmentsByPosition = new LinkedHashMap<>();
    for (final LipidFragment fragment : fragments) {
      final EvidenceGroupKey key = EvidenceGroupKey.from(fragment);
      if (key != null) {
        fragmentsByPosition.computeIfAbsent(key, _ -> new ArrayList<>()).add(fragment);
      }
    }
    return fragmentsByPosition.entrySet().stream()
        .map(entry -> EvidenceGroup.create(entry.getKey(), entry.getValue()))
        .filter(Objects::nonNull).sorted(Comparator.comparingDouble(EvidenceGroup::lowerMz))
        .toList();
  }

  private static @NotNull Paint colorAt(final @NotNull List<Color> colors, final int index) {
    return colors.isEmpty() ? Color.BLUE : colors.get(index % colors.size());
  }

  @Override
  public void draw(final @NotNull Graphics2D graphics, final @NotNull XYPlot plot,
      final @NotNull Rectangle2D dataArea, final @NotNull ValueAxis domainAxis,
      final @NotNull ValueAxis rangeAxis, final int rendererIndex,
      final @Nullable PlotRenderingInfo info) {
    if (plot.getOrientation() != PlotOrientation.VERTICAL) {
      return;
    }
    final RectangleEdge domainEdge = Plot.resolveDomainAxisLocation(plot.getDomainAxisLocation(),
        plot.getOrientation());
    final RectangleEdge rangeEdge = Plot.resolveRangeAxisLocation(plot.getRangeAxisLocation(),
        plot.getOrientation());
    final Font oldFont = graphics.getFont();
    final Paint oldPaint = graphics.getPaint();
    final Stroke oldStroke = graphics.getStroke();
    try {
      graphics.setFont(oldFont.deriveFont(Font.BOLD, 11f));
      final FontMetrics metrics = graphics.getFontMetrics();
      final List<Rectangle2D> occupied = new ArrayList<>(occupiedBoundsSupplier.get());
      final List<GroupLayout> layouts = layoutGroups(dataArea, domainAxis, rangeAxis, domainEdge,
          rangeEdge, metrics, occupied);
      drawLeaders(graphics, dataArea, layouts);
      drawLabels(graphics, metrics, rendererIndex, info, layouts);
    } finally {
      graphics.setFont(oldFont);
      graphics.setPaint(oldPaint);
      graphics.setStroke(oldStroke);
    }
  }

  private @NotNull List<GroupLayout> layoutGroups(final @NotNull Rectangle2D dataArea,
      final @NotNull ValueAxis domainAxis, final @NotNull ValueAxis rangeAxis,
      final @NotNull RectangleEdge domainEdge, final @NotNull RectangleEdge rangeEdge,
      final @NotNull FontMetrics metrics, final @NotNull List<Rectangle2D> occupied) {
    final List<VisibleGroup> visibleGroups = groups.stream()
        .map(group -> VisibleGroup.create(group, dataArea, domainAxis, rangeAxis, domainEdge,
            rangeEdge)).filter(Objects::nonNull)
        .sorted(Comparator.comparingDouble(VisibleGroup::highestIntensity).reversed()).toList();
    final List<GroupLayout> layouts = new ArrayList<>();
    for (final VisibleGroup visible : visibleGroups) {
      final GroupLayout layout = placeGroup(visible, dataArea, metrics, occupied);
      layouts.add(layout);
      occupied.add(expand(layout.labelBounds(), LABEL_PADDING));
      occupied.add(expand(layout.railBounds(), LABEL_PADDING));
    }
    return List.copyOf(layouts);
  }

  private @NotNull GroupLayout placeGroup(final @NotNull VisibleGroup visible,
      final @NotNull Rectangle2D dataArea, final @NotNull FontMetrics metrics,
      final @NotNull List<Rectangle2D> occupied) {
    final String label = visible.styledGroup().label();
    final double labelWidth = metrics.stringWidth(label);
    final double labelHeight = metrics.getHeight();
    final double preferredLeft = (visible.railStartX() + visible.railEndX() - labelWidth) / 2d;
    final double preferredTop = visible.highestSignal().y() - LABEL_GAP - labelHeight;
    GroupLayout best = null;
    double bestOverlap = Double.POSITIVE_INFINITY;

    for (int vertical = 0; vertical <= MAX_VERTICAL_CANDIDATES; vertical++) {
      final double candidateTop = Math.max(dataArea.getMinY() + LABEL_PADDING,
          preferredTop - vertical * (labelHeight + CANDIDATE_GAP));
      final Rectangle2D labelBounds = new Rectangle2D.Double(preferredLeft, candidateTop,
          labelWidth, labelHeight);
      final GroupLayout candidate = GroupLayout.create(visible, labelBounds);
      final double overlap = overlapArea(candidate.labelBounds(), occupied)
          + overlapArea(candidate.railBounds(), occupied);
      if (overlap == 0d) {
        return candidate;
      }
      if (overlap < bestOverlap) {
        best = candidate;
        bestOverlap = overlap;
      }
      if (candidateTop <= dataArea.getMinY() + LABEL_PADDING) {
        break;
      }
    }
    return Objects.requireNonNull(best);
  }

  private static double overlapArea(final @NotNull Rectangle2D candidate,
      final @NotNull List<Rectangle2D> occupied) {
    final Rectangle2D padded = expand(candidate, LABEL_PADDING);
    double overlap = 0d;
    for (final Rectangle2D other : occupied) {
      final Rectangle2D intersection = padded.createIntersection(other);
      if (!intersection.isEmpty()) {
        overlap += intersection.getWidth() * intersection.getHeight();
      }
    }
    return overlap;
  }

  private static @NotNull Rectangle2D expand(final @NotNull Rectangle2D bounds,
      final double padding) {
    return new Rectangle2D.Double(bounds.getX() - padding, bounds.getY() - padding,
        bounds.getWidth() + 2d * padding, bounds.getHeight() + 2d * padding);
  }

  private void drawLeaders(final @NotNull Graphics2D graphics,
      final @NotNull Rectangle2D dataArea, final @NotNull List<GroupLayout> layouts) {
    for (final GroupLayout layout : layouts) {
      graphics.setPaint(leaderPaint(layout.styledGroup().paint()));
      final Rectangle2D rail = layout.railBounds();
      graphics.setStroke(RAIL_STROKE);
      graphics.draw(new Line2D.Double(rail.getMinX(), rail.getCenterY(), rail.getMaxX(),
          rail.getCenterY()));
      graphics.setStroke(LEADER_STROKE);
      graphics.draw(new Line2D.Double(layout.labelBounds().getCenterX(),
          layout.labelBounds().getMaxY(), layout.labelBounds().getCenterX(), rail.getCenterY()));
      for (final RenderedSignal signal : layout.visibleSignals()) {
        final double signalY = clamp(signal.y(), dataArea.getMinY(), dataArea.getMaxY());
        graphics.draw(new Line2D.Double(signal.x(), rail.getCenterY(), signal.x(), signalY));
      }
    }
  }

  private void drawLabels(final @NotNull Graphics2D graphics, final @NotNull FontMetrics metrics,
      final int rendererIndex, final @Nullable PlotRenderingInfo info,
      final @NotNull List<GroupLayout> layouts) {
    for (final GroupLayout layout : layouts) {
      final Rectangle2D bounds = layout.labelBounds();
      graphics.setPaint(LipidLabelPainter.labelBackgroundPaint());
      graphics.fill(bounds);
      graphics.setPaint(layout.styledGroup().paint());
      graphics.setStroke(LABEL_STROKE);
      final float baseline = (float) (bounds.getY() + metrics.getAscent());
      graphics.drawString(layout.styledGroup().label(), (float) bounds.getX(), baseline);
      if (info != null) {
        addEntity(info, bounds, rendererIndex, layout.styledGroup().group().tooltip(
            layout.styledGroup().label()), getURL());
      }
    }
  }

  private static @NotNull Paint leaderPaint(final @NotNull Paint paint) {
    if (paint instanceof Color color) {
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), 105);
    }
    return paint;
  }

  private static double clamp(final double value, final double minimum, final double maximum) {
    return Math.max(minimum, Math.min(value, maximum));
  }

  int groupCount() {
    return groups.size();
  }

  @NotNull String label(final int groupIndex) {
    return groups.get(groupIndex).label();
  }

  double highestSignalMz(final int groupIndex) {
    return groups.get(groupIndex).group().highestSignal().mz();
  }

  int signalCount(final int groupIndex) {
    return groups.get(groupIndex).group().signals().size();
  }

  @NotNull String tooltip(final int groupIndex) {
    final StyledEvidenceGroup styled = groups.get(groupIndex);
    return styled.group().tooltip(styled.label());
  }

  /**
   * Returns the m/z span occupied by the evidence signals of every complete OAD group. The matched
   * fragment renderer uses these ranges to keep conventional label boxes out of the vertical
   * leader corridors whenever the available plot space permits it.
   */
  @NotNull List<Range> evidenceMzRanges() {
    return groups.stream().map(styled -> new Range(styled.group().lowerMz(),
        styled.group().upperMz())).toList();
  }

  private record EvidenceGroupKey(int chainLength, int doubleBonds, int deltaPosition,
                                  @NotNull LipidChainType chainType) {

    private static @Nullable EvidenceGroupKey from(final @NotNull LipidFragment fragment) {
      if (fragment.getRuleType()
          != LipidFragmentationRuleType.OAD_DOUBLE_BOND_POSITION_FRAGMENT
          || fragment.getDoubleBondPosition() == null || fragment.getChainLength() == null
          || fragment.getNumberOfDBEs() == null || fragment.getLipidChainType() == null) {
        return null;
      }
      return new EvidenceGroupKey(fragment.getChainLength(), fragment.getNumberOfDBEs(),
          fragment.getDoubleBondPosition(), fragment.getLipidChainType());
    }

    private @NotNull String label(final int signalCount) {
      final int omegaPosition = chainLength - deltaPosition;
      return chainLength + ":" + doubleBonds + " n-" + omegaPosition + " / Δ"
          + deltaPosition + " · " + signalCount + " ions";
    }
  }

  private record EvidenceSignal(double mz, double intensity, @NotNull String tooltipRow) {

    private static @NotNull EvidenceSignal from(final @NotNull LipidFragment fragment) {
      return new EvidenceSignal(fragment.getDataPoint().getMZ(),
          fragment.getDataPoint().getIntensity(), tooltipRow(fragment));
    }

    private static @NotNull String tooltipRow(final @NotNull LipidFragment fragment) {
      final String observedMz = ConfigService.getConfiguration().getMZFormat()
          .format(fragment.getDataPoint().getMZ());
      final String calculatedMz = ConfigService.getConfiguration().getMZFormat()
          .format(fragment.getMzExact());
      final double ppm = (fragment.getMzExact() - fragment.getDataPoint().getMZ())
          / fragment.getMzExact() * 1_000_000d;
      final String ppmText = ConfigService.getConfiguration().getPPMFormat().format(ppm);
      final String role = fragment.getLipidFragmentationRuleRating()
          == LipidFragmentationRuleRating.MAJOR ? "diagnostic pair" : "supporting";
      final String contribution = Objects.requireNonNullElse(
          fragment.getOriginatingRuleFormula(), fragment.getRuleType().toString())
          .replaceAll("\\R", " ");
      return "m/z " + observedMz + " (calc. " + calculatedMz + ") | " + role + " | "
          + contribution + " | " + fragment.getIonFormula() + " | Δ " + ppmText + " ppm";
    }
  }

  private record EvidenceGroup(@NotNull EvidenceGroupKey key,
                               @NotNull List<EvidenceSignal> signals,
                               @NotNull EvidenceSignal highestSignal, double lowerMz,
                               double upperMz) {

    private static @Nullable EvidenceGroup create(final @NotNull EvidenceGroupKey key,
        final @NotNull List<LipidFragment> fragments) {
      final long diagnosticSignals = fragments.stream().filter(fragment ->
              fragment.getLipidFragmentationRuleRating() == LipidFragmentationRuleRating.MAJOR)
          .map(LipidFragment::getDataPoint)
          .mapToLong(point -> Double.doubleToLongBits(point.getMZ())).distinct().count();
      if (diagnosticSignals < 2) {
        return null;
      }
      final Map<Long, LipidFragment> distinctFragments = new LinkedHashMap<>();
      for (final LipidFragment fragment : fragments) {
        final long mzBits = Double.doubleToLongBits(fragment.getDataPoint().getMZ());
        distinctFragments.merge(mzBits, fragment, (first, second) ->
            first.getLipidFragmentationRuleRating() == LipidFragmentationRuleRating.MAJOR ? first
                : second);
      }
      final List<EvidenceSignal> signals = distinctFragments.values().stream()
          .map(EvidenceSignal::from).sorted(Comparator.comparingDouble(EvidenceSignal::mz)).toList();
      final EvidenceSignal highestSignal = signals.stream()
          .max(Comparator.comparingDouble(EvidenceSignal::intensity)).orElseThrow();
      return new EvidenceGroup(key, signals, highestSignal, signals.getFirst().mz(),
          signals.getLast().mz());
    }

    private @NotNull String tooltip(final @NotNull String label) {
      final StringBuilder tooltip = new StringBuilder(label)
          .append("\nObserved m/z (calculated) | contribution | rule and scaffold | formula | error");
      for (final EvidenceSignal signal : signals) {
        tooltip.append("\n").append(signal.tooltipRow());
      }
      return tooltip.toString();
    }
  }

  private record StyledEvidenceGroup(@NotNull EvidenceGroup group, @NotNull Paint paint) {

    private @NotNull String label() {
      return group.key().label(group.signals().size());
    }
  }

  private record RenderedSignal(@NotNull EvidenceSignal signal, double x, double y) {
  }

  private record VisibleGroup(@NotNull StyledEvidenceGroup styledGroup,
                              @NotNull List<RenderedSignal> signals,
                              @NotNull RenderedSignal highestSignal, double highestIntensity,
                              double railStartX, double railEndX) {

    private static @Nullable VisibleGroup create(final @NotNull StyledEvidenceGroup styledGroup,
        final @NotNull Rectangle2D dataArea, final @NotNull ValueAxis domainAxis,
        final @NotNull ValueAxis rangeAxis, final @NotNull RectangleEdge domainEdge,
        final @NotNull RectangleEdge rangeEdge) {
      final List<RenderedSignal> signals = styledGroup.group().signals().stream()
          .filter(signal -> domainAxis.getRange().contains(signal.mz()))
          .map(signal -> new RenderedSignal(signal,
              domainAxis.valueToJava2D(signal.mz(), dataArea, domainEdge),
              rangeAxis.valueToJava2D(signal.intensity(), dataArea, rangeEdge)))
          .toList();
      if (signals.isEmpty()) {
        return null;
      }
      final RenderedSignal highest = signals.stream().max(
          Comparator.comparingDouble(signal -> signal.signal().intensity())).orElseThrow();
      final double railStartX = signals.stream().mapToDouble(RenderedSignal::x).min().orElseThrow();
      final double railEndX = signals.stream().mapToDouble(RenderedSignal::x).max().orElseThrow();
      return new VisibleGroup(styledGroup, signals, highest, highest.signal().intensity(),
          railStartX, railEndX);
    }
  }

  private record GroupLayout(@NotNull VisibleGroup visibleGroup,
                             @NotNull Rectangle2D labelBounds,
                             @NotNull Rectangle2D railBounds) {

    private static @NotNull GroupLayout create(final @NotNull VisibleGroup visible,
        final @NotNull Rectangle2D labelBounds) {
      final double railY = labelBounds.getMaxY() + RAIL_GAP;
      return new GroupLayout(visible, labelBounds,
          new Rectangle2D.Double(visible.railStartX(), railY - 1d,
              visible.railEndX() - visible.railStartX(), 2d));
    }

    private @NotNull StyledEvidenceGroup styledGroup() {
      return visibleGroup.styledGroup();
    }

    private @NotNull List<RenderedSignal> visibleSignals() {
      return visibleGroup.signals();
    }
  }
}
