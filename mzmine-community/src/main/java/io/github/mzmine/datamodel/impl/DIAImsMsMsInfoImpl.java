/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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
import io.github.mzmine.datamodel.SimpleRange;
import io.github.mzmine.datamodel.SimpleRange.SimpleDoubleRange;
import io.github.mzmine.datamodel.SimpleRange.SimpleIntegerRange;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.IonMobilityMsMsInfo;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.modules.io.projectload.CachedIMSRawDataFile;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.ParsingUtils;
import java.util.List;
import java.util.Objects;
import javax.validation.constraints.Null;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.Nullable;

public class DIAImsMsMsInfoImpl implements IonMobilityMsMsInfo {

  public static final String XML_TYPE_NAME = "imsdiaimsmsinfo";

  @Nullable
  private final SimpleIntegerRange spectrumNumberRange;
  private final Float collisionEnergy;
  @Nullable
  private final SimpleDoubleRange isolationWindow;
  private Frame fragmentFrame;

  public DIAImsMsMsInfoImpl(@Nullable Range<Integer> spectrumNumberRange,
      @Nullable Float collisionEnergy, @Nullable Frame fragmentFrameNumber,
      @Nullable Range<Double> isolationWindow) {

    if (fragmentFrameNumber != null && fragmentFrameNumber.getMSLevel() < 2) {
      throw new IllegalArgumentException("Fragment frame is not of ms level >= 2");
    }

    this.spectrumNumberRange = SimpleRange.ofInteger(spectrumNumberRange);
    this.collisionEnergy = collisionEnergy;
    this.fragmentFrame = fragmentFrameNumber;
    this.isolationWindow = SimpleRange.ofDouble(isolationWindow);
  }

  /**
   * @param reader          A reader at an {@link PasefMsMsInfoImpl} element.
   * @param allProjectFiles
   * @return A loaded {@link PasefMsMsInfoImpl}.
   */
  public static DIAImsMsMsInfoImpl loadFromXML(XMLStreamReader reader, IMSRawDataFile file,
      List<RawDataFile> allProjectFiles) {
    if (!reader.isStartElement() && reader.getAttributeValue(null, XML_TYPE_ATTRIBUTE)
        .equals(XML_TYPE_NAME)) {
      throw new IllegalStateException("Wrong msms info type.");
    }

    final Integer frameIndex = ParsingUtils.readAttributeValueOrDefault(reader,
        MsMsInfo.XML_FRAGMENT_SCAN_ATTR, null, Integer::parseInt);

    final Float collisionEnergy = ParsingUtils.readAttributeValueOrDefault(reader,
        MsMsInfo.XML_ACTIVATION_ENERGY_ATTR, null, Float::parseFloat);

    final Range<Double> isolationWindow = ParsingUtils.readAttributeValueOrDefault(reader,
        MsMsInfo.XML_ISOLATION_WINDOW_ATTR, null, ParsingUtils::stringToDoubleRange);

    Range<Integer> spectrumRange = ParsingUtils.parseIntegerRange(
        reader.getAttributeValue(null, IonMobilityMsMsInfo.XML_SPECTRUM_NUMBER_RANGE_ATTR));

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

    return new DIAImsMsMsInfoImpl(spectrumRange, collisionEnergy,
        frameIndex != null && file != null ? file.getFrame(frameIndex) : null, isolationWindow);
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
  public Range<Integer> getSpectrumNumberRange() {
    return spectrumNumberRange != null ? spectrumNumberRange.guava() : null;
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
    return isolationWindow != null ? isolationWindow.guava() : null;
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
    return "m/z %s - Scans %s".formatted(getIsolationWindow().toString(),
        spectrumNumberRange.toString());
  }

  /**
   * Appends a new element for an {@link DIAImsMsMsInfoImpl} at the current position. Start and
   * close tag for this {@link DIAImsMsMsInfoImpl} are created in this method.
   *
   * @param writer The writer to use.
   */
  @Override
  public void writeToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);
    writer.writeAttribute(XML_TYPE_ATTRIBUTE, XML_TYPE_NAME);

    if (fragmentFrame != null) {
      writer.writeAttribute(MsMsInfo.XML_FRAGMENT_SCAN_ATTR,
          String.valueOf(fragmentFrame.getDataFile().getScans().indexOf(fragmentFrame)));
      writer.writeAttribute(CONST.XML_RAW_FILE_ELEMENT, fragmentFrame.getDataFile().getName());
    }

    if (getActivationEnergy() != null) {
      writer.writeAttribute(MsMsInfo.XML_ACTIVATION_ENERGY_ATTR,
          String.valueOf(this.getActivationEnergy()));
    }

    writer.writeAttribute(IonMobilityMsMsInfo.XML_SPECTRUM_NUMBER_RANGE_ATTR,
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
    DIAImsMsMsInfoImpl that = (DIAImsMsMsInfoImpl) o;
    return Objects.equals(getSpectrumNumberRange(), that.getSpectrumNumberRange())
        && Objects.equals(collisionEnergy, that.collisionEnergy) && Objects.equals(
        getIsolationWindow(), that.getIsolationWindow()) && Objects.equals(fragmentFrame,
        that.fragmentFrame);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getSpectrumNumberRange(), collisionEnergy, getIsolationWindow(),
        fragmentFrame);
  }

  @Override
  public IonMobilityMsMsInfo createCopy() {
    return new DIAImsMsMsInfoImpl(spectrumNumberRange != null ? spectrumNumberRange.guava() : null,
        collisionEnergy, fragmentFrame, isolationWindow != null ? isolationWindow.guava() : null);
  }
}
