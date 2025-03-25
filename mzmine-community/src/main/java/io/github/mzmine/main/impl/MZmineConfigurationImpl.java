/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.main.impl;

import io.github.mzmine.gui.chartbasics.chartthemes.ChartThemeParameters;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.gui.preferences.ImageNormalization;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.gui.preferences.Themes;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.javafx.util.color.ColorsFX;
import io.github.mzmine.javafx.util.color.Vision;
import io.github.mzmine.main.MZmineConfiguration;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.import_rawdata_msconvert.MSConvert;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.EncryptionKeyParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameListSilentParameter;
import io.github.mzmine.util.StringCrypter;
import io.github.mzmine.util.XMLUtils;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.logging.LoggerUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * MZmine configuration class
 */
public class MZmineConfigurationImpl implements MZmineConfiguration {

  private static final Logger logger = Logger.getLogger(MZmineConfigurationImpl.class.getName());

  private final MZminePreferences preferences;

  // logging file - first is null but can be extracted from Logger.parent.handlers by reflection
  // if this fails then resort to finding the log file in user folder
  private @Nullable File logFile;

  // list of last used projects
  private final @NotNull FileNameListSilentParameter lastProjects;

  private final EncryptionKeyParameter globalEncrypter;

  /**
   * class.getName is used as keys. Classes should not be used as keys in maps
   */
  private final Map<String, ParameterSet> moduleParameters;

  private final EStandardChartTheme standardChartTheme;

  public MZmineConfigurationImpl() {
    moduleParameters = new Hashtable<>();
    preferences = new MZminePreferences();
    lastProjects = new FileNameListSilentParameter("Last projects");
    globalEncrypter = new EncryptionKeyParameter();
    standardChartTheme = new EStandardChartTheme("default");
  }

  @Override
  public StringCrypter getEncrypter() {
    if (globalEncrypter.getValue() == null) {
      globalEncrypter.setValue(new StringCrypter());
    }
    return globalEncrypter.getValue();
  }

  /**
   * Returns null if the given module does not exist
   */
  @Override
  public @Nullable ParameterSet getModuleParameters(Class<? extends MZmineModule> moduleClass) {
    if (moduleClass == null) {
      return null;
    }

    ParameterSet parameters = moduleParameters.get(moduleClass.getName());
    if (parameters == null) {
      // Create an instance of parameter set
      try {
        MZmineModule moduleInstance = MZmineCore.getModuleInstance(moduleClass);
        if (moduleInstance == null) {
          logger.log(Level.WARNING, "Module " + moduleClass + " does not exist");
          return null;
        }

        final Class<? extends ParameterSet> parameterSetClass = moduleInstance.getParameterSetClass();
        if (parameterSetClass == null) {
          logger.log(Level.WARNING,
              "Module " + moduleClass + " does not provide any ParameterSet class");
          return null;
        }

        try {
          parameters = parameterSetClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
          logger.log(Level.SEVERE,
              "Could not create an instance of parameter set class " + parameterSetClass + " "
              + e.getMessage(), e);
          return null;
        }
      } catch (NoClassDefFoundError | Exception e) {
        logger.log(Level.WARNING,
            "Could not find the module or parameter class " + moduleClass.toString() + " "
            + e.getMessage(), e);
        return null;
      }

      // Add the parameter set to the configuration
      parameters.setModuleNameAttribute(MZmineCore.getModuleInstance(moduleClass).getName());
      moduleParameters.put(moduleClass.getName(), parameters);

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
      throw new IllegalArgumentException(
          "Given parameter set is an instance of " + parameters.getClass() + " instead of "
          + parametersClass);
    }
    moduleParameters.put(moduleClass.getName(), parameters);

  }

  // color palettes
  // @Override
  // public Vision getColorVision() {
  // return preferences.getParameter(MZminePreferences.colorPalettes).getValue();
  // }

  // Number formatting functions
  @Override
  public NumberFormat getIntensityFormat() {
    return preferences.getParameter(MZminePreferences.intensityFormat).getValue();
  }

