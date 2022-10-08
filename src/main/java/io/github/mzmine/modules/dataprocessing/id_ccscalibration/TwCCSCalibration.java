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

package io.github.mzmine.modules.dataprocessing.id_ccscalibration;

import io.github.mzmine.modules.dataprocessing.id_ccscalibration.reference.CCSCalibrant;
import java.util.Objects;
import org.w3c.dom.Element;

/**
 *
 */
public class TwCCSCalibration implements CCSCalibration {

  protected static final String XML_TYPE_NAME = "twims";
  private static final String XML_COEFF_ATTR = "coeff";
  private static final String XML_EXPONENT_ATTR = "exponent";
  private static final String XML_T0_ATTR = "t0";
  private static final String XML_EDC_ATTR = "edc";

  private final double coeff;
  private final double exponent;
  private final double t0;
  private final double edcDelayCoeff;

  public TwCCSCalibration(double coeff, double exponent, double t0, double edcDelayCoeff) {
    this.coeff = coeff;
    this.exponent = exponent;
    this.t0 = t0;
    this.edcDelayCoeff = edcDelayCoeff;
  }

  /**
   * @param mz       The m/z of the ion.
   * @param charge   The charge of the ion.
   * @param mobility The drift time (TWIMS)
   * @return The calculated CCS value.
   */
  @Override
  public float getCCS(double mz, int charge, float mobility) {
    final double correctedDriftTime = mobility - (edcDelayCoeff * Math.sqrt(mz) / 1000);
    final double omegaC = (coeff * Math.pow(correctedDriftTime + t0,
        exponent)); // waters slide does not mention t0, find out if we need it or not
    final double reducedMass = CCSCalibration.getReducedMass(mz, charge, CCSCalibrant.N2_MASS);
    final double sqrt = Math.sqrt(reducedMass);
    return (float) (omegaC * Math.abs(charge) / sqrt);
  }

  @Override
  public void saveToXML(Element element) {
    element.setAttribute(XML_TYPE_ATTR, XML_TYPE_NAME);
    element.setAttribute(XML_COEFF_ATTR, String.valueOf(coeff));
    element.setAttribute(XML_EXPONENT_ATTR, String.valueOf(exponent));
    element.setAttribute(XML_T0_ATTR, String.valueOf(t0));
    element.setAttribute(XML_EDC_ATTR, String.valueOf(edcDelayCoeff));
  }

  public static CCSCalibration loadFromXML(Element element) {
    double coeff = Double.parseDouble(element.getAttribute(XML_COEFF_ATTR));
    double exp = Double.parseDouble(element.getAttribute(XML_EXPONENT_ATTR));
    double t0 = Double.parseDouble(element.getAttribute(XML_T0_ATTR));
    double edc = Double.parseDouble(element.getAttribute(XML_EDC_ATTR));

    return new TwCCSCalibration(coeff, exp, t0, edc);
  }

  @Override
  public String toString() {
    return String.format("TWIMS cal - Coeff.: %.4f, Exp.: %.4f, t0: %.4f, EDC: %.4f", coeff,
        exponent, t0, edcDelayCoeff);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TwCCSCalibration that = (TwCCSCalibration) o;
    return Double.compare(that.coeff, coeff) == 0 && Double.compare(that.exponent, exponent) == 0
        && Double.compare(that.t0, t0) == 0
        && Double.compare(that.edcDelayCoeff, edcDelayCoeff) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(coeff, exponent, t0, edcDelayCoeff);
  }
}
