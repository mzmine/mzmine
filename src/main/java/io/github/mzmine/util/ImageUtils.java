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
