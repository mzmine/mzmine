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

package io.github.mzmine.javafx.components.util;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.text.Text;

public class TextLabelMeasurementUtil {

  private static final TextLabelMeasurementUtil INSTANCE = new TextLabelMeasurementUtil();

  private final Text measurer = new Text();
  private final Scene tempScene = new Scene(new Group(measurer));

  private TextLabelMeasurementUtil() {
    super();
  }

  public static void init(ReadOnlyObjectProperty<Scene> mainScene) {
    mainScene.subscribe(ns -> {
      if (ns == null) {
        return;
      }
      INSTANCE.tempScene.getStylesheets().setAll(ns.getStylesheets());
      INSTANCE.measurer.applyCss();
    });

  }

  /**
   * Measures text width with a specific font.
   */
  public static double measureWidth(String text) {
    INSTANCE.measurer.setText(text);
    return INSTANCE.measurer.getLayoutBounds().getWidth();
  }

}
