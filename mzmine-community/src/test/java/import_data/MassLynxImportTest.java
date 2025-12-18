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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package import_data;

import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.gui.preferences.MassLynxImportOptions;
import io.github.mzmine.gui.preferences.VendorImportParameters;
import io.github.mzmine.gui.preferences.WatersLockmassParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectors;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.exactmass.ExactMassDetectorParameters;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_masslynx.MassLynxDataAccess;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.io.File;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import testutils.MZmineTestUtil;

@DisabledOnOs({OS.MAC, OS.LINUX})
public class MassLynxImportTest {

  @BeforeAll
  static void init() {
    MZmineTestUtil.startMzmineCore();
  }

  public AllSpectralDataImportParameters generateWatersCentroidingParam(File file) {
    VendorImportParameters vendorParam = VendorImportParameters.create(true,
        MassLynxImportOptions.NATIVE_WATERS_CENTROIDING, true,
        WatersLockmassParameters.createDefault(), true);
    return (AllSpectralDataImportParameters) AllSpectralDataImportParameters.create(vendorParam,
        new File[]{file}, null, null);
  }

  public AllSpectralDataImportParameters generateMzmineCentroidingParam(File file) {
    VendorImportParameters vendorParam = VendorImportParameters.create(true,
        MassLynxImportOptions.NATIVE_MZMINE_CENTROIDING, true,
        WatersLockmassParameters.createDefault(), true);
    return (AllSpectralDataImportParameters) AllSpectralDataImportParameters.create(vendorParam,
        new File[]{file}, null, null);
  }

  public AllSpectralDataImportParameters generateProfileImportParamMzmine(File file) {
    VendorImportParameters vendorParam = VendorImportParameters.create(false,
        MassLynxImportOptions.NATIVE_MZMINE_CENTROIDING, true,
        WatersLockmassParameters.createDefault(), true);
    return (AllSpectralDataImportParameters) AllSpectralDataImportParameters.create(vendorParam,
        new File[]{file}, null, null);
  }

  public AllSpectralDataImportParameters generateProfileImportParamWaters(File file) {
    VendorImportParameters vendorParam = VendorImportParameters.create(false,
        MassLynxImportOptions.NATIVE_WATERS_CENTROIDING, true,
        WatersLockmassParameters.createDefault(), true);
    return (AllSpectralDataImportParameters) AllSpectralDataImportParameters.create(vendorParam,
        new File[]{file}, null, null);
  }

  public AllSpectralDataImportParameters generateProfileImportAdvancedExactMass(File file) {
    VendorImportParameters vendorParam = VendorImportParameters.create(false,
        MassLynxImportOptions.NATIVE_MZMINE_CENTROIDING, true,
        WatersLockmassParameters.createDefault(), true);

    final var param = (ExactMassDetectorParameters) new ExactMassDetectorParameters().cloneParameterSet();
    param.setParameter(ExactMassDetectorParameters.noiseLevel, 0d);
    final var advancedParam = AdvancedSpectraImportParameters.create(MassDetectors.EXACT, param,
        MassDetectors.EXACT, param, null, ScanSelection.ALL_SCANS, false);
    return (AllSpectralDataImportParameters) AllSpectralDataImportParameters.create(vendorParam,
        new File[]{file}, null, null, advancedParam);
  }

  @Test
  public void testProfileNonIms() throws Exception {
    final File ddaNonIms = new File(
        "D:\\OneDrive - mzio GmbH\\mzio\\Example data\\Waters\\LC-MS DDA\\pos\\050325_029.raw");
    if (!ddaNonIms.exists()) {
      return;
    }

    final AllSpectralDataImportParameters mzmineProfileParam = generateProfileImportParamMzmine(
        ddaNonIms);
    final ScanImportProcessorConfig processorMzmine = AllSpectralDataImportModule.createSpectralProcessors(
        mzmineProfileParam.getEmbeddedParametersIfSelectedOrElse(
            AllSpectralDataImportParameters.advancedImport, null));

    RawDataFileImpl file = new RawDataFileImpl(ddaNonIms.getName(), ddaNonIms.getAbsolutePath(),
        null);
    final double mzmineMz;
    try (final var access = new MassLynxDataAccess(ddaNonIms,
        (VendorImportParameters) mzmineProfileParam.getEmbeddedParameterValue(
            AllSpectralDataImportParameters.vendorOptions), null, processorMzmine)) {
      SimpleScan scan = access.readScan(file, 0, 30);
      Assertions.assertEquals(MassSpectrumType.PROFILE, scan.getSpectrumType());
      mzmineMz = scan.getMzValue(30);
    }

    final AllSpectralDataImportParameters watersProfileParam = generateProfileImportParamWaters(
        ddaNonIms);
    final ScanImportProcessorConfig processorWaters = AllSpectralDataImportModule.createSpectralProcessors(
        watersProfileParam.getEmbeddedParametersIfSelectedOrElse(
            AllSpectralDataImportParameters.advancedImport, null));

    final double watersMz;
    try (final var access = new MassLynxDataAccess(ddaNonIms,
        (VendorImportParameters) watersProfileParam.getEmbeddedParameterValue(
            AllSpectralDataImportParameters.vendorOptions), null, processorWaters)) {
      SimpleScan scan = access.readScan(file, 0, 30);
      Assertions.assertEquals(MassSpectrumType.PROFILE, scan.getSpectrumType());
      watersMz = scan.getMzValue(30);
    }

    Assertions.assertEquals(watersMz, mzmineMz);
  }

