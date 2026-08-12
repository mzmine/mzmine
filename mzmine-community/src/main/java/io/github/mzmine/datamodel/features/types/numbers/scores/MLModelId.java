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

package io.github.mzmine.datamodel.features.types.numbers.scores;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import org.jetbrains.annotations.NotNull;

/**
 * Identifier for the ML model that produced an {@link MLScore}. Kept separate from
 * {@code SpectralNetworkingOptions} so the score record stays ML-only and isn't conflated with
 * cosine algorithms.
 */
public enum MLModelId implements UniqueIdSupplier {

  MS2_DEEPSCORE_2_0("MS2Deepscore", "ms2deepscore_2.0", "2.0"), //
  DREAMS_1_0("DreaMS", "dreams_1.0", "1.0");

  private final String label;
  private final String uniqueId;
  private final String version;

  MLModelId(String label, String uniqueId, String version) {
    this.label = label;
    this.uniqueId = uniqueId;
    this.version = version;
  }

  /**
   * @return short human-readable label, used in score-cell text and tooltips.
   */
  public @NotNull String labelVersion() {
    return label + " " + version;
  }

  @Override
  public @NotNull String getUniqueID() {
    return uniqueId;
  }

  /**
   * @return the {@link DBEntryField} used to cache this model's embedding vector on a
   * {@code SpectralLibraryEntry}. Runtime-only — see {@link DBEntryField#isRuntimeOnly()}.
   */
  public @NotNull DBEntryField getEmbeddingField() {
    return switch (this) {
      case MS2_DEEPSCORE_2_0 -> DBEntryField.ML_EMBEDDING_MS2DEEPSCORE_2_0;
      case DREAMS_1_0 -> DBEntryField.ML_EMBEDDING_DREAMS_1_0;
    };
  }
}
