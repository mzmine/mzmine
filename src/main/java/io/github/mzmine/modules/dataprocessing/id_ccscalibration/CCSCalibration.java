/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
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
