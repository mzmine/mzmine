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

package io.github.mzmine.modules.io.import_rawdata_agilent_d;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.msms.IonMobilityMsMsInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Metadata of one Agilent IMS frame, independent of the reader.
 *
 * @param rt                     retention time (min)
 * @param polarity               polarity
 * @param msLevel                1 = low energy, 2 = high energy (fragmentation)
 * @param mobilityScanAbundLimit max possible abundance of a mobility scan point (detector
 *                               saturation level), NaN if not stored
 * @param msMsInfo               fragmentation info of MS2 frames, null for MS1
 */
public record AgilentFrameInfo(float rt, @NotNull PolarityType polarity, int msLevel,
                               double mobilityScanAbundLimit,
                               @Nullable IonMobilityMsMsInfo msMsInfo) {

}
