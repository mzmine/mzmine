/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package datamodel;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.features.types.DetectionType;
import io.github.mzmine.datamodel.features.types.FeatureInformationType;
import io.github.mzmine.datamodel.features.types.IsotopePatternType;
import io.github.mzmine.datamodel.features.types.MobilityUnitType;
import io.github.mzmine.datamodel.impl.MultiChargeStateIsotopePattern;
import io.github.mzmine.datamodel.impl.SimpleFeatureInformation;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class GeneralTypeTests {

  @Test
  void detectionTypeTest() {
    DetectionType type = new DetectionType();
    var value = FeatureStatus.DETECTED;
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }

  @Test
  @DisplayName("SimpleIsotopePattern save load")
  void simpleIsotopePatternTypeTest() {
    IsotopePatternType type = new IsotopePatternType();
    IsotopePattern pattern = new SimpleIsotopePattern(new double[]{200d, 201d, 202d},
        new double[]{1.0, 0.5, 0.11}, 1, IsotopePatternStatus.DETECTED, "Save load test");

    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, pattern);
  }

  @Test
  @DisplayName("MultiChargeStateIsotopePattern save load")
  void multiChargeStateIsotopePatternTypeTest() {
    IsotopePatternType type = new IsotopePatternType();

    IsotopePattern pattern = new MultiChargeStateIsotopePattern(List.of(
        new SimpleIsotopePattern(new double[]{200d, 201d, 202d}, new double[]{1.0, 0.5, 0.11}, 1,
            IsotopePatternStatus.DETECTED, "Save load test1"),
        new SimpleIsotopePattern(new double[]{100d, 100.5, 101d}, new double[]{1.0, 0.5, 0.11}, 2,
            IsotopePatternStatus.DETECTED, "Save load test2")));

    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, pattern);
  }

  // todo FeatureGroupType

  @Test
  void featureInformationTypeTest() {
    FeatureInformationType type = new FeatureInformationType();
    SimpleFeatureInformation info = new SimpleFeatureInformation();
    info.addProperty("bla", "blub");
    info.addProperty("ß012eisd", "ß0widqscn/+9");
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, info);
  }

  /**
   *ImsMsMsInfoType test in {@link IMSScanTypesTest}
   */

  /**
   *
   */
  @Test
  void mobilityUnitTypeTest() {
    MobilityUnitType type = new MobilityUnitType();
    var value = MobilityType.TIMS;
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
  }
}
