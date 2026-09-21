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

package io.github.mzmine.modules.io.projectsave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The project loader does not know the sidecar file names, it derives them from the path of the
 * feature list data file by swapping the suffix. These tests pin that coupling, because a mismatch
 * would not fail anywhere - the loader would just silently find no file and skip the data.
 */
class FeatureListSidecarNamingTest {

  private static final String FLIST_NAME = "my feature list";

  @Test
  void ionNetworkSidecarIsFoundFromTheDataFileName() {
    assertEquals(FeatureListSaveTask.getIinFileName(FLIST_NAME),
        derivedByLoader(FeatureListSaveTask.IIN_FILE_SUFFIX));
  }

  @Test
  void r2rSidecarIsFoundFromTheDataFileName() {
    assertEquals(FeatureListSaveTask.getR2RFileName(FLIST_NAME),
        derivedByLoader(FeatureListSaveTask.R2R_FILE_SUFFIX));
  }

  @Test
  void sidecarsUseDistinctSuffixesInsideTheFeatureListFolder() {
    assertNotEquals(FeatureListSaveTask.IIN_FILE_SUFFIX, FeatureListSaveTask.R2R_FILE_SUFFIX);
    assertNotEquals(FeatureListSaveTask.IIN_FILE_SUFFIX, FeatureListSaveTask.DATA_FILE_SUFFIX);
    assertTrue(
        FeatureListSaveTask.getIinFileName(FLIST_NAME).startsWith(FeatureListSaveTask.FLIST_FOLDER),
        "sidecars must be extracted with the flist");
  }

  /**
   * Mirrors what the loader does: take the data file path and swap the suffix.
   */
  private static String derivedByLoader(final String sidecarSuffix) {
    return FeatureListSaveTask.getDataFileName(FLIST_NAME)
        .replace(FeatureListSaveTask.DATA_FILE_SUFFIX, sidecarSuffix);
  }
}
