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

package io.github.mzmine.datamodel.featuredata.impl;

import static io.github.mzmine.datamodel.featuredata.impl.StorageUtils.contentEquals;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.featuredata.IntensitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.MobilitySeries;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.IonMobilityUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.ParsingUtils;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.DoubleBuffer;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stores a summed mobilogram based on the intesities of the frame-specific mobilograms in the
 * constructor. It is guaranteed that mobility values are ascending with index.
 *
 * @author https://github.com/SteffenHeu
 */
public class SummedIntensityMobilitySeries implements IntensitySeries, MobilitySeries {

  public static final String XML_ELEMENT = "summedmobilogram";

  final MemorySegment intensityValues;
  final MemorySegment mobilityValues;

  /**
   * Creates a summed intensity and mobility series
   *
   * @param storage     May be null, if values shall be stored in ram.
   * @param mobilograms
   */
  public SummedIntensityMobilitySeries(@Nullable MemoryMapStorage storage,
      @NotNull List<IonMobilitySeries> mobilograms) {

    Frame exampleFrame = mobilograms.get(0).getSpectra().get(0).getFrame();
    final double smallestDelta = IonMobilityUtils.getSmallestMobilityDelta(exampleFrame);

    final RangeMap<Double, Double> mobilityIntensityValues = TreeRangeMap.create();

    for (int i = 0; i < mobilograms.size(); i++) {
      IonMobilitySeries mobilogram = mobilograms.get(i);
      for (int j = 0; j < mobilogram.getNumberOfValues(); j++) {
        double intensity = mobilogram.getIntensity(j);
        double mobility = mobilogram.getMobility(j);
        Entry<Range<Double>, Double> entry = mobilityIntensityValues.getEntry(mobility);
        if (entry != null) {
          mobilityIntensityValues.put(entry.getKey(), entry.getValue() + intensity);
        } else {
          mobilityIntensityValues
              .put(Range.open(mobility - smallestDelta / 2, mobility + smallestDelta / 2),
                  intensity);
        }
      }
    }

    // this causes tims mobilities to be reordered
    Map<Range<Double>, Double> mapOfRanges = mobilityIntensityValues.asMapOfRanges();

    double[] mobilities = mapOfRanges.keySet().stream()
        .mapToDouble(key -> (key.upperEndpoint() + key.lowerEndpoint()) / 2).toArray();
    double[] intensities = mapOfRanges.values().stream().mapToDouble(Double::doubleValue).toArray();

    mobilityValues = StorageUtils.storeValuesToDoubleBuffer(storage, mobilities);
    intensityValues = StorageUtils.storeValuesToDoubleBuffer(storage, intensities);
  }

  /**
   * Package private use intended. Can be created by a {@link SimpleIonMobilogramTimeSeries} after
   * smoothing.
   *
   * @param storage
   * @param mobilities
   * @param intensities
   */
  public SummedIntensityMobilitySeries(@Nullable MemoryMapStorage storage, double[] mobilities,
      double[] intensities) {
    if (mobilities.length > 1) {
      assert mobilities[0] < mobilities[1];
    }

    mobilityValues = StorageUtils.storeValuesToDoubleBuffer(storage, mobilities);
    intensityValues = StorageUtils.storeValuesToDoubleBuffer(storage, intensities);
  }

  public int getNumberOfDataPoints() {
    return (int) StorageUtils.numDoubles(getMobilityValues());
  }

  public double getIntensity(int index) {
    return getIntensityValueBuffer().getAtIndex(ValueLayout.JAVA_DOUBLE, index);
  }

  /**
   * Note: Since this is a summed mobilogram, the data points were summed at a given mobility, not
   * necessarily at the same mobility scan number. Therefore, a list of scans is not provided.
   * <p></p>
   * THe mobility values are guaranteed to be ascending.
   *
   * @param index
   * @return
   */
  public double getMobility(int index) {
    return getMobilityValues().getAtIndex(ValueLayout.JAVA_DOUBLE, index);
  }

  public MemorySegment getIntensityValueBuffer() {
    return intensityValues;
  }

  public MemorySegment getMobilityValues() {
    return mobilityValues;
  }

  public double[] getMobilityValues(double[] dst) {
    if (dst.length < getNumberOfValues()) {
      dst = new double[getNumberOfValues()];
    }
    MemorySegment.copy(getMobilityValues(), ValueLayout.JAVA_DOUBLE, 0, dst, 0, getNumberOfDataPoints());
    return dst;
  }

  public SummedIntensityMobilitySeries copy(@Nullable MemoryMapStorage storage) {
    return new SummedIntensityMobilitySeries(storage,
        DataPointUtils.getDoubleBufferAsArray(mobilityValues),
        DataPointUtils.getDoubleBufferAsArray(intensityValues));
  }

  public String print() {
    StringBuilder builder = new StringBuilder();
    builder.append("Num values: ");
    builder.append(getNumberOfValues());
    builder.append("/");
    builder.append(getNumberOfDataPoints());
    builder.append(": ");
    for (int i = 0; i < getNumberOfValues(); i++) {
      builder.append(String.format("(%2.5f", getMobility(i)));
      builder.append(", ");
      builder.append(String.format("(%.1f", getIntensity(i)));
      builder.append("), ");
    }
    return builder.toString();
  }

  public void saveValueToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);
    writer
        .writeAttribute(CONST.XML_NUM_VALUES_ATTR, String.valueOf(getNumberOfValues()));

    writer.writeStartElement(CONST.XML_MOBILITY_VALUES_ELEMENT);
    writer
        .writeAttribute(CONST.XML_NUM_VALUES_ATTR, String.valueOf(getNumberOfValues()));
    writer.writeCharacters(ParsingUtils.doubleBufferToString(getMobilityValues()));
    writer.writeEndElement();

    IntensitySeries.saveIntensityValuesToXML(writer, this);
    writer.writeEndElement();
  }

  public static SummedIntensityMobilitySeries loadFromXML(@NotNull XMLStreamReader reader,
      @Nullable MemoryMapStorage storage) throws XMLStreamException {

    double[] mobilities = null;
    double[] intensities = null;

    while (reader.hasNext()) {
      if (reader.isEndElement() && reader.getLocalName()
          .equals(SummedIntensityMobilitySeries.XML_ELEMENT)) {
        break;
      }

      final int next = reader.next();
      if (next != XMLEvent.START_ELEMENT) {
        continue;
      }
      switch (reader.getLocalName()) {
        case CONST.XML_INTENSITY_VALUES_ELEMENT -> intensities = ParsingUtils
            .stringToDoubleArray(reader.getElementText());
        case CONST.XML_MOBILITY_VALUES_ELEMENT -> mobilities = ParsingUtils
            .stringToDoubleArray(reader.getElementText());
      }
    }
    return new SummedIntensityMobilitySeries(storage, mobilities, intensities);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SummedIntensityMobilitySeries)) {
      return false;
    }
    SummedIntensityMobilitySeries that = (SummedIntensityMobilitySeries) o;
    return contentEquals(intensityValues, that.intensityValues) && contentEquals(
        getMobilityValues(), that.getMobilityValues())
        && IntensitySeries.seriesSubsetEqual(this, that);
  }

  @Override
  public int hashCode() {
    return Objects.hash(intensityValues.byteSize(), mobilityValues.byteSize());
  }
}
