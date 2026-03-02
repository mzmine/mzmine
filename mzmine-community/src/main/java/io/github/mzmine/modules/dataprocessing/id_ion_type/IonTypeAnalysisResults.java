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

package io.github.mzmine.modules.dataprocessing.id_ion_type;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.features.ModularDataRecord;
import io.github.mzmine.datamodel.features.SimpleModularDataModel;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.analysis.AdductsMzsType;
import io.github.mzmine.datamodel.features.types.analysis.CommonMzsRowMsnType;
import io.github.mzmine.datamodel.features.types.analysis.CommonMzsRowType;
import io.github.mzmine.datamodel.features.types.analysis.CommonMzsScanMsnType;
import io.github.mzmine.datamodel.features.types.analysis.CommonMzsScanType;
import io.github.mzmine.datamodel.features.types.analysis.FragmentedMzsType;
import io.github.mzmine.datamodel.features.types.analysis.IsotopesMzsType;
import io.github.mzmine.datamodel.features.types.analysis.Ms1IntensitiesType;
import io.github.mzmine.datamodel.features.types.analysis.Ms1MzsType;
import io.github.mzmine.datamodel.features.types.analysis.MsnIntensitiesType;
import io.github.mzmine.datamodel.features.types.analysis.MsnMzsRowType;
import io.github.mzmine.datamodel.features.types.analysis.MsnMzsScanType;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public record IonTypeAnalysisResults(//
                                     String ms1Mzs, //
                                     String ms1Intensities, //
                                     String msnMzsRow, //
                                     String msnMzsScan, //
                                     String msnIntensities, //
                                     String commonMzsRow, //
                                     String commonMzsScan, //
                                     String commonMzsRowMsn, //
                                     String commonMzsScanMsn, //
                                     String fragmentedMzs, //
                                     String adductsMzs, //
                                     String isotopesMzs) implements ModularDataRecord {

  @SuppressWarnings("rawtypes")
  public static List<DataType> getSubTypes() {
    return DataTypes.getAll( //
        Ms1MzsType.class, //
        Ms1IntensitiesType.class, //
        MsnMzsRowType.class, //
        MsnMzsScanType.class, //
        MsnIntensitiesType.class, //
        CommonMzsRowType.class, //
        CommonMzsScanType.class, //
        CommonMzsRowMsnType.class, //
        CommonMzsScanMsnType.class, //
        FragmentedMzsType.class, //
        AdductsMzsType.class, //
        IsotopesMzsType.class //
    );
  }

  public static IonTypeAnalysisResults create(final @NotNull SimpleModularDataModel values) {
    return new IonTypeAnalysisResults( //
        requireNonNullElse(values.get(Ms1MzsType.class), ""), //
        requireNonNullElse(values.get(Ms1IntensitiesType.class), ""), //
        requireNonNullElse(values.get(MsnMzsRowType.class), ""), //
        requireNonNullElse(values.get(MsnMzsScanType.class), ""), //
        requireNonNullElse(values.get(MsnIntensitiesType.class), ""), //
        requireNonNullElse(values.get(CommonMzsRowType.class), ""), //
        requireNonNullElse(values.get(CommonMzsScanType.class), ""), //
        requireNonNullElse(values.get(CommonMzsRowMsnType.class), ""), //
        requireNonNullElse(values.get(CommonMzsScanMsnType.class), ""), //
        requireNonNullElse(values.get(FragmentedMzsType.class), ""), //
        requireNonNullElse(values.get(AdductsMzsType.class), ""), //
        requireNonNullElse(values.get(IsotopesMzsType.class), "") //
    );
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
      case Ms1MzsType _ -> ms1Mzs;
      case Ms1IntensitiesType _ -> ms1Intensities;
      case MsnMzsRowType _ -> msnMzsRow;
      case MsnMzsScanType _ -> msnMzsScan;
      case MsnIntensitiesType _ -> msnIntensities;
      case CommonMzsRowType _ -> commonMzsRow;
      case CommonMzsScanType _ -> commonMzsScan;
      case CommonMzsRowMsnType _ -> commonMzsRowMsn;
      case CommonMzsScanMsnType _ -> commonMzsScanMsn;
      case FragmentedMzsType _ -> fragmentedMzs;
      case AdductsMzsType _ -> adductsMzs;
      case IsotopesMzsType _ -> isotopesMzs;

      default -> throw new IllegalStateException("Unexpected value: " + sub);
    };
  }
}
