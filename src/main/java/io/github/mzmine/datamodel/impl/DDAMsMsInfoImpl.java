/*
 * Copyright 2006-2022 The MZmine Development Team
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
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLCV;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLCVParam;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLIsolationWindow;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLPrecursorActivation;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLPrecursorElement;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLPrecursorSelectedIonList;
import io.github.mzmine.util.ParsingUtils;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.Nullable;

public class DDAMsMsInfoImpl implements DDAMsMsInfo {

  public static final String XML_TYPE_NAME = "ddamsmsinfo";

  private final double isolationMz;
  @Nullable
  private final Integer charge;
  @Nullable
  private final Float activationEnergy;
  private final int msLevel;
  @NotNull
  private final ActivationMethod method;
  @Nullable
  private final Range<Double> isolationWindow;
  @Nullable
  private final Scan parentScan;
  @Nullable
  private Scan msMsScan;

  public DDAMsMsInfoImpl(double isolationMz, @Nullable Integer charge,
      @Nullable Float activationEnergy, @Nullable Scan msMsScan, @Nullable Scan parentScan,
      int msLevel, @NotNull ActivationMethod method, @Nullable Range<Double> isolationWindow) {
    this.isolationMz = isolationMz;
    this.charge = charge;
    this.activationEnergy = activationEnergy;
    this.msMsScan = msMsScan;
    this.parentScan = parentScan;
    this.msLevel = msLevel;
    this.method = method;
    this.isolationWindow = isolationWindow;
  }

  public static DDAMsMsInfo fromMzML(MzMLPrecursorElement precursorElement, int msLevel) {
    Optional<MzMLPrecursorSelectedIonList> list = precursorElement.getSelectedIonList();
    if (!list.isPresent()) {
      return null;
    }

    Double precursorMz = null;
    Integer charge = null;
    Float energy = null;
    ActivationMethod method = ActivationMethod.UNKNOWN;

    MzMLPrecursorActivation activation = precursorElement.getActivation();
    for (MzMLCVParam mzMLCVParam : activation.getCVParamsList()) {
      if (mzMLCVParam.getAccession().equals(MzMLCV.cvActivationEnergy) || mzMLCVParam.getAccession()
          .equals(MzMLCV.cvPercentCollisionEnergy) || mzMLCVParam.getAccession()
          .equals(MzMLCV.cvActivationEnergy2)) {
        energy = Float.parseFloat(mzMLCVParam.getValue().get());
      }
      if (mzMLCVParam.getAccession().equals(MzMLCV.cvActivationCID)) {
        method = ActivationMethod.CID;
      }
      if (mzMLCVParam.getAccession().equals(MzMLCV.cvElectronCaptureDissociation)) {
        method = ActivationMethod.ECD;
      }
      if (mzMLCVParam.getAccession().equals(MzMLCV.cvHighEnergyCID)) {
        method = ActivationMethod.HCD;
      }
      if (mzMLCVParam.getAccession().equals(MzMLCV.cvLowEnergyCID)) {
        method = ActivationMethod.CID;
      }
    }

    List<MzMLCVParam> cvParamsList = list.get().getSelectedIonList().get(0).getCVParamsList();
    for (MzMLCVParam param : cvParamsList) {
      if (param.getAccession().equals(MzMLCV.cvPrecursorMz)) {
        precursorMz = Double.parseDouble(param.getValue().get());
      }
      if (param.getAccession().equals(MzMLCV.cvChargeState)) {
        charge = Integer.parseInt(param.getValue().get());
      }
    }

    Double lower = null;
    Double upper = null;
    Double targetWindowCenter = null;
    final Optional<MzMLIsolationWindow> mzmlWindow = precursorElement.getIsolationWindow();
    if (mzmlWindow.isPresent()) {
      for (var param : mzmlWindow.get().getCVParamsList()) {
        if (param.getAccession().equals(MzMLCV.cvIsolationWindowLowerOffset)) {
          lower = Double.parseDouble(param.getValue().get());
        }
        if (param.getAccession().equals(MzMLCV.cvIsolationWindowUpperOffset)) {
          upper = Double.parseDouble(param.getValue().get());
        }
        if (param.getAccession().equals(MzMLCV.cvIsolationWindowTarget)) {
          targetWindowCenter = Double.parseDouble(param.getValue().get());
        }
      }
    }

    /*
     use center of isolation window. At least for Orbitrap instruments and
     msconvert conversion we found that the isolated ion actually refers to the main peak in
     an isotope pattern whereas the isolation window is the correct isolation mz
     see issue https://github.com/mzmine/mzmine3/issues/717
    */
    if (targetWindowCenter != null) {
      precursorMz = targetWindowCenter;
    }

    if (precursorMz == null) {
      return null;
    }

    Range<Double> mzwindow = null;
    if (lower != null && upper != null) {
      mzwindow = Range.closed(precursorMz - lower, precursorMz + upper);
    }
    return new DDAMsMsInfoImpl(precursorMz, charge, energy, null, null, msLevel, method, mzwindow);
  }

  /**
   * @param reader A reader at an {@link DDAMsMsInfoImpl} element.
   * @return A loaded {@link DDAMsMsInfoImpl}.
   */
  public static DDAMsMsInfoImpl loadFromXML(XMLStreamReader reader, RawDataFile file) {

    final double precursorMz = Double.parseDouble(
        reader.getAttributeValue(null, XML_PRECURSOR_MZ_ATTR));

    final Integer precursorCharge = ParsingUtils.readAttributeValueOrDefault(reader,
        XML_PRECURSOR_CHARGE_ATTR, null, Integer::parseInt);

    final Integer scanIndex = ParsingUtils.readAttributeValueOrDefault(reader,
        XML_FRAGMENT_SCAN_ATTR, null, Integer::parseInt);

    final Integer parentScanIndex = ParsingUtils.readAttributeValueOrDefault(reader,
        XML_PARENT_SCAN_ATTR, null, Integer::parseInt);

    final Float activationEnergy = ParsingUtils.readAttributeValueOrDefault(reader,
        XML_ACTIVATION_ENERGY_ATTR, null, Float::parseFloat);

    final int msLevel = Integer.parseInt(reader.getAttributeValue(null, XML_MSLEVEL_ATTR));

    final ActivationMethod method = ActivationMethod.valueOf(
        reader.getAttributeValue(null, XML_ACTIVATION_TYPE_ATTR));

    final Range<Double> isolationWindow = ParsingUtils.readAttributeValueOrDefault(reader,
        XML_ISOLATION_WINDOW_ATTR, null, ParsingUtils::stringToDoubleRange);

    return new DDAMsMsInfoImpl(precursorMz, precursorCharge, activationEnergy,
        scanIndex != null ? file.getScan(scanIndex) : null,
        parentScanIndex != null ? file.getScan(parentScanIndex) : null, msLevel, method,
        isolationWindow);
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

    if (getMsMsScan() != null) {
      writer.writeAttribute(XML_FRAGMENT_SCAN_ATTR,
          String.valueOf(getMsMsScan().getDataFile().getScans().indexOf(getMsMsScan())));
    }

    if (getParentScan() != null) {
      writer.writeAttribute(XML_PARENT_SCAN_ATTR,
          String.valueOf(getParentScan().getDataFile().getScans().indexOf(getParentScan())));
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
        && method == that.method && Objects.equals(getIsolationWindow(), that.getIsolationWindow());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getIsolationMz(), charge, getActivationEnergy(), getMsMsScan(),
        getParentScan(), getMsLevel(), method, getIsolationWindow());
  }

  @Override
  public MsMsInfo createCopy() {
    return new DDAMsMsInfoImpl(isolationMz, charge, activationEnergy, null, parentScan, msLevel,
        method, getIsolationWindow());
  }
}
