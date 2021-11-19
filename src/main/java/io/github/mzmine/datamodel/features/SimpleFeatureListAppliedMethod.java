/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
