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

package io.github.mzmine.modules.tools.batchwizard.io;

import io.github.mzmine.modules.tools.batchwizard.BatchWizardTab;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepPreset;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Import and export the batch wizard parameters
 */
public class WizardWorkflowIOUtils {

  private static final Logger logger = Logger.getLogger(WizardWorkflowIOUtils.class.getName());
  public static final ExtensionFilter FILE_FILTER = new ExtensionFilter("MZmine wizard preset",
      "*.mzmwizard");
  private static final String PART_TAG = "wiz_part";
  private static final String ELEMENT_TAG = "wizard";
  private static final String PART_ATTRIBUTE = "part";
  private static final String PRESET_ATTRIBUTE = "preset";

  private WizardWorkflowIOUtils() {
  }

  public static void saveToFile(final List<WizardStepPreset> workflow, final File file,
      final boolean skipSensitive) throws IOException {
    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

      Document configuration = dBuilder.newDocument();
      Element configRoot = configuration.createElement(ELEMENT_TAG);
      configuration.appendChild(configRoot);

      for (var step : workflow) {
        Element moduleElement = configuration.createElement(PART_TAG);
        moduleElement.setAttribute(PART_ATTRIBUTE, step.getPart().name());
        moduleElement.setAttribute(PRESET_ATTRIBUTE, step.getUniquePresetId());
        // save parameters
        step.setSkipSensitiveParameters(skipSensitive);
        step.saveValuesToXML(moduleElement);
        configRoot.appendChild(moduleElement);
      }

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
      boolean isWindowsHiddenFile = false;
      if (file.exists() && System.getProperty("os.name").toLowerCase().contains("windows")) {
        isWindowsHiddenFile = (Boolean) Files.getAttribute(file.toPath(), "dos:hidden",
            LinkOption.NOFOLLOW_LINKS);
        if (isWindowsHiddenFile) {
          Files.setAttribute(file.toPath(), "dos:hidden", Boolean.FALSE, LinkOption.NOFOLLOW_LINKS);
        }
      }

      StreamResult result = new StreamResult(new FileOutputStream(file));
      DOMSource source = new DOMSource(configuration);
      transformer.transform(source, result);

      // make user home config file invisible on windows
      if ((!skipSensitive) && (System.getProperty("os.name").toLowerCase().contains("windows"))
          && isWindowsHiddenFile) {
        Files.setAttribute(file.toPath(), "dos:hidden", Boolean.TRUE, LinkOption.NOFOLLOW_LINKS);
      }

      logger.info("Saved parameters to file " + file);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  /**
   * Load presets from file - might be only parts of the whole workflow
   *
   * @param file       wizard preset xml file
   * @param allPresets all presets as defined in the {@link BatchWizardTab}
   * @return a new list of presets for each defined part - empty on error or if nothing was defined
   * @throws IOException when loading file
   */
  public static @NotNull WizardSequence loadFromFile(final File file,
      Map<WizardPart, List<WizardStepPreset>> allPresets) throws IOException {
    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document configuration = dBuilder.parse(file);
      XPathFactory factory = XPathFactory.newInstance();
      XPath xpath = factory.newXPath();

      logger.finest("Loading wizard parameters from file " + file.getAbsolutePath());
      // use all presets from the WizardTab
      // find the one with the unique ID and part
      // copy all parameters - this way, even new parameters are handled with their default value
      // result
      WizardSequence workflow = new WizardSequence();

      XPathExpression expr = xpath.compile("//" + ELEMENT_TAG + "/" + PART_TAG);
      NodeList nodes = (NodeList) expr.evaluate(configuration, XPathConstants.NODESET);
      WizardPart part = null;
      String uniquePresetId = null;
      int length = nodes.getLength();
      for (int i = 0; i < length; i++) {
        try {
          Element xmlNode = (Element) nodes.item(i);
          part = WizardPart.valueOf(xmlNode.getAttribute(PART_ATTRIBUTE));
          uniquePresetId = xmlNode.getAttribute(PRESET_ATTRIBUTE);
          final String uniqeId = uniquePresetId;
          // load preset parameters and add to wizard
          allPresets.get(part).stream().filter(preset -> preset.getUniquePresetId().equals(uniqeId))
              .findFirst().ifPresent(preset -> {
                workflow.add(preset);
                preset.loadValuesFromXML(xmlNode);
              });
        } catch (Exception e) {
          logger.warning("Cannot set preset " + uniquePresetId + " to part " + part
              + ". Maybe it was renamed. " + e.getMessage());
        }
      }

      workflow.sort();

      logger.info("Loaded wizard parameters from file " + file);
      return workflow;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  /**
   * Shows a file chooser and loads presets from file - might be only parts of the whole workflow
   *
   * @param allPresets all presets as defined in the {@link BatchWizardTab}
   * @return a new list of presets for each defined part - empty on error or if nothing was defined
   */
  public static @NotNull WizardSequence chooseAndLoadFile(
      final Map<WizardPart, List<WizardStepPreset>> allPresets) {
    File prefPath = getWizardSettingsPath();
    FileChooser chooser = new FileChooser();
    chooser.setInitialDirectory(prefPath);
    chooser.getExtensionFilters().add(FILE_FILTER);
    chooser.setSelectedExtensionFilter(FILE_FILTER);
    File file = chooser.showOpenDialog(null);
    if (file != null) {
      try {
        return WizardWorkflowIOUtils.loadFromFile(file, allPresets);
      } catch (IOException e) {
        logger.log(Level.WARNING, "Cannot read batch wizard presets from " + file.getAbsolutePath(),
            e);

      }
    }
    return new WizardSequence();
  }

  /**
   * Local files are save to {@link #getWizardSettingsPath()}
   *
   * @param ALL_PRESETS all presets so that the correct object can be used
   * @return A list of local wizard workflows
   */
  public static @NotNull List<LocalWizardWorkflowFile> findAllLocalPresetFiles(
      final Map<WizardPart, List<WizardStepPreset>> ALL_PRESETS) {
    File path = getWizardSettingsPath();
    if (path == null) {
      return List.of();
    }

    return FileAndPathUtil.findFilesInDir(path, FILE_FILTER, false).stream()
        .filter(Objects::nonNull).flatMap(Arrays::stream).filter(Objects::nonNull).map(file -> {
          try {
            WizardSequence presets = WizardWorkflowIOUtils.loadFromFile(file, ALL_PRESETS);
            return new LocalWizardWorkflowFile(file, presets);
          } catch (IOException e) {
            logger.warning("Could not import wizard preset file " + file.getAbsolutePath());
            return null;
          }
        }).filter(Objects::nonNull).sorted(Comparator.comparing(LocalWizardWorkflowFile::getName))
        .toList();
  }

  /**
   * User/.mzmine/wizard/
   */
  @Nullable
  public static File getWizardSettingsPath() {
    File prefPath = FileAndPathUtil.getUserSettingsDir();
    if (prefPath == null) {
      logger.warning("Cannot find parameters default location in user folder");
    } else {
      prefPath = new File(prefPath, "wizard");
      FileAndPathUtil.createDirectory(prefPath);
    }
    return prefPath;
  }

}
