/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.datamodel.features.types.graphicalnodes;

import io.github.mzmine.main.MZmineCore;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;
import javafx.animation.PauseTransition;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import org.jfree.chart.fx.ChartViewer;

/**
 * Used to buffer charts with ImageView and predefined sizes. Can save performance in tables
 */
public class BufferedChartNode extends BorderPane {

  private static final Logger logger = Logger.getLogger(BufferedChartNode.class.getName());

  private ChartViewer chart;

  private ImageView imageView;
  private boolean makeInteractiveOnClick;
  public BufferedChartNode(final boolean makeInteractiveOnClick) {
    this.makeInteractiveOnClick = makeInteractiveOnClick;
    PauseTransition resizer = new PauseTransition(Duration.seconds(2));

    widthProperty().addListener((observable, oldValue, newValue) -> {
      resizer.setOnFinished(
          event -> setChartCreateImage(chart, (int) getWidth(), (int) getHeight()));
      resizer.playFromStart();
    });
  }

  public BufferedChartNode() {
    this(true);
  }

  /**
   * Sets the chart and creates a buffered image that is set to this node. Automatically JavaFX
   * thread safe
   *
   * @param chart  the new chart
   * @param width  the width
   * @param height the height
   */
  public void setChartCreateImage(final ChartViewer chart, final int width, final int height) {
    this.chart = chart;
    createAndSetImage(width, height);
  }

  /**
   * Create and set image with predefined size
   */
  public void createAndSetImage(final int width, final int height) {
    setPrefWidth(width);
    setPrefHeight(height);
    BufferedImage img = chart.getChart().createBufferedImage(width, height);
    imageView = new ImageView(SwingFXUtils.toFXImage(img, null));
    showBufferedImage();
    // add listener to border pane so that clicks are always recognized
    setOnMouseClicked(e -> MZmineCore.runLater(() -> {
      if (makeInteractiveOnClick) {
        showInteractiveChart();
      }
    }));
  }

  public void setMakeInteractiveOnClick(final boolean makeInteractiveOnClick) {
    this.makeInteractiveOnClick = makeInteractiveOnClick;
  }

  /**
   * show buffered image to save resources
   */
  public void showBufferedImage() {
    MZmineCore.runLater(() -> setCenter(imageView));
  }

  /**
   * Show interactive chart, e.g., on click
   */
  public void showInteractiveChart() {
    MZmineCore.runLater(() -> setCenter(chart));
  }

  public ChartViewer getChart() {
    return chart;
  }
}
