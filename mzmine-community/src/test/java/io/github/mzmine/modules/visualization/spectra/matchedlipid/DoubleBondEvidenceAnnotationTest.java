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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRuleRating;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;

class DoubleBondEvidenceAnnotationTest {

  @Test
  void groupsOnlyTheSelectedDiagnosticPairForEachDoubleBond() {
    final List<LipidFragment> fragments = List.of(fragment(382d, 10d, 19,
            LipidFragmentationRuleRating.MAJOR),
        fragment(340d, 4d, 19, LipidFragmentationRuleRating.MAJOR),
        fragment(400d, 2d, 19, LipidFragmentationRuleRating.MINOR),
        fragment(422d, 5d, 16, LipidFragmentationRuleRating.MAJOR),
        fragment(380d, 3d, 16, LipidFragmentationRuleRating.MAJOR));

    final DoubleBondEvidenceAnnotation annotation = DoubleBondEvidenceAnnotation.createAnnotation(
        fragments, List.of(Color.BLUE, Color.ORANGE), List::of);

    assertNotNull(annotation);
    assertEquals(2, annotation.groupCount());
    assertEquals(382d, annotation.highestSignalMz(0));
    assertEquals(3, annotation.signalCount(0));
    assertEquals("22:5 n-3 / Δ19 · 3 ions", annotation.label(0));
    assertEquals(List.of(new Range(340d, 400d), new Range(380d, 422d)),
        annotation.evidenceMzRanges());
    assertTrue(MatchedLipidLabelGenerator.usesGroupedPositionAnnotation(fragments.getFirst()));
    final String tooltip = MatchedLipidLabelGenerator.buildFragmentAnnotation(
        fragments.getFirst(), true);
    assertTrue(tooltip.contains("omega-side +O"));
    assertTrue(tooltip.contains("ppm"));
    final String groupTooltip = annotation.tooltip(0);
    assertEquals(5, groupTooltip.lines().count());
    assertTrue(groupTooltip.contains("diagnostic pair"));
    assertTrue(groupTooltip.contains("supporting"));
    assertTrue(groupTooltip.contains("calc."));
  }

  @Test
  void jointlyPlacesGroupLabelsOutsideOccupiedFragmentLabels() {
    final List<LipidFragment> fragments = List.of(fragment(382d, 10d, 19,
            LipidFragmentationRuleRating.MAJOR),
        fragment(340d, 4d, 19, LipidFragmentationRuleRating.MAJOR),
        fragment(400d, 2d, 19, LipidFragmentationRuleRating.MINOR),
        fragment(390d, 9.5d, 16, LipidFragmentationRuleRating.MAJOR),
        fragment(370d, 8d, 16, LipidFragmentationRuleRating.MAJOR));
    final Rectangle2D conventionalLabel = new Rectangle2D.Double(340d, 35d, 210d, 65d);
    final DoubleBondEvidenceAnnotation annotation = DoubleBondEvidenceAnnotation.createAnnotation(
        fragments, List.of(Color.BLUE, Color.ORANGE), () -> List.of(conventionalLabel));
    assertNotNull(annotation);

    final NumberAxis domainAxis = new NumberAxis();
    domainAxis.setRange(300d, 450d);
    final NumberAxis rangeAxis = new NumberAxis();
    rangeAxis.setRange(0d, 12d);
    final XYPlot plot = new XYPlot(null, domainAxis, rangeAxis, null);
    final BufferedImage image = new BufferedImage(800, 500, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D graphics = image.createGraphics();
    final ChartRenderingInfo chartInfo = new ChartRenderingInfo();
    final PlotRenderingInfo plotInfo = new PlotRenderingInfo(chartInfo);
    try {
      annotation.draw(graphics, plot, new Rectangle2D.Double(0d, 0d, 800d, 500d), domainAxis,
          rangeAxis, 0, plotInfo);
    } finally {
      graphics.dispose();
    }

    final List<ChartEntity> labelEntities = IntStream.range(0,
            chartInfo.getEntityCollection().getEntityCount())
        .mapToObj(chartInfo.getEntityCollection()::getEntity).toList();
    assertEquals(2, labelEntities.size());
    final Rectangle2D first = labelEntities.getFirst().getArea().getBounds2D();
    final Rectangle2D second = labelEntities.getLast().getArea().getBounds2D();
    assertFalse(first.intersects(conventionalLabel));
    assertFalse(second.intersects(conventionalLabel));
    assertFalse(first.intersects(second));
    final Rectangle2D dataArea = new Rectangle2D.Double(0d, 0d, 800d, 500d);
    assertEquals(domainAxis.valueToJava2D(370d, dataArea, plot.getDomainAxisEdge()),
        first.getCenterX(), 0.5d);
    assertEquals(domainAxis.valueToJava2D(380d, dataArea, plot.getDomainAxisEdge()),
        second.getCenterX(), 0.5d);
  }

  private @NotNull LipidFragment fragment(final double mz, final double intensity,
      final int deltaPosition, final @NotNull LipidFragmentationRuleRating rating) {
    return new LipidFragment(LipidFragmentationRuleType.OAD_DOUBLE_BOND_POSITION_FRAGMENT,
        LipidAnnotationLevel.DOUBLE_BOND_POSITION_LEVEL, rating, mz, "C10H20+",
        new SimpleDataPoint(mz, intensity), LipidClasses.MONOACYLGLYCEROPHOSPHOCHOLINES, 22, 5, 0,
        LipidChainType.ACYL_CHAIN, null, 50, "precursor; omega-side +O; n-3", deltaPosition);
  }
}
