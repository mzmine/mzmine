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

import io.github.mzmine.util.XMLUtils;
import io.github.mzmine.util.maths.Precision;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

/**
 * Interpolates two normalization functions by weighting their returned feature factors. This
 * interpolated function is mostly used for feature specific functions. Constant factor functions
 * like {@link FactorNormalizationFunction} interpolate the factor directly and create a simple
 * {@link FactorNormalizationFunction}.
 *
 */
public record InterpolatedNormalizationFunction(@NotNull NormalizationFunction previousFunction,
                                                double previousWeight,
                                                @NotNull NormalizationFunction nextFunction,
                                                double nextWeight) implements
    NormalizationFunction {

  public static final String XML_TYPE = "interpolated";
  private static final String XML_PREVIOUS_WEIGHT_ATTR = "previousWeight";
  private static final String XML_NEXT_WEIGHT_ATTR = "nextWeight";
  private static final String XML_PREVIOUS_FUNCTION_ELEMENT = "previousFunction";
  private static final String XML_NEXT_FUNCTION_ELEMENT = "nextFunction";

  public InterpolatedNormalizationFunction {
    if (!Precision.equalRelativeSignificance(previousWeight + nextWeight, 1d, 0.00001)) {
      throw new IllegalStateException(
          "Sum of previous and next run weight must be 1. prev=%f, next=%f".formatted(
              previousWeight, nextWeight));
    }
  }

  @Override
  public @NotNull String getUniqueID() {
    return XML_TYPE;
  }

  @Override
  public double getNormalizationFactor(@NotNull final Double mz, @NotNull final Float rt) {
    return previousFunction.getNormalizationFactor(mz, rt) * previousWeight
        + nextFunction.getNormalizationFactor(mz, rt) * nextWeight;
  }

  @Override
  public void saveToXML(final @NotNull Element functionElement) {
    functionElement.setAttribute(XML_FUNCTION_TYPE_ATTR, getUniqueID());
    functionElement.setAttribute(XML_PREVIOUS_WEIGHT_ATTR, Double.toString(previousWeight));
    functionElement.setAttribute(XML_NEXT_WEIGHT_ATTR, Double.toString(nextWeight));

    final Element previousElement = functionElement.getOwnerDocument()
        .createElement(XML_PREVIOUS_FUNCTION_ELEMENT);
    previousFunction.saveToXML(previousElement);
    functionElement.appendChild(previousElement);

    final Element nextElement = functionElement.getOwnerDocument()
        .createElement(XML_NEXT_FUNCTION_ELEMENT);
    nextFunction.saveToXML(nextElement);
    functionElement.appendChild(nextElement);
  }

  public static @NotNull InterpolatedNormalizationFunction loadFromXML(
      final @NotNull Element functionElement) {
    final double previousWeight = Double.parseDouble(
        XMLUtils.requireAttribute(functionElement, XML_PREVIOUS_WEIGHT_ATTR));
    final double nextWeight = Double.parseDouble(
        XMLUtils.requireAttribute(functionElement, XML_NEXT_WEIGHT_ATTR));

    final Element previousFunctionContainer = XMLUtils.findChildElement(functionElement,
        XML_PREVIOUS_FUNCTION_ELEMENT);
    final NormalizationFunction previousFunction = NormalizationFunction.loadFromXML(
        previousFunctionContainer);

    final Element nextFunctionContainer = XMLUtils.findChildElement(functionElement,
        XML_NEXT_FUNCTION_ELEMENT);
    final NormalizationFunction nextFunction = NormalizationFunction.loadFromXML(
        nextFunctionContainer);

    return new InterpolatedNormalizationFunction(previousFunction, previousWeight, nextFunction,
        nextWeight);
  }

}
