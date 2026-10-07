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

package io.github.mzmine.modules.tools.molecular_similarity.tanimoto;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.fingerprint.CircularFingerprinter;
import org.openscience.cdk.fingerprint.ExtendedFingerprinter;
import org.openscience.cdk.fingerprint.Fingerprinter;
import org.openscience.cdk.fingerprint.IFingerprinter;

/**
 * Selectable CDK molecular fingerprint presets used for Tanimoto similarity. Each preset bundles a
 * fingerprint algorithm with a sensible bit length so users pick one ready-to-use option instead of
 * tuning raw numbers. All presets produce an
 * {@link org.openscience.cdk.fingerprint.IBitFingerprint} compatible with
 * {@link org.openscience.cdk.similarity.Tanimoto#calculate(java.util.BitSet, java.util.BitSet)}.
 * <p>
 * Each call to {@link #createFingerprinter()} returns a fresh instance. The circular
 * {@link CircularFingerprinter} keeps mutable state during calculation and is therefore not
 * thread-safe - callers that run in parallel must use one instance per thread (see
 * {@link TanimotoSimilarity} which keeps a {@link ThreadLocal}).
 */
public enum FingerprintType implements UniqueIdSupplier {

  /**
   * Circular / Morgan ECFP fingerprint, diameter 4 (radius 2), folded to 2048 bits. Common default
   * for structural similarity of small molecules - more bits reduce collisions.
   */
  ECFP4_2048,
  /**
   * Circular / Morgan ECFP fingerprint, diameter 4 (radius 2), folded to 1024 bits.
   */
  ECFP4_1024,
  /**
   * Circular / Morgan ECFP fingerprint, diameter 6 (radius 3), folded to 2048 bits. Captures larger
   * substructures than ECFP4.
   */
  ECFP6_2048,
  /**
   * Path-based Daylight-like fingerprint, 2048 bits (search depth 7).
   */
  DAYLIGHT_2048,
  /**
   * Path-based Daylight-like fingerprint, 1024 bits (search depth 7). The historic mzmine default.
   */
  DAYLIGHT_1024,
  /**
   * Like Daylight but adds ring-system information on top of the path-based bits, 2048 bits.
   */
  EXTENDED_2048;

  /**
   * @return a new {@link IFingerprinter} instance for this preset. Not shared - construct one per
   * thread for parallel processing.
   */
  @NotNull
  public IFingerprinter createFingerprinter() {
    return switch (this) {
      case ECFP4_2048 -> new CircularFingerprinter(CircularFingerprinter.CLASS_ECFP4, 2048);
      case ECFP4_1024 -> new CircularFingerprinter(CircularFingerprinter.CLASS_ECFP4, 1024);
      case ECFP6_2048 -> new CircularFingerprinter(CircularFingerprinter.CLASS_ECFP6, 2048);
      case DAYLIGHT_2048 -> new Fingerprinter(2048);
      case DAYLIGHT_1024 -> new Fingerprinter(1024);
      case EXTENDED_2048 -> new ExtendedFingerprinter(2048);
    };
  }

  @Override
  public String toString() {
    return switch (this) {
      case ECFP4_2048 -> "ECFP4 (circular, 2048 bit)";
      case ECFP4_1024 -> "ECFP4 (circular, 1024 bit)";
      case ECFP6_2048 -> "ECFP6 (circular, 2048 bit)";
      case DAYLIGHT_2048 -> "Daylight (path, 2048 bit)";
      case DAYLIGHT_1024 -> "Daylight (path, 1024 bit)";
      case EXTENDED_2048 -> "Extended (path + rings, 2048 bit)";
    };
  }

  @Override
  public @NotNull String getUniqueID() {
    // stable IDs for save/load - do not change
    return switch (this) {
      case ECFP4_2048 -> "ecfp4_2048";
      case ECFP4_1024 -> "ecfp4_1024";
      case ECFP6_2048 -> "ecfp6_2048";
      case DAYLIGHT_2048 -> "daylight_2048";
      case DAYLIGHT_1024 -> "daylight_1024";
      case EXTENDED_2048 -> "extended_2048";
    };
  }
}
