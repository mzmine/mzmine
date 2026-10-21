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

package io.github.mzmine.modules.tools.batch_resource_estimation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.category.DefaultCategoryDataset;
import org.junit.jupiter.api.Test;

class RamReliabilityBarRendererTest {

  private static BatchResourcePrediction run(final int files, final boolean firstReliable) {
    return new BatchResourcePrediction(files,
        List.of(new StepResourcePrediction(1, "Import", "threads", 1, 0.5, 0, 1, firstReliable),
            new StepResourcePrediction(2, "Alignment", "files", 2, 1, 0, 1, !firstReliable)), 2, 1,
        0, 2, 0, 3, "Alignment", "Alignment");
  }

  @Test
  void rendersSolidAndHatchedFillsWithoutChangingRunColors() throws IOException {
    final RamReliabilityBarRenderer renderer = new RamReliabilityBarRenderer();
    renderer.setRuns(List.of(run(6, true), run(12, false)));
    renderer.setSeriesPaint(0, Color.BLUE);
    renderer.setSeriesPaint(1, Color.ORANGE);

    final BufferedImage image = new BufferedImage(200, 120, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D graphics = image.createGraphics();
    try {
      graphics.setColor(Color.WHITE);
      graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
      renderer.getBarPainter()
          .paintBar(graphics, renderer, 0, 0, new Rectangle2D.Double(10, 10, 50, 100),
              RectangleEdge.BOTTOM);
      renderer.getBarPainter()
          .paintBar(graphics, renderer, 0, 1, new Rectangle2D.Double(100, 10, 50, 100),
              RectangleEdge.BOTTOM);
    } finally {
      graphics.dispose();
    }
    assertEquals(Color.BLUE.getRGB(), image.getRGB(30, 50));
    int hatchPixels = 0;
    int gapPixels = 0;
    for (int x = 105; x < 145; x++) {
      for (int y = 15; y < 105; y++) {
        if (image.getRGB(x, y) == Color.BLUE.getRGB()) {
          hatchPixels++;
        } else if (image.getRGB(x, y) == Color.WHITE.getRGB()) {
          gapPixels++;
        }
      }
    }
    assertTrue(hatchPixels > 0 && gapPixels > 0, "fill must have colored hatching and gaps");
    int outlinePixels = 0;
    for (int y = 10; y < 110; y++) {
      if (image.getRGB(100, y) == Color.BLUE.getRGB()) {
        outlinePixels++;
      }
    }
    assertEquals(100, outlinePixels, "outline must remain solid");

    final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    dataset.addValue(1, "6 files (measured)", "Import");
    dataset.addValue(2, "6 files (measured)", "Alignment");
    dataset.addValue(1.5, "12 files (measured)", "Import");
    dataset.addValue(2.5, "12 files (measured)", "Alignment");
    final CategoryPlot plot = new CategoryPlot(dataset, new CategoryAxis("Step"),
        new NumberAxis("GB"), renderer);
    final JFreeChart chart = new JFreeChart("Peak RAM", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
    assertEquals(4, renderer.getLegendItems().getItemCount());
    final Path preview = Path.of("build", "reports", "resource-estimation", "ram-reliability.png");
    Files.createDirectories(preview.getParent());
    ImageIO.write(chart.createBufferedImage(900, 500), "png", preview.toFile());
  }
}
