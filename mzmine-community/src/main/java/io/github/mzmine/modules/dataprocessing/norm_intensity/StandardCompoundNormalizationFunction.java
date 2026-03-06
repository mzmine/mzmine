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
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * File-specific standard compound normalization function.
 */
public record StandardCompoundNormalizationFunction(
    @NotNull RawDataFilePlaceholder rawDataFilePlaceholder,
    @NotNull LocalDateTime acquisitionTimestamp, @NotNull StandardUsageType usageType,
    double mzVsRtBalance,
    @NotNull List<@NotNull StandardCompoundReferencePoint> referencePoints) implements
    NormalizationFunction {

  public static final String XML_TYPE = "standardCompound";
  private static final String XML_STANDARD_USAGE_TYPE_ATTR = "standardUsageType";
  private static final String XML_MZ_VS_RT_BALANCE_ATTR = "mzVsRtBalance";
  private static final String XML_STANDARD_POINT_ELEMENT = "standardPoint";
  private static final String XML_STANDARD_POINT_MZ_ATTR = "mz";
  private static final String XML_STANDARD_POINT_RT_ATTR = "rt";
  private static final String XML_STANDARD_POINT_ABUNDANCE_ATTR = "abundance";
  private static final String XML_STANDARD_POINT_MISSING_ATTR = "missingInFile";

  public StandardCompoundNormalizationFunction(@NotNull final RawDataFile referenceFile,
      @NotNull final LocalDateTime acquisitionTimestamp, @NotNull final StandardUsageType usageType,
      final double mzVsRtBalance,
      @NotNull final List<StandardCompoundReferencePoint> referencePoints) {
    this(new RawDataFilePlaceholder(referenceFile), acquisitionTimestamp, usageType, mzVsRtBalance,
        referencePoints);
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
  public double getFactor(@NotNull final Double mz, @NotNull final Float rt) {
    final double standardAbundance = switch (usageType) {
      case Nearest -> getNearestStandardAbundance(mz, rt);
      case Weighted -> getWeightedStandardAbundance(mz, rt);
    };

    double legacyNormalizationFactor = standardAbundance / 100.0d;
    if (legacyNormalizationFactor == 0.0d) {
      legacyNormalizationFactor = Double.MIN_VALUE;
    }
    return 1.0d / legacyNormalizationFactor;
  }

  private double getNearestStandardAbundance(final double mz, final float rt) {
    StandardCompoundReferencePoint nearestPoint = null;
    double nearestDistance = Double.MAX_VALUE;
    for (final StandardCompoundReferencePoint point : referencePoints) {
      final double distance = calcDistance(mz, rt, point);
      if (distance <= nearestDistance) {
        nearestPoint = point;
        nearestDistance = distance;
      }
    }
    if (nearestPoint == null) {
      throw new IllegalStateException("No standard reference points available.");
    }
    return nearestPoint.abundance();
  }

  private double getWeightedStandardAbundance(final double mz, final float rt) {
    // decision: direct standard hits should dominate weighted interpolation.
    double directMatchSum = 0.0d;
    int directMatchCount = 0;
    double weightedSum = 0.0d;
    double sumOfWeights = 0.0d;

    for (final StandardCompoundReferencePoint point : referencePoints) {
      if (point.missingInFile()) {
        continue;
      }

      final double distance = calcDistance(mz, rt, point);
      if (distance == 0.0d) {
        directMatchSum += point.abundance();
        directMatchCount++;
        continue;
      }

      final double weight = 1.0d / distance;
      weightedSum += point.abundance() * weight;
      sumOfWeights += weight;
    }

    if (directMatchCount > 0) {
      return directMatchSum / directMatchCount;
    }
    if (sumOfWeights == 0.0d) {
      // decision: keep legacy missing-standard behavior (factor of 1) when all standards are missing.
      return 1.0d;
    }
    return weightedSum / sumOfWeights;
  }

  private double calcDistance(final double mz, final float rt,
      @NotNull final StandardCompoundReferencePoint point) {
    return mzVsRtBalance * Math.abs(mz - point.mz()) + Math.abs(rt - point.rt());
  }

  @Override
  public void saveToXML(final @NotNull Element functionElement) {
    functionElement.setAttribute(XML_FUNCTION_TYPE_ATTR, getUniqueID());
    rawDataFilePlaceholder.saveToXML(functionElement);
    NormalizationFunction.saveAcquisitionTimestamp(functionElement, acquisitionTimestamp);
    functionElement.setAttribute(XML_STANDARD_USAGE_TYPE_ATTR, usageType.name());
    functionElement.setAttribute(XML_MZ_VS_RT_BALANCE_ATTR, Double.toString(mzVsRtBalance));

    for (final StandardCompoundReferencePoint point : referencePoints) {
      final Element pointElement = functionElement.getOwnerDocument()
          .createElement(XML_STANDARD_POINT_ELEMENT);
      pointElement.setAttribute(XML_STANDARD_POINT_MZ_ATTR, Double.toString(point.mz()));
      pointElement.setAttribute(XML_STANDARD_POINT_RT_ATTR, Float.toString(point.rt()));
      pointElement.setAttribute(XML_STANDARD_POINT_ABUNDANCE_ATTR,
          Double.toString(point.abundance()));
      pointElement.setAttribute(XML_STANDARD_POINT_MISSING_ATTR,
          Boolean.toString(point.missingInFile()));
      functionElement.appendChild(pointElement);
    }
  }

  public static @NotNull StandardCompoundNormalizationFunction loadFromXML(
      final @NotNull Element functionElement) {
    final RawDataFilePlaceholder rawDataFilePlaceholder = RawDataFilePlaceholder.loadFromXML(
        functionElement);
    final LocalDateTime acquisitionTimestamp = NormalizationFunction.loadAcquisitionTimestamp(
        functionElement);
    final StandardUsageType standardUsageType = StandardUsageType.valueOf(
        XMLUtils.requireAttribute(functionElement, XML_STANDARD_USAGE_TYPE_ATTR));
    final double mzVsRtBalance = Double.parseDouble(
        XMLUtils.requireAttribute(functionElement, XML_MZ_VS_RT_BALANCE_ATTR));

    final List<StandardCompoundReferencePoint> referencePoints = new ArrayList<>();
    final NodeList pointNodes = functionElement.getElementsByTagName(XML_STANDARD_POINT_ELEMENT);
    for (int i = 0; i < pointNodes.getLength(); i++) {
      final Element pointElement = (Element) pointNodes.item(i);
      final double mz = Double.parseDouble(
          XMLUtils.requireAttribute(pointElement, XML_STANDARD_POINT_MZ_ATTR));
      final float rt = Float.parseFloat(
          XMLUtils.requireAttribute(pointElement, XML_STANDARD_POINT_RT_ATTR));
      final double abundance = Double.parseDouble(
          XMLUtils.requireAttribute(pointElement, XML_STANDARD_POINT_ABUNDANCE_ATTR));
      final boolean missingInFile = Boolean.parseBoolean(
          XMLUtils.requireAttribute(pointElement, XML_STANDARD_POINT_MISSING_ATTR));
      referencePoints.add(new StandardCompoundReferencePoint(mz, rt, abundance, missingInFile));
    }

    return new StandardCompoundNormalizationFunction(rawDataFilePlaceholder, acquisitionTimestamp,
        standardUsageType, mzVsRtBalance, referencePoints);
  }

  @Override
  public @NotNull StandardUsageType usageType() {
    return usageType;
  }

  @Override
  public @NotNull List<StandardCompoundReferencePoint> referencePoints() {
    return referencePoints;
  }
}
