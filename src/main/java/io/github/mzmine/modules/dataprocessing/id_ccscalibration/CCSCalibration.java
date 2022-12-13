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

import org.w3c.dom.Element;

public interface CCSCalibration {

  String XML_ELEMENT = "ccscalibration";
  String XML_TYPE_ATTR = "type";

  float getCCS(double mz, int charge, float mobility);

  void saveToXML(Element element);

  static CCSCalibration loadFromXML(Element element) {
    return switch (element.getAttribute(XML_TYPE_ATTR)) {
      case DriftTubeCCSCalibration.XML_TYPE_NAME -> DriftTubeCCSCalibration.loadFromXML(element);
      case TwCCSCalibration.XML_TYPE_NAME -> TwCCSCalibration.loadFromXML(element);
      default -> null;
    };
  }

  static double getReducedMass(final double mz, final int charge, final double gasWeight) {
      return mz * charge * gasWeight / (mz * charge + gasWeight);
  }
}
