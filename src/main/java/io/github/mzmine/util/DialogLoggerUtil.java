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

import java.util.Optional;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.util.Duration;

public class DialogLoggerUtil {

  /*
   * Dialogs
   */
  public static void showErrorDialog(String message, Exception e) {
    Alert alert = new Alert(AlertType.ERROR, message + " \n" + e.getMessage());
    alert.showAndWait();
  }

  public static void showErrorDialog(String title, String message) {
    Alert alert = new Alert(AlertType.ERROR, message);
    alert.setTitle(title);
    alert.showAndWait();
  }

  public static void showMessageDialog(String title, String message) {
    Alert alert = new Alert(AlertType.INFORMATION, message);
    alert.setTitle(title);
    alert.showAndWait();
  }

  public static boolean showDialogYesNo(String title, String message) {
    Alert alert = new Alert(AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
    alert.setTitle(title);
    Optional<ButtonType> result = alert.showAndWait();
    return (result.isPresent() && result.get() == ButtonType.YES);
  }

  /**
   * shows a message dialog just for a few given milliseconds
   *
   * @param parent
   * @param title
   * @param message
   * @param time
   */
  public static void showMessageDialogForTime(String title, String message, long time) {
    Alert alert = new Alert(AlertType.INFORMATION, message);
    alert.setTitle(title);
    alert.show();
    Timeline idleTimer = new Timeline(new KeyFrame(Duration.millis(time), e -> alert.hide()));
    idleTimer.setCycleCount(1);
    idleTimer.play();
  }

}
