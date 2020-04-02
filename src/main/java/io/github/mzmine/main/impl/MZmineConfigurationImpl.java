/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.main.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import io.github.mzmine.gui.chartbasics.chartthemes.ChartThemeParameters;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.main.MZmineConfiguration;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.EncryptionKeyParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameListSilentParameter;
import io.github.mzmine.util.StringCrypter;
import io.github.mzmine.util.color.ColorsFX;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.color.Vision;

/**
 * MZmine configuration class
 */
public class MZmineConfigurationImpl implements MZmineConfiguration {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private final MZminePreferences preferences;

  // list of last used projects
  private final @Nonnull FileNameListSilentParameter lastProjects;

  private final EncryptionKeyParameter globalEncrypter;

  private final Map<Class<? extends MZmineModule>, ParameterSet> moduleParameters;

  private final EStandardChartTheme standardChartTheme;

  public MZmineConfigurationImpl() {
    moduleParameters = new Hashtable<Class<? extends MZmineModule>, ParameterSet>();
    preferences = new MZminePreferences();
    lastProjects = new FileNameListSilentParameter("Last projets");
    globalEncrypter = new EncryptionKeyParameter();
    standardChartTheme = new EStandardChartTheme("default");
  }

  @Override
  public StringCrypter getEncrypter() {
    if (globalEncrypter.getValue() == null)
      globalEncrypter.setValue(new StringCrypter());
    return globalEncrypter.getValue();
  }

  @Override
  public ParameterSet getModuleParameters(Class<? extends MZmineModule> moduleClass) {
    ParameterSet parameters = moduleParameters.get(moduleClass);
    if (parameters == null) {
      // Create an instance of parameter set
      MZmineModule moduleInstance = MZmineCore.getModuleInstance(moduleClass);
      final Class<? extends ParameterSet> parameterSetClass = moduleInstance.getParameterSetClass();
      if (parameterSetClass == null)
        return null;

      try {
        parameters = parameterSetClass.getDeclaredConstructor().newInstance();
      } catch (Exception e) {
        e.printStackTrace();
        logger.log(Level.SEVERE,
            "Could not create an instance of parameter set class " + parameterSetClass, e);
        return null;
      }

      // Add the parameter set to the configuration
      moduleParameters.put(moduleClass, parameters);

    }
    return parameters;
  }

  @Override
  public void setModuleParameters(Class<? extends MZmineModule> moduleClass,
      ParameterSet parameters) {
    assert moduleClass != null;
    assert parameters != null;
    MZmineModule moduleInstance = MZmineCore.getModuleInstance(moduleClass);
    Class<? extends ParameterSet> parametersClass = moduleInstance.getParameterSetClass();
    if (parametersClass == null) {
      throw new IllegalArgumentException("Module " + moduleClass + " has no parameter set class");
    }
    if (!parametersClass.isInstance(parameters)) {
      throw new IllegalArgumentException("Given parameter set is an instance of "
          + parameters.getClass() + " instead of " + parametersClass);
    }
    moduleParameters.put(moduleClass, parameters);

  }

  // color palettes
//  @Override
//  public Vision getColorVision() {
//    return preferences.getParameter(MZminePreferences.colorPalettes).getValue();
//  }

  // Number formatting functions
  @Override
  public NumberFormat getIntensityFormat() {
    return preferences.getParameter(MZminePreferences.intensityFormat).getValue();
  }

  @Override
  public NumberFormat getMZFormat() {
    return preferences.getParameter(MZminePreferences.mzFormat).getValue();
  }

  @Override
  public NumberFormat getRTFormat() {
    return preferences.getParameter(MZminePreferences.rtFormat).getValue();
  }

  @Override
  public String getRexecPath() {
    File f = preferences.getParameter(MZminePreferences.rExecPath).getValue();
    if (f == null)
      return null;
    else
      return f.getPath();
  }

  @Override
  public Boolean getSendStatistics() {
    return preferences.getParameter(MZminePreferences.sendStatistics).getValue();
  }

