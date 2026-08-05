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

package io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import org.jetbrains.annotations.NotNull;

/**
 * Defines the type of lipid analysis experiment (e.g. reversed-phase LC-MS, HILIC, direct
 * infusion, imaging) and whether a retention-time elution pattern is expected for QC scoring.
 */
public enum LipidAnalysisType implements UniqueIdSupplier {
  LC_REVERSED_PHASE("lc_reversed_phase", "LC-MS (reversed phase)", true),
  LC_HILIC("lc_hilic", "LC-MS (HILIC)", true),
  DIRECT_INFUSION("direct_infusion", "Direct infusion", false),
  IMAGING("imaging", "Imaging", false);

  private final @NotNull String uniqueId;
  private final @NotNull String label;
  private final boolean hasRetentionTimePattern;

  LipidAnalysisType(final @NotNull String uniqueId, final @NotNull String label,
      final boolean hasRetentionTimePattern) {
    this.uniqueId = uniqueId;
    this.label = label;
    this.hasRetentionTimePattern = hasRetentionTimePattern;
  }

  public boolean hasRetentionTimePattern() {
    return hasRetentionTimePattern;
  }

  @Override
  public @NotNull String getUniqueID() {
    return uniqueId;
  }

  @Override
  public @NotNull String toString() {
    return label;
  }
}
