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

package io.github.mzmine.datamodel.features.correlation;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.tools.molecular_similarity.tanimoto.FingerprintType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Structure-based Tanimoto similarity between two rows. Stores the fingerprint algorithm used and
 * the InChI strings of the two structures that produced the highest pairwise similarity, so the
 * result is fully traceable.
 * <p>
 * inchi may be null when generation fails
 */
public final class R2RStructureSimilarity extends InternalTypedRowsRelationship {

  private final @NotNull FingerprintType fingerprintType;
  private final @Nullable String inchiA;
  private final @Nullable String inchiB;
  private final float similarity;

  public R2RStructureSimilarity(@NotNull final FeatureListRow a, @NotNull final FeatureListRow b,
      @NotNull final FingerprintType fingerprintType, @Nullable final String inchiA,
      @Nullable final String inchiB, final float similarity) {
    super(a, b, Type.STRUCTURE_TANIMOTO);
    this.fingerprintType = fingerprintType;
    this.inchiA = inchiA;
    this.inchiB = inchiB;
    this.similarity = similarity;
  }

  @Override
  public double getScore() {
    return similarity;
  }

  @NotNull
  @Override
  public String getAnnotation() {
    return "score=" + getScoreFormatted() + " fp=" + fingerprintType.getUniqueID();
  }

  public @NotNull FingerprintType getFingerprintType() {
    return fingerprintType;
  }

  public @Nullable String getInchiA() {
    return inchiA;
  }

  public @Nullable String getInchiB() {
    return inchiB;
  }
}
