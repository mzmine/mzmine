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

package io.github.mzmine.datamodel.impl;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImsMsMsInfo;
import io.github.mzmine.util.ParsingUtils;
import java.util.Objects;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

/**
 * @author https://github.com/SteffenHeu
 * @see io.github.mzmine.datamodel.ImsMsMsInfo
 */
public class ImsMsMsInfoImpl implements ImsMsMsInfo {

  private final double precursorMz;
  private final Range<Integer> spectrumNumberRange;
  private final float collisionEnergy;
  private final int precursorCharge;
  private final Frame parentFrameNumber;
  private final Frame fragmentFrameNumber;

  public ImsMsMsInfoImpl(double precursorMz, Range<Integer> spectrumNumberRange,
      float collisionEnergy, int precursorCharge, Frame parentFrameNumber,
      Frame fragmentFrameNumber) {

    if(parentFrameNumber.getMSLevel() != 1) {
      throw new IllegalArgumentException("Parent frame is not of ms level 1");
    }
    if(fragmentFrameNumber.getMSLevel() < 2) {
      throw new IllegalArgumentException("Fragment frame is not of ms level >= 2");
    }

    this.precursorMz = precursorMz;
    this.spectrumNumberRange = spectrumNumberRange;
    this.collisionEnergy = collisionEnergy;
    this.precursorCharge = precursorCharge;
    this.parentFrameNumber = parentFrameNumber;
    this.fragmentFrameNumber = fragmentFrameNumber;
  }

  @Override
  public double getLargestPeakMz() {
    return precursorMz;
  }

  @Override
  public Range<Integer> getSpectrumNumberRange() {
    return spectrumNumberRange;
  }

  @Override
  public float getCollisionEnergy() {
    return collisionEnergy;
  }

  @Override
  public int getPrecursorCharge() {
    return precursorCharge;
  }

  @Override
  public Frame getParentFrameNumber() {
    return parentFrameNumber;
  }

  @Override
  public Frame getFrameNumber() {
    return fragmentFrameNumber;
  }

  @Override
  public String toString() {
    return "m/z " + precursorMz + " - Scans " + spectrumNumberRange.toString();
  }

  public static final String XML_ELEMENT = "imsmsmsinfo";
  private static final String XML_PRECURSOR_MZ_ATTR = "precursormz";
  private static final String XML_PRECURSOR_CHARGE_ATTR = "charge";
  private static final String XML_FRAGMENT_FRAME_ATTR = "fragmentframe";
  private static final String XML_PARENT_FRAME_ATTR = "parentframe";
  private static final String XML_COLLISION_ENERGY_ATTR = "ce";
  private static final String XML_SPECTRUM_NUMBER_RANGE_ATTR = "spectrumnumberrange";

  /**
   * Appends a new element for an {@link ImsMsMsInfoImpl} at the current position. Start and close
   * tag for this {@link ImsMsMsInfoImpl} are created in this method.
   *
   * @param writer The writer to use.
   */
  @Override
  public void writeToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);
    writer.writeAttribute(XML_PRECURSOR_MZ_ATTR, String.valueOf(getLargestPeakMz()));
    writer.writeAttribute(XML_PRECURSOR_CHARGE_ATTR, String.valueOf(getPrecursorCharge()));
    writer.writeAttribute(XML_FRAGMENT_FRAME_ATTR,
        String.valueOf(getFrameNumber().getDataFile().getScans().indexOf(getFrameNumber())));
    writer.writeAttribute(XML_PARENT_FRAME_ATTR, String
        .valueOf(getParentFrameNumber().getDataFile().getScans().indexOf(getParentFrameNumber())));
    writer.writeAttribute(XML_COLLISION_ENERGY_ATTR, String.valueOf(getCollisionEnergy()));
    writer.writeAttribute(XML_SPECTRUM_NUMBER_RANGE_ATTR,
        ParsingUtils.rangeToString((Range) getSpectrumNumberRange()));
    writer.writeEndElement();
  }

  /**
   * @param reader A reader at an {@link ImsMsMsInfoImpl} element.
   * @return A loaded {@link ImsMsMsInfoImpl}.
   */
  public static ImsMsMsInfoImpl loadFromXML(XMLStreamReader reader, IMSRawDataFile file) {
    final double precursorMz = Double
        .parseDouble(reader.getAttributeValue(null, XML_PRECURSOR_MZ_ATTR));
    final int precursorCharge = Integer
        .parseInt(reader.getAttributeValue(null, XML_PRECURSOR_CHARGE_ATTR));
    final int frameIndex = Integer
        .parseInt(reader.getAttributeValue(null, XML_FRAGMENT_FRAME_ATTR));
    final int parentFrameIndex = Integer
        .parseInt(reader.getAttributeValue(null, XML_PARENT_FRAME_ATTR));
    final float collisionEnergy = Float
        .parseFloat(reader.getAttributeValue(null, XML_COLLISION_ENERGY_ATTR));
    Range<Integer> spectrumRange = ParsingUtils
        .parseIntegerRange(reader.getAttributeValue(null, XML_SPECTRUM_NUMBER_RANGE_ATTR));

    return new ImsMsMsInfoImpl(precursorMz, spectrumRange, collisionEnergy, precursorCharge,
        file.getFrame(parentFrameIndex), file.getFrame(frameIndex));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ImsMsMsInfoImpl that = (ImsMsMsInfoImpl) o;
    return Double.compare(that.precursorMz, precursorMz) == 0
        && Float.compare(that.getCollisionEnergy(), getCollisionEnergy()) == 0
        && getPrecursorCharge() == that.getPrecursorCharge() && Objects
        .equals(getSpectrumNumberRange(), that.getSpectrumNumberRange()) && Objects
        .equals(getParentFrameNumber(), that.getParentFrameNumber()) && Objects
        .equals(fragmentFrameNumber, that.fragmentFrameNumber);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(precursorMz, getSpectrumNumberRange(), getCollisionEnergy(), getPrecursorCharge(),
            getParentFrameNumber(), fragmentFrameNumber);
  }
}
