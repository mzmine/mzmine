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

package io.github.mzmine.modules.tools.batchwizard.io;

import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.util.StringUtils;
import io.github.mzmine.util.XMLUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
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
public class WizardSequenceIOUtils {

  private static final Logger logger = Logger.getLogger(WizardSequenceIOUtils.class.getName());
  private static final String WIZARD_EXTENSION = "mzmwizard";
  public static final ExtensionFilter FILE_FILTER = new ExtensionFilter("mzmine wizard preset",
      "*." + WIZARD_EXTENSION);
  private static final String PART_TAG = "wiz_part";
  private static final String ELEMENT_TAG = "wizard";
  private static final String PART_ATTRIBUTE = "part";
  private static final String PRESET_ATTRIBUTE = "preset";

  private WizardSequenceIOUtils() {
  }

  public static void saveToFile(final List<WizardStepParameters> workflow, final File file,
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

      XMLUtils.saveToFile(file, configuration);

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
   * @param file wizard preset xml file
   * @return a new list of presets for each defined part - empty on error or if nothing was defined
   * @throws IOException when loading file
   */
  public static @NotNull WizardSequence loadFromFile(final File file) throws IOException {
    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document configuration = dBuilder.parse(file);
      XPathFactory xPathFactory = XPathFactory.newInstance();
      XPath xpath = xPathFactory.newXPath();

      logger.finest("Loading wizard parameters from file " + file.getAbsolutePath());
      // use all presets from the WizardTab
      // find the one with the unique ID and part
      // copy all parameters - this way, even new parameters are handled with their default value
      // result
      WizardSequence sequence = new WizardSequence();

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

          // load preset parameters and add to sequence
          part.getParameterFactory(uniqeId).ifPresent(factory -> {
            var preset = factory.create();
            preset.loadValuesFromXML(xmlNode);
            sequence.add(preset);
          });
        } catch (Exception e) {
          logger.warning("Cannot set preset " + uniquePresetId + " to part " + part
                         + ". Maybe it was renamed. " + e.getMessage());
        }
      }

      sequence.sort();

      logger.info("Loaded wizard parameters from file " + file);
      return sequence;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  /**
   * Shows a file chooser and loads presets from file - might be only parts of the whole workflow
   *
   * @return a new list of presets for each defined part - empty on error or if nothing was defined
   */
  public static @NotNull WizardSequence chooseAndLoadFile() {
    File prefPath = getWizardSettingsPath();
    FileChooser chooser = new FileChooser();
    chooser.setInitialDirectory(prefPath);
    chooser.getExtensionFilters().add(FILE_FILTER);
    chooser.setSelectedExtensionFilter(FILE_FILTER);
    File file = chooser.showOpenDialog(null);
    if (file != null) {
      try {
        return WizardSequenceIOUtils.loadFromFile(file);
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
   * @return A list of local wizard workflows
   */
  public static @NotNull List<LocalWizardSequenceFile> findAllLocalPresetFiles() {
    File path = getWizardSettingsPath();
    if (path == null) {
      return List.of();
    }

    return FileAndPathUtil.findFilesInDir(path, FILE_FILTER, false).stream()
        .filter(Objects::nonNull).flatMap(Arrays::stream).filter(Objects::nonNull).map(file -> {
          try {
            WizardSequence presets = WizardSequenceIOUtils.loadFromFile(file);
            return new LocalWizardSequenceFile(file, presets);
          } catch (IOException e) {
            logger.warning("Could not import wizard preset file " + file.getAbsolutePath());
            return null;
          }
        }).filter(Objects::nonNull).sorted(Comparator.comparing(LocalWizardSequenceFile::getName))
        .toList();
  }

  /**
   * User/.mzmine/wizard/
   */
  @Nullable
  public static File getWizardSettingsPath() {
    File prefPath = FileAndPathUtil.getMzmineDir();
    if (prefPath == null) {
      logger.warning("Cannot find parameters default location in user folder");
    } else {
      prefPath = new File(prefPath, "wizard");
      FileAndPathUtil.createDirectory(prefPath);
    }
    return prefPath;
  }

  public static boolean isWizardFile(final String file) {
    return StringUtils.hasValue(file) && file.toLowerCase().strip().endsWith(WIZARD_EXTENSION);
  }

  public static boolean copyToUserDirectory(final File file) {
    try {
      File wizDir = getWizardSettingsPath();
      if (wizDir == null) {
        return false;
      }
      Files.copy(file.toPath(), wizDir.toPath().resolve(file.getName()),
          StandardCopyOption.REPLACE_EXISTING);
      return true;
    } catch (IOException e) {
      return false;
    }
  }
}
