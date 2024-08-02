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
import io.github.mzmine.datamodel.features.types.analysis.Ms1SharedIntensityPercentType;
import io.github.mzmine.datamodel.features.types.analysis.Ms1SharedSignalsPercentType;
import io.github.mzmine.datamodel.features.types.analysis.Ms1SignalsType;
import io.github.mzmine.datamodel.features.types.analysis.Ms2SharedIntensityPercentType;
import io.github.mzmine.datamodel.features.types.analysis.Ms2SharedSignalsPercentType;
import io.github.mzmine.datamodel.features.types.analysis.Ms2SignalsAllPrecursorsType;
import io.github.mzmine.datamodel.features.types.analysis.Ms2SignalsType;
import io.github.mzmine.datamodel.features.types.analysis.PrecursorIonsCountType;
import io.github.mzmine.datamodel.features.types.analysis.PrecursorIonsIntensityPercentType;
import io.github.mzmine.datamodel.features.types.analysis.PrecursorIonsLikelyISFragmentInMs1PercentType;
import io.github.mzmine.datamodel.features.types.analysis.PrecursorIonsPercentType;
import io.github.mzmine.datamodel.features.types.analysis.SharedSignalsType;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public record InSourceFragmentAnalysisResults(boolean isLikelyISF,
                                              double ms1FragmentedLikelyISFPercent,
                                              int sharedSignals, int ms1Signals,
                                              int ms2SignalsAllPrecursors, int ms1Fragmented,
                                              double ms1FragmentedPercent,
                                              double ms1IntensityFragmentedPercent,
                                              double ms1SharedPercent,
                                              double ms1IntensityMatchedPercent, int ms2Signals,
                                              double ms2SharedPercent,
                                              double ms2IntensitySharedPercent) implements
    ModularDataRecord {

  // Constructor without isLikelyISF
//  public InSourceFragmentAnalysisResults(double ms1FragmentedLikelyISFPercent, int matchedSignals,
//      int ms1Count, int ms1Fragmented, double ms1FragmentedPercent,
//      double ms1IntensityFragmentedPercent, double ms1MatchedPercent,
//      double ms1IntensityMatchedPercent, int ms2Count, double ms2MatchedPercent,
//      double ms2IntensityMatchedPercent) {
//    this(false, matchedSignals, ms1FragmentedLikelyISFPercent, ms1Count, , ms1Fragmented,
//        ms1FragmentedPercent, ms1IntensityFragmentedPercent, ms1MatchedPercent,
//        ms1IntensityMatchedPercent, ms2Count, ms2MatchedPercent, ms2IntensityMatchedPercent);
//  }

  @SuppressWarnings("rawtypes")
  public static List<DataType> getSubTypes() {
    return DataTypes.getAll(IsLikelyISFragmentType.class, //
        PrecursorIonsLikelyISFragmentInMs1PercentType.class, //
        SharedSignalsType.class, //
        Ms1SignalsType.class, //
        Ms2SignalsAllPrecursorsType.class, //
        Ms2SignalsType.class, //
        Ms1SharedSignalsPercentType.class, //
        Ms2SharedSignalsPercentType.class, //
        Ms1SharedIntensityPercentType.class, //
        Ms2SharedIntensityPercentType.class, //
        PrecursorIonsCountType.class, //
        PrecursorIonsPercentType.class, //
        PrecursorIonsIntensityPercentType.class //
    );
  }

  public static InSourceFragmentAnalysisResults create(
      final @NotNull SimpleModularDataModel values) {
    return new InSourceFragmentAnalysisResults( //
        requireNonNullElse(values.get(IsLikelyISFragmentType.class), false),
        requireNonNullElse(values.get(PrecursorIonsLikelyISFragmentInMs1PercentType.class), -1f),
        requireNonNullElse(values.get(SharedSignalsType.class), -1),
        requireNonNullElse(values.get(Ms1SignalsType.class), -1),
        requireNonNullElse(values.get(Ms2SignalsAllPrecursorsType.class), -1),
        requireNonNullElse(values.get(PrecursorIonsCountType.class), -1),
        requireNonNullElse(values.get(PrecursorIonsIntensityPercentType.class), -1f),
        requireNonNullElse(values.get(PrecursorIonsPercentType.class), -1f),
        requireNonNullElse(values.get(Ms1SharedSignalsPercentType.class), -1f),
        // MS2
        requireNonNullElse(values.get(Ms1SharedIntensityPercentType.class), -1f),
        requireNonNullElse(values.get(Ms2SignalsType.class), -1),
        requireNonNullElse(values.get(Ms2SharedSignalsPercentType.class), -1f),
        requireNonNullElse(values.get(Ms2SharedIntensityPercentType.class), -1f));
  }

  // Method to create a new instance with updated isLikelyISF
  public InSourceFragmentAnalysisResults withIsLikelyISF(boolean isLikelyISF) {
    return new InSourceFragmentAnalysisResults(isLikelyISF, this.ms1FragmentedLikelyISFPercent,
        this.sharedSignals, this.ms1Signals, this.ms2SignalsAllPrecursors, this.ms1Fragmented,
        this.ms1FragmentedPercent, this.ms1IntensityFragmentedPercent, this.ms1SharedPercent,
        this.ms1IntensityMatchedPercent, this.ms2Signals, this.ms2SharedPercent,
        this.ms2IntensitySharedPercent);
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
      case SharedSignalsType _ -> sharedSignals;
      case PrecursorIonsLikelyISFragmentInMs1PercentType _ -> ms1FragmentedLikelyISFPercent;
      case Ms1SignalsType _ -> ms1Signals;
      case Ms2SignalsAllPrecursorsType _ -> ms2SignalsAllPrecursors;
      case PrecursorIonsIntensityPercentType _ -> ms1IntensityFragmentedPercent;
      case PrecursorIonsCountType _ -> ms1Fragmented;
      case PrecursorIonsPercentType _ -> ms1FragmentedPercent;
      case Ms1SharedIntensityPercentType _ -> ms1IntensityMatchedPercent;
      case Ms1SharedSignalsPercentType _ -> ms1SharedPercent;
      //MS2
      case Ms2SignalsType _ -> ms2Signals;
      case Ms2SharedIntensityPercentType _ -> ms2IntensitySharedPercent;
      case Ms2SharedSignalsPercentType _ -> ms2SharedPercent;
      default -> throw new IllegalStateException("Unexpected value: " + sub);
    };
  }
}
