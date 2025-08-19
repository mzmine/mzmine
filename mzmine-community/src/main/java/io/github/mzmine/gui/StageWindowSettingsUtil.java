/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.parameters.parametertypes.WindowSettings;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StageWindowSettingsUtil {

  private static final Logger logger = Logger.getLogger(StageWindowSettingsUtil.class.getName());

  /**
   * @return a property bound to the properties of the stage
   */
  public static ObjectProperty<WindowSettings> createBoundWindowSettingsProperty(
      @NotNull final Stage stage) {
    ObjectProperty<WindowSettings> windowSettingsProperty = new SimpleObjectProperty<>();

    PropertyUtils.onChange(() -> windowSettingsProperty.set(getCurrentSettingsFrom(stage)),
        stage.xProperty(), stage.yProperty(), stage.widthProperty(), stage.heightProperty(),
        stage.maximizedProperty());

    windowSettingsProperty.set(getCurrentSettingsFrom(stage));

    return windowSettingsProperty;
  }

  public static WindowSettings getCurrentSettingsFrom(@NotNull Stage stage) {
    return new WindowSettings(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight(),
        stage.isMaximized());
  }

  /**
   * @return first screen index or -1 if none matches bounds
   */
  public static int getCurrentScreen(@NotNull Stage stage) {
    final ObservableList<Screen> screens = Screen.getScreens();
    for (int i = 0; i < screens.size(); i++) {
      Screen screen = screens.get(i);
      if (screen.getBounds()
          .intersects(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight())) {
        return i;
      }
    }
    return -1;
  }

  /**
   * applies settings to a stage and checks if stage is visible on screen otherwise maximizes on
   * primary screen
   */
  public static void applySettingsToWindow(@NotNull final Stage stage,
      @Nullable final WindowSettings p) {
    if (p == null) {
      // just pop onto primary stage maximized
      maximizeOnPrimaryScreen(stage);
      return;
    }

    logger.finest("Setting window " + stage.getTitle() + " position " + p);
    stage.setX(p.x());
    stage.setY(p.y());
    stage.setWidth(p.width());
    stage.setHeight(p.height());
    stage.setMaximized(p.maximized());

    // when still outside of screen
    // e.g. changing from 2 screens to one
    if (!isOnScreen(stage)) {
      // Maximize on screen 1
      logger.finest(
          "Window " + stage.getTitle() + " is not on screen, setting to maximized on screen 1");
      maximizeOnPrimaryScreen(stage);
    }
  }

  /**
   * Maximize on primary screen
   */
  private static void maximizeOnPrimaryScreen(@NotNull Stage stage) {
    final Rectangle2D primary = Screen.getPrimary().getBounds();
    stage.setX(primary.getMinX());
    stage.setY(primary.getMinY());
    stage.setWidth(primary.getWidth());
    stage.setHeight(primary.getHeight());
    stage.setMaximized(true);
  }

  /**
   * @return true if visible to at least x% on a single screen
   */
  public static boolean isOnScreen(Stage stage) {
    // use a smaller rectangle to check so that a bit of space can be off screen
    final double offWidth = stage.getWidth() * 0.2;
    final double offHeight = stage.getHeight() * 0.2;
    Rectangle2D stagePosition = new Rectangle2D(stage.getX() + offWidth / 2d,
        stage.getY() + offHeight / 2d, stage.getWidth() - offWidth, stage.getHeight() - offHeight);

    return Screen.getScreens().stream()
        .anyMatch(screen -> screen.getBounds().contains(stagePosition));
  }

}