  @Test
  public void testCentroidNonIms() throws Exception {
    final File ddaNonIms = new File(
        "D:\\OneDrive - mzio GmbH\\mzio\\Example data\\Waters\\LC-MS DDA\\pos\\050325_029.raw");
    if (!ddaNonIms.exists()) {
      return;
    }

    final AllSpectralDataImportParameters mzmineCentroidParam = generateMzmineCentroidingParam(
        ddaNonIms);
    final ScanImportProcessorConfig processorMzmine = AllSpectralDataImportModule.createSpectralProcessors(
        mzmineCentroidParam.getEmbeddedParametersIfSelectedOrElse(
            AllSpectralDataImportParameters.advancedImport, null));

    RawDataFileImpl file = new RawDataFileImpl(ddaNonIms.getName(), ddaNonIms.getAbsolutePath(),
        null);
    final double mzmineMz;
    try (final var access = new MassLynxDataAccess(ddaNonIms,
        (VendorImportParameters) mzmineCentroidParam.getEmbeddedParameterValue(
            AllSpectralDataImportParameters.vendorOptions), null, processorMzmine)) {
      SimpleScan scan = access.readScan(file, 0, 30);
      Assertions.assertEquals(MassSpectrumType.CENTROIDED, scan.getSpectrumType());
      mzmineMz = scan.getBasePeakMz();
    }

    final AllSpectralDataImportParameters watersCentroidParam = generateWatersCentroidingParam(
        ddaNonIms);
    final ScanImportProcessorConfig processorWaters = AllSpectralDataImportModule.createSpectralProcessors(
        watersCentroidParam.getEmbeddedParametersIfSelectedOrElse(
            AllSpectralDataImportParameters.advancedImport, null));

    final double watersMz;
    try (final var access = new MassLynxDataAccess(ddaNonIms,
        (VendorImportParameters) watersCentroidParam.getEmbeddedParameterValue(
            AllSpectralDataImportParameters.vendorOptions), null, processorWaters)) {
      SimpleScan scan = access.readScan(file, 0, 30);
      Assertions.assertEquals(MassSpectrumType.CENTROIDED, scan.getSpectrumType());
      watersMz = scan.getBasePeakMz();
    }

    Assertions.assertEquals(watersMz, mzmineMz, 0.0005);
  }

