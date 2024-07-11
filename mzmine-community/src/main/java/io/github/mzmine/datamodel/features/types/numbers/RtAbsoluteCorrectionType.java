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

package io.github.mzmine.datamodel.features.types.numbers;

import io.github.mzmine.datamodel.features.types.numbers.abstr.FloatType;
import io.github.mzmine.main.MZmineCore;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import org.jetbrains.annotations.NotNull;

/**
 * This class represents the absolute retention time (RT) correction type. It extends
 * {@link FloatType} to format and manage RT correction values.
 *
 * <p>The corrected RT value is calculated as follows:
 * <pre>
 * float correctedRt = (float) (correctedRT - originalRT);
 * correctedFeature.set(RtAbsoluteCorrectionType.class, correctedRt);
 * </pre>
 * Where:
 * <ul>
 *   <li><code>correctedRTdRT</code> is the corrected retention time.</li>
 *   <li><code>originalRT</code> is the original retention time.</li>
 * </ul>
 */
public class RtAbsoluteCorrectionType extends FloatType {

  public RtAbsoluteCorrectionType() {
    super(new DecimalFormat("0.000"));
  }

  @Override
  public @NotNull String getUniqueID() {
    return "rt_absolute_correction";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "ΔRT correction";
  }

  @Override
  public NumberFormat getFormat() {
    try {
      return MZmineCore.getConfiguration().getRTFormat();
    } catch (NullPointerException e) {
      return DEFAULT_FORMAT;
    }
  }

  @Override
  public NumberFormat getExportFormat() {
    try {
      return MZmineCore.getConfiguration().getExportFormats().rtFormat();
    } catch (NullPointerException e) {
      return DEFAULT_FORMAT;
    }
  }

}
