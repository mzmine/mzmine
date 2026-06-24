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

package io.github.mzmine.javafx.dialogs;

import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.javafx.util.IconCodeSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;

public class NotificationService {

  private static final Logger logger = Logger.getLogger(NotificationService.class.getName());

  public static final Duration DEFAULT_DURATION = Duration.seconds(15);
  public static final Pos DEFAULT_POSITION = Pos.BOTTOM_RIGHT;


  public enum NotificationType implements IconCodeSupplier {
    INFO, WARNING, ERROR, CONFIRMATION, PLAIN;

    public @Nullable FontIcon getIcon() {
      final String iconCode = getIconCode();
      if (iconCode == null) {
        return null;
      }
      try {
        FontIcon fontIcon = new FontIcon(iconCode);
//        fontIcon.setOnMouseClicked(event -> buttonAction.handle(null));
        return fontIcon;
      } catch (Exception ex) {
        logger.log(Level.WARNING, "Cannot load icon from Ikonli" + ex.getMessage(), ex);
      }
      return null;
    }

    @Override
    public @Nullable String getIconCode() {
      return switch (this) {
        case INFO, PLAIN -> FxIcons.INFO_CIRCLE.getIconCode();
        case WARNING -> FxIcons.EXCLAMATION_CIRCLE.getIconCode();
        case CONFIRMATION -> FxIcons.QUESTION_CIRCLE.getIconCode();
        case ERROR -> FxIcons.X_CIRCLE.getIconCode();
      };
    }
  }

  public static void show(NotificationMessage notification) {
    show(notification, null);
  }

  public static void show(NotificationMessage notification, EventHandler<ActionEvent> onClick) {
    show(notification.type(), notification.title(), notification.text(), onClick);
  }

  public static void show(@NotNull NotificationService.NotificationType type, @NotNull String title,
      @NotNull String text) {
    show(type, title, text, null);
  }

  public static void show(@NotNull NotificationService.NotificationType type, @NotNull String title,
      @NotNull String text, @Nullable EventHandler<ActionEvent> onClick) {
    show(type, title, text, null, DEFAULT_DURATION, DEFAULT_POSITION, onClick);
  }

  public static void show(@NotNull NotificationService.NotificationType type, @NotNull String title,
      @NotNull String text, @Nullable Node graphic, @Nullable EventHandler<ActionEvent> onClick) {
    show(type, title, text, graphic, DEFAULT_DURATION, DEFAULT_POSITION, onClick);
  }

  public static void show(@NotNull NotificationService.NotificationType type, @NotNull String title,
      @NotNull String text, @Nullable Node graphic, @NotNull Duration duration, @NotNull Pos pos,
      @Nullable EventHandler<ActionEvent> onClick) {

    if (DesktopService.isHeadLess() || !FxThread.isFxInitialized()) {
      final String msg = "%s: %s (notification)".formatted(title, text);
      switch (type) {
        case ERROR -> logger.severe(msg);
        case WARNING -> logger.warning(msg);
        default -> logger.info(msg);
      }
      return;
    }

    FxThread.runLater(() -> {
      final Node actualGraphic = graphic != null ? graphic : type.getIcon();
      if (actualGraphic != null && onClick != null) {
        actualGraphic.setOnMouseClicked(event -> onClick.handle(null));
      }

      final Notifications notification = createNotification(title, text, actualGraphic, duration,
          pos, onClick);
      notification.show();

      // this replaces icons for bad looking ones:
//      switch (type) {
//        case PLAIN -> notification.show();
//        case INFO -> notification.showInformation();
//        case WARNING -> notification.showWarning();
//        case ERROR -> notification.showError();
//        case CONFIRMATION -> notification.showConfirm();
//      }
    });
  }

  private static Notifications createNotification(@NotNull String title, @NotNull String text,
      @Nullable Node graphic, @NotNull Duration duration, @NotNull Pos pos,
      @Nullable EventHandler<ActionEvent> onClick) {
    return Notifications.create().title(title).text(text).graphic(graphic).hideAfter(duration)
        .position(pos).onAction(onClick);
  }
}
