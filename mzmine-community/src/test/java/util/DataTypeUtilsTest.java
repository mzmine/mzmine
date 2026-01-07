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

package util;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.datamodel.features.types.annotations.MissingValueType;
import io.github.mzmine.datamodel.features.types.annotations.SpectralLibraryMatchesType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.util.DataTypeUtils;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DataTypeUtilsTest {

  private static final Logger logger = Logger.getLogger(DataTypeUtilsTest.class.getName());

  @Test
  void testGrouping() {
//    final RawDataFileImpl file = new RawDataFileImpl("file", null, null);
//    final ModularFeatureList flist = new ModularFeatureList("flist", null, file);

    final FeatureListRow lipidAnnotated = Mockito.mock(FeatureListRow.class, "lipid");
    setupMock(lipidAnnotated, false, true);
    final FeatureListRow notAnnotated = Mockito.mock(FeatureListRow.class, "notAnnotated");
    setupMock(notAnnotated, false, false);
    final FeatureListRow spectralAnnotated = Mockito.mock(FeatureListRow.class, "spectral ");
    setupMock(spectralAnnotated, true, false);
    final FeatureListRow spectralLipidAnnotated = Mockito.mock(FeatureListRow.class,
        "spectral and lipid");
    setupMock(spectralLipidAnnotated, true, true);

    final List<FeatureListRow> rows = List.of(lipidAnnotated, notAnnotated, spectralAnnotated,
        spectralLipidAnnotated);
    final List<DataType> orderOne = DataTypes.getAll(LipidMatchListType.class,
        SpectralLibraryMatchesType.class);
    final Map<DataType<?>, List<FeatureListRow>> oderOneResult = DataTypeUtils.groupByBestDataType(
        rows, true, orderOne.toArray(DataType[]::new));
    Assertions.assertEquals(2, oderOneResult.get(new LipidMatchListType()).size());
    Assertions.assertEquals(1, oderOneResult.get(new SpectralLibraryMatchesType()).size());
    Assertions.assertEquals(1, oderOneResult.get(new MissingValueType()).size());

    final List<DataType> orderTwo = DataTypes.getAll(SpectralLibraryMatchesType.class,
        LipidMatchListType.class);
    final Map<DataType<?>, List<FeatureListRow>> oderTwoResult = DataTypeUtils.groupByBestDataType(
        rows, false, orderTwo.toArray(DataType[]::new));
    Assertions.assertEquals(1, oderTwoResult.get(new LipidMatchListType()).size());
    Assertions.assertEquals(2, oderTwoResult.get(new SpectralLibraryMatchesType()).size());
    Assertions.assertNull(oderTwoResult.get(new MissingValueType()));
  }

  void setupMock(FeatureListRow row, boolean mockSpectral, boolean mockLipid) {
    final List<SpectralDBAnnotation> spectralMatch =
        mockSpectral ? List.of(Mockito.mock(SpectralDBAnnotation.class)) : null;
    final List<MatchedLipid> lipidMatch =
        mockLipid ? List.of(Mockito.mock(MatchedLipid.class)) : null;

    Mockito.when(row.get(LipidMatchListType.class)).thenReturn(lipidMatch);
    Mockito.when(row.get(DataTypes.get(LipidMatchListType.class))).thenReturn(lipidMatch);
    Mockito.when(row.get(SpectralLibraryMatchesType.class)).thenReturn(spectralMatch);
    Mockito.when(row.get(DataTypes.get(SpectralLibraryMatchesType.class)))
        .thenReturn(spectralMatch);
  }
}