  @Test
  public void testProfileIms() throws Exception {
    final File mseIms = new File(
        "D:\\OneDrive - mzio GmbH\\mzio\\Example data\\Waters\\LC-IMS-MS MSe\\20250303_FuncTest2_SST_POS_006R.raw");
    if (!mseIms.exists()) {
      return;
    }

    final AllSpectralDataImportParameters mzmineProfileParam = generateProfileImportParamMzmine(
        mseIms);
    final ScanImportProcessorConfig mzmineProcessor = AllSpectralDataImportModule.createSpectralProcessors(
        mzmineProfileParam.getEmbeddedParametersIfSelectedOrElse(
            AllSpectralDataImportParameters.advancedImport, null));

    IMSRawDataFileImpl file = new IMSRawDataFileImpl(mseIms.getName(), mseIms.getAbsolutePath(),
        null);
    final double mzmineMz;
    final double mobScanMzmine;
    try (final var access = new MassLynxDataAccess(mseIms,
        (VendorImportParameters) mzmineProfileParam.getEmbeddedParameterValue(
            AllSpectralDataImportParameters.vendorOptions), null, mzmineProcessor)) {
      SimpleFrame scan = access.readFrame(file, 0, 30);
      mzmineMz = scan.getMzValue(30);
      MobilityScan mobScan = scan.getMobilityScan(30);
      mobScanMzmine = mobScan.getMzValue(1);

      Assertions.assertEquals(MassSpectrumType.PROFILE, scan.getSpectrumType());
      Assertions.assertEquals(MassSpectrumType.PROFILE, mobScan.getSpectrumType());
    }

    final AllSpectralDataImportParameters watersProfileParam = generateProfileImportParamWaters(
        mseIms);
    final ScanImportProcessorConfig processorWaters = AllSpectralDataImportModule.createSpectralProcessors(
        watersProfileParam.getEmbeddedParametersIfSelectedOrElse(
            AllSpectralDataImportParameters.advancedImport, null));

    final double watersMz;
    final double mobScanWaters;
    try (final var access = new MassLynxDataAccess(mseIms,
        (VendorImportParameters) watersProfileParam.getEmbeddedParameterValue(
            AllSpectralDataImportParameters.vendorOptions), null, processorWaters)) {
      SimpleFrame scan = access.readFrame(file, 0, 30);
      watersMz = scan.getMzValue(30);
      MobilityScan mobScan = scan.getMobilityScan(30);
      mobScanWaters = mobScan.getMzValue(1);

      Assertions.assertEquals(MassSpectrumType.PROFILE, scan.getSpectrumType());
      Assertions.assertEquals(MassSpectrumType.PROFILE, mobScan.getSpectrumType());
    }

    Assertions.assertEquals(watersMz, mzmineMz);
    Assertions.assertEquals(mobScanWaters, mobScanMzmine);
  }

  @Test
  public void testCentroidIms() throws Exception {
    final File mseIms = new File(
        "D:\\OneDrive - mzio GmbH\\mzio\\Example data\\Waters\\LC-IMS-MS MSe\\20250303_FuncTest2_SST_POS_006R.raw");
    if (!mseIms.exists()) {
      return;
    }

    final AllSpectralDataImportParameters mzmineCentroidParam = generateMzmineCentroidingParam(
        mseIms);
    final ScanImportProcessorConfig mzmineTopLevelProcessor = AllSpectralDataImportModule.createSpectralProcessors(
        mzmineCentroidParam.getEmbeddedParametersIfSelectedOrElse(
            AllSpectralDataImportParameters.advancedImport, null));

    IMSRawDataFileImpl file = new IMSRawDataFileImpl(mseIms.getName(), mseIms.getAbsolutePath(),
        null);
    final double mzmineMz;
    final double mobScanMzmine;
    try (final var access = new MassLynxDataAccess(mseIms,
        (VendorImportParameters) mzmineCentroidParam.getEmbeddedParameterValue(
            AllSpectralDataImportParameters.vendorOptions), null, mzmineTopLevelProcessor)) {
      SimpleFrame scan = access.readFrame(file, 0, 77);
      mzmineMz = scan.getBasePeakMz();
      MobilityScan mobScan = scan.getMobilityScan(44);
      mobScanMzmine = mobScan.getBasePeakMz();

      Assertions.assertEquals(MassSpectrumType.CENTROIDED, scan.getSpectrumType());
      Assertions.assertEquals(MassSpectrumType.CENTROIDED, mobScan.getSpectrumType());
    }

    final AllSpectralDataImportParameters watersProfileParam = generateWatersCentroidingParam(
        mseIms);
    final ScanImportProcessorConfig watersTopLevelProcessor = AllSpectralDataImportModule.createSpectralProcessors(
        watersProfileParam.getEmbeddedParametersIfSelectedOrElse(
            AllSpectralDataImportParameters.advancedImport, null));

    final double watersMz;
    final double mobScanWaters;
    try (final var access = new MassLynxDataAccess(mseIms,
        (VendorImportParameters) watersProfileParam.getEmbeddedParameterValue(
            AllSpectralDataImportParameters.vendorOptions), null, watersTopLevelProcessor)) {
      SimpleFrame scan = access.readFrame(file, 0, 77);
      watersMz = scan.getBasePeakMz();
      MobilityScan mobScan = scan.getMobilityScan(44);
      mobScanWaters = mobScan.getBasePeakMz();

      Assertions.assertEquals(MassSpectrumType.CENTROIDED, scan.getSpectrumType());
      Assertions.assertEquals(MassSpectrumType.CENTROIDED, mobScan.getSpectrumType());
    }

    Assertions.assertEquals(watersMz, mzmineMz, 0.0005);
    Assertions.assertEquals(mobScanWaters, mobScanMzmine, 0.0015);
  }

