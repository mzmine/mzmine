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

package io.github.mzmine.gui;

import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.Semver.SemverType;
import io.github.mzmine.gui.mainwindow.VersionCheckResult;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.InetUtils;
import io.github.mzmine.util.io.SemverVersionReader;
import java.net.URL;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;

public class NewVersionCheck implements Runnable {

  public static final String newestVersionAddress = "http://mzmine.github.io/version.txt";

  public enum CheckType {
    DESKTOP, MENU
  }

  private static final Logger logger = Logger.getLogger(NewVersionCheck.class.getName());
  private final CheckType checkType;
  private final ObjectProperty<VersionCheckResult> result = new SimpleObjectProperty<>(null);

  public NewVersionCheck(CheckType type) {
    checkType = type;
  }

  public void run() {

    // Check for updated version
    Semver currentVersion = SemverVersionReader.getMZmineVersion();

    Semver newestVersion = null;

    if (checkType.equals(CheckType.MENU)) {
      logger.info("Checking for updates...");
    }

    final MZmineDesktop desktop = MZmineCore.getDesktop();

    String newestVersionData = "";
    try {
      final URL newestVersionURL = new URL(newestVersionAddress);
      newestVersionData = InetUtils.retrieveData(newestVersionURL).trim();
      newestVersion = new Semver(newestVersionData, SemverType.LOOSE);
    } catch (Exception e) {
      result.set(new VersionCheckResult(VersionCheckResultType.NO_INTERNET, newestVersion));
//      logger.log(Level.WARNING, result.get().print(), e);
    }

    if (newestVersion == null) {
      result.set(new VersionCheckResult(VersionCheckResultType.CANNOT_PARSE, newestVersion));
      if (checkType.equals(CheckType.MENU)) {
        logger.info(result.get().print());
        desktop.displayMessage(result.get().print());
      }
      return;
    }

    // Version might be: major.minor.patch-suffix+build hash
    // disregard build hash (that we currently do not use)
    if (currentVersion.isEquivalentTo(newestVersion)) {
      result.set(new VersionCheckResult(VersionCheckResultType.CURRENT, newestVersion));
      if (checkType.equals(CheckType.MENU)) {
        logger.info(result.get().print());
        desktop.displayMessage(result.get().print());
      }
      return;
    }

    if (currentVersion.isLowerThan(newestVersion)) {
      result.set(new VersionCheckResult(VersionCheckResultType.NEW_AVAILALABLE, newestVersion));
      String url = "https://mzmine.org";
      logger.info(result.get().print());
      if (checkType.equals(CheckType.MENU)) {
        desktop.displayMessage("New version", result.get().print(), url);
      } else if (checkType.equals(CheckType.DESKTOP)) {
        String downloadUrl = "https://github.com/mzmine/mzmine3/releases/latest";
        Color color = MZmineCore.getConfiguration().getDefaultColorPalette().getNegativeColor();
        desktop.setStatusBarText(result.get().print().replace("\n", ". ") + url, color,
            downloadUrl);
      }
    }

    if (currentVersion.isGreaterThan(newestVersion)) {
      result.set(new VersionCheckResult(VersionCheckResultType.THIS_IS_NEWER, newestVersion));
      logger.info(result.get().print());
    }
  }

  public VersionCheckResult getResult() {
    return result.get();
  }

  public ObjectProperty<VersionCheckResult> resultProperty() {
    return result;
  }
}
