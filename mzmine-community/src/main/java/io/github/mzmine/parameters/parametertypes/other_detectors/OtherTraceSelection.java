/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.parameters.parametertypes.other_detectors;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFile;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.ChromatogramType;
import io.github.mzmine.util.TextUtils;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public class OtherTraceSelection {

  @Nullable
  private final ChromatogramType chromatogramType;

  @Nullable
  private final String rangeUnitFilter;

  @Nullable
  private final String rangeLabelFilter;

  @Nullable
  private final String nameFilter;

  private final OtherRawOrProcessed rawOrProcessed;

  public OtherTraceSelection(@Nullable ChromatogramType chromatogramType,
      @Nullable String rangeUnitFilter, @Nullable String rangeLabelFilter,
      @Nullable String nameFilter, @Nullable OtherRawOrProcessed rawOrProcessed) {
    this.chromatogramType = chromatogramType;
    this.rangeUnitFilter =
        rangeUnitFilter != null ? TextUtils.createRegexFromWildcards(rangeUnitFilter) : null;
    this.rangeLabelFilter =
        rangeLabelFilter != null ? TextUtils.createRegexFromWildcards(rangeLabelFilter) : null;
    this.nameFilter = nameFilter != null ? TextUtils.createRegexFromWildcards(nameFilter) : null;
    this.rawOrProcessed = rawOrProcessed;
  }

  public List<OtherFeature> getMatchingTraces(Collection<RawDataFile> msFiles) {
    return msFiles.stream().flatMap(f -> f.getOtherDataFiles().stream())
        .filter(OtherDataFile::hasTimeSeries).map(OtherDataFile::getOtherTimeSeries)//
        .filter(
            data -> chromatogramType != null || data.getChromatogramType() == chromatogramType)//
        .filter(data -> rangeUnitFilter == null || data.getTimeSeriesRangeUnit()
            .matches(rangeLabelFilter))//
        .filter(data -> rangeLabelFilter == null || data.getTimeSeriesRangeLabel()
            .matches(rangeLabelFilter))//
        .filter(data -> nameFilter != null || data.getOtherDataFile().getDescription()
            .matches(nameFilter)) //
        .flatMap(rawOrProcessed::streamMatching).toList();
  }

}
