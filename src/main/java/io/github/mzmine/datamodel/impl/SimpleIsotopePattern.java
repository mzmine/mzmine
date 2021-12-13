/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.datamodel.impl;

import com.google.common.collect.Range;
import com.google.common.collect.Streams;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.ParsingUtils;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.Iterator;
import java.util.stream.Stream;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Simple implementation of IsotopePattern interface
 */
public class SimpleIsotopePattern implements IsotopePattern {

  public static final String XML_ELEMENT = "simpleisotopepattern";
  public static final String XML_DESCRIPTION_ELEMENT = "description";
  private static final String XML_COMPOSITION_ELEMENT = "composition";
  private static final String XML_STATUS_ELEMENT = "status";
  private double mzValues[], intensityValues[];
  private int highestIsotope;
  private IsotopePatternStatus status;
  private String description;
  private Range<Double> mzRange;
  private String[] isotopeCompostion;


  public SimpleIsotopePattern(double[] mzValues, double[] intensityValues,
      IsotopePatternStatus status, String description, String[] isotopeCompostion) {
    this(mzValues, intensityValues, status, description);
    this.isotopeCompostion = isotopeCompostion;
  }


  public SimpleIsotopePattern(DataPoint[] dataPoints, IsotopePatternStatus status,
      String description, String[] isotopeCompostion) {

    this(dataPoints, status, description);
    this.isotopeCompostion = isotopeCompostion;
  }

  public SimpleIsotopePattern(DataPoint dataPoints[], IsotopePatternStatus status,
      String description) {

    mzValues = new double[dataPoints.length];
    intensityValues = new double[dataPoints.length];
    for (int i = 0; i < dataPoints.length; i++) {
      mzValues[i] = dataPoints[i].getMZ();
      intensityValues[i] = dataPoints[i].getIntensity();
    }
    this.status = status;
    this.description = description;
    this.mzRange = ScanUtils.findMzRange(mzValues);
    highestIsotope = ScanUtils.findTopDataPoint(intensityValues);
  }

  public SimpleIsotopePattern(double mzValues[], double intensityValues[],
      IsotopePatternStatus status, String description) {

    assert mzValues.length > 0;
    assert mzValues.length == intensityValues.length;

    highestIsotope = ScanUtils.findTopDataPoint(intensityValues);
    this.mzValues = mzValues;
    this.intensityValues = intensityValues;
    this.status = status;
    this.description = description;
    this.mzRange = ScanUtils.findMzRange(mzValues);
  }

