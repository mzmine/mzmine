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

package io.github.mzmine.parameters.parametertypes.ionidentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import io.github.mzmine.datamodel.identities.iontype.IonLibraries;
import io.github.mzmine.datamodel.identities.iontype.IonLibrary;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.IonTypes;
import io.github.mzmine.datamodel.identities.iontype.SimpleIonLibrary;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.UserParameter;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class IonLibraryParameterTest {

  final static String expectedXml = """
      <parameter libraryVersion="2" name="Ion library"><ionLibrary name="mzmine default comprehensive (+/-)" savedDate="2026-01-13T11:37:38.855390200"><parts><part charge="0" formula="H2O" id="0" mass="18.010564684" name="H2O"/><part charge="1" formula="H" id="1" mass="1.00727645209073" name="H"/><part charge="-1" formula="" id="2" mass="5.4857990927E-4" name="e"/><part charge="-1" formula="C2H3O2" id="3" mass="59.01385291590927" name="C2H3O2"/><part charge="-1" formula="CHO2" id="4" mass="44.99820285190927" name="CHO2"/><part charge="2" formula="Ca" id="5" mass="39.96149382018146" name="Ca"/><part charge="-1" formula="Cl" id="6" mass="34.96940125990927" name="Cl"/><part charge="2" formula="Fe" id="7" mass="55.933840340181455" name="Fe"/><part charge="3" formula="Fe" id="8" mass="55.93329176027218" name="Fe"/><part charge="1" formula="K" id="9" mass="38.96315810009073" name="K"/><part charge="1" formula="H4N" id="10" mass="18.03382554809073" name="NH4"/><part charge="1" formula="Na" id="11" mass="22.98922070009073" name="Na"/><part charge="-1" formula="[79]Br" id="12" mass="78.91888567990927" name="[79]Br"/></parts><ionTypes><ionType molecules="1"><part count="-2" id="1"/></ionType><ionType molecules="1"><part count="-1" id="1"/></ionType><ionType molecules="1"><part count="1" id="2"/></ionType><ionType molecules="1"><part count="1" id="6"/></ionType><ionType molecules="1"><part count="1" id="4"/></ionType><ionType molecules="1"><part count="1" id="3"/></ionType><ionType molecules="1"><part count="1" id="12"/></ionType><ionType molecules="1"><part count="-4" id="0"/><part count="1" id="1"/></ionType><ionType molecules="1"><part count="-3" id="0"/><part count="1" id="1"/></ionType><ionType molecules="1"><part count="-1" id="0"/><part count="-1" id="2"/></ionType><ionType molecules="1"><part count="-1" id="0"/><part count="1" id="1"/></ionType><ionType molecules="1"><part count="-1" id="2"/></ionType><ionType molecules="1"><part count="1" id="1"/></ionType><ionType molecules="1"><part count="-1" id="0"/><part count="1" id="11"/></ionType><ionType molecules="1"><part count="1" id="10"/></ionType><ionType molecules="1"><part count="1" id="11"/></ionType><ionType molecules="1"><part count="-1" id="1"/><part count="1" id="5"/></ionType><ionType molecules="1"><part count="1" id="9"/></ionType><ionType molecules="1"><part count="-1" id="1"/><part count="2" id="11"/></ionType><ionType molecules="1"><part count="-2" id="1"/><part count="1" id="8"/></ionType><ionType molecules="1"><part count="-1" id="1"/><part count="1" id="7"/></ionType><ionType molecules="1"><part count="-2" id="2"/></ionType><ionType molecules="1"><part count="2" id="1"/></ionType><ionType molecules="1"><part count="1" id="1"/><part count="1" id="10"/></ionType><ionType molecules="1"><part count="1" id="1"/><part count="1" id="11"/></ionType><ionType molecules="1"><part count="1" id="5"/></ionType><ionType molecules="1"><part count="1" id="1"/><part count="1" id="9"/></ionType><ionType molecules="1"><part count="-1" id="1"/><part count="1" id="8"/></ionType><ionType molecules="1"><part count="1" id="7"/></ionType><ionType molecules="1"><part count="3" id="1"/></ionType><ionType molecules="2"><part count="-1" id="1"/></ionType><ionType molecules="2"><part count="1" id="6"/></ionType><ionType molecules="2"><part count="-1" id="0"/><part count="1" id="1"/></ionType><ionType molecules="2"><part count="1" id="1"/></ionType><ionType molecules="2"><part count="1" id="10"/></ionType><ionType molecules="2"><part count="1" id="11"/></ionType><ionType molecules="3"><part count="1" id="1"/></ionType><ionType molecules="3"><part count="1" id="11"/></ionType><ionType molecules="4"><part count="1" id="1"/></ionType></ionTypes></ionLibrary></parameter>""";

  public static String oldXml = """
      <parameter name="Ion identity library">
                  <parameter name="Maximum charge">2</parameter>
                  <parameter name="Maximum molecules/cluster">2</parameter>
                  <parameter name="Adducts">
                      <adduct_type selected="false">
                          <subpart charge="1" mass_difference="1.007276" mol_formula="H" name="H" type="ADDUCT"/>
                          <subpart charge="1" mass_difference="1.007276" mol_formula="H" name="H" type="ADDUCT"/>
                      </adduct_type>
                      <adduct_type selected="true">
                          <subpart charge="1" mass_difference="38.963158" mol_formula="K" name="K" type="ADDUCT"/>
                      </adduct_type>
                      <adduct_type selected="false">
                          <subpart charge="2" mass_difference="47.96953482" mol_formula="Mg" name="Mg" type="ADDUCT"/>
                      </adduct_type>
                      <adduct_type selected="true">
                          <subpart charge="1" mass_difference="22.989218" mol_formula="Na" name="Na" type="ADDUCT"/>
                      </adduct_type>
                      <adduct_type selected="true">
                          <subpart charge="1" mass_difference="-5.4858E-4" mol_formula="" name="e" type="ADDUCT"/>
                      </adduct_type>
                      <adduct_type selected="false">
                          <subpart charge="1" mass_difference="1.007276" mol_formula="H" name="H" type="ADDUCT"/>
                          <subpart charge="1" mass_difference="22.989218" mol_formula="Na" name="Na" type="ADDUCT"/>
                      </adduct_type>
                      <adduct_type selected="true">
                          <subpart charge="2" mass_difference="55.93384" mol_formula="Fe" name="Fe" type="ADDUCT"/>
                      </adduct_type>
                      <adduct_type selected="false">
                          <subpart charge="1" mass_difference="1.007276" mol_formula="H" name="H" type="ADDUCT"/>
                          <subpart charge="0" mass_difference="-18.010565" mol_formula="H2O" name="H2O" type="NEUTRAL_LOSS"/>
                          <subpart charge="0" mass_difference="-18.010565" mol_formula="H2O" name="H2O" type="NEUTRAL_LOSS"/>
                      </adduct_type>
                      <adduct_type selected="true">
                          <subpart charge="1" mass_difference="18.033823" mol_formula="NH4" name="NH4" type="ADDUCT"/>
                      </adduct_type>
                      <adduct_type selected="true">
                          <subpart charge="1" mass_difference="1.007276" mol_formula="H" name="H" type="ADDUCT"/>
                      </adduct_type>
                      <adduct_type selected="false">
                          <subpart charge="-1" mass_difference="-1.007276" mol_formula="H" name="H" type="ADDUCT"/>
                      </adduct_type>
                      <modification_type selected="true">
                          <subpart charge="0" mass_difference="-18.010565" mol_formula="H2O" name="H2O" type="NEUTRAL_LOSS"/>
                      </modification_type>
                      <adduct_type selected="false">
                          <subpart charge="1" mass_difference="0.0" mol_formula="" name="e" type="ADDUCT"/>
                      </adduct_type>
                      <modification_type selected="true">
                          <subpart charge="0" mass_difference="-18.010565" mol_formula="H2O" name="H2O" type="NEUTRAL_LOSS"/>
                          <subpart charge="0" mass_difference="-18.010565" mol_formula="H2O" name="H2O" type="NEUTRAL_LOSS"/>
                      </modification_type>
                      <modification_type selected="false">
                          <subpart charge="0" mass_difference="-18.010565" mol_formula="H2O" name="H2O" type="NEUTRAL_LOSS"/>
                          <subpart charge="0" mass_difference="-18.010565" mol_formula="H2O" name="H2O" type="NEUTRAL_LOSS"/>
                          <subpart charge="0" mass_difference="-18.010565" mol_formula="H2O" name="H2O" type="NEUTRAL_LOSS"/>
                      </modification_type>
                  </parameter>
              </parameter>""";


  @Test
  void loadLegacyValueFromXML() {
    // old parameter
    // initialize with wrong ions
    final IonLibrary initialLibrary = IonLibraries.MZMINE_DEFAULT_NEG_FULL;
    final IonLibraryParameter param = new IonLibraryParameter(initialLibrary);
    // load xml of dual polarity list, in place operation
    ParameterUtils.loadParameterFromString(param, oldXml);
    final IonLibrary loadedLib = param.getValue();
    assertNotNull(loadedLib);
    assertNotEquals(initialLibrary, loadedLib);
    assertEquals(36, loadedLib.ions().size());
    assertEquals("Legacy library imported", loadedLib.name());

    // this is not all ions just a subset to test
    final List<IonType> testIons = IonTypes.listIons(false, IonTypes.K, IonTypes.M2_NA, IonTypes.NA,
        IonTypes.NA_H2O, IonTypes.M_PLUS, IonTypes.FEII, IonTypes.H_H2O, IonTypes.M_PLUS_H2O,
        IonTypes.H_2H2O);

    final Set<IonType> ions = Set.copyOf(loadedLib.ions());
    for (final IonType testIon : testIons) {
      Assertions.assertTrue(ions.contains(testIon), "Missing test ion: " + testIon);
    }
  }

  @Test
  void loadValueFromXML() {
    // initialize with wrong ions
    final IonLibrary initialLibrary = IonLibraries.MZMINE_DEFAULT_NEG_FULL;
    final IonLibraryParameter param = new IonLibraryParameter(initialLibrary);
    // load xml of dual polarity list, in place operation
    ParameterUtils.loadParameterFromString(param, expectedXml);
    assertNotNull(param.getValue());
    assertNotEquals(initialLibrary, param.getValue());
    assertEquals(IonLibraries.MZMINE_DEFAULT_DUAL_POLARITY_FULL, param.getValue());
  }

  @Test
  void saveLoadValueToXML() {
    final IonLibraryParameter param = new IonLibraryParameter(
        IonLibraries.MZMINE_DEFAULT_DUAL_POLARITY_FULL);

    // save and load from string
    final String xml = ParameterUtils.saveParameterToXMLString(param);
    // load with a different instance to see if loaded is correct
    final var clone = ParameterUtils.loadParameterFromString(
        new IonLibraryParameter(IonLibraries.MZMINE_DEFAULT_NEG_FULL), xml);

    assertNotNull(clone);
    assertEquals(param.getValue(), clone.getValue());
    param.setValue(IonLibraries.MZMINE_DEFAULT_NEG_FULL);
    assertNotEquals(param.getValue(), clone.getValue());
  }

  @Test
  void saveLoadValueToXMLOnlyElectron() {
    final IonLibraryParameter param = new IonLibraryParameter(new SimpleIonLibrary("Only e-",
        List.of(IonTypes.M_PLUS.asIonType(), IonTypes.M_MINUS.asIonType())));

    // save and load from string
    final String xml = ParameterUtils.saveParameterToXMLString(param);
    // load with a different instance to see if loaded is correct
    final var clone = ParameterUtils.loadParameterFromString(
        new IonLibraryParameter(IonLibraries.MZMINE_DEFAULT_NEG_FULL), xml);

    assertNotNull(clone);
    assertEquals(param.getValue(), clone.getValue());
    param.setValue(IonLibraries.MZMINE_DEFAULT_NEG_FULL);
    assertNotEquals(param.getValue(), clone.getValue());
  }

  @Test
  void cloneParameter() {
    final IonLibraryParameter param = new IonLibraryParameter(
        IonLibraries.MZMINE_DEFAULT_DUAL_POLARITY_FULL);
    final UserParameter<IonLibrary, IonLibraryComponent> clone = param.cloneParameter();
    assertNotNull(clone);
    assertNotSame(param, clone);
    assertEquals(param.getValue(), clone.getValue());
  }
}