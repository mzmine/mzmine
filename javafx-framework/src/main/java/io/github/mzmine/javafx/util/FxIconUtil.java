/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.javafx.util;

import io.github.mzmine.javafx.components.factories.FxIconButtonBuilder;
import io.github.mzmine.javafx.components.factories.FxIconButtonBuilder.EventHandling;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.binding.BooleanExpression;
import javafx.scene.control.ButtonBase;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;

public class FxIconUtil {

  private static final Logger logger = Logger.getLogger(FxIconUtil.class.getName());
  public static final int DEFAULT_ICON_SIZE = 18;


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
      logger.log(Level.WARNING, "error while loading resource", e);
    }
    return icon;
  }

  public static ImageView resizeImage(String path, double maxWidth, double maxHeight) {
    final Image image = loadImageFromResources(path);
    ImageView view = new ImageView(image);
    view.setImage(image);
    view.setPreserveRatio(true);
    view.setSmooth(true);
    view.setCache(true);
    view.setFitHeight(maxHeight);
    view.setFitWidth(maxWidth);

    return view;
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
        tintColor(color, 0.25));
    colorsMapping.put(new Color(1.0, 0.7372549176216125, 0.4470588266849518, 1.0),
        tintColor(color, 0.5));

    // Recolor file icon according to the mapping
    return ImageUtils.recolor(loadImageFromResources("icons/fileicon.png"), colorsMapping);
  }

  /**
   * Returns tinted version of the given color.
   *
   * @param color  input color
   * @param factor tint factor, must be from [0, 1], higher value is, brighter output color is
   * @return new color
   */
  public static Color tintColor(Color color, double factor) {
    return new Color(color.getRed() + (1d - color.getRed()) * factor,
        color.getGreen() + (1d - color.getGreen()) * factor,
        color.getBlue() + (1d - color.getBlue()) * factor, color.getOpacity());
  }


  /**
   * Get FontIcon from Ikonli library
   * <a href="https://kordamp.org/ikonli/cheat-sheet-bootstrapicons.html">Icon list</a>
   *
   * @param iconCode icon code supplier
   * @param size     the size of the icon (default = {@link FxIconUtil#DEFAULT_ICON_SIZE}.
   * @return Icon in color and size
   */
  public static FontIcon getFontIcon(IconCodeSupplier iconCode, int size) {
    return getFontIcon(iconCode.getIconCode(), size);
  }

  /**
   * Get FontIcon from Ikonli library
   * <a href="https://kordamp.org/ikonli/cheat-sheet-bootstrapicons.html">Icon list</a>
   *
   * @param iconCode icon code supplier
   * @return Icon in color and size
   */
  public static FontIcon getFontIcon(IconCodeSupplier iconCode) {
    return getFontIcon(iconCode.getIconCode(), DEFAULT_ICON_SIZE);
  }

  /**
   * Get FontIcon from Ikonli library
   * <a href="https://kordamp.org/ikonli/cheat-sheet-bootstrapicons.html">Icon list</a>
   *
   * @param iconCode icon code
   * @return Icon in color and size
   */
  public static FontIcon getFontIcon(String iconCode, int size) {
    return new FontIcon(iconCode + ":" + size);
  }

  /**
   * Get FontIcon from Ikonli library
   * <a href="https://kordamp.org/ikonli/cheat-sheet-bootstrapicons.html">Icon list</a>
   *
   * @param iconCode icon code supplier, often an enum
   * @return Icon in color and size
   */
  public static FontIcon getFontIcon(IconCodeSupplier iconCode, int size, Color color) {
    return getFontIcon(iconCode.getIconCode(), size, color);
  }

  /**
   * Get FontIcon from Ikonli library
   * <a href="https://kordamp.org/ikonli/cheat-sheet-bootstrapicons.html">Icon list</a>
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


  public static ButtonBase newIconButton(final IconCodeSupplier fxIcons, Runnable onAction) {
    return newIconButton(fxIcons, DEFAULT_ICON_SIZE, onAction);
  }

  public static ButtonBase newIconButton(final IconCodeSupplier fxIcons, int size,
      @Nullable Runnable onAction) {
    return newIconButton(fxIcons, size, null, onAction);
  }

  public static ButtonBase newIconButton(final IconCodeSupplier fxIcons, @Nullable String tooltip,
      @Nullable Runnable onAction) {
    return newIconButton(fxIcons, DEFAULT_ICON_SIZE, tooltip, onAction);
  }

  public static ButtonBase newIconButton(final IconCodeSupplier fxIcons, @Nullable String tooltip,
      @NotNull EventHandling eventHandling, @Nullable Runnable onAction) {
    return newIconButton(fxIcons, DEFAULT_ICON_SIZE, tooltip, eventHandling, onAction);
  }

  public static ButtonBase newIconButton(final IconCodeSupplier fxIcons, int size,
      @Nullable String tooltip, @Nullable Runnable onAction) {
    return newIconButton(fxIcons, size, tooltip, EventHandling.DEFAULT_PASS, onAction);
  }

  public static ButtonBase newIconButton(final IconCodeSupplier fxIcons, int size,
      @Nullable String tooltip, @NotNull EventHandling eventHandling, @Nullable Runnable onAction) {
    return FxIconButtonBuilder.ofIconButton(fxIcons, eventHandling).size(size).tooltip(tooltip)
        .onAction(onAction).build();
  }

  public static ButtonBase newFlashableIconButton(final FxIcons fxIcons, final int size,
      final BooleanExpression flashingProperty, final String tooltip, final Runnable onAction) {
    return FxIconButtonBuilder.ofIconButton(fxIcons).size(size).tooltip(tooltip).onAction(onAction)
        .flashingProperty(flashingProperty).build();
  }
}
