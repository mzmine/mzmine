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

package io.github.mzmine.gui.mainwindow;

import com.vdurmont.semver4j.Semver;
import io.github.mzmine.gui.VersionCheckResultType;
import io.github.mzmine.util.io.SemverVersionReader;

public record VersionCheckResult(VersionCheckResultType type, Semver newVersion) {

  public static VersionCheckResult of(Semver newVersion) {
    final Semver current = SemverVersionReader.getMZmineVersion();
    if (newVersion == null) {
      return new VersionCheckResult(VersionCheckResultType.CANNOT_PARSE, newVersion);
    } else if (current.isLowerThan(newVersion)) {
      return new VersionCheckResult(VersionCheckResultType.NEW_AVAILALABLE, newVersion);
    } else if (current.isEquivalentTo(newVersion)) {
      return new VersionCheckResult(VersionCheckResultType.CURRENT, newVersion);
    } else if (current.isGreaterThan(newVersion)) {
      return new VersionCheckResult(VersionCheckResultType.THIS_IS_NEWER, newVersion);
    }
    return new VersionCheckResult(VersionCheckResultType.CANNOT_PARSE, newVersion);
  }

  public String print() {
    return switch (type) {
      case NEW_AVAILALABLE ->
          "An updated version is available: mzmine %s. Please download the newest version.".formatted(
              newVersion);
      case CURRENT -> "No updated version of mzmine is available.";
      case THIS_IS_NEWER ->
          "It seems you are running mzmine version %s which is newer than the latest official release %s".formatted(
              SemverVersionReader.getMZmineVersion(), newVersion);
      case NO_INTERNET -> "Error retrieving or parsing latest version number from MZmine website.";
      case CANNOT_PARSE ->
          "An error occurred parsing or retrieving the latest version number. Please make sure that you are connected to the internet or try again later.";
    };
  }
}
