/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.EmbeddedParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.MultiChoiceParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataListGroupsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import java.text.DecimalFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class StandardParameterTest {

  static List<ParameterTestCase> defaultCases() {
    final StringParameter stringParam = new StringParameter("name", "", "myvalue");
    final ComboParameter<PolarityType> comboParam = new ComboParameter<>("test", "",
        PolarityType.values(), PolarityType.NEGATIVE);

    List<ParameterTestCase> tests = List.of( //
        // simple parameters
        new ParameterTestCase(stringParam, "other value"), //
        new ParameterTestCase(new DoubleParameter("t", "", new DecimalFormat("0.0000"), 5d), 1d), //
        new ParameterTestCase(new IntegerParameter("t", "", 2), 1), //
        new ParameterTestCase(new BooleanParameter("test", "", true), false), //
        new ParameterTestCase(new MultiChoiceParameter<>("t", "", PolarityType.values(),
            new PolarityType[]{PolarityType.POSITIVE, PolarityType.NEGATIVE}),
            new PolarityType[]{PolarityType.NEGATIVE, PolarityType.POSITIVE}), //
        new ParameterTestCase(comboParam, PolarityType.POSITIVE), //
        // optional parameters
        new ParameterTestCase(new OptionalParameter<>(comboParam, true), false,
            PolarityType.POSITIVE), //
        new ParameterTestCase(new OptionalParameter<>(stringParam, true), false,
            "otherEmbeddedValue") //
    );
    return tests;
  }

  @Test
  void testRawDataFileSelection() {
    final RawDataFilesSelection byMetadata = new RawDataFilesSelection(
        RawDataFilesSelectionType.BY_METADATA);
    byMetadata.setNamePattern("metadata files no pattern");

    final RawDataFilesSelection selection = new RawDataFilesSelection(
        RawDataFilesSelectionType.SPECIFIC_FILES);
    selection.setSpecificFiles(
        new RawDataFile[]{new RawDataFilePlaceholder("Test name.raw", "some path/somefile")});
    testParameter(new ParameterTestCase<>(new RawDataFilesParameter(selection), byMetadata));

    selection.setMetadataSelection(
        new MetadataListGroupsSelection("Column", List.of("included1", "included2")),
        new MetadataListGroupsSelection("exc column", List.of()));
    selection.setNamePattern("*pattern");

    testParameter(new ParameterTestCase<>(new RawDataFilesParameter(selection), byMetadata));

    selection.setSelectionType(RawDataFilesSelectionType.NAME_PATTERN);
    // only save when specific selected
    selection.setSpecificFiles(new RawDataFilePlaceholder[0]);

    testParameter(new ParameterTestCase<>(new RawDataFilesParameter(selection), byMetadata));
  }

  void testParameter(ParameterTestCase<?> test) {
    saveLoadParameter(test);
    cloneParameter(test);
  }

  @ParameterizedTest
  @MethodSource("defaultCases")
  void saveLoadParameter(ParameterTestCase test) {
    final Parameter param = test.param();
    final String xml = ParameterUtils.saveParameterToXMLString(param);

    // use a changed clone to see that values are actually loaded
    final Parameter clone = test.cloneParamPermutated();
    ParameterUtils.loadParameterFromString(clone, xml);

    // check value and embedded
    assertNotSame(param, clone);
    assertValueEqual(param, clone, true);
    assertEmbeddedEqual(param, clone, true);
  }

  @ParameterizedTest
  @MethodSource("defaultCases")
  void cloneParameter(ParameterTestCase test) {
    final Parameter param = test.param();
    final Parameter clone = param.cloneParameter();
    assertNotSame(param, clone);
    assertValueEqual(param, clone, true);
    assertEquals(param.getName(), clone.getName());
    if (param instanceof UserParameter up) {
      assertEquals(up.getDescription(), ((UserParameter) clone).getDescription());
    }

    // handle optional and other embedded
    if (param instanceof EmbeddedParameter parent) {
      final UserParameter embedded = parent.getEmbeddedParameter();
      final UserParameter embeddedClone = ((EmbeddedParameter<?, ?, ?>) clone).getEmbeddedParameter();

      // should not be the same instance
      assertNotSame(embedded, embeddedClone);
      assertValueEqual(embedded, embeddedClone, true);
      // change value and see that the clone is independent
      embeddedClone.setValue(test.embeddedDifferentValue());
      assertValueEqual(embedded, embeddedClone, false);
    }

    // change value and see that the clone is independent
    clone.setValue(test.differentValue());
    assertEquals(test.differentValue(), clone.getValue());
    assertValueEqual(param, clone, false);
  }


  void assertValueEqual(Parameter param, Parameter clone, boolean equal) {
    assertEquals(equal, param.valueEquals(clone),
        "Parameter %s: Value equals should be %s but is %s".formatted(param.getName(), equal,
            !equal));
  }

  void assertEmbeddedEqual(Parameter param, Parameter clone, boolean equal) {
    if (param instanceof EmbeddedParameter parent) {
      final UserParameter embedded = parent.getEmbeddedParameter();
      final UserParameter embeddedClone = ((EmbeddedParameter<?, ?, ?>) clone).getEmbeddedParameter();

      // should not be the same instance
      assertNotSame(embedded, embeddedClone);
      assertValueEqual(embedded, embeddedClone, equal);
    }
  }

}