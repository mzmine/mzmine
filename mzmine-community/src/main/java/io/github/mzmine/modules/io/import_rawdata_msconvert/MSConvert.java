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

package io.github.mzmine.modules.io.import_rawdata_msconvert;

import static io.github.mzmine.util.files.ExtensionFilters.MSCONVERT;

import com.vdurmont.semver4j.Semver;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.util.FxFileChooser;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.stage.FileChooser.ExtensionFilter;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MSConvert {

  private static final Logger logger = Logger.getLogger(MSConvert.class.getName());
  private static final Pattern FOLDER_PATTERN = Pattern.compile(
      "(ProteoWizard)\\s(3.[0-9]+.[0-9]+.[0-9a-z]+)\\s(.+)");

  private MSConvert() {
  }

  public static File getMsConvertPath() {
    return ConfigService.getConfiguration().getMsConvertPath();
  }

  public static boolean validateMsConvertPath(File path) {
    if (!path.exists()) {
      return false;
    }

    if (!path.getName().equals("msconvert.exe")) {
      return false;
    }

    return true;
  }

  @Nullable
  public static synchronized File discoverMsConvertPath() {
    final File file = autoDiscover();
    if (file != null && file.exists()) {
      return file;
    }

    final AtomicReference<File> selected = new AtomicReference<>();
    if (MZmineCore.isHeadLessMode()) {
      logger.warning(
          () -> "Cannot find MSConvert in the regular install directories. Please set the MSConvert path in the config before launching in headless mode.");
    } else {
      FxThread.runOnFxThreadAndWait(() -> {
        selected.set(
            FxFileChooser.openSelectDialog(FxFileChooser.FileSelectionType.OPEN, List.of(MSCONVERT),
                null, "Please select the MSConvert path."));
      });
    }

    return selected.get();
  }

  private static File autoDiscover() {
    final List<File> inAppFolder = autoDiscoverInAppFolder();

    final List<File> otherPaths = List.of(new File("C:/Program Files/Proteowizard"),
        new File("C:/Program Files (x86)/Proteowizard"));
    final List<File> paths = otherPaths.stream().map(MSConvert::autoDiscoverInPath)
        .filter(Objects::nonNull).flatMap(List::stream).toList();

    List<File> allVersions = new ArrayList<>();
    if (inAppFolder != null && !inAppFolder.isEmpty()) {
      allVersions.addAll(inAppFolder);
    }
    allVersions.addAll(paths);
    if (allVersions.isEmpty()) {
      return null;
    }

    final File latestVersion = getLatestVersion(allVersions);
    logger.finest(() -> "Latest MSConvert version found at %s".formatted(
        latestVersion.getAbsolutePath().toString()));
    return latestVersion;
  }

  @Nullable
  private static List<File> autoDiscoverInAppFolder() {
    final File userDirectory = FileUtils.getUserDirectory();
    final File appFolder = new File(userDirectory, "AppData/Local/Apps");
    if (!appFolder.exists()) {
      logger.info(() -> "Cannot locate MSConvert. Path to %s folder does not exist.".formatted(
          appFolder.getAbsolutePath()));
      return null;
    }

    final List<File> proteowizardFolders = listProteowizardFolders(appFolder);
    if (proteowizardFolders.isEmpty()) {
      logger.info(() -> "Cannot locate MSConvert. No ProteoWizard 3 folder found in %s.".formatted(
          appFolder.getAbsolutePath()));
      return null;
    }
    return proteowizardFolders;
  }

  @Nullable
  private static List<File> autoDiscoverInPath(File path) {
    if (!path.exists()) {
      logger.info(() -> "Cannot locate MSConvert. Path to %s folder does not exist.".formatted(
          path.getAbsolutePath()));
      return null;
    }

    final List<File> proteowizardFolders = listProteowizardFolders(path);
    if (proteowizardFolders.isEmpty()) {
      logger.info(() -> "Cannot locate MSConvert. No ProteoWizard 3 folder found in %s.".formatted(
          path.getAbsolutePath()));
      return null;
    }
    return proteowizardFolders;
  }

  private static @NotNull List<File> listProteowizardFolders(File appFolder) {
    final File[] files = appFolder.listFiles();
    if (files == null) {
      return List.of();
    }

    final List<File> proteowizardFolders = new ArrayList<>();
    for (File file : files) {
      if (FOLDER_PATTERN.matcher(file.getName()).matches()) {
        proteowizardFolders.add(file);
      }
    }
    return proteowizardFolders;
  }

  private static @NotNull File getLatestVersion(@NotNull List<File> proteowizardFolders) {
    if (proteowizardFolders.isEmpty()) {
      throw new RuntimeException("Cannot find any version of MSConvert on this computer.");
    }

    // get the latest MSConvert version
    final File latestMsConvertFolder = proteowizardFolders.stream()
        .sorted(MSConvert::compareVersions).toList().getLast();

    final File msconvert = new File(latestMsConvertFolder, "msconvert.exe");
    if (!msconvert.exists()) {
      logger.info(
          () -> "Cannot locate MSConvert. MSConvert does not exist in folder found in %s.".formatted(
              latestMsConvertFolder.getAbsolutePath()));
    }
    return msconvert;
  }

  private static int compareVersions(File o1, File o2) {
    final Matcher matcher1 = FOLDER_PATTERN.matcher(o1.getName());
    final Matcher matcher2 = FOLDER_PATTERN.matcher(o2.getName());

    matcher1.matches();
    matcher2.matches();

    Semver v1 = new Semver(matcher1.group(2));
    Semver v2 = new Semver(matcher2.group(2));
    return v1.compareTo(v2);
  }
}