  @SuppressWarnings("unchecked")
  @Override
  public void loadConfiguration(File file) throws IOException {

    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document configuration = dBuilder.parse(file);

      XPathFactory factory = XPathFactory.newInstance();
      XPath xpath = factory.newXPath();

      logger.finest("Loading desktop configuration");

      XPathExpression expr = xpath.compile("//configuration/preferences");
      NodeList nodes = (NodeList) expr.evaluate(configuration, XPathConstants.NODESET);
      if (nodes.getLength() == 1) {
        Element preferencesElement = (Element) nodes.item(0);
        // loading encryption key
        // this has to be read first because following parameters may
        // already contain encrypted data
        // that needs this key for encryption
        if (file.equals(MZmineConfiguration.CONFIG_FILE))
          new SimpleParameterSet(new Parameter[] {globalEncrypter})
              .loadValuesFromXML(preferencesElement);
        preferences.loadValuesFromXML(preferencesElement);
      }

      logger.finest("Loading last projects");
      expr = xpath.compile("//configuration/lastprojects");
      nodes = (NodeList) expr.evaluate(configuration, XPathConstants.NODESET);
      if (nodes.getLength() == 1) {
        Element lastProjectsElement = (Element) nodes.item(0);
        lastProjects.loadValueFromXML(lastProjectsElement);
      }

      logger.finest("Loading modules configuration");

      expr = xpath.compile("//configuration/modules/module");
      nodes = (NodeList) expr.evaluate(configuration, XPathConstants.NODESET);
      for (int i = 0; i < nodes.getLength(); i++) {
        Element moduleElement = (Element) nodes.item(i);
        String moduleClassName = moduleElement.getAttribute("class");

        try {
          Class<? extends MZmineModule> moduleClass =
              (Class<? extends MZmineModule>) Class.forName(moduleClassName);

          ParameterSet moduleParameters = getModuleParameters(moduleClass);
          moduleParameters.loadValuesFromXML(moduleElement);
        } catch (Exception e) {
          logger.log(Level.WARNING, "Failed to load configuration for module " + moduleClassName,
              e);
        }
      }

      logger.info("Loaded configuration from file " + file);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void saveConfiguration(File file) throws IOException {
    try {
      // write sensitive parameters only to the local config file
      final boolean skipSensitive = !file.equals(MZmineConfiguration.CONFIG_FILE);

      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

      Document configuration = dBuilder.newDocument();
      Element configRoot = configuration.createElement("configuration");
      configuration.appendChild(configRoot);

      Element prefElement = configuration.createElement("preferences");
      configRoot.appendChild(prefElement);
      preferences.setSkipSensitiveParameters(skipSensitive);
      preferences.saveValuesToXML(prefElement);

      Element lastFilesElement = configuration.createElement("lastprojects");
      configRoot.appendChild(lastFilesElement);
      lastProjects.saveValueToXML(lastFilesElement);

      Element modulesElement = configuration.createElement("modules");
      configRoot.appendChild(modulesElement);

      // traverse modules
      for (MZmineModule module : MZmineCore.getAllModules()) {

        String className = module.getClass().getName();

        Element moduleElement = configuration.createElement("module");
        moduleElement.setAttribute("class", className);
        modulesElement.appendChild(moduleElement);

        Element paramElement = configuration.createElement("parameters");
        moduleElement.appendChild(paramElement);

        ParameterSet moduleParameters = getModuleParameters(module.getClass());
        moduleParameters.setSkipSensitiveParameters(skipSensitive);
        moduleParameters.saveValuesToXML(paramElement);
      }

      // save encryption key to local config only
      // ATTENTION: this should to be written after all other configs
      final SimpleParameterSet encSet = new SimpleParameterSet(new Parameter[] {globalEncrypter});
      encSet.setSkipSensitiveParameters(skipSensitive);
      encSet.saveValuesToXML(prefElement);

      TransformerFactory transfac = TransformerFactory.newInstance();
      Transformer transformer = transfac.newTransformer();
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

      // Create parent folder if it does not exist
      File confParent = file.getParentFile();
      if ((confParent != null) && (!confParent.exists())) {
        confParent.mkdirs();
      }

      // Java fails to write into hidden files on Windows, see
      // https://bugs.openjdk.java.net/browse/JDK-8047342
      if (file.exists() && System.getProperty("os.name").toLowerCase().contains("windows")) {
        if ((Boolean) Files.getAttribute(file.toPath(), "dos:hidden", LinkOption.NOFOLLOW_LINKS)) {
          Files.setAttribute(file.toPath(), "dos:hidden", Boolean.FALSE, LinkOption.NOFOLLOW_LINKS);
        }
      }

      StreamResult result = new StreamResult(new FileOutputStream(file));
      DOMSource source = new DOMSource(configuration);
      transformer.transform(source, result);

      // make user home config file invisible on windows
      if ((!skipSensitive) && (System.getProperty("os.name").toLowerCase().contains("windows"))) {
        Files.setAttribute(file.toPath(), "dos:hidden", Boolean.TRUE, LinkOption.NOFOLLOW_LINKS);
      }

      logger.info("Saved configuration to file " + file);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public MZminePreferences getPreferences() {
    return preferences;
  }

  @Override
  @Nonnull
  public List<File> getLastProjects() {
    return lastProjects.getValue();
  }

  @Override
  @Nonnull
  public FileNameListSilentParameter getLastProjectsParameter() {
    return lastProjects;
  }

  @Override
  public SimpleColorPalette getDefaultColorPalette() {
    SimpleColorPalette p = preferences.getParameter(MZminePreferences.stdColorPalette).getValue();
    if (!p.isValid()) {
      logger.warning(
          "Current default color palette set in preferences is invalid. Returning standard "
              + "colors.");
      p = new SimpleColorPalette(ColorsFX.getSevenColorPalette(Vision.DEUTERANOPIA, true));
      p.setName("default-deuternopia");
    }
    return p;
  }

  @Override
  public ChartThemeParameters getDefaultChartThemeParameters() {
    return (ChartThemeParameters) preferences.getParameter(MZminePreferences.chartParam).getValue();
  }

  @Override
  public EStandardChartTheme getDefaultChartTheme() {
    // update the theme settings first
    ChartThemeParameters ctp = this.getDefaultChartThemeParameters();
    ctp.applyToChartTheme(standardChartTheme);
    SimpleColorPalette scp = this.getDefaultColorPalette();
    scp.applyToChartTheme(standardChartTheme);

    return standardChartTheme;
  }

}
