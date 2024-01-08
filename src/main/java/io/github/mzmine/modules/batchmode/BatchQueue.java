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

package io.github.mzmine.modules.batchmode;

import com.vdurmont.semver4j.Semver;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterParameters;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.CollectionUtils;
import io.github.mzmine.util.javafx.ArrayObservableList;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Batch steps queue
 */
public class BatchQueue extends ArrayObservableList<MZmineProcessingStep<MZmineProcessingModule>> {

  public static final Logger logger = Logger.getLogger(BatchQueue.class.getName());

  // Batch step element name.
  private static final String BATCH_STEP_ELEMENT = "batchstep";

  // Method element name.
  private static final String METHOD_ELEMENT = "method";
  private static final String MODULE_VERSION_ATTR = "parameter_version";

  // attr of the main xmlElement
  public static final String XML_MZMINE_VERSION_ATTR = "mzmine_version";

  /**
   * De-serialize from XML.
   *
   * @param xmlElement the element that holds the XML.
   * @return the de-serialized value.
   */
  public static BatchQueue loadFromXml(final Element xmlElement,
      @NotNull final List<String> errorMessages) {
    final Semver mzmineVersion;
    final String mzmineVersionError;
    if (xmlElement.hasAttribute(XML_MZMINE_VERSION_ATTR)) {
      mzmineVersion = new Semver(xmlElement.getAttribute(XML_MZMINE_VERSION_ATTR));

      int versionCompare = mzmineVersion.compareTo(MZmineCore.getMZmineVersion());
      String vstring = switch (versionCompare) {
        case -1 -> "an older";
        case 1 -> "a newer";
        case 0 -> "the same";
        default -> "";
      };
      String msg = "The batch file was created with %s version of MZmine%s (this version is %s).".formatted(
          vstring, mzmineVersion, MZmineCore.getMZmineVersion());
      logger.info(msg);
      //
      if (versionCompare != 0) {
        mzmineVersionError = msg;
      } else {
        // same version no error
        mzmineVersionError = null;
      }
    } else {
      mzmineVersionError = "Batch was created with an older version of MZmine prior to MZmine 3.4.0 (this version is %s).".formatted(
          MZmineCore.getMZmineVersion());
      logger.warning(mzmineVersionError);
    }

    // Set the parameter choice for the RowsFilterModule
    String[] choices;
    choices = new String[1];
    choices[0] = "No parameters defined";
    MZmineCore.getConfiguration().getModuleParameters(RowsFilterModule.class)
        .getParameter(RowsFilterParameters.GROUPSPARAMETER).setChoices(choices);

    // Create an empty queue.
    final BatchQueue queue = new BatchQueue();

    // Get the loaded modules.
    final Collection<MZmineModule> allModules = MZmineCore.getAllModules();

    // prior to versioning of batch steps
    boolean noModuleVersion = false;

    // Process the batch step elements.
    final NodeList nodes = xmlElement.getElementsByTagName(BATCH_STEP_ELEMENT);
    final int nodesLength = nodes.getLength();
    for (int i = 0; i < nodesLength; i++) {

      final Element stepElement = (Element) nodes.item(i);
      final String methodName = stepElement.getAttribute(METHOD_ELEMENT);

      logger.fine("Loading batch step: " + methodName);
      // Find a matching module.
      MZmineModule moduleFound = null;
      for (MZmineModule module : allModules) {
        if (module instanceof MZmineProcessingModule && module.getClass().getName()
            .equals(methodName)) {
          moduleFound = module;
          break;
        }
      }

      if (moduleFound == null) {
        try {
          moduleFound = MZmineCore.getModuleInstance(
              (Class<MZmineModule>) Class.forName(methodName));
        } catch (ClassNotFoundException e) {
          logger.warning(
              "Module not found for class " + methodName + " (maybe recreate the batch file)");
          throw new UnknownModuleNameException(methodName, e);
        }
      }
      if (moduleFound != null) {
        // Get parameters and add step to queue.
        final ParameterSet parameterSet = MZmineCore.getConfiguration()
            .getModuleParameters(moduleFound.getClass());
        final ParameterSet methodParams = parameterSet.cloneParameterSet();
        int currentVersion = parameterSet.getVersion();

        // check version introduced in MZmine 3.4.0
        if (!stepElement.hasAttribute(MODULE_VERSION_ATTR)) {
          noModuleVersion = true;
          // version is known to have changed in MZmine 3.4.0
          if (currentVersion > 1) {
            errorMessages.add(
                "'%s' step parameters were changed.".formatted(moduleFound.getName()));
          }
        } else {
          int version = Integer.parseInt(stepElement.getAttribute(MODULE_VERSION_ATTR));
          String diff = switch (Integer.compare(version, currentVersion)) {
            case -1 -> "outdated";
            case 1 -> "newer";
            default -> null;
          };
          if (diff != null) {
            errorMessages.add(
                "'%s' step uses %s parameters.".formatted(moduleFound.getName(), diff));
          }
        }

        methodParams.loadValuesFromXML(stepElement);
        queue.add(
            new MZmineProcessingStepImpl<>((MZmineProcessingModule) moduleFound, methodParams));
      }
    }
    CollectionUtils.dropDuplicatesRetainOrder(errorMessages);

    if ((noModuleVersion || !errorMessages.isEmpty()) && mzmineVersionError != null) {
      errorMessages.add(0, mzmineVersionError);
      errorMessages.add(1, "Check all steps and parameters carefully; then save the batch again.");
    }
    return queue;
  }

