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

import io.github.mzmine.main.ConfigService;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameComponent;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.util.files.ExtensionFilters;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MSConvert {

  private static final Logger logger = Logger.getLogger(MSConvert.class.getName());
  private static final Pattern FOLDER_PATTERN = Pattern.compile(
      "(ProteoWizard)\\s(3).([0-9]+).([0-9]+)(.+)");

  private MSConvert() {
  }

  public static File getMsConvertPath() {
    return ConfigService.getConfiguration().getMsConvertPath();
  }

  public static synchronized File discoverMsConvertPath() {
    final File file = autoDiscover();
    if (file != null && file.exists()) {
      return file;
    }

    final FileNameComponent component = new FileNameComponent(List.of(), FileSelectionType.OPEN,
        List.of(ExtensionFilters.EXE, ExtensionFilters.ALL_FILES));
    final File selected = component.openSelectDialog(List.of(), FileSelectionType.OPEN,
        List.of(ExtensionFilters.EXE, ExtensionFilters.ALL_FILES));

    return selected;
  }

  private static File autoDiscover() {
    final List<File> inAppFolder = autoDiscoverInAppFolder();

    final List<File> otherPaths = List.of(new File("C:/Program Files/Proteowizard"),
        new File("C:/Program Files (x86)/Proteowizard"));
    final List<File> paths = otherPaths.stream().map(MSConvert::autoDiscoverInPath)
        .filter(Objects::nonNull).flatMap(List::stream).toList();

    List<File> allVersions = new ArrayList<>();
    if (!inAppFolder.isEmpty()) {
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

    final String m1v2 = matcher1.group(3);
    final String m2v2 = matcher2.group(3);
    if (!m1v2.equals(m2v2)) {
      return Integer.compare(Integer.parseInt(m1v2), Integer.parseInt(m2v2));
    }

    final String m1v3 = matcher1.group(4);
    final String m2v3 = matcher2.group(4);
    if (!m1v3.equals(m2v3)) {
      return Integer.compare(Integer.parseInt(m1v3), Integer.parseInt(m2v3));
    }

    return 0;
  }
}
