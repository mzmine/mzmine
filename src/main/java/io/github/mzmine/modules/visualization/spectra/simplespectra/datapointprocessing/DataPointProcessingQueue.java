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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.XMLUtils;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Vector;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class DataPointProcessingQueue extends
    Vector<MZmineProcessingStep<DataPointProcessingModule>> {

  private static final long serialVersionUID = 1L;

  private static final Logger logger = Logger.getLogger(DataPointProcessingQueue.class.getName());

  private static final String DATA_POINT_PROCESSING_STEP_ELEMENT = "processingstep";
  private static final String METHOD_ELEMENT = "method";

  public static @NotNull DataPointProcessingQueue loadfromXML(final @NotNull Element xmlElement) {
    DataPointProcessingQueue queue = new DataPointProcessingQueue();

    // Get the loaded modules.
    final Collection<MZmineModule> allModules = MZmineCore.getAllModules();

    // Process the processing step elements.
    final NodeList nodes = xmlElement.getElementsByTagName(DATA_POINT_PROCESSING_STEP_ELEMENT);
    final int nodesLength = nodes.getLength();

    for (int i = 0; i < nodesLength; i++) {

      final Element stepElement = (Element) nodes.item(i);
      final String methodName = stepElement.getAttribute(METHOD_ELEMENT);
      logger.finest("loading method " + methodName);

      for (MZmineModule module : allModules) {
        if (module instanceof DataPointProcessingModule && module.getClass().getName()
            .equals(methodName)) {

          // since the same module can be used in different ms levels,
          // we need to clone the
          // parameter set, so we can have different values for every
          // ms level
          ParameterSet parameterSet = MZmineCore.getConfiguration()
              .getModuleParameters(module.getClass()).cloneParameterSet();

          parameterSet.loadValuesFromXML(stepElement);
          queue.add(new MZmineProcessingStepImpl<DataPointProcessingModule>(
              (DataPointProcessingModule) module, parameterSet));
          // add to treeView
          break;
        }

      }

    }
    return queue;
  }

  public static @NotNull DataPointProcessingQueue loadFromFile(@NotNull File file) {
    try {
      Element element = XMLUtils.load(file).getDocumentElement();
      return loadfromXML(element);
    } catch (SAXException | IOException | ParserConfigurationException e) {
      e.printStackTrace();
      return new DataPointProcessingQueue();
    }
  }

  public void saveToXML(final @NotNull Element xmlElement) {

    final Document document = xmlElement.getOwnerDocument();

    // Process each step.
    for (final MZmineProcessingStep<?> step : this) {

      // Append a new batch step element.
      final Element stepElement = document.createElement(DATA_POINT_PROCESSING_STEP_ELEMENT);

      stepElement.setAttribute(METHOD_ELEMENT, step.getModule().getClass().getName());
      xmlElement.appendChild(stepElement);

      // Save parameters.
      final ParameterSet parameters = step.getParameterSet();
      if (parameters != null) {
        parameters.saveValuesToXML(stepElement);
      }
    }
  }

  public void saveToFile(final @NotNull File file) {
    try {
      Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      final Element element = document.createElement("DataPointProcessing");
      document.appendChild(element);

      // Serialize batch queue.
      this.saveToXML(element);

      XMLUtils.saveToFile(file, document);

      logger.finest("Saved " + this.size() + " processing step(s) to " + file.getName());

    } catch (ParserConfigurationException | TransformerFactoryConfigurationError |
             TransformerException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  /**
   * @return Returns true if the module list is initialized and > 0.
   */
  public boolean stepsValid() {
    return !this.isEmpty();
  }

  /**
   * @param current A pointer to the current module.
   * @return Returns true if there is one or more steps, false if not.
   */
  public boolean hasNextStep(MZmineProcessingStep<DataPointProcessingModule> current) {
    if (this.contains(current)) {
      int index = this.indexOf(current);
      return index + 1 < this.size();
    }
    return false;
  }

  /**
   * @param current A pointer to the current module.
   * @return Returns the next module in this PlotModuleCombo. If this pmc has no next module the
   * return is null. Use hasNextModule to check beforehand.
   */
  public @Nullable MZmineProcessingStep<DataPointProcessingModule> getNextStep(
      @NotNull MZmineProcessingStep<DataPointProcessingModule> current) {
    if (hasNextStep(current)) {
      return this.get(this.indexOf(current) + 1);
    }
    return null;
  }

  /**
   * @return Returns the first module in this PlotModuleCombo. If the list of steps is not
   * initialised, the return is null.
   */
  public @Nullable MZmineProcessingStep<DataPointProcessingModule> getFirstStep() {
    if (this.size() > 0) {
      return this.get(0);
    }
    return null;
  }

  public DataPointProcessingQueue clone() {
    DataPointProcessingQueue clone = new DataPointProcessingQueue();

    for (int i = 0; i < this.size(); i++) {
      clone.add(new MZmineProcessingStepImpl<DataPointProcessingModule>(this.get(i).getModule(),
          this.get(i).getParameterSet().cloneParameterSet()));
    }

    return clone;
  }
}