  @Override
  public BatchQueue clone() {
    // Clone the parameters.
    final BatchQueue clonedQueue = new BatchQueue();
    for (final MZmineProcessingStep<MZmineProcessingModule> step : this) {
      final ParameterSet parameters = step.getParameterSet();
      final MZmineProcessingStepImpl<MZmineProcessingModule> stepCopy = new MZmineProcessingStepImpl<>(
          step.getModule(), parameters.cloneParameterSet());
      clonedQueue.add(stepCopy);
    }
    return clonedQueue;
  }

  /**
   * Serialize to XML.
   *
   * @param xmlElement the XML element to append to.
   */
  public void saveToXml(final Element xmlElement) {
    // set MZmine version always to the latest
    xmlElement.setAttribute(XML_MZMINE_VERSION_ATTR, MZmineCore.getMZmineVersion().toString());

    final Document document = xmlElement.getOwnerDocument();

    // Process each step.
    for (final MZmineProcessingStep<?> step : this) {

      // Append a new batch step element.
      final Element stepElement = document.createElement(BATCH_STEP_ELEMENT);
      stepElement.setAttribute(METHOD_ELEMENT, step.getModule().getClass().getName());
      xmlElement.appendChild(stepElement);

      // Save parameters.
      final ParameterSet parameters = step.getParameterSet();
      if (parameters != null) {
        // save version, since MZmine 3.4.0
        stepElement.setAttribute(MODULE_VERSION_ATTR, String.valueOf(parameters.getVersion()));
        parameters.saveValuesToXML(stepElement);
      }
    }
  }

  /**
   * Replace all import files in the {@link AllSpectralDataImportModule} - which needs to be the
   * first step in batch
   *
   * @param allDataFiles replaces import files
   * @return true if success, false if not. e.g., if there was no data import step in the batch file
   */
  public boolean setImportFiles(final File[] allDataFiles, final File[] allLibraryFiles)
      throws IllegalStateException {
    MZmineProcessingStep<?> currentStep = get(0);
    ParameterSet importParameters = currentStep.getParameterSet();
    try {
      if (allDataFiles != null) {
        importParameters.getParameter(AllSpectralDataImportParameters.fileNames)
            .setValue(allDataFiles);
      }
      if (allLibraryFiles != null) {
        importParameters.getParameter(SpectralLibraryImportParameters.dataBaseFiles)
            .setValue(allLibraryFiles);
      }
      return true;
    } catch (Exception ex) {
      logger.log(Level.WARNING,
          "Could not change input data files in batch. When running batch and changing the data input, the first step in the batch needs to be the all spectral data import module.",
          ex);
      return false;
    }
  }

}
