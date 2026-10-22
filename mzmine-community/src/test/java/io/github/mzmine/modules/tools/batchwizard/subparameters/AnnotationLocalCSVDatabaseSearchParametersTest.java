/*
 * Copyright (c) 2004-2026 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.mzmine.modules.tools.batchwizard.subparameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.identities.iontype.IonLibraries;
import io.github.mzmine.datamodel.identities.iontype.IonLibrary;
import io.github.mzmine.datamodel.identities.iontype.IonTypes;
import io.github.mzmine.datamodel.identities.iontype.UnmodifiableIonLibrary;
import io.github.mzmine.modules.tools.batchwizard.subparameters.custom_parameters.WizardMsPolarity;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.util.XMLUtils;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnnotationLocalCSVDatabaseSearchParametersTest {

  @Test
  void usesTheExistingPolaritySpecificMainLibrariesByDefault() {
    final var parameters = new AnnotationLocalCSVDatabaseSearchParameters();

    assertFalse(parameters.getValue(AnnotationLocalCSVDatabaseSearchParameters.adductLibrary));
    assertSame(IonLibraries.MZMINE_DEFAULT_POS_MAIN,
        parameters.getEffectiveIonLibrary(WizardMsPolarity.Positive));
    assertSame(IonLibraries.MZMINE_DEFAULT_NEG_MAIN,
        parameters.getEffectiveIonLibrary(WizardMsPolarity.Negative));
    assertSame(IonLibraries.MZMINE_DEFAULT_DUAL_POLARITY_MAIN,
        parameters.getEffectiveIonLibrary(WizardMsPolarity.No_filter));
  }

  @Test
  void usesTheSelectedAdductOverrideAndKeepsItDetached() {
    final IonLibrary override = new UnmodifiableIonLibrary("Proton and sodium", List.of(
        IonTypes.H.asIonType(), IonTypes.NA.asIonType()));
    final var parameters = new AnnotationLocalCSVDatabaseSearchParameters();
    parameters.setParameter(AnnotationLocalCSVDatabaseSearchParameters.adductLibrary, true,
        override);

    final IonLibrary effective = parameters.getEffectiveIonLibrary(WizardMsPolarity.Positive);
    assertEquals(override, effective);
    assertNotSame(override, effective);

    final IonLibrary batchLibrary = effective.copy();
    assertEquals(effective, batchLibrary);
    assertNotSame(effective, batchLibrary);
  }

  @Test
  void preservesTheOverrideAcrossParameterSetCopyAndXmlRoundTrip() throws Exception {
    final IonLibrary override = new UnmodifiableIonLibrary("Proton and sodium", List.of(
        IonTypes.H.asIonType(), IonTypes.NA.asIonType()));
    final var parameters = new AnnotationLocalCSVDatabaseSearchParameters();
    parameters.setParameter(AnnotationLocalCSVDatabaseSearchParameters.adductLibrary, true,
        override);

    final var copied = (AnnotationLocalCSVDatabaseSearchParameters) parameters.cloneParameterSet();
    assertEquals(override, copied.getEffectiveIonLibrary(WizardMsPolarity.Negative));
    assertNotSame(parameters.getEffectiveIonLibrary(WizardMsPolarity.Negative),
        copied.getEffectiveIonLibrary(WizardMsPolarity.Negative));

    final String xml = ParameterUtils.saveValuesToXMLString(parameters);
    final var loaded = new AnnotationLocalCSVDatabaseSearchParameters();
    ParameterUtils.loadValuesFromXMLString(loaded, xml);
    assertEquals(override, loaded.getEffectiveIonLibrary(WizardMsPolarity.Positive));
    assertNotSame(parameters.getEffectiveIonLibrary(WizardMsPolarity.Positive),
        loaded.getEffectiveIonLibrary(WizardMsPolarity.Positive));
  }

  @Test
  void preservesExplicitSelectionFromThePreviousParameterName() throws Exception {
    final IonLibrary override = new UnmodifiableIonLibrary("Sodium only",
        List.of(IonTypes.NA.asIonType()));
    final var saved = new AnnotationLocalCSVDatabaseSearchParameters();
    saved.setParameter(AnnotationLocalCSVDatabaseSearchParameters.adductLibrary, true, override);
    final String legacyXml = ParameterUtils.saveValuesToXMLString(saved)
        .replace("name=\"Specific adducts\"", "name=\"Override adducts\"");
    final var loaded = new AnnotationLocalCSVDatabaseSearchParameters();

    ParameterUtils.loadValuesFromXMLString(loaded, legacyXml);

    assertTrue(loaded.getValue(AnnotationLocalCSVDatabaseSearchParameters.adductLibrary));
    assertEquals(override, loaded.getEffectiveIonLibrary(WizardMsPolarity.Positive));
    assertFalse(new AnnotationLocalCSVDatabaseSearchParameters()
        .getValue(AnnotationLocalCSVDatabaseSearchParameters.adductLibrary));
  }

  @Test
  void legacyPresetWithoutTheSettingDisablesAPreviouslySelectedLibrary() throws Exception {
    final var parameters = new AnnotationLocalCSVDatabaseSearchParameters();
    parameters.setParameter(AnnotationLocalCSVDatabaseSearchParameters.adductLibrary, true,
        new UnmodifiableIonLibrary("Sodium only", List.of(IonTypes.NA.asIonType())));
    final var document = XMLUtils.newDocument();
    final var element = document.createElement("parameterset");
    document.appendChild(element);
    parameters.saveValuesToXML(element);
    final var adductElement = XMLUtils.streamChildElementsByTagName(element, "parameter")
        .filter(parameter -> AnnotationLocalCSVDatabaseSearchParameters.adductLibrary.getName()
            .equals(parameter.getAttribute("name"))).findFirst().orElseThrow();
    element.removeChild(adductElement);

    parameters.loadValuesFromXML(element);

    assertFalse(parameters.getValue(AnnotationLocalCSVDatabaseSearchParameters.adductLibrary));
    assertSame(IonLibraries.MZMINE_DEFAULT_POS_MAIN,
        parameters.getEffectiveIonLibrary(WizardMsPolarity.Positive));
    assertSame(IonLibraries.MZMINE_DEFAULT_NEG_MAIN,
        parameters.getEffectiveIonLibrary(WizardMsPolarity.Negative));
  }
}
