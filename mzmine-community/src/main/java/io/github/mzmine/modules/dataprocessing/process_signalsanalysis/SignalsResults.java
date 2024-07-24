/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.process_signalsanalysis;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.features.ModularDataRecord;
import io.github.mzmine.datamodel.features.SimpleModularDataModel;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.analysis.Ms1MatchedIntensityPercentType;
import io.github.mzmine.datamodel.features.types.analysis.Ms1MatchedSignalsPercentType;
import io.github.mzmine.datamodel.features.types.analysis.Ms1MatchedSignalsType;
import io.github.mzmine.datamodel.features.types.analysis.Ms1SignalsType;
import io.github.mzmine.datamodel.features.types.analysis.Ms2MatchedIntensityPercentType;
import io.github.mzmine.datamodel.features.types.analysis.Ms2MatchedSignalsPercentType;
import io.github.mzmine.datamodel.features.types.analysis.Ms2MatchedSignalsType;
import io.github.mzmine.datamodel.features.types.analysis.Ms2SignalsType;
import io.github.mzmine.datamodel.features.types.numbers.UniqueFragmentedPrecursorsType;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public record SignalsResults(int ms1Matched, int ms1Count, double ms1MatchedPercent,
                             double ms1IntensityMatchedPercent, int ms2Matched, int ms2Count,
                             double ms2MatchedPercent, double ms2IntensityMatchedPercent,
                             int precursorsCount) implements ModularDataRecord {

  @SuppressWarnings("rawtypes")
  public static List<DataType> getSubTypes() {
    return DataTypes.getAll(Ms1MatchedSignalsType.class, //
        Ms1SignalsType.class, //
        Ms1MatchedSignalsPercentType.class, //
        Ms1MatchedIntensityPercentType.class, //
        //MS2
        Ms2MatchedSignalsType.class, //
        Ms2SignalsType.class, //
        Ms2MatchedSignalsPercentType.class, //
        Ms2MatchedIntensityPercentType.class, //
        // other
        UniqueFragmentedPrecursorsType.class//
    );

  }

  public static SignalsResults create(final @NotNull SimpleModularDataModel values) {
    return new SignalsResults( //
        requireNonNullElse(values.get(Ms1MatchedSignalsType.class), -1),
        requireNonNullElse(values.get(Ms1SignalsType.class), -1),
        requireNonNullElse(values.get(Ms1MatchedSignalsPercentType.class), -1f),
        requireNonNullElse(values.get(Ms1MatchedIntensityPercentType.class), -1f),
        // MS2
        requireNonNullElse(values.get(Ms2MatchedSignalsType.class), -1),
        requireNonNullElse(values.get(Ms2SignalsType.class), -1),
        requireNonNullElse(values.get(Ms2MatchedSignalsPercentType.class), -1f),
        requireNonNullElse(values.get(Ms2MatchedIntensityPercentType.class), -1f),
        // other
        requireNonNullElse(values.get(UniqueFragmentedPrecursorsType.class), -1));
  }


  /**
   * Provides data for data types
   *
   * @param sub data column
   * @return the score for this column
   */
  @Override
  public Object getValue(final DataType sub) {
    return switch (sub) {
      case Ms1MatchedSignalsType _ -> ms1Matched;
      case Ms1SignalsType _ -> ms1Count;
      case Ms1MatchedSignalsPercentType _ -> ms1MatchedPercent;
      case Ms1MatchedIntensityPercentType _ -> ms1IntensityMatchedPercent;
      //MS2
      case Ms2MatchedSignalsType _ -> ms2Matched;
      case Ms2SignalsType _ -> ms2Count;
      case Ms2MatchedSignalsPercentType _ -> ms2MatchedPercent;
      case Ms2MatchedIntensityPercentType _ -> ms2IntensityMatchedPercent;
      // other
      case UniqueFragmentedPrecursorsType _ -> precursorsCount;
      default -> throw new IllegalStateException("Unexpected value: " + sub);
    };
  }
}
