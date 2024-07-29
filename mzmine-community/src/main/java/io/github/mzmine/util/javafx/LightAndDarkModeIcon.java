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

import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.main.ConfigService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

/**
 * Icon that automatically changes if mzmine is switched from light to dark mode.
 */
public class LightAndDarkModeIcon extends HBox {

  public static LightAndDarkModeIcon mzmineImage(int maxWidth, int maxHeight) {
    return new LightAndDarkModeIcon("icons/introductiontab/logos_mzio_mzmine.png",
        "icons/introductiontab/logos_mzio_mzmine_light.png", maxWidth, maxHeight);
  }

  public static LightAndDarkModeIcon mzwizardImage(int maxWidth, int maxHeight) {
    return new LightAndDarkModeIcon("icons/introductiontab/logos_mzio_mzwizard.png",
        "icons/introductiontab/logos_mzio_mzwizard_light.png", maxWidth, maxHeight);
  }

  public static LightAndDarkModeIcon mzwizardImageTab(int maxWidth, int maxHeight) {
    return new LightAndDarkModeIcon("icons/introductiontab/logos_mzio_mzwizard_lowres.png",
        "icons/introductiontab/logos_mzio_mzwizard_light_lowres.png", maxWidth, maxHeight);
  }

  public LightAndDarkModeIcon(String resourcePathForLightMode, String resourcePathForDarkMode,
      int maxWidth, int maxHeight) {
    this(resourcePathForLightMode, resourcePathForDarkMode, maxWidth, maxHeight,
        ConfigService.isDarkModeProperty());
  }

  public LightAndDarkModeIcon(String resourcePathForLightMode, String resourcePathForDarkMode,
      int maxWidth, int maxHeight, BooleanProperty isDarkModeProperty) {
    final Image lightModeImage = FxIconUtil.loadImageFromResources(resourcePathForLightMode);
    final Image darkModeImage = FxIconUtil.loadImageFromResources(resourcePathForDarkMode);

    ImageView view = new ImageView();
    getChildren().add(view);
    view.setPreserveRatio(true);
    view.setFitHeight(maxHeight);
    view.setFitWidth(maxWidth);
    view.imageProperty().bind(Bindings.createObjectBinding(() -> {
      if (isDarkModeProperty.get()) {
        return darkModeImage;
      } else {
        return lightModeImage;
      }
    }, isDarkModeProperty));
  }
}
