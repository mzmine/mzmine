/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.main;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.mzio.mzmine.startup.MZmineCoreArgumentParser;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

class ArgsToConfigUtilsTest {

  @TempDir
  Path root;

  private File[] previousPreferences;
  private File[] previousDirectories;

  @BeforeEach
  void saveConfiguration() {
    previousPreferences = ConfigService.getPreferences().getValue(MZminePreferences.tempDirectory);
    previousDirectories = FileAndPathUtil.getTempDirs();
    ConfigService.getPreferences().setParameter(MZminePreferences.tempDirectory,
        new File[]{root.resolve("preference-directory").toFile()});
  }

  @AfterEach
  void restoreConfiguration() {
    ConfigService.getPreferences()
        .setParameter(MZminePreferences.tempDirectory, previousPreferences);
    FileAndPathUtil.setTempDirs(previousDirectories);
  }

  @Test
  void cliReplacesThePreferenceListAndConfiguresAllTemporaryDirectories() {
    File first = root.resolve("first temp directory").toFile();
    File second = root.resolve("second temp directory").toFile();
    var parser = new MZmineCoreArgumentParser(
        new String[]{"--temp", first + File.pathSeparator + second, "--temp", first.toString()});

    ArgsToConfigUtils.checkAndOverrideArgsTempDir(parser);

    assertArrayEquals(new File[]{first, second},
        ConfigService.getPreferences().getValue(MZminePreferences.tempDirectory));
    assertArrayEquals(new File[]{first, second}, FileAndPathUtil.getTempDirs());
    assertTrue(first.isDirectory());
    assertTrue(second.isDirectory());
  }

  @Test
  @ResourceLock(Resources.SYSTEM_PROPERTIES)
  void cleanupAcceptsAnUnsetSystemTemporaryDirectory() {
    String previous = System.clearProperty("java.io.tmpdir");
    try {
      assertDoesNotThrow(() -> {
        new TmpFileCleanup();
      });
    } finally {
      if (previous == null) {
        System.clearProperty("java.io.tmpdir");
      } else {
        System.setProperty("java.io.tmpdir", previous);
      }
    }
  }

  @Test
  void absentOptionKeepsThePreferenceList() {
    ArgsToConfigUtils.checkAndOverrideArgsTempDir(new MZmineCoreArgumentParser(new String[0]));

    assertArrayEquals(new File[]{root.resolve("preference-directory").toFile()},
        ConfigService.getPreferences().getValue(MZminePreferences.tempDirectory));
  }

  @Test
  void skipsInvalidDirectoriesAndRetainsTheRemainingOrder() throws Exception {
    File invalid = Files.createFile(root.resolve("file")).toFile();
    File first = root.resolve("first").toFile();
    File second = root.resolve("second").toFile();
    var parser = new MZmineCoreArgumentParser(
        new String[]{"--temp", first.toString(), invalid.toString(), second.toString()});

    ArgsToConfigUtils.checkAndOverrideArgsTempDir(parser);

    assertArrayEquals(new File[]{first, second},
        ConfigService.getPreferences().getValue(MZminePreferences.tempDirectory));
  }

  @Test
  void mzmineDirOverridesAreSkipped() {
    File mzmineSubdirectory = new File(FileAndPathUtil.getMzmineDir(), "temp-" + System.nanoTime());
    File allowed = root.resolve("allowed").toFile();

    ArgsToConfigUtils.checkAndOverrideArgsTempDir(new MZmineCoreArgumentParser(
        new String[]{"--temp", FileAndPathUtil.getMzmineDir().toString(),
            mzmineSubdirectory.toString(), allowed.toString()}));

    assertArrayEquals(new File[]{allowed},
        ConfigService.getPreferences().getValue(MZminePreferences.tempDirectory));
    assertFalse(mzmineSubdirectory.exists());
  }

  @Test
  void unusableOverridesKeepTheConfiguredPreference() throws Exception {
    File invalid = Files.createFile(root.resolve("file")).toFile();

    ArgsToConfigUtils.checkAndOverrideArgsTempDir(
        new MZmineCoreArgumentParser(new String[]{"--temp", invalid.toString()}));

    assertArrayEquals(new File[]{root.resolve("preference-directory").toFile()},
        ConfigService.getPreferences().getValue(MZminePreferences.tempDirectory));
  }
}
