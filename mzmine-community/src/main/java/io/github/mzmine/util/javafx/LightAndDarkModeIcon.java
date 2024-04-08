/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.util.javafx;

import io.github.mzmine.main.ConfigService;
import javafx.event.EventHandler;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.util.Subscription;

/**
 * Icon that automatically changes if mzmine is switched from light to dark mode.
 * <p></p>
 * <b>Attention</b>: When the view containing this icon is closed, call {@link #unsubscribe()}, e.g.
 * in {@link Tab#setOnCloseRequest(EventHandler)}, to
 * avoid memory leaks.
 */
public class LightAndDarkModeIcon extends HBox {

  private final Subscription subscription;

  public LightAndDarkModeIcon(String resourcePathForLightMode, String resourcePathForDarkMode,
      int maxWidth, int maxHeight) {
    final Image lightModeImage = FxIconUtil.loadImageFromResources(resourcePathForLightMode);
    final Image darkModeImage = FxIconUtil.loadImageFromResources(resourcePathForDarkMode);

    ImageView view = new ImageView();
    getChildren().add(view);
    subscription = ConfigService.isDarkModeProperty().subscribe(isDarkMode -> {
      if (isDarkMode) {
        view.setImage(darkModeImage);
      } else {
        view.setImage(lightModeImage);
      }
      view.setPreserveRatio(true);
      view.setFitHeight(maxHeight);
      view.setFitWidth(maxWidth);
    });
  }

  public void unsubscribe() {
    subscription.unsubscribe();
  }
}
