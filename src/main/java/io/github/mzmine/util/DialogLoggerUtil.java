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
