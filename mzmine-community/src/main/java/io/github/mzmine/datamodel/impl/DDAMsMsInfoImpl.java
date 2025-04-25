/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
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

  public DDAMsMsInfoImpl(double isolationMz, @Nullable Integer charge, final int msLevel) {
    this(isolationMz, charge, null, null, null, msLevel, null, null);
  }

  public static DDAMsMsInfo fromMzML(MzMLPrecursorElement precursorElement, int msLevel) {
    Optional<MzMLPrecursorSelectedIonList> list = precursorElement.getSelectedIonList();
    if (list.isEmpty()) {
      return null;
    }

    Double precursorMz = null;
    Integer charge = null;
    Float energy = null;
    // this energy should be preferred over the also defined CID energy in EAD which is used to transfer the ions
    Float energyEAD = null;
    ActivationMethod method = ActivationMethod.UNKNOWN;

    MzMLPrecursorActivation activation = precursorElement.getActivation();
    for (MzMLCVParam mzMLCVParam : activation.getCVParamsList()) {
      if (mzMLCVParam.getAccession().equals(MzMLCV.cvActivationEnergy) || mzMLCVParam.getAccession()
          .equals(MzMLCV.cvPercentCollisionEnergy) || mzMLCVParam.getAccession()
          .equals(MzMLCV.cvActivationEnergy2)) {
        energy = Float.parseFloat(mzMLCVParam.getValue().get());
      }
      if (mzMLCVParam.getAccession().equals(MzMLCV.cvElectronBeamEnergyEAD)) {
        energyEAD = Float.parseFloat(mzMLCVParam.getValue().get());
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

    if (energyEAD != null) {
      // the regular energy is only used for ion transfer?
      energy = energyEAD;
      method = ActivationMethod.EAD;
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
  public static DDAMsMsInfoImpl loadFromXML(XMLStreamReader reader, RawDataFile file,
      List<RawDataFile> allProjectFiles) {

    final double precursorMz = Double.parseDouble(
        reader.getAttributeValue(null, XML_PRECURSOR_MZ_ATTR));

    final Integer precursorCharge = ParsingUtils.readAttributeValueOrDefault(reader,
        XML_PRECURSOR_CHARGE_ATTR, null, Integer::parseInt);

    final Integer scanIndex = ParsingUtils.readAttributeValueOrDefault(reader,
        MsMsInfo.XML_FRAGMENT_SCAN_ATTR, null, Integer::parseInt);

    final Integer parentScanIndex = ParsingUtils.readAttributeValueOrDefault(reader,
        XML_PARENT_SCAN_ATTR, null, Integer::parseInt);

    final Float activationEnergy = ParsingUtils.readAttributeValueOrDefault(reader,
        MsMsInfo.XML_ACTIVATION_ENERGY_ATTR, null, Float::parseFloat);

    final int msLevel = Integer.parseInt(reader.getAttributeValue(null, MsMsInfo.XML_MSLEVEL_ATTR));

    final ActivationMethod method = ActivationMethod.valueOf(
        reader.getAttributeValue(null, MsMsInfo.XML_ACTIVATION_TYPE_ATTR));

    final Range<Double> isolationWindow = ParsingUtils.readAttributeValueOrDefault(reader,
        MsMsInfo.XML_ISOLATION_WINDOW_ATTR, null, ParsingUtils::stringToDoubleRange);

    final String rawFileName = ParsingUtils.readAttributeValueOrDefault(reader,
        CONST.XML_RAW_FILE_ELEMENT, null, s -> s);

    // use the correct file if the ms info comes from a different file.
    if (rawFileName != null && !rawFileName.equals(file != null ? file.getName() : null)) {
      file = allProjectFiles.stream().filter(f -> f.getName().equals(rawFileName)).findFirst()
          .orElse(file);
    }

    return new DDAMsMsInfoImpl(precursorMz, precursorCharge, activationEnergy,
        scanIndex != null && scanIndex != -1 && file != null ? file.getScan(scanIndex) : null,
        parentScanIndex != null && parentScanIndex != -1 && file != null ? file.getScan(
            parentScanIndex) : null, msLevel, method, isolationWindow);
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
      writer.writeAttribute(MsMsInfo.XML_FRAGMENT_SCAN_ATTR,
          String.valueOf(getMsMsScan().getDataFile().getScans().indexOf(getMsMsScan())));
      writer.writeAttribute(CONST.XML_RAW_FILE_ELEMENT, getMsMsScan().getDataFile().getName());
    }

    if (getParentScan() != null) {
      writer.writeAttribute(XML_PARENT_SCAN_ATTR,
          String.valueOf(getParentScan().getDataFile().getScans().indexOf(getParentScan())));
    }

    if (getActivationEnergy() != null) {
      writer.writeAttribute(MsMsInfo.XML_ACTIVATION_ENERGY_ATTR,
          String.valueOf(this.getActivationEnergy()));
    }
    writer.writeAttribute(MsMsInfo.XML_ACTIVATION_TYPE_ATTR, this.getActivationMethod().name());
    writer.writeAttribute(MsMsInfo.XML_MSLEVEL_ATTR, String.valueOf(getMsLevel()));

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
    DDAMsMsInfoImpl that = (DDAMsMsInfoImpl) o;
    return Double.compare(that.getIsolationMz(), getIsolationMz()) == 0
        && getMsLevel() == that.getMsLevel() && Objects.equals(charge, that.charge)
        && Objects.equals(getActivationEnergy(), that.getActivationEnergy()) && (
        Objects.equals(getMsMsScan(), that.getMsMsScan()) || (getMsMsScan() != null
            && that.getMsMsScan() != null && getMsMsScan().getScanNumber() == that.getMsMsScan()
            .getScanNumber())) && (Objects.equals(getParentScan(), that.getParentScan())
        || Objects.equals(getMsMsScan(), that.getMsMsScan()) || (getParentScan() != null
        && that.getParentScan() != null && getParentScan().getScanNumber() == that.getParentScan()
        .getScanNumber())

    ) && method == that.method && Objects.equals(getIsolationWindow(), that.getIsolationWindow());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getIsolationMz(), charge, getActivationEnergy(), getMsMsScan(),
        getParentScan(), getMsLevel(), method, getIsolationWindow());
  }

  @Override
  public String toString() {
    return "DDAMsMsInfoImpl{" + "isolationMz=" + isolationMz + ", charge=" + charge
        + ", activationEnergy=" + activationEnergy + ", msLevel=" + msLevel + ", method=" + method
        + ", isolationWindow=" + isolationWindow + ", parentScan=" + parentScan + ", msMsScan="
        + msMsScan + '}';
  }

  @Override
  public MsMsInfo createCopy() {
    return new DDAMsMsInfoImpl(isolationMz, charge, activationEnergy, null, parentScan, msLevel,
        method, getIsolationWindow());
  }
}
