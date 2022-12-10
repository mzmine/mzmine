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

package io.github.mzmine.gui;

import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.Semver.SemverType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.InetUtils;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.paint.Color;

public class NewVersionCheck implements Runnable {

  private static final String newestVersionAddress = "http://mzmine.github.io/version.txt";

  public enum CheckType {
    DESKTOP, MENU
  }

  private static final Logger logger = Logger.getLogger(NewVersionCheck.class.getName());
  private final CheckType checkType;

  public NewVersionCheck(CheckType type) {
    checkType = type;
  }

  public void run() {

    // Check for updated version
    Semver currentVersion = MZmineCore.getMZmineVersion();

    Semver newestVersion = null;

    if (checkType.equals(CheckType.MENU)) {
      logger.info("Checking for updates...");
    }

    final Desktop desktop = MZmineCore.getDesktop();

    String newestVersionData = "";
    try {
      final URL newestVersionURL = new URL(newestVersionAddress);
      newestVersionData = InetUtils.retrieveData(newestVersionURL).trim();
      newestVersion = new Semver(newestVersionData, SemverType.LOOSE);
    } catch (Exception e) {
//      if (checkType.equals(CheckType.MENU)) {
      logger.log(Level.WARNING,
          "Error retrieving or parsing latest version number from MZmine website: "
              + newestVersionData, e);
//      }
    }

    if (newestVersion == null) {
      if (checkType.equals(CheckType.MENU)) {
        final String msg =
            "An error occured parsing or retrieving the latest version number. Please make sure that"
                + " you are connected to the internet or try again later.";
        logger.info(msg);
        desktop.displayMessage(msg);
      }
      return;
    }

    // Version might be: major.minor.patch-suffix+build hash
    // disregard build hash (that we currently do not use)
    if (currentVersion.isEquivalentTo(newestVersion)) {
      if (checkType.equals(CheckType.MENU)) {
        final String msg = "No updated version of MZmine is available.";
        logger.info(msg);
        desktop.displayMessage(msg);
      }
      return;
    }

    if (currentVersion.isLowerThan(newestVersion)) {
      final String msg = "An updated version is available: MZmine " + newestVersion;
      final String msg2 = "Please download the newest version from: https://mzmine.github.io";
      logger.info(msg);
      if (checkType.equals(CheckType.MENU)) {
        desktop.displayMessage(msg + "\n" + msg2);
      } else if (checkType.equals(CheckType.DESKTOP)) {
        desktop.setStatusBarText(msg + ". " + msg2, Color.RED);
      }
    }

    if (currentVersion.isGreaterThan(newestVersion)) {
      final String msg = "It seems you are running MZmine version " + currentVersion
          + ", which is newer than the latest official release " + newestVersion;
      logger.info(msg);
    }

  }
}
