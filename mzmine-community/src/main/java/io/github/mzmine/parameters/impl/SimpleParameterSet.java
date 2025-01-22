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

package io.github.mzmine.parameters.impl;

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterContainer;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.parametertypes.EncryptionKeyParameter;
import io.github.mzmine.parameters.parametertypes.HiddenParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.util.ExitCode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.ButtonType;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Simple storage for the parameters. A typical MZmine module will inherit this class and define the
 * parameters for the constructor.
 */
public class SimpleParameterSet implements ParameterSet {

  public static final String parameterElement = "parameter";
  private static final String nameAttribute = "name";
  private static final Logger logger = Logger.getLogger(MZmineCore.class.getName());
  private final BooleanProperty parametersChangeProperty = new SimpleBooleanProperty();
  protected Parameter<?>[] parameters;
  protected String helpUrl = null;
  private String moduleNameAttribute;
  private boolean skipSensitiveParameters = false;

  public SimpleParameterSet() {
    this(new Parameter<?>[0], null);
  }

  public SimpleParameterSet(Parameter<?>... parameters) {
    this(parameters, null);
  }

  public SimpleParameterSet(String onlineHelpUrl, Parameter<?>... parameters) {
    this(parameters, onlineHelpUrl);
  }

  public SimpleParameterSet(Parameter<?>[] parameters, String onlineHelpUrl) {
    this.parameters = parameters;
    this.helpUrl = onlineHelpUrl;
  }

  @Override
  public Parameter<?>[] getParameters() {
    return parameters;
  }

  @Override
  public void setSkipSensitiveParameters(boolean skipSensitiveParameters) {
    this.skipSensitiveParameters = skipSensitiveParameters;
    for (Parameter<?> parameter : parameters) {
      if (parameter instanceof ParameterContainer) {
        ((ParameterContainer) parameter).setSkipSensitiveParameters(skipSensitiveParameters);
      }
    }
  }

