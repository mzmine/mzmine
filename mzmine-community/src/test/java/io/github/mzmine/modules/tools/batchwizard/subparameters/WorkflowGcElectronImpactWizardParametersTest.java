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

import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows.WorkflowDeconvolution;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Tests that GC-EI wizard presets saved before the gap filling and duplicate filter option existed
 * keep their previous workflow.
 */
class WorkflowGcElectronImpactWizardParametersTest {

  @Test
  void newParametersApplyGapFillingAndDuplicateFilter() {
    Assertions.assertTrue(create().getValue(
        WorkflowGcElectronImpactWizardParameters.applyGapFillingAndDuplicateFilter));
  }

  @Test
  void oldPresetWithoutOptionDisablesIt() throws ParserConfigurationException {
    final Element saved = save(create());
    removeParameter(saved,
        WorkflowGcElectronImpactWizardParameters.applyGapFillingAndDuplicateFilter.getName());

    final WorkflowGcElectronImpactWizardParameters loaded = create();
    loaded.loadValuesFromXML(saved);

    Assertions.assertFalse(loaded.getValue(
        WorkflowGcElectronImpactWizardParameters.applyGapFillingAndDuplicateFilter));
  }

  @Test
  void savedPresetKeepsOption() throws ParserConfigurationException {
    final Element saved = save(create());

    final WorkflowGcElectronImpactWizardParameters loaded = create();
    loaded.setParameter(WorkflowGcElectronImpactWizardParameters.applyGapFillingAndDuplicateFilter,
        false);
    loaded.loadValuesFromXML(saved);

    Assertions.assertTrue(loaded.getValue(
        WorkflowGcElectronImpactWizardParameters.applyGapFillingAndDuplicateFilter));
  }

  /**
   * @return new wizard parameters with the defaults of the GC-EI workflow preset
   */
  private static @NotNull WorkflowGcElectronImpactWizardParameters create() {
    return (WorkflowGcElectronImpactWizardParameters) new WorkflowDeconvolution().create();
  }

  /**
   * @return a new XML element with the saved parameter values
   */
  private static @NotNull Element save(
      @NotNull final WorkflowGcElectronImpactWizardParameters params)
      throws ParserConfigurationException {
    final Element element = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        .createElement("parameters");
    params.saveValuesToXML(element);
    return element;
  }

  /**
   * Removes a saved parameter to simulate a preset from an older version.
   */
  private static void removeParameter(@NotNull final Element element, @NotNull final String name) {
    for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node instanceof Element child && name.equals(
          child.getAttribute(SimpleParameterSet.nameAttribute))) {
        element.removeChild(child);
        return;
      }
    }
    Assertions.fail("Parameter " + name + " was not saved");
  }
}
