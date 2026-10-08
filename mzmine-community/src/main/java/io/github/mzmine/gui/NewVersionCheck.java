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
import io.github.mzmine.gui.update.UpdateCheckService;
import io.github.mzmine.gui.update.UpdateStatus;
import io.github.mzmine.main.MZmineCore;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NewVersionCheck implements Runnable {

  public static final String newestVersionAddress = UpdateCheckService.VERSION_URL;

  public enum CheckType {
    DESKTOP, MENU
  }

  private static final Logger logger = Logger.getLogger(NewVersionCheck.class.getName());
  private final CheckType checkType;
  private final ObjectProperty<VersionCheckResult> result = new SimpleObjectProperty<>(null);

  public NewVersionCheck(@NotNull final CheckType type) {
    checkType = type;
  }

  public void run() {
    if (checkType.equals(CheckType.MENU)) {
      logger.info("Checking for updates...");
    }

    final UpdateStatus status = UpdateCheckService.getInstance().check();
    final VersionCheckResult mapped = toLegacyResult(status);
    result.set(mapped);

    final MZmineDesktop desktop = MZmineCore.getDesktop();
    logger.info(mapped.print());
    if (checkType.equals(CheckType.MENU)) {
      if (mapped.type() == VersionCheckResultType.NEW_AVAILALABLE) {
        desktop.displayMessage("New version", mapped.print(), status.releaseUrl());
      } else {
        desktop.displayMessage(mapped.print());
      }
    } else if (checkType.equals(CheckType.DESKTOP)
        && mapped.type() == VersionCheckResultType.NEW_AVAILALABLE) {
      final Color color = MZmineCore.getConfiguration().getDefaultColorPalette().getNegativeColor();
      desktop.setStatusBarText(mapped.print().replace("\n", ". ") + status.releaseUrl(), color,
          status.releaseUrl());
    }
  }

  @Nullable
  public VersionCheckResult getResult() {
    return result.get();
  }

  @NotNull
  public ObjectProperty<VersionCheckResult> resultProperty() {
    return result;
  }

  @NotNull
  private static VersionCheckResult toLegacyResult(@NotNull final UpdateStatus status) {
    final Semver latest = status.latestVersion() == null ? null : new Semver(status.latestVersion(),
        SemverType.LOOSE);
    return switch (status.status()) {
      case UpdateStatus.CURRENT -> new VersionCheckResult(VersionCheckResultType.CURRENT, latest);
      case UpdateStatus.UPDATE_AVAILABLE -> new VersionCheckResult(VersionCheckResultType.NEW_AVAILALABLE,
          latest);
      case UpdateStatus.DEVELOPMENT_NEWER -> new VersionCheckResult(VersionCheckResultType.THIS_IS_NEWER,
          latest);
      case UpdateStatus.UNAVAILABLE -> new VersionCheckResult(VersionCheckResultType.NO_INTERNET, null);
      default -> new VersionCheckResult(VersionCheckResultType.CANNOT_PARSE, null);
    };
  }
}
