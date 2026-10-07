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

package io.github.mzmine.modules.visualization.dash_lipidqc.quality;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.visualization.dash_lipidqc.kendrick.KendrickFalseNegativeCandidate;
import io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils.InterferenceMetrics;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Computation result for the annotation quality pane, containing quality cards for all candidate
 * annotations, interference and duplicate-row information, and optional false-positive / false-
 * negative flags for the selected row.
 */
record QualityComputationResult(@Nullable String placeholderText,
                                @Nullable MatchedLipid selectedAnnotation,
                                @NotNull InterferenceMetrics interferenceMetrics,
                                @NotNull List<FeatureListRow> duplicateRows,
                                @NotNull List<QualityCardData> qualityCards,
                                @Nullable String falsePositiveReason,
                                @Nullable KendrickFalseNegativeCandidate falseNegativeCandidate,
                                @Nullable QualityCardData falseNegativeQualityCard) {

}
