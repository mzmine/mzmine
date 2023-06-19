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

package io.github.mzmine.util.swing;

import java.awt.Image;
import java.net.URL;
import javafx.scene.image.ImageView;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.jetbrains.annotations.NotNull;

/**
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class IconUtil {

  public static @NotNull ImageIcon loadIconFromResources(final @NotNull String resourcePath) {
    final URL iconResourcePath = IconUtil.class.getClassLoader().getResource(resourcePath);
    if (iconResourcePath == null)
      throw new IllegalArgumentException("Could not find an icon file at path " + resourcePath);
    final ImageIcon icon = new ImageIcon(iconResourcePath);
    return icon;
  }

  public static @NotNull Icon loadIconFromResources(final @NotNull String resourcePath,
      final int width) {
    final ImageIcon icon = loadIconFromResources(resourcePath);
    final ImageIcon scaledIcon = scaled(icon, width);
    return scaledIcon;
  }

  public static @NotNull ImageIcon scaled(final ImageIcon icon, final int width) {
    int height = Math.round(icon.getIconHeight() / (float) icon.getIconWidth() * width);
    Image image = icon.getImage();
    Image newimg = image.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);
    return new ImageIcon(newimg);
  }

  public static ImageView scaledImageView(final javafx.scene.image.Image img, final double width) {
    ImageView view = new ImageView(img);
    view.setFitWidth(width);
    return view;
  }
}
