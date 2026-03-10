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
 * Factor normalization function represented by one global factor.
 */
public record FactorNormalizationFunction(@NotNull RawDataFilePlaceholder rawDataFilePlaceholder,
                                          @NotNull LocalDateTime acquisitionTimestamp,
                                          double factor) implements NormalizationFunction {

  public static final String XML_TYPE = "factor";
  private static final String XML_FACTOR_ATTR = "factor";

  public FactorNormalizationFunction(@NotNull final RawDataFile referenceFile,
      @NotNull final LocalDateTime acquisitionTimestamp, final double factor) {
    this(new RawDataFilePlaceholder(referenceFile), acquisitionTimestamp, factor);
  }

  public FactorNormalizationFunction(@NotNull final RawDataFilePlaceholder rawDataFilePlaceholder,
      @NotNull final LocalDateTime acquisitionTimestamp, final double factor) {
    this.rawDataFilePlaceholder = rawDataFilePlaceholder;
    this.acquisitionTimestamp = acquisitionTimestamp;
    this.factor = factor;
  }

  @Override
  public @NotNull RawDataFilePlaceholder rawDataFilePlaceholder() {
    return rawDataFilePlaceholder;
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
  public double getNormalizationFactor(@NotNull final Double mz, @NotNull final Float rt) {
    return factor;
  }

  @Override
  public void saveToXML(final @NotNull Element functionElement) {
    functionElement.setAttribute(XML_FUNCTION_TYPE_ATTR, getUniqueID());
    rawDataFilePlaceholder.saveToXML(functionElement);
    NormalizationFunction.saveAcquisitionTimestamp(functionElement, acquisitionTimestamp);
    functionElement.setAttribute(XML_FACTOR_ATTR, Double.toString(factor));
  }

  public static @NotNull FactorNormalizationFunction loadFromXML(
      final @NotNull Element functionElement) {
    final RawDataFilePlaceholder rawDataFilePlaceholder = RawDataFilePlaceholder.loadFromXML(
        functionElement);
    final LocalDateTime acquisitionTimestamp = NormalizationFunction.loadAcquisitionTimestamp(
        functionElement);
    final double factor = Double.parseDouble(
        XMLUtils.requireAttribute(functionElement, XML_FACTOR_ATTR));
    return new FactorNormalizationFunction(rawDataFilePlaceholder, acquisitionTimestamp, factor);
  }

}
