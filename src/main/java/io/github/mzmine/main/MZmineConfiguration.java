/*
 * Copyright 2006-2016 The MZmine 3 Development Team
 * 
 * This file is part of MZmine 3.
 * 
 * MZmine 3 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 3 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 3; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;

/**
 * MZmine configuration interface
 */
public final class MZmineConfiguration {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  public static final File CONFIG_FILE = new File("conf/config.xml");

  private final Map<Class<? extends MZmineModule>, ParameterSet> moduleParameters;
  private final MZminePreferences preferences;

  public MZmineConfiguration() {
    moduleParameters = new Hashtable<Class<? extends MZmineModule>, ParameterSet>();
    preferences = new MZminePreferences();
  }

  public MZminePreferences getPreferences() {
    return preferences;
  }

  public ParameterSet getModuleParameters(Class<? extends MZmineModule> moduleClass) {
    ParameterSet parameters = moduleParameters.get(moduleClass);
    if (parameters == null) {
      throw new IllegalArgumentException(
          "Module " + moduleClass + " does not have any parameter set instance");
    }
    return parameters;
  }

  public void setModuleParameters(Class<? extends MZmineModule> moduleClass,
      ParameterSet parameters) {
    assert moduleClass != null;
    assert parameters != null;
    MZmineModule moduleInstance = MZmineModuleStarter.getModuleInstance(moduleClass);
    Class<? extends ParameterSet> parametersClass = moduleInstance.getParameterSetClass();
    if (!parametersClass.isInstance(parameters)) {
      throw new IllegalArgumentException("Given parameter set is an instance of "
          + parameters.getClass() + " instead of " + parametersClass);
    }
    moduleParameters.put(moduleClass, parameters);

  }

  public DecimalFormat getIntensityFormat() {
    return preferences.getParameter(MZminePreferences.intensityFormat).getValue();
  }

  public DecimalFormat getMZFormat() {
    return preferences.getParameter(MZminePreferences.mzFormat).getValue();
  }

  public DecimalFormat getRTFormat() {
    return preferences.getParameter(MZminePreferences.rtFormat).getValue();
  }

  public Boolean getSendStatistics() {
    return preferences.getParameter(MZminePreferences.sendStatistics).getValue();
  }

  public void loadConfiguration(File file) throws IOException {

    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document configuration = dBuilder.parse(file);

      XPathFactory factory = XPathFactory.newInstance();
      XPath xpath = factory.newXPath();

      logger.info("Loading desktop configuration");

      XPathExpression expr = xpath.compile("//configuration/preferences");
      NodeList nodes = (NodeList) expr.evaluate(configuration, XPathConstants.NODESET);
      if (nodes.getLength() == 1) {
        Element preferencesElement = (Element) nodes.item(0);
        preferences.loadValuesFromXML(preferencesElement);
      }

      logger.info("Loading modules configuration");

      for (MZmineModule module : MZmineModuleStarter.getAllModules()) {

        String className = module.getClass().getName();
        expr =
            xpath.compile("//configuration/modules/module[@class='" + className + "']/parameters");
        nodes = (NodeList) expr.evaluate(configuration, XPathConstants.NODESET);
        if (nodes.getLength() != 1)
          continue;

        Element moduleElement = (Element) nodes.item(0);

        ParameterSet moduleParameters = getModuleParameters(module.getClass());
        moduleParameters.loadValuesFromXML(moduleElement);
      }

      logger.info("Loaded configuration from file " + file);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  public void saveConfiguration(File file) throws IOException {
    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

      Document configuration = dBuilder.newDocument();
      Element configRoot = configuration.createElement("configuration");
      configuration.appendChild(configRoot);

      Element prefElement = configuration.createElement("preferences");
      configRoot.appendChild(prefElement);
      preferences.saveValuesToXML(prefElement);

      Element modulesElement = configuration.createElement("modules");
      configRoot.appendChild(modulesElement);

      // traverse modules
      for (MZmineModule module : MZmineModuleStarter.getAllModules()) {

        String className = module.getClass().getName();

        Element moduleElement = configuration.createElement("module");
        moduleElement.setAttribute("class", className);
        modulesElement.appendChild(moduleElement);

        Element paramElement = configuration.createElement("parameters");
        moduleElement.appendChild(paramElement);

        ParameterSet moduleParameters = getModuleParameters(module.getClass());
        moduleParameters.saveValuesToXML(paramElement);

      }

      TransformerFactory transfac = TransformerFactory.newInstance();
      Transformer transformer = transfac.newTransformer();
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

      StreamResult result = new StreamResult(new FileOutputStream(file));
      DOMSource source = new DOMSource(configuration);
      transformer.transform(source, result);

      logger.info("Saved configuration to file " + file);

    } catch (Exception e) {
      throw new IOException(e);
    }
  }

}
