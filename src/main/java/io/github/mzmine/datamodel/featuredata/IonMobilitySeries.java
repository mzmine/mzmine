/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.datamodel.featuredata;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Tag interface for mobilograms.
 */
public interface IonMobilitySeries extends IonSpectrumSeries<MobilityScan>, MobilitySeries {

  public static final String XML_ION_MOBILITY_SERIES_ELEMENT = "ionmobilityseries";
  public static final String XML_FRAME_INDEX_ELEMENT = "frameindex";

  public static void saveMobilogramToXML(XMLStreamWriter writer, IonMobilitySeries series,
      List<MobilityScan> allScans) throws XMLStreamException {
    writer.writeStartElement(XML_ION_MOBILITY_SERIES_ELEMENT);
    writer.writeAttribute(CONST.XML_NUM_VALUES_ATTR,
        String.valueOf(series.getNumberOfValues()));

    final Frame frame = series.getSpectrum(0).getFrame();
    final int frameIndex = frame.getDataFile().getScans().indexOf(frame);
    if (frameIndex == -1) {
      throw new IllegalArgumentException("Cannot find frame in data file.");
    }
    writer.writeAttribute(XML_FRAME_INDEX_ELEMENT, String.valueOf(frameIndex));

    IonSpectrumSeries.saveSpectraIndicesToXML(writer, series, allScans);
    IntensitySeries.saveIntensityValuesToXML(writer, series);
    MzSeries.saveMzValuesToXML(writer, series);

    writer.writeEndElement();
  }

  @Override
  default double getMobility(int index) {
    return getSpectrum(index).getMobility();
  }
}
