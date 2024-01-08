/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.io.export_features_all_speclib_matches;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.MZmineCore;

class ExportFormatter {

  private static final String template = "%d-%s_mz-%s_score-%s_%s_match-%d";

  public static String getName(int id, String graphicsType, double mz, double score,
      String additionalText, int matchNumber) {
    final NumberFormats ef = MZmineCore.getConfiguration().getExportFormats();
    return template.formatted(id, graphicsType, ef.mz(mz), ef.score(score), additionalText,
        matchNumber);
  }

  public static String getName(int id, String graphicsType, double mz, double score,
      int matchNumber) {
    final NumberFormats ef = MZmineCore.getConfiguration().getExportFormats();
    return template.formatted(id, graphicsType, ef.mz(mz), ef.score(score), "", matchNumber);
  }

  public static String getName(FeatureListRow row, String graphicsType, double score,
      String additionalText, int matchNumber) {
    return getName(row.getID(), graphicsType, row.getAverageMZ(), score, additionalText,
        matchNumber);
  }
}
