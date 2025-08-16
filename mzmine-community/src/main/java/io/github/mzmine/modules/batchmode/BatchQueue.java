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

package io.github.mzmine.modules.batchmode;

import com.vdurmont.semver4j.Semver;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.change_outfiles.ChangeOutputFilesUtils;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterParameters;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.collections.CollectionUtils;
import io.github.mzmine.util.io.SemverVersionReader;
import io.github.mzmine.util.javafx.ArrayObservableList;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Batch steps queue
 */
public class BatchQueue extends ArrayObservableList<MZmineProcessingStep<MZmineProcessingModule>> {

  public static final Logger logger = Logger.getLogger(BatchQueue.class.getName());
  // attr of the main xmlElement
  public static final String XML_MZMINE_VERSION_ATTR = "mzmine_version";
  // Batch step element name.
  private static final String BATCH_STEP_ELEMENT = "batchstep";
  // Method element name.
  private static final String METHOD_ELEMENT = "method";
  private static final String MODULE_NAME_ATTR = "module_name";
  public static final String MODULE_VERSION_ATTR = "parameter_version";

  /**
   * De-serialize from XML.
   *
   * @param xmlElement        the element that holds the XML.
   * @param skipUnkownModules skipping unknown modules is discouraged in batch mode. In GUI mode
   *                          errors can be seen.
   * @return the de-serialized value.
   */
  public static BatchQueue loadFromXml(final Element xmlElement,
      @NotNull final List<String> errorMessages, boolean skipUnkownModules) {
    Semver batchMzmineVersion = null;
    final String mzmineVersionError;
    Semver mzmineVersion = SemverVersionReader.getMZmineVersion();
    if (xmlElement.hasAttribute(XML_MZMINE_VERSION_ATTR)) {
      batchMzmineVersion = new Semver(xmlElement.getAttribute(XML_MZMINE_VERSION_ATTR));

      int versionCompare = batchMzmineVersion.compareTo(mzmineVersion);
      String vstring = switch (versionCompare) {
        case -1 -> "an older";
        case 1 -> "a newer";
        case 0 -> "the same";
        default -> "";
      };
      String msg = "The batch file was created with %s version of mzmine %s (this version is %s).".formatted(
          vstring, batchMzmineVersion, mzmineVersion);
      logger.info(msg);
      //
      if (versionCompare != 0) {
        mzmineVersionError = msg;
      } else {
        // same version no error
        mzmineVersionError = null;
      }
    } else {
      mzmineVersionError = "Batch was created with an older version of mzmine prior to MZmine 3.4.0 (this version is %s).".formatted(
          mzmineVersion);
      logger.warning(mzmineVersionError);
    }

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
          String batchVersionStr =
              batchMzmineVersion == null ? "of unspecified version" : batchMzmineVersion.toString();

          String warning = """
              Module not found for class %s (maybe recreate the batch file).
              Current MZmine version: %s (batch was created with MZmine %s)""".formatted(methodName,
              mzmineVersion, batchVersionStr);

          errorMessages.add(warning);
          logger.warning(warning);
          if (!skipUnkownModules) {
            throw new UnknownModuleNameException(methodName, e);
          }
        }
      }
      if (moduleFound != null) {
        // Get parameters and add step to queue.
        final ParameterSet parameterSet = MZmineCore.getConfiguration()
            .getModuleParameters(moduleFound.getClass());
        final ParameterSet methodParams = parameterSet.cloneParameterSet();
        int currentVersion = parameterSet.getVersion();

        int batchStepVersion = 1; // default before introduction is 1
        // check version introduced in MZmine 3.4.0
        if (!stepElement.hasAttribute(MODULE_VERSION_ATTR)) {
          noModuleVersion = true;
        } else {
          // the actual step version saved to batch file
          batchStepVersion = Integer.parseInt(stepElement.getAttribute(MODULE_VERSION_ATTR));
        }

        if (batchStepVersion < currentVersion) {
          // this mzmine is newer, join find potential messages for user
          String versionMessages = IntStream.range(batchStepVersion + 1, currentVersion + 1)
              .mapToObj(parameterSet::getVersionMessage).filter(Objects::nonNull)
              .collect(Collectors.joining(" "));
          if (!versionMessages.isBlank()) {
            versionMessages += "\n"; // add additional break after long version messages
          }

          errorMessages.add(
              "'%s' step uses outdated parameters. %s".formatted(moduleFound.getName(),
                  versionMessages));
        } else if (batchStepVersion > currentVersion) {
          errorMessages.add("'%s' step uses parameters from a newer mzmine version.".formatted(
              moduleFound.getName()));
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
    xmlElement.setAttribute(XML_MZMINE_VERSION_ATTR,
        SemverVersionReader.getMZmineVersion().toString());

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
        stepElement.setAttribute(MODULE_NAME_ATTR, step.getModule().getName());
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
  public boolean setImportFiles(final File[] allDataFiles, final @Nullable File metadataFile,
      final File[] allLibraryFiles) throws IllegalStateException {
    if (isEmpty()) {
      logger.severe(
          "Batch queue is empty. Maybe there was an error while parsing the batch file. Better to recreate the batch in this mzmine version.");
      return false;
    }

    var module = MZmineCore.getModuleInstance(AllSpectralDataImportModule.class);
    var potentialErrorMessage = """
        Could not change input data files in batch. When running batch and changing the data input, the first step in the batch needs to be the %s module.""".formatted(
        module.getName());

    // preconditions
    // cannot have data import steps after first step
    var extraImportSteps = stream().skip(1).map(MZmineProcessingStep::getModule)
        .filter(m -> m.getModuleCategory() == MZmineModuleCategory.RAWDATAIMPORT).toList();
    if (!extraImportSteps.isEmpty()) {
      logger.severe(potentialErrorMessage);
      var message = "There were too many raw data import modules in the batch list:"
          + extraImportSteps.stream().map(MZmineModule::getName).collect(Collectors.joining("\n"));
      logger.severe(message);
      return false;
    }

    // data import needs to be first step
    MZmineProcessingStep<MZmineProcessingModule> currentStep = this.getFirst();
    if (currentStep.getModule() instanceof AllSpectralDataImportModule) {
      // use existing first import step
      ParameterSet importParameters = currentStep.getParameterSet();
      try {
        if (allDataFiles != null) {
          importParameters.setParameter(AllSpectralDataImportParameters.fileNames, allDataFiles);
        }
        importParameters.setParameter(AllSpectralDataImportParameters.metadataFile,
            metadataFile != null, metadataFile);
        if (allLibraryFiles != null) {
          importParameters.setParameter(SpectralLibraryImportParameters.dataBaseFiles,
              allLibraryFiles);
        }
        return true;
      } catch (Exception ex) {
        logger.log(Level.WARNING,
            "Could not change input data files in batch. When running batch and changing the data input, the first step in the batch needs to be the all spectral data import module.",
            ex);
        return false;
      }
    } else {
      // first step was not all MS data import step
      // cannot be other data import step - unsupported for now
      if (currentStep.getModule().getModuleCategory() == MZmineModuleCategory.RAWDATAIMPORT) {
        logger.severe(potentialErrorMessage);
        return false;
      }
      // if a new step is added - allDataFiles are required non null
      if (allDataFiles == null || allDataFiles.length == 0) {
        var msg = "Trying to set an empty list of files in data import. Please specify files to import.";
        logger.severe(msg);
        return false;
      }

      ParameterSet parameters = AllSpectralDataImportParameters.create(
          // use the last set value, not the preference
          ConfigService.getConfiguration().getModuleParameters(AllSpectralDataImportModule.class)
              .getValue(AllSpectralDataImportParameters.applyVendorCentroiding), //
          allDataFiles, metadataFile, allLibraryFiles);
      addFirst(new MZmineProcessingStepImpl<>(module, parameters));

      return true;
    }
  }

  /**
   * Change all output files to this base file by adding a module specific suffix
   */
  public void setOutputBaseFile(final String overrideOutBaseFile) {
    logger.info("Changing all output files with path and base filename: " + overrideOutBaseFile);
    File baseFile = new File(overrideOutBaseFile);

    ChangeOutputFilesUtils.applyTo(this, baseFile);
    logger.info("Done changing output file paths.");
  }

  /**
   * @param moduleClass filter steps for this module class
   * @return stream of ParameterSets of specific steps
   */
  @NotNull
  public Stream<ParameterSet> streamStepParameterSets(
      @NotNull final Class<? extends MZmineModule> moduleClass) {
    return stream().filter(
            step -> step.getModule().getClass().getName().equals(moduleClass.getName()))
        .map(MZmineProcessingStep::getParameterSet).filter(Objects::nonNull);
  }

  /**
   * @param moduleClass filter steps for this module class
   * @return stream of ParameterSets of specific steps
   */
  @NotNull
  public Optional<ParameterSet> findFirst(
      @NotNull final Class<? extends MZmineModule> moduleClass) {
    return streamStepParameterSets(moduleClass).findFirst();
  }
}

