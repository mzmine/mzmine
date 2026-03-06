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
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import io.github.mzmine.util.XMLUtils;
import java.time.LocalDateTime;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

/**
 * Interpolates two normalization functions by weighting their returned feature factors.
 */
public class InterpolatedNormalizationFunction implements NormalizationFunction {

  public static final String XML_TYPE = "interpolated";
  private static final String XML_PREVIOUS_WEIGHT_ATTR = "previousWeight";
  private static final String XML_NEXT_WEIGHT_ATTR = "nextWeight";
  private static final String XML_PREVIOUS_FUNCTION_ELEMENT = "previousFunction";
  private static final String XML_NEXT_FUNCTION_ELEMENT = "nextFunction";

  private final RawDataFilePlaceholder referenceFilePlaceholder;
  private final LocalDateTime acquisitionTimestamp;
  private final NormalizationFunction previousFunction;
  private final double previousWeight;
  private final NormalizationFunction nextFunction;
  private final double nextWeight;

  public InterpolatedNormalizationFunction(@NotNull final RawDataFile targetFile,
      @NotNull final LocalDateTime acquisitionTimestamp,
      @NotNull final NormalizationFunction previousFunction, final double previousWeight,
      @NotNull final NormalizationFunction nextFunction, final double nextWeight) {
    this(new RawDataFilePlaceholder(targetFile), acquisitionTimestamp, previousFunction,
        previousWeight, nextFunction, nextWeight);
  }

  public InterpolatedNormalizationFunction(
      @NotNull final RawDataFilePlaceholder targetFilePlaceholder,
      @NotNull final LocalDateTime acquisitionTimestamp,
      @NotNull final NormalizationFunction previousFunction, final double previousWeight,
      @NotNull final NormalizationFunction nextFunction, final double nextWeight) {
    this.referenceFilePlaceholder = targetFilePlaceholder;
    this.acquisitionTimestamp = acquisitionTimestamp;
    this.previousFunction = previousFunction;
    this.previousWeight = previousWeight;
    this.nextFunction = nextFunction;
    this.nextWeight = nextWeight;
  }

  @Override
  public @NotNull RawDataFilePlaceholder rawDataFilePlaceholder() {
    return referenceFilePlaceholder;
  }

  @Override
  public @NotNull LocalDateTime acquisitionTimestamp() {
    return acquisitionTimestamp;
  }

  @Override
  public @NotNull String getUniqueID() {
    return XML_TYPE;
  }

  @Override
  public double getFactor(@NotNull final Double mz, @NotNull final Float rt) {
    return previousFunction.getFactor(mz, rt) * previousWeight
        + nextFunction.getFactor(mz, rt) * nextWeight;
  }

  @Override
  public void saveToXML(final @NotNull Element functionElement) {
    functionElement.setAttribute(XML_FUNCTION_TYPE_ATTR, getUniqueID());
    referenceFilePlaceholder.saveToXML(functionElement);
    NormalizationFunction.saveAcquisitionTimestamp(functionElement, acquisitionTimestamp);
    functionElement.setAttribute(XML_PREVIOUS_WEIGHT_ATTR, Double.toString(previousWeight));
    functionElement.setAttribute(XML_NEXT_WEIGHT_ATTR, Double.toString(nextWeight));

    final Element previousElement = functionElement.getOwnerDocument()
        .createElement(XML_PREVIOUS_FUNCTION_ELEMENT);
    NormalizationFunction.appendFunctionElement(previousElement, previousFunction);
    functionElement.appendChild(previousElement);

    final Element nextElement = functionElement.getOwnerDocument()
        .createElement(XML_NEXT_FUNCTION_ELEMENT);
    NormalizationFunction.appendFunctionElement(nextElement, nextFunction);
    functionElement.appendChild(nextElement);
  }

  public static @NotNull InterpolatedNormalizationFunction loadFromXML(
      final @NotNull Element functionElement) {
    final RawDataFilePlaceholder rawDataFilePlaceholder = RawDataFilePlaceholder.loadFromXML(
        functionElement);
    final LocalDateTime acquisitionTimestamp = NormalizationFunction.loadAcquisitionTimestamp(
        functionElement);
    final double previousWeight = Double.parseDouble(
        XMLUtils.requireAttribute(functionElement, XML_PREVIOUS_WEIGHT_ATTR));
    final double nextWeight = Double.parseDouble(
        XMLUtils.requireAttribute(functionElement, XML_NEXT_WEIGHT_ATTR));

    final Element previousFunctionContainer = XMLUtils.findChildElement(functionElement,
        XML_PREVIOUS_FUNCTION_ELEMENT);
    final Element previousFunctionElement = XMLUtils.findChildElement(previousFunctionContainer,
        XML_FUNCTION_ELEMENT);
    final NormalizationFunction previousFunction = NormalizationFunction.loadFromXML(
        previousFunctionElement);

    final Element nextFunctionContainer = XMLUtils.findChildElement(functionElement,
        XML_NEXT_FUNCTION_ELEMENT);
    final Element nextFunctionElement = XMLUtils.findChildElement(nextFunctionContainer,
        XML_FUNCTION_ELEMENT);
    final NormalizationFunction nextFunction = NormalizationFunction.loadFromXML(
        nextFunctionElement);

    return new InterpolatedNormalizationFunction(rawDataFilePlaceholder, acquisitionTimestamp,
        previousFunction, previousWeight, nextFunction, nextWeight);
  }

  public @NotNull NormalizationFunction getPreviousFunction() {
    return previousFunction;
  }

  public double getPreviousWeight() {
    return previousWeight;
  }

  public @NotNull NormalizationFunction getNextFunction() {
    return nextFunction;
  }

  public double getNextWeight() {
    return nextWeight;
  }
}
