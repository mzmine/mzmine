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

package io.github.mzmine.javafx.dialogs;

import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.gui.JavaFxDesktop;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import java.util.Optional;
import java.util.logging.Logger;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.jetbrains.annotations.Nullable;

public class DialogLoggerUtil {

  private static final Logger logger = Logger.getLogger(DialogLoggerUtil.class.getName());

  public static void showErrorDialog(String title, String message) {
    logger.info(title + ": " + message);
    if (DesktopService.isHeadLess()) {
      return;
    }
    Alert alert = new Alert(AlertType.ERROR, null);
    if (DesktopService.getDesktop() instanceof JavaFxDesktop fx) {
      alert.getDialogPane().getScene().getStylesheets()
          .setAll(fx.getMainWindow().getScene().getStylesheets());
    }
    alert.setTitle(title);
    // seems like a good size for the dialog message when an old batch is loaded into new version
    Text label = new Text(message);
    label.setWrappingWidth(415);
    HBox box = new HBox(label);
    box.setPadding(new Insets(5));
    alert.getDialogPane().setContent(box);
    alert.showAndWait();
  }

  @Nullable
  public static Alert showMessageDialog(String title, String message) {
    return showMessageDialog(title, message, true);
  }

  @Nullable
  public static Alert showMessageDialog(String title, String message, boolean modal) {
    logger.info(title + ": " + message);
    if (DesktopService.isHeadLess()) {
      return null;
    }
    Alert alert = new Alert(AlertType.INFORMATION, null);
    applyMainWindowStyle(alert);

    alert.setTitle(title);
    alert.setHeaderText(title);
    // seems like a good size for the dialog message when an old batch is loaded into new version
    Text label = new Text(message);
    label.setWrappingWidth(415);
    HBox box = new HBox(label);
    box.setPadding(new Insets(5));
    alert.getDialogPane().setContent(box);
    if (modal) {
      alert.showAndWait();
    } else {
      alert.show();
    }
    return alert;
  }

  public static void applyMainWindowStyle(final Alert alert) {
    if (DesktopService.getDesktop() instanceof JavaFxDesktop fx) {
      var alertScene = alert.getDialogPane().getScene();
      alertScene.getStylesheets().setAll(fx.getMainWindow().getScene().getStylesheets());

      if (alertScene.getWindow() instanceof Stage alertStage) {
        alertStage.getIcons().setAll(fx.getMainWindow().getIcons());
      }
    }
  }

  /**
   * @return true if yes was clicked, false on No and on cancel
   */
  public static boolean showDialogYesNo(String title, String message) {
    return showDialogYesNo(AlertType.CONFIRMATION, title, message);
  }

  /**
   * @return true if yes was clicked, false on No and on cancel
   */
  public static boolean showDialogYesNo(AlertType type, String title, String message) {
    return showDialog(type, title, message, ButtonType.YES, ButtonType.NO).map(
        res -> res == ButtonType.YES).orElse(false);
  }

  /**
   * @param type    usually uses {@link AlertType#CONFIRMATION}
   * @param title   title of dialog and also repeated in dialog header
   * @param message message in box wrapped
   * @param buttons Either use predefined buttons or define new buttons with
   *                {@link ButtonType#ButtonType(String, ButtonData)} (String, ButtonData)}requires
   *                a {@link ButtonData#CANCEL_CLOSE} or {@link ButtonData#NO} button that will also
   *                trigger on X close button. Otherwise X button will not work
   * @return optional button type. Easy to use the ButtonData or title to match the button
   */
  public static Optional<ButtonType> showDialog(AlertType type, String title, String message,
      ButtonType... buttons) {
    Alert alert = new Alert(type, message, buttons);
    applyMainWindowStyle(alert);

    alert.setTitle(title);
    alert.setHeaderText(title);
    // seems like a good size for the dialog message when an old batch is loaded into new version
    Text label = new Text(message);
    label.setWrappingWidth(415);
    HBox box = new HBox(label);
    box.setPadding(new Insets(5));
    alert.getDialogPane().setContent(box);
    return alert.showAndWait();
  }

  /**
   * shows a message dialog just for a few given milliseconds
   */
  public static void showMessageDialogForTime(String title, String message) {
    showMessageDialogForTime(title, message, 3500);
  }

  public static void showMessageDialogForTime(String title, String message, long timeMillis) {
    FxThread.runLater(() -> {
      Alert alert = showMessageDialog(title, message, false);
      if (alert == null) {
        return;
      }

      Timeline idleTimer = new Timeline(
          new KeyFrame(Duration.millis(timeMillis), e -> alert.hide()));
      idleTimer.setCycleCount(1);
      idleTimer.play();
    });
  }

}
