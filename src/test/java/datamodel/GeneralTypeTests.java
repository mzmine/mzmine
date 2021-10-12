package datamodel;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.features.types.DetectionType;
import io.github.mzmine.datamodel.features.types.FeatureInformationType;
import io.github.mzmine.datamodel.features.types.MobilityUnitType;
import io.github.mzmine.datamodel.impl.SimpleFeatureInformation;
import org.junit.jupiter.api.Test;

public class GeneralTypeTests {

  @Test
  void detectionTypeTest() {
    DetectionType type = new DetectionType();
    var value = FeatureStatus.DETECTED;
    DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value);
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
