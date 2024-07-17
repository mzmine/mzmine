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
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.MSnInfoImpl;
import io.github.mzmine.datamodel.impl.PasefMsMsInfoImpl;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.Nullable;

public interface MsMsInfo {

  String XML_ELEMENT = "msmsinfo";
  String XML_TYPE_ATTRIBUTE = "type";
  String XML_FRAGMENT_SCAN_ATTR = "fragmentscan";
  String XML_ACTIVATION_ENERGY_ATTR = "energy";
  String XML_ACTIVATION_TYPE_ATTR = "activationtype";
  String XML_MSLEVEL_ATTR = "mslevel";
  String XML_ISOLATION_WINDOW_ATTR = "isolationwindow";

  /**
   * Reads a {@link MsMsInfo} from an XML file. The current position must be the start element of
   * the {@link MsMsInfo}.
   *
   * @param reader The reader.
   * @param file   The file this ms ms info belongs to.
   * @return The {@link MsMsInfo}.
   */
  static MsMsInfo loadFromXML(XMLStreamReader reader, RawDataFile file,
      List<RawDataFile> allProjectFiles) {
    if (!reader.isStartElement() || !reader.getLocalName().equals(MsMsInfo.XML_ELEMENT)) {
      throw new IllegalStateException("Wrong element.");
    }

    return switch (reader.getAttributeValue(null, XML_TYPE_ATTRIBUTE)) {
      case PasefMsMsInfoImpl.XML_TYPE_NAME ->
          PasefMsMsInfoImpl.loadFromXML(reader, (IMSRawDataFile) file, allProjectFiles);
      case DDAMsMsInfoImpl.XML_TYPE_NAME ->
          DDAMsMsInfoImpl.loadFromXML(reader, file, allProjectFiles);
      case MSnInfoImpl.XML_TYPE_NAME -> MSnInfoImpl.loadFromXML(reader, file, allProjectFiles);
      default -> throw new IllegalStateException("Unknown msms info type");
    };
  }

  /**
   * @return The energy used to activate this fragmentation or null if unknown;
   */
  @Nullable Float getActivationEnergy();

  /**
   * @return The scan this MS/MS event was executed in or null if it has not been set.
   */
  @Nullable Scan getMsMsScan();

  /**
   * @param scan The scan this event took place.
   * @return false if the msms scan was already set.
   */
  boolean setMsMsScan(Scan scan);

  /**
   * @return The MS level of this fragmentation.
   */
  int getMsLevel();

  /**
   * @return The activation method of this fragmentation. {@link ActivationMethod#UNKNOWN} if
   * unknown.
   */
  @NotNull ActivationMethod getActivationMethod();

  /**
   * @return The isolation window of this msms event. May be null if unknown or not set, cover a
   * small range(DDA) or a larger m/z range (SWATH/DIA).
   */
  @Nullable Range<Double> getIsolationWindow();

  /**
   * Appends a new element for an {@link MsMsInfo} at the current position. Start and close tag for
   * this {@link MsMsInfo} are created in this method.
   *
   * @param writer The writer to use.
   */
  void writeToXML(XMLStreamWriter writer) throws XMLStreamException;

  /**
   * @return A copy without setting the MsMsScan.
   */
  MsMsInfo createCopy();
}
