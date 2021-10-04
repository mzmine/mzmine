package datamodel;

import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.ModularTypeProperty;
import io.github.mzmine.datamodel.features.types.annotations.IdentityType;
import io.github.mzmine.datamodel.features.types.annotations.ManualAnnotationType;
import io.github.mzmine.datamodel.impl.SimpleFeatureIdentity;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import javafx.beans.property.ListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ModularTypesTest {

  @Test
  void manualAnnotationTypeTest() {

    RawDataFile file = null;
    try {
      file = new RawDataFileImpl("testfile", null, null, Color.BLACK);
    } catch (IOException e) {
      e.printStackTrace();
      Assertions.fail("Cannot initialise data file.");
    }
    Assertions.assertNotNull(file);

    // test load/save for row
    final ModularFeatureList flist = new ModularFeatureList("flist", null, file);
    final ModularFeatureListRow row = new ModularFeatureListRow(flist, 1);

    FeatureIdentity id1 = new SimpleFeatureIdentity("name1", "form1", "method1", "id1", "url1");
    FeatureIdentity id2 = new SimpleFeatureIdentity("name2", "form2", "method2", "id2", "url2");

    ManualAnnotationType type = new ManualAnnotationType();
    ObservableList<FeatureIdentity> list = FXCollections.observableList(List.of(id1, id2));
    ModularTypeProperty value = type.createProperty();
    value.set(IdentityType.class, list);
    final ModularTypeProperty loaded = (ModularTypeProperty) DataTypeTestUtils
        .saveAndLoad(type, value, flist, row, null, null);

    ListProperty<FeatureIdentity> featureIdentities = loaded.get(new IdentityType());
    Assertions.assertEquals(list.size(), featureIdentities.size());

    FeatureIdentity loaded1 = featureIdentities.get().get(0);
    FeatureIdentity loaded2 = featureIdentities.get().get(1);

    Assertions.assertEquals(id1.getAllProperties().size(), loaded1.getAllProperties().size());
    for (Entry<String, String> entry : id1.getAllProperties().entrySet()) {
      Assertions.assertEquals(entry.getValue(), loaded1.getPropertyValue(entry.getKey()));
    }

    Assertions.assertEquals(id2.getAllProperties().size(), loaded2.getAllProperties().size());
    for (Entry<String, String> entry : id2.getAllProperties().entrySet()) {
      Assertions.assertEquals(entry.getValue(), loaded2.getPropertyValue(entry.getKey()));
    }
  }

}
