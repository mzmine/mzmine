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

package io.github.mzmine.modules.dataprocessing.featdet_manualintegration;

import com.google.common.collect.Range;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A single manual (re-)integration captured in the integration dashboard, holding everything needed
 * to replay it: which row of which raw file, the extraction m/z window, the manual RT integration
 * range and - for ion mobility data - the mobility window.
 *
 * @param feature       identifies the target row
 * @param rawFile       the raw file the integration was applied to, resolvable on replay via
 *                      {@link RawDataFilePlaceholder#getMatchingFile()}
 * @param mzRange       the m/z window used to extract the chromatogram
 * @param rtRange       the manual RT integration range; null only when {@code deleted} is true
 * @param mobilityRange the mobility window for IMS extraction, or null for non-IMS data
 * @param deleted       true if the feature was removed (no integration); replay removes the
 *                      feature
 */
public record ManualIntegrationEntry(@NotNull FeatureRecord feature,
                                     @NotNull RawDataFilePlaceholder rawFile,
                                     @NotNull Range<Double> mzRange, @Nullable Range<Float> rtRange,
                                     @Nullable Range<Float> mobilityRange, boolean deleted) {

}
