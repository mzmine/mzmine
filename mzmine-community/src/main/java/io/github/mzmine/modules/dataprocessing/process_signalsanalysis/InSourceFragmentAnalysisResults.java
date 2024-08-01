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
import io.github.mzmine.datamodel.features.types.analysis.IsLikelyISFragmentType;
import io.github.mzmine.datamodel.features.types.analysis.Ms1FragmentedIntensityPercentType;
import io.github.mzmine.datamodel.features.types.analysis.Ms1FragmentedLikelyISFPercentType;
import io.github.mzmine.datamodel.features.types.analysis.Ms1FragmentedSignalsPercentType;
import io.github.mzmine.datamodel.features.types.analysis.Ms1FragmentedSignalsType;
import io.github.mzmine.datamodel.features.types.analysis.Ms1MatchedIntensityPercentType;
import io.github.mzmine.datamodel.features.types.analysis.Ms1MatchedSignalsPercentType;
import io.github.mzmine.datamodel.features.types.analysis.Ms1MatchedSignalsType;
import io.github.mzmine.datamodel.features.types.analysis.Ms1SignalsType;
import io.github.mzmine.datamodel.features.types.analysis.Ms2MatchedIntensityPercentType;
import io.github.mzmine.datamodel.features.types.analysis.Ms2MatchedSignalsPercentType;
import io.github.mzmine.datamodel.features.types.analysis.Ms2MatchedSignalsType;
import io.github.mzmine.datamodel.features.types.analysis.Ms2SignalsType;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public record InSourceFragmentAnalysisResults(boolean isLikelyISF,
                                              double ms1FragmentedLikelyISFPercent, int ms1Count,
                                              int ms1Fragmented, double ms1FragmentedPercent,
                                              double ms1IntensityFragmentedPercent, int ms1Matched,
                                              double ms1MatchedPercent,
                                              double ms1IntensityMatchedPercent, int ms2Count,
                                              int ms2Matched, double ms2MatchedPercent,
                                              double ms2IntensityMatchedPercent) implements
    ModularDataRecord {

  // Constructor without isLikelyISF
  public InSourceFragmentAnalysisResults(double ms1FragmentedLikelyISFPercent, int ms1Count,
      int ms1Fragmented, double ms1FragmentedPercent, double ms1IntensityFragmentedPercent,
      int ms1Matched, double ms1MatchedPercent, double ms1IntensityMatchedPercent, int ms2Count,
      int ms2Matched, double ms2MatchedPercent, double ms2IntensityMatchedPercent) {
    this(false, ms1FragmentedLikelyISFPercent, ms1Count, ms1Fragmented, ms1FragmentedPercent,
        ms1IntensityFragmentedPercent, ms1Matched, ms1MatchedPercent, ms1IntensityMatchedPercent,
        ms2Count, ms2Matched, ms2MatchedPercent, ms2IntensityMatchedPercent);
  }

  @SuppressWarnings("rawtypes")
  public static List<DataType> getSubTypes() {
    return DataTypes.getAll(IsLikelyISFragmentType.class, //
        Ms1FragmentedLikelyISFPercentType.class, //
        Ms1SignalsType.class, //
        Ms1FragmentedSignalsType.class, //
        Ms1FragmentedSignalsPercentType.class, //
        Ms1FragmentedIntensityPercentType.class, //
        Ms1MatchedSignalsType.class, //
        Ms1MatchedSignalsPercentType.class, //
        Ms1MatchedIntensityPercentType.class, //
        //MS2
        Ms2SignalsType.class, //
        Ms2MatchedSignalsType.class, //
        Ms2MatchedSignalsPercentType.class, //
        Ms2MatchedIntensityPercentType.class //
    );
  }

  public static InSourceFragmentAnalysisResults create(
      final @NotNull SimpleModularDataModel values) {
    return new InSourceFragmentAnalysisResults( //
        requireNonNullElse(values.get(IsLikelyISFragmentType.class), false),
        requireNonNullElse(values.get(Ms1FragmentedLikelyISFPercentType.class), -1f),
        requireNonNullElse(values.get(Ms1SignalsType.class), -1),
        requireNonNullElse(values.get(Ms1FragmentedSignalsType.class), -1),
        requireNonNullElse(values.get(Ms1FragmentedIntensityPercentType.class), -1f),
        requireNonNullElse(values.get(Ms1FragmentedSignalsPercentType.class), -1f),
        requireNonNullElse(values.get(Ms1MatchedSignalsType.class), -1),
        requireNonNullElse(values.get(Ms1MatchedSignalsPercentType.class), -1f),
        requireNonNullElse(values.get(Ms1MatchedIntensityPercentType.class), -1f),
        // MS2
        requireNonNullElse(values.get(Ms2SignalsType.class), -1),
        requireNonNullElse(values.get(Ms2MatchedSignalsType.class), -1),
        requireNonNullElse(values.get(Ms2MatchedSignalsPercentType.class), -1f),
        requireNonNullElse(values.get(Ms2MatchedIntensityPercentType.class), -1f));
  }

  // Method to create a new instance with updated isLikelyISF
  public InSourceFragmentAnalysisResults withIsLikelyISF(boolean isLikelyISF) {
    return new InSourceFragmentAnalysisResults(isLikelyISF, this.ms1FragmentedLikelyISFPercent,
        this.ms1Count, this.ms1Fragmented, this.ms1FragmentedPercent,
        this.ms1IntensityFragmentedPercent, this.ms1Matched, this.ms1MatchedPercent,
        this.ms1IntensityMatchedPercent, this.ms2Count, this.ms2Matched, this.ms2MatchedPercent,
        this.ms2IntensityMatchedPercent);
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
      case IsLikelyISFragmentType _ -> isLikelyISF;
      case Ms1FragmentedLikelyISFPercentType _ -> ms1FragmentedLikelyISFPercent;
      case Ms1SignalsType _ -> ms1Count;
      case Ms1FragmentedIntensityPercentType _ -> ms1IntensityFragmentedPercent;
      case Ms1FragmentedSignalsType _ -> ms1Fragmented;
      case Ms1FragmentedSignalsPercentType _ -> ms1FragmentedPercent;
      case Ms1MatchedIntensityPercentType _ -> ms1IntensityMatchedPercent;
      case Ms1MatchedSignalsType _ -> ms1Matched;
      case Ms1MatchedSignalsPercentType _ -> ms1MatchedPercent;
      //MS2
      case Ms2SignalsType _ -> ms2Count;
      case Ms2MatchedIntensityPercentType _ -> ms2IntensityMatchedPercent;
      case Ms2MatchedSignalsType _ -> ms2Matched;
      case Ms2MatchedSignalsPercentType _ -> ms2MatchedPercent;
      default -> throw new IllegalStateException("Unexpected value: " + sub);
    };
  }
}
