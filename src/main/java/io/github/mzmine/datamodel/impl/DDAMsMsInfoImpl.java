/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.datamodel.impl;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import java.util.Objects;
import javax.validation.constraints.NotNull;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.Nullable;

public class DDAMsMsInfoImpl implements DDAMsMsInfo {

  public static final String XML_TYPE_NAME = "ddamsmsinfo";

  @NotNull private final double isolationMz;
  @Nullable private final Integer charge;
  @Nullable private final Float activationEnergy;
  @Nullable private Scan msMsScan;
  @Nullable private Scan parentScan;
  private final int msLevel;
  @NotNull private final ActivationMethod method;

  public DDAMsMsInfoImpl(double isolationMz, @Nullable Integer charge,
      @Nullable Float activationEnergy, @Nullable Scan msMsScan, @Nullable Scan parentScan,
      int msLevel, @NotNull ActivationMethod method) {
    this.isolationMz = isolationMz;
    this.charge = charge;
    this.activationEnergy = activationEnergy;
    this.msMsScan = msMsScan;
    this.parentScan = parentScan;
    this.msLevel = msLevel;
    this.method = method;
  }

  @Override
  public @Nullable Float getActivationEnergy() {
    return activationEnergy;
  }

  @Override
  public double getIsolationMz() {
    return isolationMz;
  }

  @Override
  public @Nullable Integer getPrecursorCharge() {
    return charge;
  }

  @Override
  public @Nullable Scan getParentScan() {
    return parentScan;
  }

  @NotNull
  @Override
  public @Nullable Scan getMsMsScan() {
    return msMsScan;
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
   * Appends a new element for an {@link DDAMsMsInfoImpl} at the current position. Start and close
   * tag for this {@link DDAMsMsInfoImpl} are created in this method.
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

    writer.writeAttribute(XML_FRAGMENT_SCAN_ATTR,
        String.valueOf(getMsMsScan().getDataFile().getScans().indexOf(getMsMsScan())));

    if (getParentScan() != null) {
      writer.writeAttribute(XML_PARENT_SCAN_ATTR,
          String.valueOf(getParentScan().getDataFile().getScans().indexOf(getParentScan())));
    }

    if (getActivationEnergy() != null) {
      writer.writeAttribute(XML_ACTIVATION_ENERGY_ATTR, String.valueOf(this.getActivationEnergy()));
    }
    writer.writeAttribute(XML_ACTIVATION_TYPE_ATTR, this.getActivationMethod().name());
    writer.writeAttribute(XML_MSLEVEL_ATTR, String.valueOf(getMsLevel()));

    writer.writeEndElement();
  }

  /**
   * @param reader A reader at an {@link DDAMsMsInfoImpl} element.
   * @return A loaded {@link DDAMsMsInfoImpl}.
   */
  public static DDAMsMsInfoImpl loadFromXML(XMLStreamReader reader, RawDataFile file) {


    final Double precursorMz = Double.parseDouble(
        reader.getAttributeValue(null, XML_PRECURSOR_MZ_ATTR));
    final Integer precursorCharge =
        reader.getAttributeValue(null, XML_PRECURSOR_CHARGE_ATTR) != null ? Integer.parseInt(
            reader.getAttributeValue(null, XML_PRECURSOR_CHARGE_ATTR)) : null;
    final int scanIndex = Integer.parseInt(reader.getAttributeValue(null, XML_FRAGMENT_SCAN_ATTR));
    final Integer parentScanIndex =
        reader.getAttributeValue(null, XML_PARENT_SCAN_ATTR) != null ? Integer.parseInt(
            reader.getAttributeValue(null, XML_PARENT_SCAN_ATTR)) : null;
    final Float activationEnergy =
        reader.getAttributeValue(null, XML_ACTIVATION_ENERGY_ATTR) != null ? Float.parseFloat(
            reader.getAttributeValue(null, XML_ACTIVATION_ENERGY_ATTR)) : null;
    final int msLevel = Integer.parseInt(reader.getAttributeValue(null, XML_MSLEVEL_ATTR));
    final ActivationMethod method = ActivationMethod.valueOf(
        reader.getAttributeValue(null, XML_ACTIVATION_TYPE_ATTR));

    return new DDAMsMsInfoImpl(precursorMz, precursorCharge, activationEnergy,
        file.getScan(scanIndex), parentScanIndex != null ? file.getScan(parentScanIndex) : null,
        msLevel, method);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DDAMsMsInfoImpl that = (DDAMsMsInfoImpl) o;
    return Double.compare(that.getIsolationMz(), getIsolationMz()) == 0
        && getMsLevel() == that.getMsLevel() && Objects.equals(charge, that.charge)
        && Objects.equals(getActivationEnergy(), that.getActivationEnergy()) && Objects.equals(
        getMsMsScan(), that.getMsMsScan()) && Objects.equals(getParentScan(), that.getParentScan())
        && method == that.method;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getIsolationMz(), charge, getActivationEnergy(), getMsMsScan(),
        getParentScan(), getMsLevel(), method);
  }

  @Override
  public MsMsInfo createCopy() {
    return new DDAMsMsInfoImpl(isolationMz, charge, activationEnergy, null, parentScan, msLevel,
        method);
  }
}