  @Override
  public Map<String, Parameter<?>> loadValuesFromXML(Element xmlElement) {
    var nameParameterMap = getNameParameterMap();
    // cannot use getElementsByTagName, this goes recursively through all levels
    // finding nested ParameterSets
//    NodeList list = xmlElement.getElementsByTagName(parameterElement);

    Map<String, Parameter<?>> loadedParameters = HashMap.newHashMap(nameParameterMap.size());

    var childNodes = xmlElement.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      if (!(childNodes.item(i) instanceof Element nextElement) || !parameterElement.equals(
          nextElement.getTagName())) {
        continue;
      }

      String paramName = nextElement.getAttribute(nameAttribute);
      Parameter<?> param = nameParameterMap.get(paramName);
      if (param != null) {
        try {
          param.loadValueFromXML(nextElement);
          // keep track of all parameters that were actually loaded - this means that some may be missing
          loadedParameters.put(param.getName(), param);
        } catch (Exception e) {
          logger.log(Level.WARNING, "Error while loading parameter values for " + param.getName(),
              e);
        }
      } else {
        // load config reads the EncryptionKeyParameter in a second go
        if (nameParameterMap.values().stream()
            .noneMatch(p -> p instanceof EncryptionKeyParameter)) {
          logger.warning(
              "Cannot find parameter of name %s in ParameterSet %s. This might indicate changes of the parameterset and parameter types".formatted(
                  paramName, getClass().getName()));
        }
      }
    }
    handleLoadedParameters(loadedParameters);
    return loadedParameters;
  }

  @Override
  public void saveValuesToXML(Element xmlElement) {
    Document parentDocument = xmlElement.getOwnerDocument();
    for (Parameter<?> param : parameters) {
      if (skipSensitiveParameters && param.isSensitive()) {
        continue;
      }
      Element paramElement = parentDocument.createElement(parameterElement);
      paramElement.setAttribute(nameAttribute, param.getName());
      xmlElement.appendChild(paramElement);
      param.saveValueToXML(paramElement);

    }
  }

  /**
   * Represent method's parameters and their values in human-readable format
   */
  @Override
  public String toString() {

    StringBuilder s = new StringBuilder();
    for (int i = 0; i < parameters.length; i++) {

      Parameter<?> param = parameters[i];
      Object value = param.getValue();

      if (value == null) {
        continue;
      }

      s.append(param.getName());
      s.append(": ");
      if (value.getClass().isArray()) {
        s.append(Arrays.toString((Object[]) value));
      } else {
        s.append(value);
      }
      if (i < parameters.length - 1) {
        s.append(", ");
      }
    }
    return s.toString();
  }

  @Override
  public ParameterSet cloneParameterSet() {
    return cloneParameterSet(false);
  }

  /**
   * Make a deep copy
   */
  @Override
  public ParameterSet cloneParameterSet(boolean keepSelection) {

    // Make a deep copy of the parameters
    Parameter<?>[] newParameters = new Parameter[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      if (keepSelection && parameters[i] instanceof RawDataFilesParameter rfp) {
        newParameters[i] = rfp.cloneParameter(keepSelection);
      } else {
        newParameters[i] = parameters[i].cloneParameter();
      }
    }

    try {
      /*
       * Do not create a new instance of SimpleParameterSet, but instead clone the runtime class of
       * this instance - runtime type may be inherited class. This is important in order to keep the
       * proper behavior of showSetupDialog(xxx) method for cloned classes.
       */
      SimpleParameterSet newSet = this.getClass().getDeclaredConstructor().newInstance();
      newSet.parameters = newParameters;
      newSet.setSkipSensitiveParameters(skipSensitiveParameters);
      newSet.setModuleNameAttribute(this.getModuleNameAttribute());
      newSet.helpUrl = helpUrl;

      return newSet;
    } catch (Throwable e) {
      logger.log(Level.WARNING, "While cloning parameters: " + e.getMessage(), e);
      e.printStackTrace();
      return null;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Parameter<?>> T getParameter(T parameter) {
    for (Parameter<?> p : parameters) {
      if (p.getName().equals(parameter.getName())) {
        return (T) p;
      }
    }
    throw new IllegalArgumentException("Parameter " + parameter.getName() + " does not exist");
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    assert Platform.isFxApplicationThread();

    if ((parameters == null) || (parameters.length == 0)) {
      return ExitCode.OK;
    }
    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this, null);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages,
      final boolean skipRawDataAndFeatureListParameters) {
    boolean allParametersOK = true;
    for (Parameter<?> p : parameters) {
      // this is done in batch mode where no data is loaded when the parameters are checked
      if (skipRawDataAndFeatureListParameters && (p instanceof RawDataFilesParameter
                                                  || p instanceof FeatureListsParameter)) {
        continue;
      }

      boolean pOK = p.checkValue(errorMessages);
      if (!pOK) {
        allParametersOK = false;
      }
      if (p instanceof HiddenParameter<?> hidden) {
        p = hidden.getEmbeddedParameter();
      }

      if (p instanceof RawDataFilesParameter rfp) {
        pOK = checkRawDataFileIonMobilitySupport(rfp.getValue().getMatchingRawDataFiles(),
            errorMessages);
        rfp.getValue().resetSelection(); // has to be reset after evaluation
      } else if (p instanceof FeatureListsParameter) {
        FeatureList[] lists = ((FeatureListsParameter) p).getValue().getMatchingFeatureLists();
        Set<RawDataFile> files = new HashSet<>();
        Arrays.stream(lists).map(FeatureList::getRawDataFiles)
            .forEach(listFiles -> listFiles.forEach(file -> files.add(file)));
        pOK = checkRawDataFileIonMobilitySupport(files.toArray(new RawDataFile[0]), errorMessages);
      }
      if (!pOK) {
        allParametersOK = false;
      }
    }
    return allParametersOK;
  }

  public boolean checkRawDataFileIonMobilitySupport(RawDataFile[] rawDataFiles,
      Collection<String> errorMessages) {
    boolean onlyImsFiles = true;
    boolean containsImsFile = false;
    List<String> nonImsFilesList = new ArrayList<>();

    for (RawDataFile file : rawDataFiles) {
      if (!(file instanceof IMSRawDataFile)) {
        onlyImsFiles = false;
        nonImsFilesList.add("Non-ion mobility spectrometry files: " + file.getName());
      } else {
        containsImsFile = true;
      }
    }

    Map<String, Boolean> showMsgMap = MZmineCore.getConfiguration().getPreferences()
        .getParameter(MZminePreferences.imsModuleWarnings).getValue();
    String className = this.getClass().getName();
    Boolean showMsg = showMsgMap.getOrDefault(className, true);

    if (containsImsFile && getIonMobilitySupport() == IonMobilitySupport.UNTESTED) {
      logger.warning(
          "This module has not been tested with ion mobility data files. This could lead to unexpected results.");
      if (showMsg) {
        return MZmineCore.getDesktop()
                   .createAlertWithOptOut("Compatibility warning", "Untested compatibility",
                       "This module has not been tested with ion mobility data files. This could lead "
                       + "to unexpected results. Do you want to continue anyway?",
                       "Do not show again",
                       optOut -> showMsgMap.put(this.getClass().getName(), !optOut))
               == ButtonType.YES;
      }
      return true;
    } else if (containsImsFile && getIonMobilitySupport() == IonMobilitySupport.RESTRICTED) {
      logger.warning(
          "This module has certain restrictions when processing ion mobility data files. This"
          + " could lead to unexpected results");
      if (showMsg) {
        return MZmineCore.getDesktop()
                   .createAlertWithOptOut("Compatibility warning", "Restricted compatibility",
                       getRestrictedIonMobilitySupportMessage(), "Do not show again",
                       optOut -> showMsgMap.put(this.getClass().getName(), !optOut))
               == ButtonType.YES;
      }
    } else if (!onlyImsFiles && getIonMobilitySupport() == IonMobilitySupport.ONLY) {
      logger.warning(
          "This module is designed for ion mobility data only. Cannot process non-ion mobility files.");
      errorMessages.add(
          "This module is designed for ion mobility data only. Cannot process non-ion mobility files.");
      errorMessages.addAll(nonImsFilesList);
      return false;
    } else if (containsImsFile && getIonMobilitySupport() == IonMobilitySupport.UNSUPPORTED) {
      logger.warning("This module does not support ion mobility data.");
      errorMessages.add("This module does not support ion mobility data.");

      boolean returnVal = DialogLoggerUtil.showDialogYesNo("Untested IMS support",
          "This module does not support ion mobility data. This will lead to unexpected "
          + "results. Do you want to continue anyway?");
      if (!returnVal) {
        errorMessages.addAll(nonImsFilesList);
      }
      return returnVal;
    } // dont have to check for IonMobilitySupport.SUPPORTED

    return true;
  }

  /**
   * This message is displayed when a ion mobility file is processed with this module without it
   * explicitly supporting ion mobility data. This method can be overridden to display a more
   * specific user information on the expected outcome.
   *
   * @return The message.
   */
  public String getRestrictedIonMobilitySupportMessage() {
    return "This module has certain restrictions when processing ion mobility data files. This "
           + "could lead to unexpected results. Do you want to continue anyway?";
  }

  /**
   * Returns BooleanProperty which value is changed when some parameter of this ParameterSet is
   * changed. It is useful to perform operations directly dependant on the components corresponding
   * to this ParameterSet (e.g. TextField of a parameter is changed -> preview plot is updated).
   *
   * @return BooleanProperty signalizing a change of any parameter of this ParameterSet
   */
  public BooleanProperty parametersChangeProperty() {
    return parametersChangeProperty;
  }

  @Override
  public @Nullable String getOnlineHelpUrl() {
    return helpUrl;
  }

  @Override
  public String getModuleNameAttribute() {
    return moduleNameAttribute;
  }

  @Override
  public void setModuleNameAttribute(String moduleName) {
    this.moduleNameAttribute = moduleName;
  }
}
