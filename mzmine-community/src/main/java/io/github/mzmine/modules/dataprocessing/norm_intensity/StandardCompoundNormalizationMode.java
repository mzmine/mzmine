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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import org.jetbrains.annotations.NotNull;

/**
 * Defines how strict the internal standard normalization is about standards that were not detected
 * in a reference sample.
 * <p>
 * All modes only apply to the reference samples selected by the "Reference samples" parameter. Files
 * that are not reference samples are never checked for standards, they are always normalized by
 * interpolation between the neighboring reference samples.
 * <p>
 * Replaces the legacy boolean "Require all standards" parameter: {@code true} maps to
 * {@link #REQUIRE_ALL_IN_ALL_SAMPLES} and {@code false} to {@link #REQUIRE_N_SAMPLES}.
 */
public enum StandardCompoundNormalizationMode implements UniqueIdSupplier {

  /**
   * Every matched standard must be detected with a usable abundance in every reference sample.
   * Otherwise the normalization fails. This is the default and the strictest option.
   */
  REQUIRE_ALL_IN_ALL_SAMPLES,
  /**
   * Missing standards are skipped, but each reference sample needs at least N usable standard. A
   * reference sample without any standard fails the normalization.
   */
  REQUIRE_N_SAMPLES,
  /**
   * Missing standards are skipped and reference samples without any usable standard are skipped as
   * well. Such samples are then normalized by interpolation between the neighboring reference
   * samples.
   */
  SKIP_FILES_WITHOUT_STANDARD;

  public static @NotNull StandardCompoundNormalizationMode getDefault() {
    return REQUIRE_ALL_IN_ALL_SAMPLES;
  }

  @Override
  public String toString() {
    return switch (this) {
      case REQUIRE_ALL_IN_ALL_SAMPLES -> "Require all standards in all reference samples (default)";
      case REQUIRE_N_SAMPLES -> "Require at least N standard in each reference sample";
      case SKIP_FILES_WITHOUT_STANDARD -> "Skip reference samples without standard";
    };
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case REQUIRE_ALL_IN_ALL_SAMPLES -> "require_all_in_all_samples";
      case REQUIRE_N_SAMPLES -> "require_n_per_sample";
      case SKIP_FILES_WITHOUT_STANDARD -> "skip_files_without_standard";
    };
  }
}
