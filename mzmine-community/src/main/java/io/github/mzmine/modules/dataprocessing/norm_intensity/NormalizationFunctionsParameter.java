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

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.util.XMLUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.Label;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NormalizationFunctionsParameter implements
    UserParameter<IntensityNormalizationSummary, Label> {

  private static final Logger logger = Logger.getLogger(
      NormalizationFunctionsParameter.class.getName());

  @NotNull
  private IntensityNormalizationSummary summary = new IntensityNormalizationSummary(
      List.of());

  @Override
  public @NotNull String getName() {
    return "Normalization functions";
  }

  @Override
  public @NotNull String getDescription() {
    return "Stores per-file normalization functions used to normalize this feature list. Summary contains multiple steps, applied in sequence.";
  }

  @Override
  public @NotNull Label createEditingComponent() {
    return FxLabels.newLabel("This parameter is only used for project save/load.");
  }

  @Override
  public void setValueFromComponent(final @NotNull Label component) {
    // no UI component editing for hidden parameter
  }

  @Override
  public void setValueToComponent(final @NotNull Label component,
      final @Nullable IntensityNormalizationSummary newValue) {
    // no UI component editing for hidden parameter
  }

  @Override
  public @NotNull UserParameter<IntensityNormalizationSummary, Label> cloneParameter() {
    final NormalizationFunctionsParameter clonedParameter = new NormalizationFunctionsParameter();
    clonedParameter.setValue(summary.copy());
    return clonedParameter;
  }

  /**
   * @return defensive copy of summary
   */
  @Override
  public @NotNull IntensityNormalizationSummary getValue() {
    return summary.copy();
  }

  @Override
  public void setValue(final @Nullable IntensityNormalizationSummary newValue) {
    summary = requireNonNullElse(newValue, IntensityNormalizationSummary.EMPTY);
  }

  @Override
  public boolean checkValue(final @NotNull Collection<String> errorMessages) {
    return true;
  }

  @Override
  public boolean valueEquals(final @Nullable Parameter<?> that) {
    if (!(that instanceof final NormalizationFunctionsParameter other)) {
      return false;
    }

    return summary.equals(other.summary);
  }

  @Override
  public void loadValueFromXML(final @NotNull Element xmlElement) {
    ArrayList<RawFileNormalizationFunction> functions = new ArrayList<>();
    ArrayList<String> messages = new ArrayList<>();

    final Element functionsElement;
    try {
      functionsElement = XMLUtils.findChildElement(xmlElement, "functions");
    } catch (Exception e) {
      // no valid xml
      logger.log(Level.WARNING, "Error while loading normalization functions from XML. No functions tag.", e);
      summary = IntensityNormalizationSummary.EMPTY;
      return;
    }
    final NodeList functionElements = functionsElement.getElementsByTagName(
        RawFileNormalizationFunction.XML_PARENT_ELEMENT);
    for (int i = 0; i < functionElements.getLength(); i++) {
      final Node node = functionElements.item(i);
      if (!(node instanceof Element functionElement) || node.getParentNode() != functionsElement) {
        continue;
      }
      try {
        functions.add(RawFileNormalizationFunction.loadFromXML(functionElement));
      } catch (RuntimeException e) {
        logger.log(Level.WARNING, "Error while loading normalization function", e);
        // do not set a summary if loading of a function fails. This will result in wrong results
        // normalization has to be applied a new
        summary = IntensityNormalizationSummary.EMPTY;
        return;
      }
    }

    // messages
    final Element messagesElements = XMLUtils.findChildElement(xmlElement, "messages");
    final NodeList msgElements = messagesElements.getElementsByTagName("msg");
    for (int i = 0; i < msgElements.getLength(); i++) {
      final Node node = msgElements.item(i);
      if (!(node instanceof Element element) || node.getParentNode() != messagesElements) {
        continue;
      }
      messages.add(element.getTextContent());
    }

    // finally loaded summary
    summary = new IntensityNormalizationSummary(functions, messages);
  }

  @Override
  public void saveValueToXML(final @NotNull Element xmlElement) {
    final Document doc = xmlElement.getOwnerDocument();

    final Element funcElement = doc.createElement("functions");
    xmlElement.appendChild(funcElement);

    for (var function : summary.functions()) {
      function.appendFunctionElement(funcElement);
    }

    final Element messagesElement = doc.createElement("messages");
    xmlElement.appendChild(messagesElement);

    for (var message : summary.messages()) {
      final Element msgElement = doc.createElement("msg");
      msgElement.setTextContent(message);
      messagesElement.appendChild(msgElement);
    }

  }
}
