/*
 * Copyright 2006-2021 The MZmine Development Team
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

package io.github.mzmine.util;

import java.util.HashMap;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class ImageUtils {

  /**
   * Recolors pixels of the given image according to the given colors mapping.
   *
   * @param inputImage input image
   * @param colorMapping hash map containing colors mapping, for example {Color.RED: Color.GREEN}
   *                     would change all red pixels to green
   * @return new image
   */
  public static Image recolor(Image inputImage, HashMap<Color, Color> colorMapping) {
    int width = (int) inputImage.getWidth();
    int height = (int) inputImage.getHeight();
    WritableImage outputImage = new WritableImage(width, height);
    PixelReader reader = inputImage.getPixelReader();
    PixelWriter writer = outputImage.getPixelWriter();

    // Iterate over all pixels of the input image and recolor ones defined in the mapping
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        Color color = reader.getColor(x, y);
        writer.setColor(x, y, colorMapping.getOrDefault(color, color));
      }
    }

    return outputImage;
  }

}