  @Test
  public void testAdvancedCentroiding() throws Exception {
    final File mseIms = new File(
        "D:\\OneDrive - mzio GmbH\\mzio\\Example data\\Waters\\LC-IMS-MS MSe\\20250303_FuncTest2_SST_POS_006R.raw");
    if (!mseIms.exists()) {
      return;
    }

    IMSRawDataFileImpl file = new IMSRawDataFileImpl(mseIms.getName(), mseIms.getAbsolutePath(),
        null);
    final AllSpectralDataImportParameters exactParam = generateProfileImportAdvancedExactMass(
        mseIms);
    final ScanImportProcessorConfig watersTopLevelProcessor = AllSpectralDataImportModule.createSpectralProcessors(
        exactParam.getEmbeddedParametersIfSelectedOrElse(
            AllSpectralDataImportParameters.advancedImport, null));

    final double watersMz;
    final double mobScanWaters;
    try (final var access = new MassLynxDataAccess(mseIms,
        (VendorImportParameters) exactParam.getEmbeddedParameterValue(
            AllSpectralDataImportParameters.vendorOptions), null, watersTopLevelProcessor)) {
      SimpleFrame scan = access.readFrame(file, 0, 77);
      watersMz = scan.getBasePeakMz();
      MobilityScan mobScan = scan.getMobilityScan(44);
      mobScanWaters = mobScan.getBasePeakMz();

      Assertions.assertEquals(MassSpectrumType.CENTROIDED, scan.getSpectrumType());
      Assertions.assertEquals(MassSpectrumType.CENTROIDED, mobScan.getSpectrumType());
    }

    Assertions.assertEquals(380.2170104980469, watersMz, 0.005);
    Assertions.assertEquals(380.21661376953125, mobScanWaters, 0.0015);
  }

  @Test
  public void testAlreadyCentroided() throws Exception {
    final File alreadyCentroided = new File(
        "D:\\OneDrive - mzio GmbH\\mzio\\Example data\\Waters\\mse_20180205_0125.raw");
    if (!alreadyCentroided.exists()) {
      return;
    }

    final AllSpectralDataImportParameters mzmineCentroidParam = generateMzmineCentroidingParam(
        alreadyCentroided);
    final ScanImportProcessorConfig processorMzmine = AllSpectralDataImportModule.createSpectralProcessors(
        mzmineCentroidParam.getEmbeddedParametersIfSelectedOrElse(
            AllSpectralDataImportParameters.advancedImport, null));

    RawDataFileImpl file = new RawDataFileImpl(alreadyCentroided.getName(),
        alreadyCentroided.getAbsolutePath(), null);
    final double mzmineMz;
    try (final var access = new MassLynxDataAccess(alreadyCentroided,
        (VendorImportParameters) mzmineCentroidParam.getEmbeddedParameterValue(
            AllSpectralDataImportParameters.vendorOptions), null, processorMzmine)) {
      SimpleScan scan = access.readScan(file, 0, 30);
      Assertions.assertEquals(MassSpectrumType.CENTROIDED, scan.getSpectrumType());
      mzmineMz = scan.getMzValue(30);
    }

    final AllSpectralDataImportParameters profileImport = generateProfileImportParamMzmine(
        alreadyCentroided);
    final ScanImportProcessorConfig profileProcessor = AllSpectralDataImportModule.createSpectralProcessors(
        profileImport.getEmbeddedParametersIfSelectedOrElse(
            AllSpectralDataImportParameters.advancedImport, null));

    final double rawMz;
    try (final var access = new MassLynxDataAccess(alreadyCentroided,
        (VendorImportParameters) profileImport.getEmbeddedParameterValue(
            AllSpectralDataImportParameters.vendorOptions), null, profileProcessor)) {
      SimpleScan scan = access.readScan(file, 0, 30);
      Assertions.assertEquals(MassSpectrumType.CENTROIDED, scan.getSpectrumType());
      rawMz = scan.getMzValue(30);
    }

    Assertions.assertEquals(rawMz, mzmineMz);
  }
}
