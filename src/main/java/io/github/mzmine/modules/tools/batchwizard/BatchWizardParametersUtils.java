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

package io.github.mzmine.modules.tools.batchwizard;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
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

/**
 * Import and export the batch wizard parameters
 */
public class BatchWizardParametersUtils {

  private static final Logger logger = Logger.getLogger(BatchWizardParametersUtils.class.getName());
  private static final String PART_TAG = "wiz_part";
  private static final String ELEMENT_TAG = "wizard";
  private static final String PART_ATTRIBUTE = "part";
  private static final String PRESET_ATTRIBUTE = "preset";

  private BatchWizardParametersUtils() {
  }

  public static void saveToFile(final List<WizardPreset> parts, final File file,
      final boolean skipSensitive) throws IOException {
    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

      Document configuration = dBuilder.newDocument();
      Element configRoot = configuration.createElement(ELEMENT_TAG);
      configuration.appendChild(configRoot);

      for (var part : parts) {
        Element moduleElement = configuration.createElement(PART_TAG);
        moduleElement.setAttribute(PART_ATTRIBUTE, part.part().name());
        moduleElement.setAttribute(PRESET_ATTRIBUTE, part.name());
        // save parameters
        part.parameters().setSkipSensitiveParameters(skipSensitive);
        part.parameters().saveValuesToXML(moduleElement);
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

  public static List<WizardPreset> loadFromFile(final File file) throws IOException {
    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document configuration = dBuilder.parse(file);
      XPathFactory factory = XPathFactory.newInstance();
      XPath xpath = factory.newXPath();

      logger.finest("Loading wizard parameters from file " + file.getAbsolutePath());
      // all presets
      Map<WizardPart, List<WizardPreset>> allPresets = WizardDefaultPresets.createPresets();
      // result
      List<WizardPreset> parts = new ArrayList<>();

      XPathExpression expr = xpath.compile("//" + ELEMENT_TAG + "/" + PART_TAG);
      NodeList nodes = (NodeList) expr.evaluate(configuration, XPathConstants.NODESET);
      WizardPart part = null;
      String presetName = null;
      int length = nodes.getLength();
      for (int i = 0; i < length; i++) {
        try {
          Element xmlNode = (Element) nodes.item(i);
          part = WizardPart.valueOf(xmlNode.getAttribute(PART_ATTRIBUTE));
          presetName = xmlNode.getAttribute(PRESET_ATTRIBUTE);
          final String finalPresetName = presetName;
          // load preset parameters and add to wizard
          allPresets.get(part).stream().filter(preset -> preset.name().equals(finalPresetName))
              .findFirst().ifPresent(preset -> {
                parts.add(preset);
                preset.parameters().loadValuesFromXML(xmlNode);
              });
        } catch (Exception e) {
          logger.warning(
              "Cannot set preset " + presetName + " to part " + part + ". Maybe it was renamed. "
                  + e.getMessage());
        }
      }

      // make sure that all parts are loaded... otherwise resort to the first (default) preset for each missing
      parts.sort(Comparator.comparingInt(value -> value.part().ordinal()));
      WizardPart[] sequence = WizardPart.values();
      for (int i = 0; i < sequence.length; i++) {
        WizardPart sequencePart = sequence[i];
        // out of range or parts do not match?
        if (i >= parts.size() || sequencePart != parts.get(i).part()) {
          WizardPreset defaultPreset = allPresets.get(sequencePart).get(0);
          parts.add(i, defaultPreset);
        }
      }

      logger.info("Loaded wizard parameters from file " + file);
      return parts;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

}
