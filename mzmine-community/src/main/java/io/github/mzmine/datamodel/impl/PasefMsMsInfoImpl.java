/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import io.github.mzmine.modules.io.projectload.CachedIMSRawDataFile;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.ParsingUtils;
import java.util.List;
import java.util.Objects;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.Nullable;

/**
 * @author https://github.com/SteffenHeu
 * @see PasefMsMsInfo
 */
public class PasefMsMsInfoImpl implements PasefMsMsInfo {

  public static final String XML_TYPE_NAME = "pasefmsmsinfo";

  private final double precursorMz;
  private final Range<Integer> spectrumNumberRange;
  private final Float collisionEnergy;
  private final Integer precursorCharge;
  private final Range<Double> isolationWindow;
  private Frame parentFrame;
  private Frame fragmentFrame;

  public PasefMsMsInfoImpl(double precursorMz, Range<Integer> spectrumNumberRange,
      @Nullable Float collisionEnergy, @Nullable Integer precursorCharge,
      @Nullable Frame parentScan, @Nullable Frame fragmentFrameNumber,
      @Nullable Range<Double> isolationWindow) {

    if (parentScan != null && parentScan.getMSLevel() != 1) {
      throw new IllegalArgumentException("Parent frame is not of ms level 1");
    }
    if (fragmentFrameNumber != null && fragmentFrameNumber.getMSLevel() < 2) {
      throw new IllegalArgumentException("Fragment frame is not of ms level >= 2");
    }

    this.precursorMz = precursorMz;
    this.spectrumNumberRange = spectrumNumberRange;
    this.collisionEnergy = collisionEnergy;
    this.precursorCharge = precursorCharge;
    this.parentFrame = parentScan;
    this.fragmentFrame = fragmentFrameNumber;
    this.isolationWindow = isolationWindow;
  }

  /**
   * @param reader          A reader at an {@link PasefMsMsInfoImpl} element.
   * @param allProjectFiles
   * @return A loaded {@link PasefMsMsInfoImpl}.
   */
  public static PasefMsMsInfoImpl loadFromXML(XMLStreamReader reader, IMSRawDataFile file,
      List<RawDataFile> allProjectFiles) {
    if (!reader.isStartElement() && reader.getAttributeValue(null, XML_TYPE_ATTRIBUTE)
        .equals(XML_TYPE_NAME)) {
      throw new IllegalStateException("Wrong msms info type.");
    }

    final double precursorMz = Double.parseDouble(
        reader.getAttributeValue(null, XML_PRECURSOR_MZ_ATTR));

    final Integer precursorCharge = ParsingUtils.readAttributeValueOrDefault(reader,
        XML_PRECURSOR_CHARGE_ATTR, null, Integer::parseInt);

    final Integer frameIndex = ParsingUtils.readAttributeValueOrDefault(reader,
        MsMsInfo.XML_FRAGMENT_SCAN_ATTR, null, Integer::parseInt);

    final Integer parentFrameIndex = ParsingUtils.readAttributeValueOrDefault(reader,
        XML_PARENT_SCAN_ATTR, null, Integer::parseInt);

    final Float collisionEnergy = ParsingUtils.readAttributeValueOrDefault(reader,
        MsMsInfo.XML_ACTIVATION_ENERGY_ATTR, null, Float::parseFloat);

    final Range<Double> isolationWindow = ParsingUtils.readAttributeValueOrDefault(reader,
        MsMsInfo.XML_ISOLATION_WINDOW_ATTR, null, ParsingUtils::stringToDoubleRange);

    Range<Integer> spectrumRange = ParsingUtils.parseIntegerRange(
        reader.getAttributeValue(null, XML_SPECTRUM_NUMBER_RANGE_ATTR));

    final String rawFileName = ParsingUtils.readAttributeValueOrDefault(reader,
        CONST.XML_RAW_FILE_ELEMENT, null, s -> s);

    // use the correct file if the ms info comes from a different file.
    if (rawFileName != null && !rawFileName.equals(file != null ? file.getName() : null)) {
      file = (IMSRawDataFile) allProjectFiles.stream()
          .filter(f -> f.getName().equals(rawFileName) && f instanceof IMSRawDataFile).findFirst()
          .orElse(file);
    }

    // replace by original file so we store the original frames and not the cached ones.
    if (file instanceof CachedIMSRawDataFile cachedIMSRawDataFile) {
      file = (IMSRawDataFile) cachedIMSRawDataFile.getOriginalFile();
    }

    return new PasefMsMsInfoImpl(precursorMz, spectrumRange, collisionEnergy, precursorCharge,
        parentFrameIndex != null && file != null ? file.getFrame(parentFrameIndex) : null,
        frameIndex != null && file != null ? file.getFrame(frameIndex) : null, isolationWindow);
  }

