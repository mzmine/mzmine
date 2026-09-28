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

package io.github.mzmine.modules.visualization.projectmetadata;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.util.StringUtils;
import java.util.List;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The sample types mzmine knows out of the box. They are only <b>defaults</b>: the actual sample
 * type of a file is the free text value of the {@code mzmine_sample_type} metadata column, which
 * the user may overwrite with any group name. See
 * {@link io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter} for the matching
 * side, which works on plain strings and never on the heuristics below.
 * <p>
 * {@link #guessFromName(String)} is a <b>file name heuristic</b> that is only used to seed the
 * metadata column on import (and by the batch wizard pre-check, which runs before any data is
 * loaded). It must never be used to interpret a value that is already stored in the metadata column
 * - such a value is authoritative and is compared case insensitively as-is.
 */
public enum SampleType implements UniqueIdSupplier {
  BLANK, SAMPLE, QC, CALIBRATION,
  /// system suitability test (standard mix injection)
  SST;

  /**
   * The keyword of a type must not be glued to other letters: the lookarounds give it the letter
   * boundaries that the previous patterns intended but never applied. They used to quantify the
   * lookarounds ({@code (?<![a-z])*}), which makes them match zero times - a no-op that turned
   * every pattern into a plain substring search, so {@code chemical} looked like a calibration and
   * {@code multimedia} like a blank.
   * <p>
   * Digits, {@code -} and {@code _} are not letters and therefore already allowed on both sides,
   * which is what makes {@code qc3}, {@code 3qc} and {@code sample-QC-1.raw} a QC. Spellings that
   * concatenate two words ({@code pooledqc}, {@code mediablank}, {@code systemsuitability}) or join
   * them with a separator ({@code std_mix}) have to be listed explicitly - by design, since the
   * whole point of the boundaries is that a keyword touching other letters does not count. Plural
   * forms are deliberately not matched.
   */
  private static final Pattern QC_PATTERN = Pattern.compile(
      ".*(?<![a-zA-Z])((pooled[-_]?)?qc([-_]?pool(ed)?)?)(?![a-zA-Z]).*");
  private static final Pattern BLANK_PATTERN = Pattern.compile(
      ".*(?<![a-zA-Z])((media[-_]?)?blank|media|blk)(?![a-zA-Z]).*");
  private static final Pattern CALIBRATION_PATTERN = Pattern.compile(
      ".*(?<![a-zA-Z])(cal|calibration|calibrant|quant)(?![a-zA-Z]).*");
  private static final Pattern SST_PATTERN = Pattern.compile(
      ".*(?<![a-zA-Z])(sst|system[-_ ]?suitability|standards?[-_ ]?mix|std[-_ ]?mix)(?![a-zA-Z]).*");

  /**
   * Guesses the default sample type from a file name. Checked in the order QC, blank, calibration,
   * SST so that the historic behaviour of the previous implementation is preserved for the types it
   * knew; anything unrecognised defaults to {@link #SAMPLE}.
   * <p>
   * A keyword only counts if no letter touches it, so {@code QC_01}, {@code pooled_qc} and
   * {@code pooledqc} are all {@link #QC}, while {@code chemical}, {@code typical} and
   * {@code multimedia} are {@link #SAMPLE}.
   *
   * @param name a file name, never a value read back from the sample type metadata column
   */
  @NotNull
  public static SampleType guessFromName(@Nullable String name) {
    if (name == null) {
      return SAMPLE;
    }
    final String normalized = StringUtils.normalizeStripLowerCase(name);

    if (QC_PATTERN.matcher(normalized).matches()) {
      return QC;
    }
    if (BLANK_PATTERN.matcher(normalized).matches()) {
      return BLANK;
    }
    if (CALIBRATION_PATTERN.matcher(normalized).matches()) {
      return CALIBRATION;
    }
    if (SST_PATTERN.matcher(normalized).matches()) {
      return SST;
    }
    return SAMPLE;
  }

  /**
   * Guesses the default sample type from the file name, see {@link #guessFromName(String)}.
   */
  @NotNull
  public static SampleType ofFile(@NotNull final RawDataFile file) {
    return guessFromName(file.getName());
  }

  /**
   * @return the type whose {@link #toString()} equals the value ignoring case and surrounding
   * whitespace, or null for a custom user defined value
   */
  @Nullable
  public static SampleType ofExactValue(@Nullable final String value) {
    final String normalized = StringUtils.normalizeStripLowerCase(value);
    for (SampleType type : values()) {
      if (type.toString().equals(normalized)) {
        return type;
      }
    }
    return null;
  }

  /**
   * @return the {@link #toString()} values of all predefined types
   */
  @NotNull
  public static List<String> allValueStrings() {
    return List.of(values()).stream().map(SampleType::toString).toList();
  }

  @Override
  public String toString() {
    return switch (this) {
      case BLANK -> "blank";
      case SAMPLE -> "sample";
      case QC -> "qc";
      case CALIBRATION -> "calibration";
      case SST -> "sst";
    };
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case BLANK -> "blank";
      case SAMPLE -> "sample";
      case QC -> "qc";
      case CALIBRATION -> "calibration";
      case SST -> "sst";
    };
  }
}
