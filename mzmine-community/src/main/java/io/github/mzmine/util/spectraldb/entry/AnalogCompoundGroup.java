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

package io.github.mzmine.util.spectraldb.entry;

import io.github.mzmine.datamodel.features.FeatureListRow;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A deduplicated cluster of analog spectral-library matches that all describe the same chemical
 * compound (linked by any matching
 * {@link io.github.mzmine.datamodel.features.compoundannotations.CompoundNameIdentifier} value).
 *
 * @param members        every (row, annotation) pair that fell into the cluster
 * @param representative the annotation that best represents the cluster (most non-null identifier
 *                       fields, ties broken by highest similarity score)
 * @param compoundKey    the preferred identifier value for this cluster (compound name → InChIKey →
 *                       SMILES → ... in
 *                       {@link
 *                       io.github.mzmine.datamodel.features.compoundannotations.CompoundNameIdentifier}
 *                       order); used as a stable display label
 */
public record AnalogCompoundGroup(@NotNull List<RowAnnotation> members,
                                  @NotNull SpectralDBAnnotation representative,
                                  @Nullable String compoundKey) {

  /**
   * A (row, annotation) pair input to {@link AnalogCompoundGrouper#group(List)}. Keeps the source
   * row alongside its analog annotation so the grouper can emit edges back to the right rows.
   */
  public record RowAnnotation(@NotNull FeatureListRow row,
                              @NotNull SpectralDBAnnotation annotation) {

  }
}
