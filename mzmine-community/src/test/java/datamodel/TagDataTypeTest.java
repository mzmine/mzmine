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

package datamodel;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.TagDataType;
import java.util.BitSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TagDataTypeTest {

  @Test
  void testXmlRoundTrip() {
    final BitSet tags = new BitSet();
    tags.set(0);
    tags.set(2);
    tags.set(19);

    DataTypeTestUtils.simpleDataTypeSaveLoadTest(new TagDataType(), tags);
  }

  @Test
  void testNewFeatureListsContainTagsColumn() {
    final ModularFeatureList featureList = new ModularFeatureList("test", null);

    Assertions.assertTrue(featureList.getRowTypes().contains(new TagDataType()));
  }

  @Test
  void testFormattedStringUsesOneBasedTagNumbers() {
    final BitSet tags = new BitSet();
    tags.set(0);
    tags.set(5);

    Assertions.assertEquals("1, 6", new TagDataType().getFormattedExportString(tags));
  }
}
