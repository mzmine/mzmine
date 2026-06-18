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

package io.github.mzmine.modules.dataanalysis.compoundrowquality.checks;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/// One "agreeing" bucket of annotations after deduplication by InChIKey first block.
///
/// {@code annotation} is the highest-scoring representative in the group that carries a
/// {@code MolecularStructure} (so the result pane can always render a 2D structure for it).
/// {@code row} is the member {@link FeatureListRow} that contributed the representative.
/// {@code agreeingRows} is the set of distinct member rows whose annotation falls into this bucket,
/// sorted ascending by average m/z so the result pane can list them in a stable, scannable order.
record AnnotationAgreementGroup(@NotNull FeatureAnnotation annotation, @NotNull FeatureListRow row,
                                @NotNull List<@NotNull FeatureListRow> agreeingRows) {

}
