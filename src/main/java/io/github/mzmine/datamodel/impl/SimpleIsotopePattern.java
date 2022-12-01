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

package io.github.mzmine.datamodel.impl;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.ParsingUtils;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
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
  private static final String XML_CHARGE_ELEMENT = "charge";

  private final double[] mzValues;
  private final double[] intensityValues;
  private final double tic;
  private final int charge;
  private final int highestIsotope;
  private final IsotopePatternStatus status;
  private final String description;
  private final Range<Double> mzRange;
  private final String[] isotopeCompostion;


  public SimpleIsotopePattern(double[] mzValues, double[] intensityValues, int charge,
      IsotopePatternStatus status, String description) {
    this(mzValues, intensityValues, charge, status, description, null);
  }


  public SimpleIsotopePattern(DataPoint[] dataPoints, int charge, IsotopePatternStatus status,
      String description) {
    this(dataPoints, charge, status, description, null);
  }

  public SimpleIsotopePattern(DataPoint[] dataPoints, int charge, IsotopePatternStatus status,
      String description, String[] isotopeCompostion) {

    mzValues = new double[dataPoints.length];
    intensityValues = new double[dataPoints.length];
    double tic = 0;
    for (int i = 0; i < dataPoints.length; i++) {
      mzValues[i] = dataPoints[i].getMZ();
      intensityValues[i] = dataPoints[i].getIntensity();
      tic += intensityValues[i];
    }
    this.tic = tic;
    this.charge = charge;
    this.status = status;
    this.description = description;
    this.isotopeCompostion = isotopeCompostion;
    this.mzRange = ScanUtils.findMzRange(mzValues);
    this.highestIsotope = ScanUtils.findTopDataPoint(intensityValues);
  }

  public SimpleIsotopePattern(double[] mzValues, double[] intensityValues, int charge,
      IsotopePatternStatus status, String description, String[] isotopeCompostion) {

    assert mzValues.length > 0;
    assert mzValues.length == intensityValues.length;

    this.charge = charge;
    this.mzValues = mzValues;
    this.intensityValues = intensityValues;
    this.status = status;
    this.description = description;
    this.isotopeCompostion = isotopeCompostion;
    this.mzRange = ScanUtils.findMzRange(mzValues);
    this.highestIsotope = ScanUtils.findTopDataPoint(intensityValues);
    this.tic = Arrays.stream(intensityValues).sum();
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
    int charge = 1;

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(XML_ELEMENT))) {
      int next = reader.next();
      if (next != XMLEvent.START_ELEMENT) {
        continue;
      }

      switch (reader.getLocalName()) {
        case CONST.XML_MZ_VALUES_ELEMENT ->
            mzs = ParsingUtils.stringToDoubleArray(reader.getElementText());
        case CONST.XML_INTENSITY_VALUES_ELEMENT ->
            intensities = ParsingUtils.stringToDoubleArray(reader.getElementText());
        case XML_DESCRIPTION_ELEMENT -> desc = reader.getElementText();
        case XML_COMPOSITION_ELEMENT -> {
          if (!reader.getElementText().trim().isEmpty()) {
            comp = ParsingUtils.stringToStringArray(reader.getElementText());
          }
        }
        case XML_STATUS_ELEMENT -> status = IsotopePatternStatus.valueOf(reader.getElementText());
        case XML_CHARGE_ELEMENT -> charge = Integer.parseInt(reader.getElementText());
      }
    }
    return new SimpleIsotopePattern(mzs, intensities, charge, status, desc, comp);
  }

  @Override
  public int getCharge() {
    return charge;
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
    return tic;
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

    System.arraycopy(mzValues, 0, dst, 0, mzValues.length);
    return dst;
  }

  @Override
  public double[] getIntensityValues(@NotNull double[] dst) {
    if (dst.length < intensityValues.length) {
      return intensityValues;
    }

    System.arraycopy(intensityValues, 0, dst, 0, intensityValues.length);
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
    DataPoint[] d = new DataPoint[getNumberOfDataPoints()];
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
  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);

    writer.writeStartElement(CONST.XML_MZ_VALUES_ELEMENT);
    writer.writeCharacters(ParsingUtils.doubleArrayToString(mzValues, mzValues.length));
    writer.writeEndElement();

    writer.writeStartElement(CONST.XML_INTENSITY_VALUES_ELEMENT);
    writer.writeCharacters(
        ParsingUtils.doubleArrayToString(intensityValues, intensityValues.length));
    writer.writeEndElement();

    writer.writeStartElement(XML_DESCRIPTION_ELEMENT);
    writer.writeCharacters(description);
    writer.writeEndElement();

    writer.writeStartElement(XML_STATUS_ELEMENT);
    writer.writeCharacters(status.name());
    writer.writeEndElement();

    writer.writeStartElement(XML_CHARGE_ELEMENT);
    writer.writeCharacters(String.valueOf(charge));
    writer.writeEndElement();

    if (isotopeCompostion != null) {
      writer.writeStartElement(XML_COMPOSITION_ELEMENT);
      writer.writeCharacters(ParsingUtils.stringArrayToString(isotopeCompostion));
      writer.writeEndElement();
    }

    writer.writeEndElement();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SimpleIsotopePattern that = (SimpleIsotopePattern) o;
    return charge == that.charge && Arrays.equals(mzValues, that.mzValues) && Arrays.equals(
        intensityValues, that.intensityValues) && status == that.status && Objects.equals(
        description, that.description);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(charge, status, description);
    result = 31 * result + Arrays.hashCode(mzValues);
    result = 31 * result + Arrays.hashCode(intensityValues);
    return result;
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
