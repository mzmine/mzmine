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

package io.github.mzmine.modules.tools.batchwizard.subparameters;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import java.text.NumberFormat;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ParameterOverrideTest {

  @Test
  void explicitValuesNeverModifyOrShareThePrototype() {
    final DoubleParameter prototype = new DoubleParameter("Intensity", "Minimum intensity",
        NumberFormat.getNumberInstance(), 100d);
    final ParameterOverride first = new ParameterOverride("module", "Module", prototype, 200d,
        ApplicationScope.FIRST);
    final ParameterOverride second = new ParameterOverride("module", "Module", prototype, 300d,
        ApplicationScope.LAST);

    Assertions.assertEquals(100d, prototype.getValue());
    Assertions.assertNotSame(prototype, first.parameterWithValue());
    Assertions.assertNotSame(first.parameterWithValue(), second.parameterWithValue());
    prototype.setValue(400d);
    Assertions.assertEquals(200d, first.parameterWithValue().getValue());
    Assertions.assertEquals(300d, second.parameterWithValue().getValue());
  }

  @Test
  void nullableValuesAreAssignedOnlyToTheClone() {
    final DoubleParameter prototype = new DoubleParameter("Intensity", "Minimum intensity",
        NumberFormat.getNumberInstance(), 100d);
    final ParameterOverride override = new ParameterOverride("module", "Module", prototype, null,
        ApplicationScope.ALL);

    Assertions.assertNull(override.parameterWithValue().getValue());
    Assertions.assertEquals(100d, prototype.getValue());
  }

  @Test
  void loadedEmbeddedSettingsSurviveSnapshotAndScopeChange() throws Exception {
    final OptionalParameter<DoubleParameter> edited = new OptionalParameter<>(
        new DoubleParameter("Intensity", "Minimum intensity", NumberFormat.getNumberInstance(),
            250d), true);
    final var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    final var element = document.createElement("override");
    edited.saveValueToXML(element);
    final OptionalParameter<DoubleParameter> loaded = new OptionalParameter<>(
        new DoubleParameter("Intensity", "Minimum intensity"));
    loaded.loadValueFromXML(element);
    final Parameter<?> wildcard = loaded;
    final ParameterOverride override = ParameterOverride.fromParameter("module", "Module", wildcard,
        ApplicationScope.FIRST);
    final ParameterOverride changedScope = ParameterOverride.fromParameter("module", "Module",
        override.parameterWithValue(), ApplicationScope.LAST);
    loaded.getEmbeddedParameter().setValue(500d);
    loaded.setValue(false);

    final OptionalParameter<?> snapshot = Assertions.assertInstanceOf(OptionalParameter.class,
        changedScope.parameterWithValue());
    Assertions.assertTrue(snapshot.getValue());
    Assertions.assertEquals(250d, snapshot.getEmbeddedParameter().getValue());
    Assertions.assertNotSame(override.parameterWithValue(), changedScope.parameterWithValue());
    Assertions.assertEquals(ApplicationScope.LAST, changedScope.scope());
  }
}
