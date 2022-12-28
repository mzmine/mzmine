/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.gui;

import io.github.mzmine.util.javafx.FxIconUtil;
import java.util.Random;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Winter season theme
 */
public class SnowFallPane extends AnchorPane {

  private final Random random = new Random();

  public SnowFallPane(int flakes) {
    this(null, flakes);
  }

  public SnowFallPane(Region parent, int flakes) {
//    if (parent != null) {
//      prefWidthProperty().bind(parent.widthProperty());
//      prefHeightProperty().bind(parent.heightProperty());
////      maxWidthProperty().bind(parent.widthProperty());
//      maxHeightProperty().bind(parent.heightProperty());
//    }
    setMaxWidth(500);
    setWidth(500);
    // 1000 snowflakes
    for (int i = 0; i < flakes; i++) {
      getChildren().add(create());
    }
  }


  public Node create() {
    int height = 1400;
    int startY = random.nextInt(height) - height - 80;
    int moveY = height + Math.abs(startY);
    // start left because we move right
    int startX = random.nextInt(-150, 1250);
    int moveX = random.nextInt(400);
    long milliseconds = random.nextLong(3000, 8_000);
    double size = random.nextDouble(5, 15);
    double transparency = random.nextDouble(0.4, 0.9);
    return create(startX, startY, moveX, moveY, milliseconds, size, transparency);
  }

  public Node create(int x, int y, int moveX, int moveY, long milliseconds, double size,
      double transparency) {
    Color color = Color.rgb(255, 255, 255, transparency);
    // three types of snow
    int iconNumber = random.nextInt(3) + 1;
    String iconCode = "bi-snow" + (iconNumber > 1 ? iconNumber : "");
    var node = FxIconUtil.getFontIcon(iconCode, (int) size, color);
    node.setX(x);
    node.setY(y);
    node.setFill(color);

    Duration duration = Duration.millis(milliseconds * size / 2);
    TranslateTransition tt = new TranslateTransition(duration, node);
    tt.setByX(moveX);
    tt.setByY(moveY);
    // loop
    tt.setCycleCount(TranslateTransition.INDEFINITE);
    tt.play();
    return node;
  }

}