  @Override
  public NumberFormat getScoreFormat() {
    return preferences.getParameter(MZminePreferences.scoreFormat).getValue();
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
  public NumberFormat getMobilityFormat() {
    return preferences.getParameter(MZminePreferences.mobilityFormat).getValue();
  }

  @Override
  public NumberFormat getCCSFormat() {
    return preferences.getParameter(MZminePreferences.ccsFormat).getValue();
  }

  @Override
  public NumberFormat getPPMFormat() {
    return preferences.getParameter(MZminePreferences.ppmFormat).getValue();
  }

  @Override
  public NumberFormat getPercentFormat() {
    return preferences.getParameter(MZminePreferences.percentFormat).getValue();
  }

  @Override
  public UnitFormat getUnitFormat() {
    return preferences.getParameter(MZminePreferences.unitFormat).getValue();
  }

  @Override
  public NumberFormats getGuiFormats() {
    return preferences.getGuiFormats();
  }

  @Override
  public NumberFormats getExportFormats() {
    return preferences.getExportFormats();
  }

  @Override
  public void loadConfiguration(File file, boolean loadPreferences) throws IOException {
    List<String> exculdeWarningsFor = List.of("jmzml", "adap");

    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document configuration = dBuilder.parse(file);

      XPathFactory factory = XPathFactory.newInstance();
      XPath xpath = factory.newXPath();

      logger.finest("Loading desktop configuration");

      XPathExpression expr;
      NodeList nodes;
      if (loadPreferences) {
        expr = xpath.compile("//configuration/preferences");
        nodes = (NodeList) expr.evaluate(configuration, XPathConstants.NODESET);
        if (nodes.getLength() == 1) {
          Element preferencesElement = (Element) nodes.item(0);
          // loading encryption key
          // this has to be read first because following parameters may
          // already contain encrypted data
          // that needs this key for encryption
          if (file.equals(MZmineConfiguration.CONFIG_FILE)) {
            new SimpleParameterSet(globalEncrypter).loadValuesFromXML(preferencesElement);
          }
          preferences.loadValuesFromXML(preferencesElement);
        }

        logger.finest("Loading last projects");
        expr = xpath.compile("//configuration/lastprojects");
        nodes = (NodeList) expr.evaluate(configuration, XPathConstants.NODESET);
        if (nodes.getLength() == 1) {
          Element lastProjectsElement = (Element) nodes.item(0);
          lastProjects.loadValueFromXML(lastProjectsElement);
        }
        // apply preferences to all parts of mzmine
        preferences.applyConfig();
      }

      logger.finest("Loading modules configuration");

      expr = xpath.compile("//configuration/modules/module");
      nodes = (NodeList) expr.evaluate(configuration, XPathConstants.NODESET);
      for (int i = 0; i < nodes.getLength(); i++) {
        Element moduleElement = (Element) nodes.item(i);
        String moduleClassName = moduleElement.getAttribute("class");

        try {
          Class<? extends MZmineModule> moduleClass = (Class<? extends MZmineModule>) Class.forName(
              moduleClassName);

          ParameterSet moduleParameters = getModuleParameters(moduleClass);
          if (moduleParameters == null) {
            logger.info(
                "Module %s was in the config file but was not found in the current version of MZmine".formatted(
                    moduleClass.getName()));
          }

          MZmineModule moduleInstance = MZmineCore.getModuleInstance(moduleClass);
          if (moduleInstance != null && moduleInstance.getParameterSetClass() == null) {
            // some modules do not have a parameterset class
            continue;
          }
          var parameterElement = (Element) moduleElement.getElementsByTagName("parameters").item(0);
          moduleParameters.loadValuesFromXML(parameterElement);
        } catch (Exception | NoClassDefFoundError e) {
          boolean exclude = exculdeWarningsFor.stream()
              .anyMatch(ex -> moduleClassName.toLowerCase().contains(ex));
          if (!exclude) {
            logger.log(Level.WARNING, "Failed to load configuration for module " + moduleClassName,
                e);
          }
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
      List<MZmineModule> allModules = new ArrayList<>(MZmineCore.getAllModules());
      for (MZmineModule module : allModules) {
        Element moduleElement = configuration.createElement("module");
        Element paramElement = configuration.createElement("parameters");
        String className = module.getClass().getName();
        try {
          moduleElement.setAttribute("class", className);

          ParameterSet moduleParameters = getModuleParameters(module.getClass());
          if (moduleParameters != null) {
            moduleParameters.setSkipSensitiveParameters(skipSensitive);
            moduleParameters.saveValuesToXML(paramElement);
          }

          // only add if there is no exception
          modulesElement.appendChild(moduleElement);
          moduleElement.appendChild(paramElement);
        } catch (Exception ex) {
          logger.log(Level.WARNING,
              "Error while saving module parameters to config. Skipping class %s".formatted(
                  className));
        }
      }

      // save encryption key to local config only
      // ATTENTION: this should to be written after all other configs
      final SimpleParameterSet encSet = new SimpleParameterSet(globalEncrypter);
      encSet.setSkipSensitiveParameters(skipSensitive);
      encSet.saveValuesToXML(prefElement);

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

      XMLUtils.saveToFile(file, configuration);

      // make user home config file invisible on windows
      if ((!skipSensitive) && (System.getProperty("os.name").toLowerCase().contains("windows"))) {
        Files.setAttribute(file.toPath(), "dos:hidden", Boolean.TRUE, LinkOption.NOFOLLOW_LINKS);
      }

      logger.info("Saved configuration to file " + file);
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      throw new IOException(e);
    }
  }

  @Override
  public MZminePreferences getPreferences() {
    return preferences;
  }

  @Override
  @NotNull
  public List<File> getLastProjects() {
    return lastProjects.getValue();
  }

  @Override
  public int getNumOfThreads() {
    return preferences.getValue(MZminePreferences.numOfThreads);
  }

  @Override
  @NotNull
  public FileNameListSilentParameter getLastProjectsParameter() {
    return lastProjects;
  }

  @Override
  public SimpleColorPalette getDefaultColorPalette() {
    SimpleColorPalette p = preferences.getParameter(MZminePreferences.defaultColorPalette)
        .getValue();
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
  public SimpleColorPalette getDefaultPaintScalePalette() {
    SimpleColorPalette p = preferences.getParameter(MZminePreferences.defaultPaintScale).getValue();
    if (!p.isValid()) {
      logger.warning(
          "Current default paint scale set in preferences is invalid. Returning standard "
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

  @Override
  public Themes getTheme() {
    return getPreferences().getValue(MZminePreferences.theme);
  }

  @Override
  public boolean isDarkMode() {
    Boolean darkMode = preferences.isDarkMode();
    return darkMode != null && darkMode;
  }

  @Override
  public ImageNormalization getImageNormalization() {
    final ImageNormalization normalization = preferences.getParameter(
        MZminePreferences.imageNormalization).getValue();
    return normalization != null ? normalization : ImageNormalization.NO_NORMALIZATION;
  }

  @Override
  public PaintScaleTransform getImageTransformation() {
    final PaintScaleTransform transformation = preferences.getParameter(
        MZminePreferences.imageTransformation).getValue();
    return transformation != null ? transformation : PaintScaleTransform.LINEAR;
  }

  @Override
  public File getMsConvertPath() {
    synchronized (MSConvert.class) {
      File path = preferences.getValue(MZminePreferences.msConvertPath);
      if (path == null || !MSConvert.validateMsConvertPath(path)) {
        path = MSConvert.discoverMsConvertPath();
        preferences.setParameter(MZminePreferences.msConvertPath, path);
      }
      return path;
    }
  }

  @Override
  public synchronized @NotNull File getLogFile() {
    if (logFile != null) {
      return logFile;
    }

    logFile = LoggerUtils.getLogFile();
    if (logFile != null) {
      logger.fine("Found log file: " + logFile.getAbsolutePath());
      return logFile;
    }
    // just use the first log file? Or maybe evaluate the log files based on creation date
    logger.finest("No log file found. Using default log file.");
    logFile = new File(FileUtils.getUserDirectory(), "mzmine_0_0.log");
    return logFile;
  }
}
