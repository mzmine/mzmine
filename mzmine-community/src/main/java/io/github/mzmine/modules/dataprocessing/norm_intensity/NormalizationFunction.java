/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import io.github.mzmine.util.XMLUtils;
import java.time.LocalDateTime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Function that returns a normalization factor for specific feature coordinates.
 */
public interface NormalizationFunction extends UniqueIdSupplier {

  String XML_FUNCTION_ELEMENT = "normalizationFunction";
  String XML_FUNCTION_TYPE_ATTR = "type";
  String XML_ACQUISITION_TIMESTAMP_ATTR = "acquisitionTimestamp";

  @NotNull RawDataFilePlaceholder rawDataFilePlaceholder();

  @NotNull LocalDateTime acquisitionTimestamp();

  double getFactor(@NotNull Double mz, @NotNull Float rt);

  void saveToXML(@NotNull Element functionElement);

  default @Nullable RawDataFile getRawDataFile() {
    return rawDataFilePlaceholder().getMatchingFile();
  }

  static void appendFunctionElement(final @NotNull Element parentElement,
      final @NotNull NormalizationFunction normalizationFunction) {
    final Document document = parentElement.getOwnerDocument();
    final Element functionElement = document.createElement(XML_FUNCTION_ELEMENT);
    normalizationFunction.saveToXML(functionElement);
    parentElement.appendChild(functionElement);
  }

  static @NotNull NormalizationFunction loadFromXML(final @NotNull Element functionElement) {
    final String functionType = XMLUtils.requireAttribute(functionElement, XML_FUNCTION_TYPE_ATTR);
    return switch (functionType) {
      case FactorNormalizationFunction.XML_TYPE ->
          FactorNormalizationFunction.loadFromXML(functionElement);
      case StandardCompoundNormalizationFunction.XML_TYPE ->
          StandardCompoundNormalizationFunction.loadFromXML(functionElement);
      case InterpolatedNormalizationFunction.XML_TYPE ->
          InterpolatedNormalizationFunction.loadFromXML(functionElement);
      default -> throw new IllegalArgumentException(
          "Unsupported normalization function type: " + functionType);
    };
  }

  static void saveAcquisitionTimestamp(final @NotNull Element functionElement,
      final @NotNull LocalDateTime acquisitionTimestamp) {
    functionElement.setAttribute(XML_ACQUISITION_TIMESTAMP_ATTR, acquisitionTimestamp.toString());
  }

  static @NotNull LocalDateTime loadAcquisitionTimestamp(final @NotNull Element functionElement) {
    return LocalDateTime.parse(
        XMLUtils.requireAttribute(functionElement, XML_ACQUISITION_TIMESTAMP_ATTR));
  }

  static @NotNull Element findDirectChild(final @NotNull Element parent,
      final @NotNull String tagName) {
    final NodeList matchingNodes = parent.getElementsByTagName(tagName);
    for (int i = 0; i < matchingNodes.getLength(); i++) {
      final Node node = matchingNodes.item(i);
      if (node.getParentNode() == parent && node instanceof final Element element) {
        return element;
      }
    }
    throw new IllegalArgumentException(
        "Missing required child element '" + tagName + "' in " + parent.getTagName());
  }

}
