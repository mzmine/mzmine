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

package io.github.mzmine.datamodel.otherdetectors;

import io.github.mzmine.datamodel.RawDataFile;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * A link from one MS feature row to one aligned other-detector row ({@link OtherFeatureListRow}),
 * identified by the other-row ID only (no embedded feature). The per-file map records, for each
 * {@link RawDataFile} where the MS feature actually correlated to that file's peak of the aligned
 * other-row, the correlation details ({@link PerFileCorrelation}). A file absent from the map did not
 * correlate.
 *
 * @param otherRowId the {@link OtherFeatureListRow#getID()} of the correlated aligned other-row
 * @param perFile    per-file correlation truth; resolve the actual other feature via
 *                   {@code otherRow.getFeature(file)}
 */
public record OtherCorrelationLink(int otherRowId,
                                   @NotNull Map<RawDataFile, PerFileCorrelation> perFile) {

}
