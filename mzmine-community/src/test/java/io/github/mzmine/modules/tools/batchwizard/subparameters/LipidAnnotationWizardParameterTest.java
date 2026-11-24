package io.github.mzmine.modules.tools.batchwizard.subparameters;

import io.github.mzmine.modules.tools.batchwizard.LipidDoubleBondWizardTechnique;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * "Annotate lipids" was a plain checkbox before it received sub parameters. Saved presets and
 * generated wizard files in that format must keep their selection.
 */
class LipidAnnotationWizardParameterTest {

  @Test
  void loadsLegacyCheckboxValue() throws ParserConfigurationException {
    final LipidAnnotationWizardParameter parameter = AnnotationWizardParameters.lipidAnnotation.cloneParameter();
    parameter.setValue(true);

    parameter.loadValueFromXML(legacyElement("false"));
    Assertions.assertFalse(parameter.getValue());

    parameter.loadValueFromXML(legacyElement("true"));
    Assertions.assertTrue(parameter.getValue());
    Assertions.assertFalse(parameter.getEmbeddedParameters()
        .getValue(LipidAnnotationWizardSubParameters.lipidDoubleBonds));
  }

  @Test
  void roundTripsSelectionAndDoubleBondOption() throws ParserConfigurationException {
    final LipidAnnotationWizardParameter saved = AnnotationWizardParameters.lipidAnnotation.cloneParameter();
    saved.setValue(false);
    saved.getEmbeddedParameters()
        .setParameter(LipidAnnotationWizardSubParameters.lipidDoubleBonds, true,
            LipidDoubleBondWizardTechnique.OAD);
    final Element element = newDocument().createElement("parameter");
    saved.saveValueToXML(element);

    final LipidAnnotationWizardParameter loaded = AnnotationWizardParameters.lipidAnnotation.cloneParameter();
    loaded.setValue(true);
    loaded.loadValueFromXML(element);

    Assertions.assertFalse(loaded.getValue());
    Assertions.assertEquals(LipidDoubleBondWizardTechnique.OAD, loaded.getEmbeddedParameters()
        .getEmbeddedParameterValueIfSelectedOrElse(
            LipidAnnotationWizardSubParameters.lipidDoubleBonds, null));
  }

  private static @NotNull Element legacyElement(final @NotNull String value)
      throws ParserConfigurationException {
    final Element element = newDocument().createElement("parameter");
    element.setAttribute("name", "Annotate lipids");
    element.setTextContent(value);
    return element;
  }

  private static @NotNull Document newDocument() throws ParserConfigurationException {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
  }
}
