package io.github.mzmine.parameters.impl;

import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.SubModuleParameter;
import io.github.mzmine.util.XMLUtils;
import javax.xml.parsers.ParserConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Embedded parameter sets are saved into the element of their parameter. They need their own
 * version attribute to tell old saved values from current ones during loading.
 */
class SimpleParameterSetVersionTest {

  private static final SubModuleParameter<VersionedTestParameters> embedded = new SubModuleParameter<>(
      "Embedded", "", new VersionedTestParameters());

  @Test
  void embeddedSetLoadsItsSavedVersion() throws ParserConfigurationException {
    final Element element = newElement();
    newOuter().saveValuesToXML(element);

    final ParameterSet loaded = newOuter();
    loaded.loadValuesFromXML(element);

    Assertions.assertEquals(VersionedTestParameters.VERSION,
        loaded.getParameter(embedded).getEmbeddedParameters().getLoadedVersion());
  }

  @Test
  void elementWithoutVersionLoadsAsVersionOne() throws ParserConfigurationException {
    final Element element = newElement();
    new VersionedTestParameters().saveValuesToXML(element);
    // values saved before the version was stored
    element.removeAttribute(ParameterSet.MODULE_VERSION_ATTR);

    final VersionedTestParameters loaded = new VersionedTestParameters();
    loaded.loadValuesFromXML(element);

    Assertions.assertEquals(1, loaded.getLoadedVersion());
  }

  private static @NotNull ParameterSet newOuter() {
    return new SimpleParameterSet(embedded).cloneParameterSet();
  }

  private static @NotNull Element newElement() throws ParserConfigurationException {
    final Document document = XMLUtils.newDocument();
    final Element element = document.createElement("parameterset");
    document.appendChild(element);
    return element;
  }
}
