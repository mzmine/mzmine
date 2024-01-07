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

import io.github.mzmine.main.MZmineCore;
import java.util.Optional;
import java.util.logging.Logger;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.jetbrains.annotations.Nullable;

public class DialogLoggerUtil {

  private static final Logger logger = Logger.getLogger(DialogLoggerUtil.class.getName());

  /*
   * Dialogs
   */
  public static void showErrorDialog(String message, Exception e) {
    Alert alert = new Alert(AlertType.ERROR, message + " \n" + e.getMessage());
    alert.showAndWait();
  }

  public static void showErrorDialog(String title, String message) {
    if (MZmineCore.isHeadLessMode()) {
      logger.info(title + ": " + message);
      return;
    }
    Alert alert = new Alert(AlertType.ERROR, null);
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
    if (MZmineCore.isHeadLessMode()) {
      logger.info(title + ": " + message);
      return null;
    }
    Alert alert = new Alert(AlertType.INFORMATION, null);
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

  public static boolean showDialogYesNo(String title, String message) {
    Alert alert = new Alert(AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
    alert.setTitle(title);
    Optional<ButtonType> result = alert.showAndWait();
    return (result.isPresent() && result.get() == ButtonType.YES);
  }

  /**
   * shows a message dialog just for a few given milliseconds
   */
  public static void showMessageDialogForTime(String title, String message, long timeMillis) {
    Alert alert = showMessageDialog(title, message, false);
    if (alert == null) {
      return;
    }

    Timeline idleTimer = new Timeline(new KeyFrame(Duration.millis(timeMillis), e -> alert.hide()));
    idleTimer.setCycleCount(1);
    idleTimer.play();
  }

}
