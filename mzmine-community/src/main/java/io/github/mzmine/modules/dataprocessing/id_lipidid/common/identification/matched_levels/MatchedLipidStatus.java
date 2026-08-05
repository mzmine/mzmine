/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum MatchedLipidStatus implements UniqueIdSupplier {

  /**
   * confirmed by MS2 fragmentation rules
   */
  MATCHED,//
  /**
   * MS1-only match, not confirmed by MS2
   */
  UNCONFIRMED;//


  @Nullable
  public String getComment() {
    return switch (this) {
      case MATCHED -> null;
      case UNCONFIRMED -> "Warning, this annotation is based on MS1 mass accuracy only!";
    };
  }


  public boolean isConfirmedByMS2() {
    return switch (this) {
      case MATCHED -> true;
      case UNCONFIRMED -> false;
    };
  }

  public static MatchedLipidStatus parseOrElse(@Nullable String str,
      MatchedLipidStatus defaultStatus) {
    return UniqueIdSupplier.parseOrElse(str, values(), defaultStatus);
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case MATCHED -> "MATCHED";
      case UNCONFIRMED -> "UNCONFIRMED";
    };
  }
}