  public static IsotopePattern loadFromXML(XMLStreamReader reader) throws XMLStreamException {
    if (!reader.getLocalName().equals(XML_ELEMENT)) {
      throw new IllegalStateException("Invalid element");
    }

    double[] mzs = null;
    double[] intensities = null;
    String desc = null;
    String[] comp = null;
    IsotopePatternStatus status = null;

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(XML_ELEMENT))) {
      int next = reader.next();
      if (next != XMLEvent.START_ELEMENT) {
        continue;
      }

      switch (reader.getLocalName()) {
        case CONST.XML_MZ_VALUES_ELEMENT -> mzs = ParsingUtils
            .stringToDoubleArray(reader.getElementText());
        case CONST.XML_INTENSITY_VALUES_ELEMENT -> intensities = ParsingUtils
            .stringToDoubleArray(reader.getElementText());
        case XML_DESCRIPTION_ELEMENT -> desc = reader.getElementText();
        case XML_COMPOSITION_ELEMENT -> {
          if (!reader.getElementText().trim().isEmpty()) {
            comp = ParsingUtils.stringToStringArray(reader.getElementText());
          }
        }
        case XML_STATUS_ELEMENT -> status = IsotopePatternStatus.valueOf(reader.getElementText());
      }
    }
    return new SimpleIsotopePattern(mzs, intensities, status, desc, comp);
  }

  @Override
  public int getNumberOfDataPoints() {
    return mzValues.length;
  }

  @Override
  public @NotNull IsotopePatternStatus getStatus() {
    return status;
  }

  @Override
  public @Nullable Integer getBasePeakIndex() {
    return highestIsotope;
  }

  @Override
  public @NotNull String getDescription() {
    return description;
  }

  @Override
  public String toString() {
    return "Isotope pattern: " + description;
  }

  @Override
  @NotNull
  public Range<Double> getDataPointMZRange() {
    return mzRange;
  }

  @Override
  public @NotNull Double getTIC() {
    return 0.0;
  }

  @Override
  public MassSpectrumType getSpectrumType() {
    return MassSpectrumType.CENTROIDED;
  }

  @Override
  public double[] getMzValues(@NotNull double[] dst) {
    if (dst.length < mzValues.length) {
      return mzValues;
    }

    for (int i = 0; i < mzValues.length; i++) {
      dst[i] = mzValues[i];
    }
    return dst;
  }

  @Override
  public double[] getIntensityValues(@NotNull double[] dst) {
    if (dst.length < intensityValues.length) {
      return intensityValues;
    }

    for (int i = 0; i < intensityValues.length; i++) {
      dst[i] = intensityValues[i];
    }
    return dst;
  }

  /*
   * @Override public DataPoint getHighestDataPoint() { if (highestIsotope < 0) return null; return
   * getDataPoints()[highestIsotope]; }
   *
   * @Override public DataPoint[] getDataPointsByMass(Range<Double> mzRange) {
   *
   * DataPoint[] dataPoints = getDataPoints(); int startIndex, endIndex; for (startIndex = 0;
   * startIndex < dataPoints.length; startIndex++) { if (dataPoints[startIndex].getMZ() >=
   * mzRange.lowerEndpoint()) { break; } }
   *
   * for (endIndex = startIndex; endIndex < dataPoints.length; endIndex++) { if
   * (dataPoints[endIndex].getMZ() > mzRange.upperEndpoint()) { break; } }
   *
   * DataPoint pointsWithinRange[] = new DataPoint[endIndex - startIndex];
   *
   * // Copy the relevant points System.arraycopy(dataPoints, startIndex, pointsWithinRange, 0,
   * endIndex - startIndex);
   *
   * return pointsWithinRange; }
   */

  public String getIsotopeComposition(int num) {
    if (isotopeCompostion != null && num < isotopeCompostion.length) {
      return isotopeCompostion[num];
    }
    return "";
  }

  public String[] getIsotopeCompositions() {
    if (isotopeCompostion != null) {
      return isotopeCompostion;
    }
    return null;
  }

  private DataPoint[] getDataPoints() {
    DataPoint d[] = new DataPoint[getNumberOfDataPoints()];
    for (int i = 0; i < getNumberOfDataPoints(); i++) {
      d[i] = new SimpleDataPoint(getMzValue(i), getIntensityValue(i));
    }
    return d;
  }

  @Override
  public double getMzValue(int index) {
    return mzValues[index];
  }

  @Override
  public double getIntensityValue(int index) {
    return intensityValues[index];
  }

  @Override
  @Nullable
  public Double getBasePeakMz() {
    if (highestIsotope < 0) {
      return null;
    } else {
      return mzValues[highestIsotope];
    }
  }

  @Override
  @Nullable
  public Double getBasePeakIntensity() {
    if (highestIsotope < 0) {
      return null;
    } else {
      return intensityValues[highestIsotope];
    }
  }

  @Override
  public Iterator<DataPoint> iterator() {
    return new DataPointIterator(this);
  }

  @Override
  public Stream<DataPoint> stream() {
    return Streams.stream(this);
  }

  @Override
  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);

    writer.writeStartElement(CONST.XML_MZ_VALUES_ELEMENT);
    writer.writeCharacters(ParsingUtils.doubleArrayToString(mzValues, mzValues.length));
    writer.writeEndElement();

    writer.writeStartElement(CONST.XML_INTENSITY_VALUES_ELEMENT);
    writer
        .writeCharacters(ParsingUtils.doubleArrayToString(intensityValues, intensityValues.length));
    writer.writeEndElement();

    writer.writeStartElement(XML_DESCRIPTION_ELEMENT);
    writer.writeCharacters(description);
    writer.writeEndElement();

    writer.writeStartElement(XML_STATUS_ELEMENT);
    writer.writeCharacters(status.name());
    writer.writeEndElement();

    if (isotopeCompostion != null) {
      writer.writeStartElement(XML_COMPOSITION_ELEMENT);
      writer.writeCharacters(ParsingUtils.stringArrayToString(isotopeCompostion));
      writer.writeEndElement();
    }

    writer.writeEndElement();
  }

  private class DataPointIterator implements Iterator<DataPoint>, DataPoint {

    private final MassSpectrum spectrum;
    // We start at -1 so the first call to next() moves us to index 0
    private int cursor = -1;

    DataPointIterator(MassSpectrum spectrum) {
      this.spectrum = spectrum;
    }

    @Override
    public boolean hasNext() {
      return (cursor + 1) < spectrum.getNumberOfDataPoints();
    }

    @Override
    public DataPoint next() {
      cursor++;
      return this;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    @Override
    public double getMZ() {
      return spectrum.getMzValue(cursor);
    }

    @Override
    public double getIntensity() {
      return spectrum.getIntensityValue(cursor);
    }
  }
}
