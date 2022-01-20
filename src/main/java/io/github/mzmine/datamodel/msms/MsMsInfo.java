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

package io.github.mzmine.datamodel.msms;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.MSnInfoImpl;
import io.github.mzmine.datamodel.impl.PasefMsMsInfoImpl;
import javax.validation.constraints.NotNull;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.Nullable;

public interface MsMsInfo {

  String XML_ELEMENT = "msmsinfo";
  String XML_TYPE_ATTRIBUTE = "type";

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
   * Reads a {@link MsMsInfo} from an XML file. The current position must be the start element of
   * the {@link MsMsInfo}.
   *
   * @param reader The reader.
   * @param file   The file this ms ms info belongs to.
   * @return The {@link MsMsInfo}.
   */
  static MsMsInfo loadFromXML(XMLStreamReader reader, RawDataFile file) {
    if (!reader.isStartElement()) {
      throw new IllegalStateException("Wrong element.");
    }

    return switch (reader.getAttributeValue(null, XML_TYPE_ATTRIBUTE)) {
      case PasefMsMsInfoImpl.XML_TYPE_NAME -> PasefMsMsInfoImpl.loadFromXML(reader,
          (IMSRawDataFile) file);
      case DDAMsMsInfoImpl.XML_TYPE_NAME -> DDAMsMsInfoImpl.loadFromXML(reader, file);
      case MSnInfoImpl.XML_TYPE_NAME -> MSnInfoImpl.loadFromXML(reader, file);
      default -> throw new IllegalStateException("Unknown msms info type");
    };
  }

  /**
   * @return A copy without setting the MsMsScan.
   */
  MsMsInfo createCopy();
}
