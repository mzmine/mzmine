package datamodel;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.abstr.StringType;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

/**
 * Intentionally creates a data type that reads for too long. Used to test if a change to {@link
 * DataTypeTestUtils} is consistent.
 *
 * @author https://github.com/SteffenHeu
 */
public class IntentionalOvershootTypeTest {

  @Test
  void overshootTypeTest() {
    OvershootType type = new OvershootType();
    var value = "hi";
    Assertions.assertThrows(AssertionFailedError.class,
        () -> DataTypeTestUtils.simpleDataTypeSaveLoadTest(type, value));
  }

  public static class OvershootType extends StringType {

    public OvershootType() {
      super();
    }

    @Override
    public @NotNull String getUniqueID() {
      return "overshoot_type";
    }

    @Override
    public @NotNull String getHeaderString() {
      return "overshoot";
    }

    @Override
    public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull ModularFeatureList flist,
        @NotNull ModularFeatureListRow row, @Nullable ModularFeature feature,
        @Nullable RawDataFile file) throws XMLStreamException {
      while (reader.hasNext()) {
        reader.next();
      }
      return null;
    }
  }
}
