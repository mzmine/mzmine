/*
 * Copyright 2006-2022 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.util.javafx;

import io.github.mzmine.util.ImageUtils;
import io.github.mzmine.util.color.ColorUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.logging.Logger;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.kordamp.ikonli.javafx.FontIcon;

public class FxIconUtil {

  private static final Logger logger = Logger.getLogger(FxIconUtil.class.getName());

  @NotNull
  public static Image loadImageFromResources(final @NotNull String resourcePath) {
    final InputStream iconResource = FxIconUtil.class.getClassLoader()
        .getResourceAsStream(resourcePath);
    if (iconResource == null) {
      logger.warning("Could not find an icon file at path " + resourcePath);
      throw new IllegalArgumentException("Could not find an icon file at path " + resourcePath);
    }
    final Image icon = new Image(iconResource);
    try {
      iconResource.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return icon;
  }

  /**
   * Returns file icon of the given color.
   *
   * @param color color of the icon
   * @return file icon
   */
  public static Image getFileIcon(Color color) {
    // Define colors mapping for the initial file icon
    HashMap<Color, Color> colorsMapping = new HashMap<>();
    colorsMapping.put(new Color(1.0, 0.5333333611488342, 0.0235294122248888, 1.0), color);
    colorsMapping.put(new Color(0.9921568632125854, 0.6078431606292725, 0.1882352977991104, 1.0),
        ColorUtils.tintColor(color, 0.25));
    colorsMapping.put(new Color(1.0, 0.7372549176216125, 0.4470588266849518, 1.0),
        ColorUtils.tintColor(color, 0.5));

    // Recolor file icon according to the mapping
    return ImageUtils.recolor(loadImageFromResources("icons/fileicon.png"), colorsMapping);
  }

  /**
   * Get FontIcon from Ikonli library
   *
   * @param iconCode icon code
   * @return Icon in color and size
   */
  public static FontIcon getFontIcon(String iconCode, int size, Color color) {
    FontIcon icon = new FontIcon();
    String b = "-fx-icon-color:" + FxColorUtil.colorToHex(color) + ";-fx-icon-code:" + iconCode
        + ";-fx-icon-size:" + size + ";";
    icon.setStyle(b);
    return icon;
  }

}