  @Override
  public @Nullable Scan getParentScan() {
    return getParentFrame();
  }

  @Override
  public @Nullable Scan getMsMsScan() {
    return getMsMsFrame();
  }

  @Override
  public Frame getMsMsFrame() {
    return fragmentFrame;
  }

  @Override
  public Frame getParentFrame() {
    return parentFrame;
  }

  @Override
  public double getIsolationMz() {
    return precursorMz;
  }

  @Override
  public Range<Integer> getSpectrumNumberRange() {
    return spectrumNumberRange;
  }

  @Override
  public Float getActivationEnergy() {
    return collisionEnergy;
  }

  @Override
  public int getMsLevel() {
    return 2;
  }

  @Override
  public @Nullable ActivationMethod getActivationMethod() {
    return ActivationMethod.CID;
  }

  @Override
  public @Nullable Range<Double> getIsolationWindow() {
    return isolationWindow;
  }

  @Override
  public Integer getPrecursorCharge() {
    return precursorCharge;
  }

  @Override
  public boolean setMsMsScan(Scan scan) {
    if (!(scan instanceof Frame frame)) {
      return false;
    }
    if (fragmentFrame != null && !fragmentFrame.equals(scan)) {
      return false;
    }
    fragmentFrame = frame;
    return true;
  }

  @Override
  public String toString() {
    return "m/z " + precursorMz + " - Scans " + spectrumNumberRange.toString();
  }

  /**
   * Appends a new element for an {@link PasefMsMsInfoImpl} at the current position. Start and close
   * tag for this {@link PasefMsMsInfoImpl} are created in this method.
   *
   * @param writer The writer to use.
   */
  @Override
  public void writeToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);
    writer.writeAttribute(XML_TYPE_ATTRIBUTE, XML_TYPE_NAME);
    writer.writeAttribute(XML_PRECURSOR_MZ_ATTR, String.valueOf(getIsolationMz()));

    if (getPrecursorCharge() != null) {
      writer.writeAttribute(XML_PRECURSOR_CHARGE_ATTR, String.valueOf(getPrecursorCharge()));
    }

    // todo nullable
    if (fragmentFrame != null) {
      writer.writeAttribute(MsMsInfo.XML_FRAGMENT_SCAN_ATTR,
          String.valueOf(fragmentFrame.getDataFile().getScans().indexOf(fragmentFrame)));
      writer.writeAttribute(CONST.XML_RAW_FILE_ELEMENT, fragmentFrame.getDataFile().getName());
    }

    if (parentFrame != null) {
      writer.writeAttribute(XML_PARENT_SCAN_ATTR,
          String.valueOf(getParentFrame().getDataFile().getScans().indexOf(getParentFrame())));
    }

    if (getActivationEnergy() != null) {
      writer.writeAttribute(MsMsInfo.XML_ACTIVATION_ENERGY_ATTR,
          String.valueOf(this.getActivationEnergy()));
    }

    writer.writeAttribute(XML_SPECTRUM_NUMBER_RANGE_ATTR,
        ParsingUtils.rangeToString((Range) getSpectrumNumberRange()));

    if (getIsolationWindow() != null) {
      writer.writeAttribute(MsMsInfo.XML_ISOLATION_WINDOW_ATTR,
          ParsingUtils.rangeToString((Range) getIsolationWindow()));
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
    PasefMsMsInfoImpl that = (PasefMsMsInfoImpl) o;
    return Double.compare(that.precursorMz, precursorMz) == 0 && Objects.equals(
        getSpectrumNumberRange(), that.getSpectrumNumberRange()) && Objects.equals(collisionEnergy,
        that.collisionEnergy) && Objects.equals(getPrecursorCharge(), that.getPrecursorCharge())
        && Objects.equals(getIsolationWindow(), that.getIsolationWindow()) && Objects.equals(
        getParentFrame(), that.getParentFrame()) && Objects.equals(fragmentFrame,
        that.fragmentFrame);
  }

  @Override
  public int hashCode() {
    return Objects.hash(precursorMz, getSpectrumNumberRange(), collisionEnergy,
        getPrecursorCharge(), getIsolationWindow(), getParentFrame(), fragmentFrame);
  }

  @Override
  public MsMsInfo createCopy() {
    return new PasefMsMsInfoImpl(precursorMz, spectrumNumberRange, collisionEnergy, precursorCharge,
        parentFrame, fragmentFrame, isolationWindow);
  }
}
