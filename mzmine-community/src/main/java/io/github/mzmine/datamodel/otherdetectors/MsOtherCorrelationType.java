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

package io.github.mzmine.datamodel.otherdetectors;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import org.jetbrains.annotations.NotNull;

public enum MsOtherCorrelationType implements UniqueIdSupplier {
  /**
   * The correlation was set manually in the dashboard.
   */
  MANUAL,
  /**
   * The correlation was calculated (Pearson score above threshold).
   */
  CALCULATED,
  /**
   * The other-detector trace is aligned into this raw file, but the file's MS feature did not
   * correlate to it (below threshold, no RT overlap, or no MS feature). This is a derived status: it
   * is never stored, only computed at render time for files that lack a real correlation entry.
   */
  ALIGNED;


  @Override
  public String toString() {
    return switch (this) {
      case MANUAL -> "Manual";
      case CALCULATED -> "Calculated";
      case ALIGNED -> "Aligned";
    };
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case MANUAL -> "MANUAL";
      case CALCULATED -> "CALCULATED";
      case ALIGNED -> "ALIGNED";
    };
  }
}
