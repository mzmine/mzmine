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

package io.github.mzmine.datamodel.features;

import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * TODO: Move to io.github.mzmine.datamodel and rename to SimpleAppliedMethod
 */
public class SimpleFeatureListAppliedMethod implements FeatureListAppliedMethod {

  private static final Logger logger = Logger
      .getLogger(SimpleFeatureListAppliedMethod.class.getName());

  private final String description;
  private final Instant moduleCallDate;
  private final ParameterSet parameters;
  private final MZmineModule module;

  /**
   * @param parameters The parameter set used to create this feature list. A clone of the parameter
   *                   set is created in the constructor and saved in this class.
   * @param moduleCallDate
   */
  public SimpleFeatureListAppliedMethod(MZmineModule module, ParameterSet parameters,
      final @NotNull Instant moduleCallDate) {
    this.parameters = parameters.cloneParameterSet(true);
    this.module = module;
    this.description = module.getName();
    this.moduleCallDate = moduleCallDate;
  }

  public SimpleFeatureListAppliedMethod(Class<? extends MZmineModule> moduleClass,
      ParameterSet parameters, final @NotNull Instant moduleCallDate) {
    this(MZmineCore.getModuleInstance(moduleClass), parameters, moduleCallDate);
  }

  public SimpleFeatureListAppliedMethod(String description, MZmineModule module,
      ParameterSet parameters, final @NotNull Instant moduleCallDate) {
    this.description = description;
    this.parameters = parameters.cloneParameterSet(true);
    this.module = module;
    this.moduleCallDate = moduleCallDate;
  }

  public SimpleFeatureListAppliedMethod(String description,
      Class<? extends MZmineModule> moduleClass, ParameterSet parameters,
      final @NotNull Instant moduleCallDate) {
    this.description = description;
    this.parameters = parameters.cloneParameterSet(true);
    this.module = MZmineCore.getModuleInstance(moduleClass);
    this.moduleCallDate = moduleCallDate;
  }

  public @NotNull String getDescription() {
    return description;
  }

  public String toString() {
    return description;
  }

  public @NotNull ParameterSet getParameters() {
    // don't return the saved parameters, return a clone so parameters cannot be altered by accident.
    return parameters.cloneParameterSet(true);
  }

  @Override
  public MZmineModule getModule() {
    return module;
  }

  @Override
  public Instant getModuleCallDate() {
    return moduleCallDate;
  }

  /**
   * @param element The xml element for this {@link FeatureListAppliedMethod}.
   */
  @Override
  public void saveValueToXML(Element element) {
    final Document doc = element.getOwnerDocument();

    final Element descriptionElement = doc.createElement("description");
    descriptionElement.setTextContent(description);
    element.appendChild(descriptionElement);

    final Element moduleElement = doc.createElement("module");
    moduleElement.setAttribute("class", module.getClass().getName());
    moduleElement.setAttribute("date", moduleCallDate.toString());
    element.appendChild(moduleElement);

    final Element parametersElement = doc.createElement("parameters");
    parameters.saveValuesToXML(parametersElement);
    element.appendChild(parametersElement);
  }

  public static SimpleFeatureListAppliedMethod loadValueFromXML(Element element) {
    final Element moduleElement = (Element) element.getElementsByTagName("module").item(0);
    String moduleClassName = moduleElement.getAttribute("class");
    final Element parametersElement = (Element) element.getElementsByTagName("parameters").item(0);
    final Element descriptionElement = (Element) element.getElementsByTagName("description")
        .item(0);

    Class<? extends MZmineModule> moduleClass;
    ParameterSet moduleParameters;

    try {
      moduleClass = (Class<? extends MZmineModule>) Class.forName(moduleClassName);
      moduleParameters = MZmineCore.getConfiguration().getModuleParameters(moduleClass)
          .cloneParameterSet();
      moduleParameters.loadValuesFromXML(parametersElement);
    } catch (Exception | NoClassDefFoundError e) {
      logger.log(Level.SEVERE, "Cannot parse module parameters", e);
      return null;
    }

    final Instant date;
    try {
      date = Instant.parse(moduleElement.getAttribute("date"));
      return new SimpleFeatureListAppliedMethod(descriptionElement.getTextContent(), moduleClass,
          moduleParameters, date);
    } catch (DateTimeParseException e) {
      final Date oldDate = new Date(Long.parseLong(moduleElement.getAttribute("date")));
      return new SimpleFeatureListAppliedMethod(descriptionElement.getTextContent(), moduleClass,
          moduleParameters, oldDate.toInstant());
    }
  }
}
