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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.jfree.data.xy.DefaultXYDataset;

class StyledLipidSpectrumBarRendererTest {

  @Test
  void placesConventionalLabelOutsideOadLeaderCorridorWhenPossible() {
    final DefaultXYDataset dataset = new DefaultXYDataset();
    dataset.addSeries("matched", new double[][]{{500d}, {1d}});
    final NumberAxis domainAxis = new NumberAxis();
    domainAxis.setRange(0d, 800d);
    final NumberAxis rangeAxis = new NumberAxis();
    rangeAxis.setRange(0d, 10d);
    final StyledLipidSpectrumBarRenderer renderer = new StyledLipidSpectrumBarRenderer(true,
        Color.BLUE);
    renderer.setReservedLabelMzRanges(List.of(new Range(500d, 600d)));
    final XYPlot plot = new XYPlot(dataset, domainAxis, rangeAxis, renderer);
    final Rectangle2D dataArea = new Rectangle2D.Double(0d, 0d, 800d, 500d);
    final BufferedImage image = new BufferedImage(800, 500, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D graphics = image.createGraphics();
    graphics.setClip(dataArea);
    try {
      renderer.initialise(graphics, dataArea, plot, dataset, null);
      final XYItemLabelGenerator generator = (_, _, _) -> "Conventional fragment";
      renderer.drawItemLabel(graphics, dataset, 0, 0, plot,
          generator, new Rectangle2D.Double(499d, 300d, 2d, 200d), false);
    } finally {
      graphics.dispose();
    }

    assertEquals(1, renderer.getPlacedLabelBounds().size());
    final double corridorStart = domainAxis.valueToJava2D(500d, dataArea,
        plot.getDomainAxisEdge());
    assertTrue(renderer.getPlacedLabelBounds().getFirst().getMaxX() < corridorStart);
  }
}
