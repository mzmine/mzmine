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

import io.github.mzmine.datamodel.MZmineProject;
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
    public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull MZmineProject project,
        @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
        @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
      while (reader.hasNext()) {
        reader.next();
      }
      return null;
    }
  }
}
