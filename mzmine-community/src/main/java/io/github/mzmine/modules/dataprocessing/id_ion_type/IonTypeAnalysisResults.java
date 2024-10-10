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

package io.github.mzmine.modules.dataprocessing.id_ion_type;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.features.ModularDataRecord;
import io.github.mzmine.datamodel.features.SimpleModularDataModel;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.analysis.AICType;
import io.github.mzmine.datamodel.features.types.analysis.AIIType;
import io.github.mzmine.datamodel.features.types.analysis.AdductsOnlyType;
import io.github.mzmine.datamodel.features.types.analysis.CommonOnlyType;
import io.github.mzmine.datamodel.features.types.analysis.CommonSignalsType;
import io.github.mzmine.datamodel.features.types.analysis.ExplainedIType;
import io.github.mzmine.datamodel.features.types.analysis.IICType;
import io.github.mzmine.datamodel.features.types.analysis.IsotopesOnlyType;
import io.github.mzmine.datamodel.features.types.analysis.Ms1AdductsAndCoType;
import io.github.mzmine.datamodel.features.types.analysis.Ms1CommonIntensityPercentType;
import io.github.mzmine.datamodel.features.types.analysis.Ms1IsotopesType;
import io.github.mzmine.datamodel.features.types.analysis.Ms1SignalsType;
import io.github.mzmine.datamodel.features.types.analysis.Ms1UnexplainedIntensityPercentType;
import io.github.mzmine.datamodel.features.types.analysis.Ms1UnexplainedType;
import io.github.mzmine.datamodel.features.types.analysis.Ms2CommonIntensityPercentType;
import io.github.mzmine.datamodel.features.types.analysis.Ms2SignalsAllPrecursorsType;
import io.github.mzmine.datamodel.features.types.analysis.PrecursorIonsIntensityPercentType;
import io.github.mzmine.datamodel.features.types.analysis.PrecursorIonsType;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public record IonTypeAnalysisResults(int commonSignalsAllPrecursors, int ms1Signals,
                                     int ms2SignalsAllPrecursors, int ms1AdductsAndCo,
                                     int ms1Isotopes, int ms1Fragmented, int ms1Unexplained,
                                     double ms1FragmentedIntensityPercent,
                                     double ms1CommonIntensityPercent,
                                     double ms2CommonIntensityPercent,
                                     double ms1UnexplainedIntensityPercent, int adductsOnly,
                                     int isotopesOnly, int commonOnly, int aIC, int aII, int iIC,
                                     int explainedI) implements ModularDataRecord {

  @SuppressWarnings("rawtypes")
  public static List<DataType> getSubTypes() {
    return DataTypes.getAll( //
        CommonSignalsType.class, //
        Ms1SignalsType.class, //
        Ms2SignalsAllPrecursorsType.class, //
        Ms1AdductsAndCoType.class, //
        Ms1IsotopesType.class, //
        PrecursorIonsType.class, //
        Ms1UnexplainedType.class, //
        Ms1CommonIntensityPercentType.class, //
        Ms2CommonIntensityPercentType.class, //
        PrecursorIonsIntensityPercentType.class, //
        Ms1UnexplainedIntensityPercentType.class, //
        AdductsOnlyType.class, //
        IsotopesOnlyType.class, //
        CommonOnlyType.class, //
        AIIType.class, //
        AICType.class, //
        IICType.class, //
        ExplainedIType.class //
    );
  }

  public static IonTypeAnalysisResults create(final @NotNull SimpleModularDataModel values) {
    return new IonTypeAnalysisResults( //
        requireNonNullElse(values.get(CommonSignalsType.class), -1),
        requireNonNullElse(values.get(Ms1SignalsType.class), -1),
        requireNonNullElse(values.get(Ms2SignalsAllPrecursorsType.class), -1),
        requireNonNullElse(values.get(Ms1AdductsAndCoType.class), -1),
        requireNonNullElse(values.get(Ms1IsotopesType.class), -1),
        requireNonNullElse(values.get(PrecursorIonsType.class), -1),
        requireNonNullElse(values.get(Ms1UnexplainedType.class), -1),
        requireNonNullElse(values.get(PrecursorIonsIntensityPercentType.class), -1f),
        requireNonNullElse(values.get(Ms1CommonIntensityPercentType.class), -1f),
        requireNonNullElse(values.get(Ms2CommonIntensityPercentType.class), -1f),
        requireNonNullElse(values.get(Ms1UnexplainedIntensityPercentType.class), -1f),
        requireNonNullElse(values.get(AdductsOnlyType.class), -1),
        requireNonNullElse(values.get(IsotopesOnlyType.class), -1),
        requireNonNullElse(values.get(CommonOnlyType.class), -1),
        requireNonNullElse(values.get(AIIType.class), -1),
        requireNonNullElse(values.get(AICType.class), -1),
        requireNonNullElse(values.get(IICType.class), -1),
        requireNonNullElse(values.get(ExplainedIType.class), -1));
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
      case CommonSignalsType _ -> commonSignalsAllPrecursors;
      case Ms1SignalsType _ -> ms1Signals;
      case Ms2SignalsAllPrecursorsType _ -> ms2SignalsAllPrecursors;
      case Ms1AdductsAndCoType _ -> ms1AdductsAndCo;
      case Ms1IsotopesType _ -> ms1Isotopes;
      case PrecursorIonsType _ -> ms1Fragmented;
      case Ms1UnexplainedType _ -> ms1Unexplained;
      case PrecursorIonsIntensityPercentType _ -> ms1FragmentedIntensityPercent;
      case Ms1CommonIntensityPercentType _ -> ms1CommonIntensityPercent;
      case Ms2CommonIntensityPercentType _ -> ms2CommonIntensityPercent;
      case Ms1UnexplainedIntensityPercentType _ -> ms1UnexplainedIntensityPercent;
      case AdductsOnlyType _ -> adductsOnly;
      case IsotopesOnlyType _ -> isotopesOnly;
      case CommonOnlyType _ -> commonOnly;
      case AIIType _ -> aII;
      case AICType _ -> aIC;
      case IICType _ -> iIC;
      case ExplainedIType _ -> explainedI;

      default -> throw new IllegalStateException("Unexpected value: " + sub);
    };
  }
}
