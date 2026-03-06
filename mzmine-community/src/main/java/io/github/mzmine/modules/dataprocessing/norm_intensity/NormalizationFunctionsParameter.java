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

import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.UserParameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.Label;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class NormalizationFunctionsParameter implements
    UserParameter<List<NormalizationFunction>, Label> {

  private static final Logger logger = Logger.getLogger(
      NormalizationFunctionsParameter.class.getName());

  @NotNull
  private final List<NormalizationFunction> normalizationFunctions = new ArrayList<>();

  @Override
  public @NotNull String getName() {
    return "Normalization functions";
  }

  @Override
  public @NotNull String getDescription() {
    return "Stores per-file normalization functions used to normalize this feature list.";
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
      final @Nullable List<NormalizationFunction> newValue) {
    // no UI component editing for hidden parameter
  }

  @Override
  public @NotNull UserParameter<List<NormalizationFunction>, Label> cloneParameter() {
    final NormalizationFunctionsParameter clonedParameter = new NormalizationFunctionsParameter();
    clonedParameter.setValue(List.copyOf(normalizationFunctions));
    return clonedParameter;
  }

  @Override
  public @NotNull List<NormalizationFunction> getValue() {
    return normalizationFunctions;
  }

  @Override
  public void setValue(final @Nullable List<NormalizationFunction> newValue) {
    normalizationFunctions.clear();
    if (newValue != null) {
      normalizationFunctions.addAll(newValue);
    }
  }

  @Override
  public boolean checkValue(final @NotNull Collection<String> errorMessages) {
    return normalizationFunctions != null;
  }

  @Override
  public boolean valueEquals(final @Nullable Parameter<?> that) {
    if (!(that instanceof final NormalizationFunctionsParameter other)) {
      return false;
    }

    if (normalizationFunctions.size() != other.normalizationFunctions.size()) {
      return false;
    }

    for (int i = 0; i < normalizationFunctions.size(); i++) {
      if (!normalizationFunctionsEqual(normalizationFunctions.get(i),
          other.normalizationFunctions.get(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void loadValueFromXML(final @NotNull Element xmlElement) {
    normalizationFunctions.clear();

    final NodeList functionElements = xmlElement.getElementsByTagName(
        NormalizationFunction.XML_FUNCTION_ELEMENT);
    for (int i = 0; i < functionElements.getLength(); i++) {
      final org.w3c.dom.Node node = functionElements.item(i);
      if (!(node instanceof Element functionElement) || node.getParentNode() != xmlElement) {
        continue;
      }
      try {
        normalizationFunctions.add(NormalizationFunction.loadFromXML(functionElement));
      } catch (RuntimeException e) {
        logger.log(Level.WARNING, "Error while loading normalization function", e);
      }
    }
  }

  @Override
  public void saveValueToXML(final @NotNull Element xmlElement) {
    for (final NormalizationFunction normalizationFunction : normalizationFunctions) {
      NormalizationFunction.appendFunctionElement(xmlElement, normalizationFunction);
    }
  }

  private static boolean normalizationFunctionsEqual(final @NotNull NormalizationFunction first,
      final @NotNull NormalizationFunction second) {
    if (!first.rawDataFilePlaceholder().equals(second.rawDataFilePlaceholder())
        || !first.acquisitionTimestamp().equals(second.acquisitionTimestamp())) {
      return false;
    }

    if (first instanceof final FactorNormalizationFunction firstFactor
        && second instanceof final FactorNormalizationFunction secondFactor) {
      return Double.compare(firstFactor.getConstantFactor(), secondFactor.getConstantFactor()) == 0;
    }

    if (first instanceof final StandardCompoundNormalizationFunction firstStandard
        && second instanceof final StandardCompoundNormalizationFunction secondStandard) {
      return firstStandard.usageType() == secondStandard.usageType()
          && Double.compare(firstStandard.mzVsRtBalance(), secondStandard.mzVsRtBalance()) == 0
          && firstStandard.referencePoints().equals(secondStandard.referencePoints());
    }

    if (first instanceof final InterpolatedNormalizationFunction firstInterpolated
        && second instanceof final InterpolatedNormalizationFunction secondInterpolated) {
      return Double.compare(firstInterpolated.getPreviousWeight(),
          secondInterpolated.getPreviousWeight()) == 0
          && Double.compare(firstInterpolated.getNextWeight(), secondInterpolated.getNextWeight())
          == 0 && normalizationFunctionsEqual(firstInterpolated.getPreviousFunction(),
          secondInterpolated.getPreviousFunction()) && normalizationFunctionsEqual(
          firstInterpolated.getNextFunction(), secondInterpolated.getNextFunction());
    }

    // decision: unhandled future function implementations are considered unequal until explicitly mapped.
    return Objects.equals(first.getClass(), second.getClass())
        && Double.compare(first.getFactor(0d, 0f), second.getFactor(0d, 0f)) == 0;
  }
}
