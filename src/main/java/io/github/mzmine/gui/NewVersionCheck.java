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
