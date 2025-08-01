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

package io.github.mzmine.datamodel.features.types.numbers.abstr;

import io.github.mzmine.main.MZmineCore;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import org.jetbrains.annotations.NotNull;

/**
 * Used to export to generic formats. Header is just score
 */
public class ScoreType extends FloatType {

  public static final DecimalFormat DEFAULT = new DecimalFormat("0.000");

  public ScoreType() {
    super(DEFAULT);
  }

  @Override
  public @NotNull String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "score";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Score";
  }

  @Override
  public NumberFormat getFormat() {
    try {
      return MZmineCore.getConfiguration().getGuiFormats().scoreFormat();
    } catch (NullPointerException e) {
      // only happens if types are used without initializing the MZmineCore
      return DEFAULT_FORMAT;
    }
  }

  @Override
  public NumberFormat getExportFormat() {
    try {
      return MZmineCore.getConfiguration().getExportFormats().scoreFormat();
    } catch (NullPointerException e) {
      // only happens if types are used without initializing the MZmineCore
      return DEFAULT_FORMAT;
    }
  }
}
