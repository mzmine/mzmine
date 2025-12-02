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

package io.github.mzmine.parameters.parametertypes.ionidentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import io.github.mzmine.datamodel.identities.IonLibraries;
import io.github.mzmine.datamodel.identities.IonLibrary;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.UserParameter;
import org.junit.jupiter.api.Test;

class IonLibraryParameterTest {

  final static String expectedXml = """
      <parameter name="Ion library"><ionLibrary name="mzmine default (+/-)"><parts><part charge="0" formula="H2O" id="0" mass="18.010564684" name="H2O"/><part charge="1" formula="H" id="1" mass="1.00727645209073" name="H"/><part charge="-1" formula="" id="2" mass="5.4857990927E-4" name="e"/><part charge="-1" formula="C2H3O2" id="3" mass="59.01385291590927" name="C2H3O2"/><part charge="-1" formula="CHO2" id="4" mass="44.99820285190927" name="CHO2"/><part charge="2" formula="Ca" id="5" mass="39.96149382018146" name="Ca"/><part charge="-1" formula="Cl" id="6" mass="34.96940125990927" name="Cl"/><part charge="2" formula="Fe" id="7" mass="55.933840340181455" name="Fe"/><part charge="3" formula="Fe" id="8" mass="55.93329176027218" name="Fe"/><part charge="1" formula="K" id="9" mass="38.96315810009073" name="K"/><part charge="1" formula="H4N" id="10" mass="18.03382554809073" name="NH4"/><part charge="1" formula="Na" id="11" mass="22.98922070009073" name="Na"/><part charge="-1" formula="[79]Br" id="12" mass="78.91888567990927" name="[79]Br"/></parts><ionTypes><ionType molecules="1"><part count="-2" id="1"/></ionType><ionType molecules="1"><part count="-1" id="1"/></ionType><ionType molecules="1"><part count="1" id="2"/></ionType><ionType molecules="1"><part count="1" id="6"/></ionType><ionType molecules="1"><part count="1" id="4"/></ionType><ionType molecules="1"><part count="1" id="3"/></ionType><ionType molecules="1"><part count="1" id="12"/></ionType><ionType molecules="1"><part count="-4" id="0"/><part count="1" id="1"/></ionType><ionType molecules="1"><part count="-3" id="0"/><part count="1" id="1"/></ionType><ionType molecules="1"><part count="-2" id="0"/><part count="1" id="1"/></ionType><ionType molecules="1"><part count="-1" id="0"/><part count="-1" id="2"/></ionType><ionType molecules="1"><part count="-1" id="0"/><part count="1" id="1"/></ionType><ionType molecules="1"><part count="-1" id="2"/></ionType><ionType molecules="1"><part count="1" id="1"/></ionType><ionType molecules="1"><part count="-1" id="0"/><part count="1" id="11"/></ionType><ionType molecules="1"><part count="1" id="10"/></ionType><ionType molecules="1"><part count="1" id="11"/></ionType><ionType molecules="1"><part count="-1" id="1"/><part count="1" id="5"/></ionType><ionType molecules="1"><part count="1" id="9"/></ionType><ionType molecules="1"><part count="-1" id="1"/><part count="2" id="11"/></ionType><ionType molecules="1"><part count="-2" id="1"/><part count="1" id="8"/></ionType><ionType molecules="1"><part count="-1" id="1"/><part count="1" id="7"/></ionType><ionType molecules="1"><part count="-2" id="2"/></ionType><ionType molecules="1"><part count="2" id="1"/></ionType><ionType molecules="1"><part count="1" id="1"/><part count="1" id="10"/></ionType><ionType molecules="1"><part count="1" id="1"/><part count="1" id="11"/></ionType><ionType molecules="1"><part count="1" id="5"/></ionType><ionType molecules="1"><part count="1" id="1"/><part count="1" id="9"/></ionType><ionType molecules="1"><part count="-1" id="1"/><part count="1" id="8"/></ionType><ionType molecules="1"><part count="1" id="7"/></ionType><ionType molecules="1"><part count="3" id="1"/></ionType><ionType molecules="2"><part count="-1" id="1"/></ionType><ionType molecules="2"><part count="1" id="6"/></ionType><ionType molecules="2"><part count="-1" id="0"/><part count="1" id="1"/></ionType><ionType molecules="2"><part count="1" id="1"/></ionType><ionType molecules="2"><part count="1" id="10"/></ionType><ionType molecules="2"><part count="1" id="11"/></ionType><ionType molecules="3"><part count="1" id="1"/></ionType><ionType molecules="3"><part count="1" id="11"/></ionType></ionTypes></ionLibrary></parameter>""";

  @Test
  void loadValueFromXML() {
    // initialize with wrong ions
    final IonLibrary initialLibrary = IonLibraries.MZMINE_DEFAULT_NEG;
    final IonLibraryParameter param = new IonLibraryParameter(initialLibrary);
    // load xml of dual polarity list, in place operation
    ParameterUtils.loadParameterFromString(param, expectedXml);
    assertNotNull(param.getValue());
    assertNotEquals(initialLibrary, param.getValue());
    assertEquals(IonLibraries.MZMINE_DEFAULT_DUAL_POLARITY, param.getValue());
  }

  @Test
  void saveValueToXML() {
    final IonLibraryParameter param = new IonLibraryParameter(
        IonLibraries.MZMINE_DEFAULT_DUAL_POLARITY);

    // save and load from string
    final String xml = ParameterUtils.saveParameterToXMLString(param);
    final var clone = ParameterUtils.loadParameterFromString(param.cloneParameter(), xml);

    assertEquals(expectedXml, xml);
    assertNotNull(clone);
    assertEquals(param.getValue(), clone.getValue());
    param.setValue(IonLibraries.MZMINE_DEFAULT_NEG);
    assertNotEquals(param.getValue(), clone.getValue());
  }

  @Test
  void cloneParameter() {
    final IonLibraryParameter param = new IonLibraryParameter(
        IonLibraries.MZMINE_DEFAULT_DUAL_POLARITY);
    final UserParameter<IonLibrary, IonLibraryComponent> clone = param.cloneParameter();
    assertNotNull(clone);
    assertNotSame(param, clone);
    assertEquals(param.getValue(), clone.getValue());
  }
}