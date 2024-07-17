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

package io.github.mzmine.datamodel.msms;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.ParsingUtils;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.Nullable;

public class DIAMsMsInfoImpl implements MsMsInfo {

  public static final String XML_TYPE_NAME = "ddamsmsinfo";

  @Nullable
  private final Float activationEnergy;
  private final int msLevel;
  @NotNull
  private final ActivationMethod method;
  @Nullable
  private final Range<Double> isolationWindow;
  @Nullable
  private Scan msMsScan;

  public DIAMsMsInfoImpl(@Nullable Float activationEnergy, @Nullable Scan msMsScan, int msLevel,
      @NotNull ActivationMethod method, @Nullable Range<Double> isolationWindow) {
    this.activationEnergy = activationEnergy;
    this.msMsScan = msMsScan;
    this.msLevel = msLevel;
    this.method = method;
    this.isolationWindow = isolationWindow;
  }

  /**
   * @param reader A reader at an {@link io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl} element.
   * @return A loaded {@link io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl}.
   */
  public static DIAMsMsInfoImpl loadFromXML(XMLStreamReader reader, RawDataFile file,
      List<RawDataFile> allProjectFiles) {

    final Integer scanIndex = ParsingUtils.readAttributeValueOrDefault(reader,
        XML_FRAGMENT_SCAN_ATTR, null, Integer::parseInt);

    final Float activationEnergy = ParsingUtils.readAttributeValueOrDefault(reader,
        XML_ACTIVATION_ENERGY_ATTR, null, Float::parseFloat);

    final int msLevel = Integer.parseInt(reader.getAttributeValue(null, XML_MSLEVEL_ATTR));

    final ActivationMethod method = ActivationMethod.valueOf(
        reader.getAttributeValue(null, XML_ACTIVATION_TYPE_ATTR));

    final Range<Double> isolationWindow = ParsingUtils.readAttributeValueOrDefault(reader,
        XML_ISOLATION_WINDOW_ATTR, null, ParsingUtils::stringToDoubleRange);

    final String rawFileName = ParsingUtils.readAttributeValueOrDefault(reader,
        CONST.XML_RAW_FILE_ELEMENT, null, s -> s);

    // use the correct file if the ms info comes from a different file.
    if (rawFileName != null && !rawFileName.equals(file != null ? file.getName() : null)) {
      file = allProjectFiles.stream().filter(f -> f.getName().equals(rawFileName)).findFirst()
          .orElse(file);
    }

    return new DIAMsMsInfoImpl(activationEnergy,
        scanIndex != null && scanIndex != -1 && file != null ? file.getScan(scanIndex) : null,
        msLevel, method, isolationWindow);
  }

  @Override
  public @Nullable Float getActivationEnergy() {
    return activationEnergy;
  }

  @NotNull
  @Override
  public @Nullable Scan getMsMsScan() {
    return msMsScan;
  }

  @Override
  public @Nullable Range<Double> getIsolationWindow() {
    return isolationWindow;
  }

  public boolean setMsMsScan(Scan scan) {
    if (msMsScan != null && !msMsScan.equals(scan)) {
      return false;
    }
    msMsScan = scan;
    return true;
  }

  @Override
  public int getMsLevel() {
    return msLevel;
  }

  @Override
  public @NotNull ActivationMethod getActivationMethod() {
    return method;
  }

  /**
   * Appends a new element for an {@link io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl} at the
   * current position. Start and close tag for this
   * {@link io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl} are created in this method.
   *
   * @param writer The writer to use.
   */
  @Override
  public void writeToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);
    writer.writeAttribute(XML_TYPE_ATTRIBUTE, XML_TYPE_NAME);

    if (getMsMsScan() != null) {
      writer.writeAttribute(XML_FRAGMENT_SCAN_ATTR,
          String.valueOf(getMsMsScan().getDataFile().getScans().indexOf(getMsMsScan())));
      writer.writeAttribute(CONST.XML_RAW_FILE_ELEMENT, getMsMsScan().getDataFile().getName());
    }

    if (getActivationEnergy() != null) {
      writer.writeAttribute(XML_ACTIVATION_ENERGY_ATTR, String.valueOf(this.getActivationEnergy()));
    }
    writer.writeAttribute(XML_ACTIVATION_TYPE_ATTR, this.getActivationMethod().name());
    writer.writeAttribute(XML_MSLEVEL_ATTR, String.valueOf(getMsLevel()));

    if (getIsolationWindow() != null) {
      writer.writeAttribute(XML_ISOLATION_WINDOW_ATTR,
          ParsingUtils.rangeToString((Range) getIsolationWindow()));
    }

    writer.writeEndElement();
  }

  @Override
  public MsMsInfo createCopy() {
    return new DIAMsMsInfoImpl(activationEnergy, null, msLevel, method, getIsolationWindow());
  }
}
