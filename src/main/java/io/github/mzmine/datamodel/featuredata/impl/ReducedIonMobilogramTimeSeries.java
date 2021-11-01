package io.github.mzmine.datamodel.featuredata.impl;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.featuredata.IntensitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonSeries;
import io.github.mzmine.datamodel.featuredata.IonSpectrumSeries;
import io.github.mzmine.datamodel.featuredata.MzSeries;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.ParsingUtils;
import java.nio.DoubleBuffer;
import java.util.List;
import java.util.Objects;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public class ReducedIonMobilogramTimeSeries implements IonMobilogramTimeSeries {

  public static final String XML_ELEMENT = "reducedionmobilogramtimeseries";

  private final List<Frame> frames;
  private final DoubleBuffer intensityValueBuffer;
  private final DoubleBuffer mzValueBuffer;
  private final SummedIntensityMobilitySeries summedMobilogram;

  public ReducedIonMobilogramTimeSeries(@NotNull final IonMobilogramTimeSeries series) {
    frames = series.getSpectraModifiable();
    intensityValueBuffer = series.getIntensityValueBuffer();
    summedMobilogram = series.getSummedMobilogram();
    mzValueBuffer = series.getMZValueBuffer();
  }

  public ReducedIonMobilogramTimeSeries(MemoryMapStorage storage, List<Frame> frames,
      double[] mzValues, double[] intensityValues, SummedIntensityMobilitySeries summed) {

    if (frames.size() != mzValues.length || mzValues.length != intensityValues.length) {
      throw new IllegalArgumentException(
          "m/z, intensity and frames do not have the same number of values.");
    }
    this.frames = frames;
    intensityValueBuffer = StorageUtils.storeValuesToDoubleBuffer(storage, intensityValues);
    summedMobilogram = summed;
    mzValueBuffer = StorageUtils.storeValuesToDoubleBuffer(storage, mzValues);
  }

  @Override
  public DoubleBuffer getIntensityValueBuffer() {
    return intensityValueBuffer;
  }

  @Override
  public IonMobilogramTimeSeries subSeries(@Nullable MemoryMapStorage storage,
      @NotNull List<Frame> subset, @NotNull BinningMobilogramDataAccess mobilogramBinning) {
    throw new UnsupportedOperationException(
        "Reduced traces do not support this operation. Please apply this module before reducing ion mobility traces.");
  }

  @Override
  public List<IonMobilitySeries> getMobilograms() {
    throw new UnsupportedOperationException(
        "Reduced traces do not support this operation. Please apply this module before reducing ion mobility traces.");
  }

  @Override
  public SummedIntensityMobilitySeries getSummedMobilogram() {
    return summedMobilogram;
  }

  @Override
  public IonMobilogramTimeSeries copyAndReplace(@Nullable MemoryMapStorage storage,
      @NotNull SummedIntensityMobilitySeries summedMobilogram) {
    return new ReducedIonMobilogramTimeSeries(storage, frames,
        DataPointUtils.getDoubleBufferAsArray(mzValueBuffer),
        DataPointUtils.getDoubleBufferAsArray(intensityValueBuffer), summedMobilogram);
  }

  @Override
  public List<Frame> getSpectra() {
    return frames;
  }

  @Override
  public IonMobilogramTimeSeries subSeries(@Nullable MemoryMapStorage storage,
      @NotNull List<Frame> subset) {
    throw new UnsupportedOperationException(
        "Reduced traces do not support this operation. Please apply this module before reducing ion mobility traces.");
  }

  @Override
  public IonSpectrumSeries<Frame> copyAndReplace(@Nullable MemoryMapStorage storage,
      @NotNull double[] newMzValues, @NotNull double[] newIntensityValues) {
    return new ReducedIonMobilogramTimeSeries(storage, frames, newMzValues, newIntensityValues,
        summedMobilogram.copy(storage));
  }

  @Override
  public void saveValueToXML(XMLStreamWriter writer, List<Frame> allScans)
      throws XMLStreamException {
    writer.writeStartElement(ReducedIonMobilogramTimeSeries.XML_ELEMENT);

    IntensitySeries.saveIntensityValuesToXML(writer, this);
    MzSeries.saveMzValuesToXML(writer, this);
    IonSpectrumSeries.saveSpectraIndicesToXML(writer, this, allScans);

    summedMobilogram.saveValueToXML(writer);

    writer.writeEndElement();
  }

  @Override
  public IonSeries copy(MemoryMapStorage storage) {
    return new ReducedIonMobilogramTimeSeries(storage, frames,
        DataPointUtils.getDoubleBufferAsArray(mzValueBuffer),
        DataPointUtils.getDoubleBufferAsArray(intensityValueBuffer),
        summedMobilogram.copy(storage));
  }

  @Override
  public DoubleBuffer getMZValueBuffer() {
    return mzValueBuffer;
  }

  @Override
  public List<Frame> getSpectraModifiable() {
    return frames;
  }

  public static IonMobilogramTimeSeries loadFromXML(@NotNull XMLStreamReader reader,
      @Nullable MemoryMapStorage storage, @NotNull IMSRawDataFile file) throws XMLStreamException {

    if (!reader.isStartElement() || !reader.getLocalName()
        .equals(ReducedIonMobilogramTimeSeries.XML_ELEMENT)) {
      throw new IllegalStateException("Wrong element position.");
    }

    List<Frame> scans = null;
    double[] mzs = null;
    double[] intensities = null;
    SummedIntensityMobilitySeries summedMobilogram = null;

    while (reader.hasNext()) {
      if (reader.isEndElement() && reader.getLocalName()
          .equals(ReducedIonMobilogramTimeSeries.XML_ELEMENT)) {
        break;
      }

      final int next = reader.next();
      if (next != XMLEvent.START_ELEMENT) {
        continue;
      }
      switch (reader.getLocalName()) {
        case CONST.XML_SCAN_LIST_ELEMENT -> {
          int[] indices = ParsingUtils.stringToIntArray(reader.getElementText());
          scans = ParsingUtils.getSublistFromIndices((List<Frame>) file.getFrames(), indices);
        }
        case CONST.XML_MZ_VALUES_ELEMENT -> mzs = ParsingUtils.stringToDoubleArray(
            reader.getElementText());
        case CONST.XML_INTENSITY_VALUES_ELEMENT -> intensities = ParsingUtils.stringToDoubleArray(
            reader.getElementText());
        case SummedIntensityMobilitySeries.XML_ELEMENT -> summedMobilogram = SummedIntensityMobilitySeries.loadFromXML(
            reader, storage);
      }
    }

    if (scans == null || mzs == null || intensities == null || summedMobilogram == null) {
      throw new IllegalStateException(
          "Incomplete parsing of " + ReducedIonMobilogramTimeSeries.class.getName());
    }

    return new ReducedIonMobilogramTimeSeries(storage, scans, mzs, intensities, summedMobilogram);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReducedIonMobilogramTimeSeries that = (ReducedIonMobilogramTimeSeries) o;
    return Objects.equals(frames, that.frames) && Objects.equals(getIntensityValueBuffer(),
        that.getIntensityValueBuffer()) && Objects.equals(mzValueBuffer, that.mzValueBuffer)
        && Objects.equals(getSummedMobilogram(), that.getSummedMobilogram())
        && MzSeries.seriesSubsetEqual(this, that) && IntensitySeries.seriesSubsetEqual(this, that);
  }

  @Override
  public int hashCode() {
    return Objects.hash(frames, getIntensityValueBuffer(), mzValueBuffer, getSummedMobilogram());
  }
}
