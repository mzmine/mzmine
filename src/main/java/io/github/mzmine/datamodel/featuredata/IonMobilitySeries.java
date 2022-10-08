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
