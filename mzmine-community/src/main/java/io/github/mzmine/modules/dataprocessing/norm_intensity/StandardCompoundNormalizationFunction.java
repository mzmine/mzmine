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

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.util.XMLUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * File-specific standard compound normalization function.
 */
public record StandardCompoundNormalizationFunction(@NotNull StandardUsageType usageType,
                                                    double mzVsRtBalance,
                                                    @NotNull StandardCompoundFactorMode factorMode,
                                                    @NotNull List<@NotNull StandardCompoundReferencePoint> referencePoints) implements
    NormalizationFunction {

  public static final String XML_TYPE = "standardCompound";
  private static final String XML_STANDARD_USAGE_TYPE_ATTR = "standardUsageType";
  private static final String XML_MZ_VS_RT_BALANCE_ATTR = "mzVsRtBalance";
  private static final String XML_FACTOR_MODE_ATTR = "factorMode";
  private static final String XML_STANDARD_POINT_ELEMENT = "standardPoint";
  private static final String XML_STANDARD_POINT_MZ_ATTR = "mz";
  private static final String XML_STANDARD_POINT_RT_ATTR = "rt";
  private static final String XML_STANDARD_POINT_ABUNDANCE_ATTR = "abundance";
  private static final String XML_STANDARD_POINT_REF_ABUNDANCE_ATTR = "refAbundance";

  public StandardCompoundNormalizationFunction {
    // decision: enforce at least one standard point to avoid runtime failure paths.
    if (referencePoints.isEmpty()) {
      throw new IllegalStateException("No standard reference points available.");
    }
  }

  /**
   * A function without a reference level. Only used for
   * {@link StandardCompoundFactorMode#ABSOLUTE_LEGACY}, where the factor is 1 / abundance.
   */
  public StandardCompoundNormalizationFunction(@NotNull StandardUsageType usageType,
      double mzVsRtBalance,
      @NotNull List<@NotNull StandardCompoundReferencePoint> referencePoints) {
    this(usageType, mzVsRtBalance, StandardCompoundFactorMode.ABSOLUTE_LEGACY, referencePoints);
  }

  @Override
  public @NotNull String getUniqueID() {
    return XML_TYPE;
  }

  @Override
  public double getNormalizationFactor(@NotNull final Double mz, @NotNull final Float rt) {
    return switch (factorMode) {
      case MEDIAN_SCALED -> getMedianScaledFactor(mz, rt);
      case ABSOLUTE_LEGACY -> getAbsoluteLegacyFactor(mz, rt);
    };
  }

  /**
   * Combines the relative {@link StandardCompoundReferencePoint#factor()} of the reference points,
   * see {@link StandardCompoundFactorMode#MEDIAN_SCALED}.
   */
  private double getMedianScaledFactor(final double mz, final float rt) {
    final double factor = switch (usageType) {
      case Nearest -> getNearestPoint(mz, rt).factor();
      case Weighted -> getWeightedFactor(mz, rt);
    };

    if (Double.compare(factor, 0.0d) == 0 || !Double.isFinite(factor)) {
      throw new IllegalStateException(
          "Illegal standard normalization factor of %.2f.".formatted(factor));
    }
    return factor;
  }

  /**
   * The behavior of normalization functions that were saved before the median scaling was
   * introduced, see {@link StandardCompoundFactorMode#ABSOLUTE_LEGACY}.
   */
  private double getAbsoluteLegacyFactor(final double mz, final float rt) {
    final double standardAbundance = switch (usageType) {
      case Nearest -> getNearestPoint(mz, rt).abundance();
      case Weighted -> getWeightedStandardAbundance(mz, rt);
    };

    if (Double.compare(standardAbundance, 0.0d) == 0 || !Double.isFinite(standardAbundance)) {
      throw new IllegalStateException(
          "Illegal standard abundance of %.2f.".formatted(standardAbundance));
    }
    return 1.0d / standardAbundance;
  }

  private @NotNull StandardCompoundReferencePoint getNearestPoint(final double mz, final float rt) {
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
    return nearestPoint;
  }

  private double getWeightedFactor(final double mz, final float rt) {
    return weightByInverseDistance(mz, rt, StandardCompoundReferencePoint::factor);
  }

  private double getWeightedStandardAbundance(final double mz, final float rt) {
    return weightByInverseDistance(mz, rt, StandardCompoundReferencePoint::abundance);
  }

  private double weightByInverseDistance(final double mz, final float rt,
      final @NotNull ToDoubleFunction<StandardCompoundReferencePoint> valueExtractor) {
    if (referencePoints.isEmpty()) {
      throw new IllegalStateException("No standard reference points available.");
    }

    // decision: direct standard hits should dominate weighted interpolation.
    double directMatchSum = 0.0d;
    int directMatchCount = 0;
    double weightedSum = 0.0d;
    double sumOfWeights = 0.0d;

    for (final StandardCompoundReferencePoint point : referencePoints) {
      final double value = valueExtractor.applyAsDouble(point);
      final double distance = calcDistance(mz, rt, point);
      if (distance == 0.0d) {
        directMatchSum += value;
        directMatchCount++;
        continue;
      }

      final double weight = 1.0d / distance;
      weightedSum += value * weight;
      sumOfWeights += weight;
    }

    if (directMatchCount > 0) {
      return directMatchSum / directMatchCount;
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
    functionElement.setAttribute(XML_STANDARD_USAGE_TYPE_ATTR, usageType.name());
    functionElement.setAttribute(XML_MZ_VS_RT_BALANCE_ATTR, Double.toString(mzVsRtBalance));
    functionElement.setAttribute(XML_FACTOR_MODE_ATTR, factorMode.getUniqueID());

    for (final StandardCompoundReferencePoint point : referencePoints) {
      final Element pointElement = functionElement.getOwnerDocument()
          .createElement(XML_STANDARD_POINT_ELEMENT);
      pointElement.setAttribute(XML_STANDARD_POINT_MZ_ATTR, Double.toString(point.mz()));
      pointElement.setAttribute(XML_STANDARD_POINT_RT_ATTR, Float.toString(point.rt()));
      pointElement.setAttribute(XML_STANDARD_POINT_ABUNDANCE_ATTR,
          Double.toString(point.abundance()));
      pointElement.setAttribute(XML_STANDARD_POINT_REF_ABUNDANCE_ATTR,
          Double.toString(point.referenceAbundance()));
      functionElement.appendChild(pointElement);
    }
  }

  public static @NotNull StandardCompoundNormalizationFunction loadFromXML(
      final @NotNull Element functionElement) {
    final StandardUsageType standardUsageType = StandardUsageType.valueOf(
        XMLUtils.requireAttribute(functionElement, XML_STANDARD_USAGE_TYPE_ATTR));
    final double mzVsRtBalance = Double.parseDouble(
        XMLUtils.requireAttribute(functionElement, XML_MZ_VS_RT_BALANCE_ATTR));
    // functions saved before the median scaling was introduced have no factor mode and no
    // reference abundance. Those need to keep normalizing by 1 / abundance.
    final StandardCompoundFactorMode factorMode = UniqueIdSupplier.parseOrElse(
        functionElement.getAttribute(XML_FACTOR_MODE_ATTR), StandardCompoundFactorMode.values(),
        StandardCompoundFactorMode.ABSOLUTE_LEGACY);

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
      final String refAbundance = pointElement.getAttribute(
          XML_STANDARD_POINT_REF_ABUNDANCE_ATTR);
      referencePoints.add(new StandardCompoundReferencePoint(mz, rt, abundance,
          refAbundance.isBlank() ? 1d : Double.parseDouble(refAbundance)));
    }

    return new StandardCompoundNormalizationFunction(standardUsageType, mzVsRtBalance, factorMode,
        referencePoints);
  }
}
