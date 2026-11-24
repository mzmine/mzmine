/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package import_data;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.RawFileWarningType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.types.MsMsInfoType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.otherdectectors.PolarityTypeType;
import io.github.mzmine.datamodel.impl.MSnInfoImpl;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.datamodel.msms.DIAMsMsInfoImpl;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.MsProcessorList;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.processors.SortByMzMsProcessor;
import io.github.mzmine.modules.io.import_rawdata_mzml.MSDKmzMLImportModule;
import io.github.mzmine.modules.io.import_rawdata_mzml.MSDKmzMLImportParameters;
import io.github.mzmine.modules.io.import_rawdata_mzml.MzMLImportTask;
import io.github.mzmine.modules.io.import_rawdata_zip.ZipImportTask;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Minimal XML cases for historical mzML import fixes. Assertions use the imported data model so the
 * same cases can exercise a replacement importer.
 */
class MzMLSyntheticImportTest {

  @TempDir
  Path tempDir;

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void sortsAndRenumbersScansAndKeepsPeakPairsFromFilesAndStreams(final boolean stream)
      throws IOException {
    final String xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <indexedmzML xmlns="http://psi.hupo.org/ms/mzml">
        %s
        </indexedmzML>
        """.formatted(document(
        spectra(spectrum(0, "scan=30", 1, 120, "", "", new double[]{300}, new double[]{30}),
            spectrum(1, "scanId=30", 1, 60, "<cvParam accession=\"MS:1000512\" value=\"é測한🧪\"/>",
                "", new double[]{202, 201}, new double[]{20, 10}),
            spectrum(2, "scan=31", 1, 60, "", "", new double[]{250}, new double[]{25}))));
    // Shimadzu and UNIFI converters add a line break and whitespace around the binary text.
    final RawDataFile file = importMzml(
        xml.replace("<binary>", "<binary>\n").replace("</binary>", "\n</binary>"), stream);

    Assertions.assertEquals(List.of(1, 2, 3),
        file.getScans().stream().map(Scan::getScanNumber).toList());
    Assertions.assertEquals(List.of(1f, 1f, 2f),
        file.getScans().stream().map(Scan::getRetentionTime).toList());
    Assertions.assertEquals("é測한🧪", file.getScan(0).getScanDefinition());
    assertPeaks(file.getScan(0), new double[]{201, 202}, new double[]{10, 20});
    assertPeaks(file.getScan(1), new double[]{250}, new double[]{25});
    assertPeaks(file.getScan(2), new double[]{300}, new double[]{30});
  }

  @Test
  void retainsNativeScanNumberAndFallsBackToOneBasedIndex() throws IOException {
    final RawDataFile file = importMzml(document(
        spectra(spectrum(0, "scanId=0", 1, 60, "", "", new double[]{100}, new double[]{1}),
            spectrum(1, "sample=1 period=1 cycle=20 experiment=1", 1, 120, "", "",
                new double[]{100}, new double[]{2}))), true);

    Assertions.assertEquals(List.of(0, 2),
        file.getScans().stream().map(Scan::getScanNumber).toList());
  }

  @Test
  void renumbersDuplicateNativeScanNumbersEvenWhenRetentionTimesIncrease() throws IOException {
    final RawDataFile file = importMzml(document(
        spectra(spectrum(0, "scan=42", 1, 60, "", "", new double[]{100}, new double[]{1}),
            spectrum(1, "scanId=42", 1, 120, "", "", new double[]{100}, new double[]{2}))), true);

    Assertions.assertEquals(List.of(1, 2),
        file.getScans().stream().map(Scan::getScanNumber).toList());
  }

  @ParameterizedTest
  @ValueSource(strings = {"401.25", "inf"})
  void usesIsolationTargetAndPrefersEadEnergy(@NotNull final String selectedIonMz)
      throws IOException {
    final String precursor = precursor("", 400, 2, selectedIonMz, """
        <cvParam accession="MS:1000045" value="12"/>
        <cvParam accession="MS:1003410" value="10"/>
        """, "");
    final String wideWindow = precursor("", 500, 20, "501", "", "");
    final RawDataFile file = importMzml(document(spectra(
        spectrum(0, "scan=1", 2, 60, "", precursors(precursor), new double[]{100}, new double[]{1}),
        spectrum(1, "scan=2", 2, 120, "", precursors(wideWindow), new double[]{100},
            new double[]{2}))), true);

    final DDAMsMsInfo dda = Assertions.assertInstanceOf(DDAMsMsInfo.class,
        file.getScan(0).getMsMsInfo());
    Assertions.assertEquals(400d, dda.getIsolationMz());
    Assertions.assertEquals(Range.closed(399d, 402d), dda.getIsolationWindow());
    Assertions.assertEquals(2, dda.getPrecursorCharge());
    Assertions.assertEquals(ActivationMethod.EAD, dda.getActivationMethod());
    Assertions.assertEquals(10f, dda.getActivationEnergy());

    final DIAMsMsInfoImpl dia = Assertions.assertInstanceOf(DIAMsMsInfoImpl.class,
        file.getScan(1).getMsMsInfo());
    Assertions.assertEquals(Range.closed(499d, 520d), dia.getIsolationWindow());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void reconstructsMsnStagesFromReverseXmlOrder(final boolean explicitLevels) throws IOException {
    final String earlyStage = precursor(
        explicitLevels ? "controllerType=0 scan=20" : "controllerType=0 scan=10", 500, 2, "501", "",
        explicitLevels ? "<userParam name=\"ms level\" value=\"1\"/>" : "");
    final String lateStage = precursor(
        explicitLevels ? "controllerType=0 scan=10" : "controllerType=0 scan=20", 250, 2, "251", "",
        explicitLevels ? "<userParam name=\"ms level\" value=\"2\"/>" : "");
    final RawDataFile file = importMzml(document(spectra(
        spectrum(0, "scan=30", 3, 60, "", precursors(lateStage, earlyStage), new double[]{100},
            new double[]{10}))), true);

    final MSnInfoImpl info = Assertions.assertInstanceOf(MSnInfoImpl.class,
        file.getScan(0).getMsMsInfo());
    Assertions.assertEquals(List.of(500d, 250d),
        info.getPrecursors().stream().map(DDAMsMsInfo::getIsolationMz).toList());
    Assertions.assertEquals(List.of(2, 3),
        info.getPrecursors().stream().map(DDAMsMsInfo::getMsLevel).toList());
  }

  @Test
  void fillsAnOmittedDriftBinAndPreservesPackedPeaksAndIsolation() throws IOException {
    final String fragment = precursors(precursor("", 500, 2, "505", "", ""));
    final RawDataFile file = importMzml(document(spectra(
        spectrum(0, "scanId=1", 1, 60, driftTime(10), "", new double[]{100}, new double[]{1}),
        spectrum(1, "scanId=2", 1, 60, driftTime(20), "", new double[0], new double[0]),
        spectrum(2, "scanId=3", 1, 60, driftTime(30), "", new double[]{300}, new double[]{3}),
        spectrum(3, "scanId=4", 2, 120, driftTime(10), fragment, new double[]{110},
            new double[]{4}),
        spectrum(4, "scanId=5", 2, 120, driftTime(30), fragment, new double[]{305, 302},
            new double[]{11, 5}))), true);

    final IMSRawDataFile ims = Assertions.assertInstanceOf(IMSRawDataFile.class, file);
    Assertions.assertEquals(2, ims.getNumOfScans());
    final Frame frame = ims.getFrames().getLast();
    Assertions.assertEquals(3, frame.getNumberOfMobilityScans());
    Assertions.assertEquals(2f, frame.getRetentionTime());
    Assertions.assertEquals(20d, frame.getMobilityForMobilityScanNumber(1));
    assertPeaks(frame.getMobilityScans().get(0), new double[]{110}, new double[]{4});
    assertPeaks(frame.getMobilityScans().get(1), new double[0], new double[0]);
    Assertions.assertNull(frame.getMobilityScans().get(1).getBasePeakIndex());
    assertPeaks(frame.getMobilityScans().get(2), new double[]{302, 305}, new double[]{5, 11});
    Assertions.assertEquals(1, frame.getMobilityScans().get(2).getBasePeakIndex());
    Assertions.assertEquals(305d, frame.getMobilityScans().get(2).getBasePeakMz());
    Assertions.assertEquals(1, frame.getImsMsMsInfos().size());
    final PasefMsMsInfo info = Assertions.assertInstanceOf(PasefMsMsInfo.class,
        frame.getImsMsMsInfos().iterator().next());
    Assertions.assertEquals(505d, info.getIsolationMz());
    Assertions.assertEquals(Range.closed(499d, 502d), info.getIsolationWindow());
    Assertions.assertEquals(Range.closed(0, 2), info.getSpectrumNumberRange());
    Assertions.assertNull(info.getActivationEnergy());
  }

  @Test
  void trailingGapPreservesTheLastMeasuredIsolationState() throws IOException {
    final String isolation = precursors(precursor("", 500, 2, "505", "", ""));
    final String xml = document(
        spectra(spectrum(0, "scan=1", 1, 60, driftTime(30), "", new double[]{300}, new double[]{1}),
            spectrum(1, "scan=2", 2, 120, driftTime(10), isolation, new double[]{101},
                new double[]{2}),
            spectrum(2, "scan=3", 2, 120, driftTime(20), "", new double[]{201}, new double[]{3}),
            spectrum(3, "scan=4", 2, 180, driftTime(20), isolation, new double[]{202},
                new double[]{4})));
    final var frames = Assertions.assertInstanceOf(IMSRawDataFile.class, importMzml(xml, true))
        .getFrames();
    final var inactive = frames.get(1);
    assertPeaks(inactive.getMobilityScans().get(1), new double[]{201}, new double[]{3});
    assertPeaks(inactive.getMobilityScans().get(2), new double[0], new double[0]);
    final var first = inactive.getImsMsMsInfos().iterator().next();
    Assertions.assertEquals(Range.closed(0, 0), first.getSpectrumNumberRange());
    final var last = frames.getLast().getImsMsMsInfos().iterator().next();
    Assertions.assertEquals(Range.closed(0, 2), last.getSpectrumNumberRange());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void importsChromatogramOnlyMrmWithSortedDeduplicatedPointsAndTransition(final boolean useGroups)
      throws IOException {
    final String trace = """
        <chromatogram id="transition" index="0" defaultArrayLength="3">
          <cvParam accession="MS:1001473" value=""/>
          <cvParam accession="MS:1000130" value=""/>
          <precursor>
            <isolationWindow><cvParam accession="MS:1000827" value="500"/></isolationWindow>
            <activation>
              <cvParam accession="MS:1000133" value=""/>
              <cvParam accession="MS:1000045" value="18"/>
            </activation>
          </precursor>
          <product>
            <isolationWindow><cvParam accession="MS:1000827" value="150"/></isolationWindow>
          </product>
          <binaryDataArrayList count="2">%s%s</binaryDataArrayList>
        </chromatogram>
        """.formatted(binaryArray("MS:1000595", "UO:0000031", 2, 1, 1),
        binaryArray("MS:1000515", "MS:1000131", 20, 10, 10));
    final String emptyTrace = """
        <chromatogram id="empty" index="1" defaultArrayLength="0">
          <cvParam accession="MS:1001473" value=""/>
          <binaryDataArrayList count="2">%s%s</binaryDataArrayList>
        </chromatogram>
        """.formatted(binaryArray("MS:1000595", "UO:0000031"),
        binaryArray("MS:1000515", "MS:1000131"));
    String xml = document(
        "<chromatogramList count=\"2\">" + trace + emptyTrace + "</chromatogramList>");
    if (useGroups) {
      final String product = "<cvParam accession=\"MS:1000827\" value=\"150\"/>";
      final String time = "<cvParam accession=\"MS:1000595\" unitAccession=\"UO:0000031\" value=\"\"/>";
      xml = xml.replace(product, "<referenceableParamGroupRef ref=\"product\"/>")
          .replace(time, "<referenceableParamGroupRef ref=\"time\"/>").replace("<run id=", """
              <referenceableParamGroupList count="2">
                <referenceableParamGroup id="product">%s</referenceableParamGroup>
                <referenceableParamGroup id="time">%s</referenceableParamGroup>
              </referenceableParamGroupList>
              <run id=""".formatted(product, time)).replace("<product>", """
              <vendor><vendor><product><isolationWindow>
                <cvParam accession="MS:1000827" value="999"/>
              </isolationWindow></product></vendor></vendor>
              <product>""");
    }
    final RawDataFile file = importMzml(xml, false);

    Assertions.assertEquals(0, file.getNumOfScans());
    Assertions.assertEquals(1, file.getOtherDataFiles().size());
    final var traces = file.getOtherDataFiles().getFirst().getOtherTimeSeriesData().getRawTraces();
    Assertions.assertEquals(1, traces.size());
    final var feature = traces.getFirst();
    final var series = feature.getFeatureData();
    Assertions.assertEquals(2, series.getNumberOfValues());
    Assertions.assertEquals(1f, series.getRetentionTime(0));
    Assertions.assertEquals(10d, series.getIntensity(0));
    Assertions.assertEquals(2f, series.getRetentionTime(1));
    Assertions.assertEquals(20d, series.getIntensity(1));
    Assertions.assertEquals(PolarityType.POSITIVE, feature.get(PolarityTypeType.class));
    Assertions.assertEquals(150d, feature.get(MZType.class));
    final DDAMsMsInfo info = Assertions.assertInstanceOf(DDAMsMsInfo.class,
        feature.get(MsMsInfoType.class).getFirst());
    Assertions.assertEquals(500d, info.getIsolationMz());
    Assertions.assertEquals(18f, info.getActivationEnergy());
  }

  @Test
  void importsInterleavedUvAndMassSpectra() throws IOException {
    final String uv = spectrum(1, "scan=2", 1, 90, "", "", new double[]{210, 254},
        new double[]{3, 8}).replace("MS:1000514", "MS:1000617").replace("MS:1000040", "UO:0000018")
        .replace("MS:1000127", "MS:1000804");
    final RawDataFile file = importMzml(document(
        spectra(spectrum(0, "scan=1", 1, 60, "", "", new double[]{110}, new double[]{4}), uv,
            spectrum(2, "scan=3", 1, 120, "", "", new double[]{220}, new double[]{5}))), true);
    Assertions.assertEquals(List.of(1, 3),
        file.getScans().stream().map(Scan::getScanNumber).toList());
    final var spectra = file.getOtherDataFiles().getFirst().getOtherSpectralData().getSpectra();
    Assertions.assertEquals(1, spectra.size());
    final var spectrum = spectra.getFirst();
    Assertions.assertEquals(1.5f, spectrum.getRetentionTime());
    Assertions.assertEquals(2, spectrum.getNumberOfValues());
    Assertions.assertEquals(210d, spectrum.getDomainValue(0));
    Assertions.assertEquals(254d, spectrum.getDomainValue(1));
    Assertions.assertEquals(3d, spectrum.getRangeValue(0));
    Assertions.assertEquals(8d, spectrum.getRangeValue(1));
  }

  @Test
  void skipsSpectrumWithoutBinaryArrays() throws IOException {
    final String noArrays = """
        <spectrum index="0" id="scan=1" defaultArrayLength="0">
          <cvParam accession="MS:1000511" value="1"/>
          <scanList count="1">
            <scan><cvParam accession="MS:1000016" value="60" unitAccession="UO:0000010"/></scan>
          </scanList>
        </spectrum>
        """;
    final RawDataFile file = importMzml(document(spectra(noArrays,
        spectrum(1, "scan=2", 1, 120, "", "", new double[]{100}, new double[]{1}))), true);

    Assertions.assertEquals(List.of(2), file.getScans().stream().map(Scan::getScanNumber).toList());
  }

  @Test
  void readsDiaIsolationWindowWithoutASelectedIonOrActivation() throws IOException {
    final String window = """
        <precursor><isolationWindow>
          <cvParam accession="MS:1000827" value="400"/>
          <cvParam accession="MS:1000828" value="3"/>
          <cvParam accession="MS:1000829" value="5"/>
        </isolationWindow></precursor>
        """;
    final RawDataFile file = importMzml(document(spectra(
            spectrum(0, "scan=1", 2, 60, "", precursors(window), new double[]{100}, new double[]{1}))),
        true);
    final var info = Assertions.assertInstanceOf(DIAMsMsInfoImpl.class,
        file.getScan(0).getMsMsInfo());
    Assertions.assertEquals(Range.closed(397d, 405d), info.getIsolationWindow());
    Assertions.assertNull(info.getActivationEnergy());
  }

  @ParameterizedTest
  @ValueSource(strings = {"zip", "gz"})
  void archivePublishesExactlyTheFileItReturns(@NotNull final String extension) throws IOException {
    final Path path = tempDir.resolve("synthetic.mzML." + extension);
    final String xml = document(
        spectra(spectrum(0, "scan=1", 1, 60, "", "", new double[]{100}, new double[]{2})));
    final byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
    if (extension.equals("zip")) {
      try (final var out = new ZipOutputStream(Files.newOutputStream(path))) {
        out.putNextEntry(new ZipEntry("synthetic.mzML"));
        out.write(bytes);
      }
    } else {
      try (final var out = new GZIPOutputStream(Files.newOutputStream(path))) {
        out.write(bytes);
      }
    }
    final var project = new MZmineProjectImpl();
    final var task = new ZipImportTask(project, path.toFile(),
        ScanImportProcessorConfig.createDefault(), MSDKmzMLImportModule.class,
        new MSDKmzMLImportParameters().cloneParameterSet(), Instant.EPOCH, null);
    task.run();
    Assertions.assertEquals(TaskStatus.FINISHED, task.getStatus(), task::getErrorMessage);
    Assertions.assertEquals(1, project.getCurrentRawDataFiles().size());
    final var file = project.getCurrentRawDataFiles().getFirst();
    Assertions.assertEquals(List.of(file), task.getImportedRawDataFiles());
    Assertions.assertEquals(1, file.getAppliedMethods().size());
    assertPeaks(file.getScan(0), new double[]{100}, new double[]{2});
  }

  @Test
  void rejectsAWellFormedConverterStreamWithMissingSpectra() {
    final String xml = document(
        spectra(spectrum(0, "scan=42", 1, 60, "", "", new double[]{100}, new double[]{2}))).replace(
        "spectrumList count=\"1\"", "spectrumList count=\"2\"");
    final var project = new MZmineProjectImpl();
    final var task = new MzMLImportTask(project, tempDir.resolve("vendor.raw").toFile(),
        new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),
        ScanImportProcessorConfig.createDefault(), MSDKmzMLImportModule.class,
        new MSDKmzMLImportParameters().cloneParameterSet(), Instant.EPOCH, null);
    Assertions.assertNull(task.importConvertedData());
    Assertions.assertEquals(TaskStatus.ERROR, task.getStatus());
    Assertions.assertTrue(task.getErrorMessage().contains("1 of 2"));
    Assertions.assertTrue(project.getCurrentRawDataFiles().isEmpty());
  }

  @Test
  void doesNotRestartCanceledImport() {
    final var project = new MZmineProjectImpl();
    final var task = new MzMLImportTask(project, tempDir.resolve("never-opened.mzML").toFile(),
        null, ScanImportProcessorConfig.createDefault(), MSDKmzMLImportModule.class,
        new MSDKmzMLImportParameters().cloneParameterSet(), Instant.EPOCH, null);
    task.cancel();
    task.run();
    Assertions.assertEquals(TaskStatus.CANCELED, task.getStatus());
    Assertions.assertTrue(project.getCurrentRawDataFiles().isEmpty());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void reportsSpectrumIdForDamagedBinaryData(final boolean empty) {
    final String document = document(
        spectra(spectrum(0, "scan=42", 1, 60, "", "", new double[]{100}, new double[]{2})));
    final String xml = empty ? document.replaceFirst("encodedLength=\"12\"", "encodedLength=\"0\"")
        .replaceFirst("<binary>[^<]+", "<binary>")
        : document.replaceFirst("<binary>[^<]+", "<binary>AA==");
    final var project = new MZmineProjectImpl();
    final var task = new MzMLImportTask(project, tempDir.resolve("vendor.raw").toFile(),
        new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),
        ScanImportProcessorConfig.createDefault(), MSDKmzMLImportModule.class,
        new MSDKmzMLImportParameters().cloneParameterSet(), Instant.EPOCH, null);
    task.run();
    Assertions.assertEquals(TaskStatus.ERROR, task.getStatus());
    Assertions.assertTrue(task.getErrorMessage().contains("scan=42"));
    Assertions.assertTrue(project.getCurrentRawDataFiles().isEmpty());
  }

  @Test
  void truncatedStreamPublishesNothing() {
    final var project = new MZmineProjectImpl();
    final byte[] truncated = "<mzML><run defaultInstrumentConfigurationRef=\"IC1\">".getBytes(
        StandardCharsets.UTF_8);
    final var task = new MzMLImportTask(project, tempDir.resolve("vendor.raw").toFile(),
        new ByteArrayInputStream(truncated), ScanImportProcessorConfig.createDefault(),
        MSDKmzMLImportModule.class, new MSDKmzMLImportParameters().cloneParameterSet(),
        Instant.EPOCH, null);
    task.run();
    Assertions.assertEquals(TaskStatus.ERROR, task.getStatus());
    Assertions.assertTrue(project.getCurrentRawDataFiles().isEmpty());
    Assertions.assertTrue(task.getImportedRawDataFiles().isEmpty());
  }

  @Test
  void appliesMobilityRangeFilterDuringImport() throws IOException {
    final var filter = new ScanSelection(null, null, null, Range.closed(15d, 25d), PolarityType.ANY,
        MassSpectrumType.ANY, MsLevelFilter.ALL_LEVELS, null);
    final var config = new ScanImportProcessorConfig(filter,
        new MsProcessorList(List.of(new SortByMzMsProcessor())));
    final String input = document(
        spectra(spectrum(0, "scan=1", 1, 60, driftTime(10), "", new double[]{110}, new double[]{1}),
            spectrum(1, "scan=2", 1, 60, driftTime(20), "", new double[]{120}, new double[]{2}),
            spectrum(2, "scan=3", 1, 60, driftTime(30), "", new double[]{130}, new double[]{3}),
            spectrum(3, "scan=4", 1, 120, driftTime(30), "", new double[]{230}, new double[]{4})));
    final IMSRawDataFile ims = Assertions.assertInstanceOf(IMSRawDataFile.class,
        importMzml(input, true, config));

    Assertions.assertEquals(1, ims.getFrames().size());
    final Frame frame = ims.getFrames().getFirst();
    Assertions.assertEquals(1f, frame.getRetentionTime());
    final var bins = frame.getMobilityScans();
    Assertions.assertEquals(List.of(10d, 20d, 30d),
        bins.stream().map(MobilityScan::getMobility).toList());
    assertPeaks(bins.get(0), new double[]{110}, new double[]{1});
    assertPeaks(bins.get(1), new double[]{120}, new double[]{2});
    assertPeaks(bins.get(2), new double[]{130}, new double[]{3});
  }

  @Test
  void acceptsDescendingDriftTimesWithinAFrame() throws IOException {
    final IMSRawDataFile ims = Assertions.assertInstanceOf(IMSRawDataFile.class,
        importMzml(driftFrame(30, 20, 10).replaceFirst("<scan>", """
            <scan><cvParam accession="MS:1000512" value="first acquired bin"/>
            """), true));

    final Frame frame = ims.getFrames().getFirst();
    Assertions.assertEquals("first acquired bin", frame.getScanDefinition());
    Assertions.assertEquals(List.of(10d, 20d, 30d),
        frame.getMobilityScans().stream().map(MobilityScan::getMobility).toList());
    for (final MobilityScan scan : frame.getMobilityScans()) {
      // driftFrame stores the drift time as m/z
      assertPeaks(scan, new double[]{scan.getMobility()}, new double[]{1});
    }
  }

  @Test
  void splitsWatersFunctionsAtEqualRtAndPadsOnlyOnTheirOwnAxis() throws IOException {
    final String xml = document(
        spectra(mobilitySpectrum(0, 1, 60, 10), mobilitySpectrum(1, 1, 60, 20),
            mobilitySpectrum(2, 2, 60, 10), mobilitySpectrum(3, 3, 60, 40),
            mobilitySpectrum(4, 3, 120, 40)));
    final var file = Assertions.assertInstanceOf(IMSRawDataFile.class, importMzml(xml, true));
    final var frames = file.getFrames();
    Assertions.assertEquals(4, frames.size());
    final var padded = frames.get(1).getMobilityScans();
    Assertions.assertEquals(List.of(10d, 20d),
        padded.stream().map(MobilityScan::getMobility).toList());
    assertPeaks(padded.getFirst(), new double[]{102}, new double[]{2});
    assertPeaks(padded.getLast(), new double[0], new double[0]);
    Assertions.assertNull(padded.getLast().getBasePeakIndex());
    for (final var frame : frames.subList(2, 4)) {
      Assertions.assertEquals(1, frame.getNumberOfMobilityScans());
      Assertions.assertEquals(40d, frame.getMobilityForMobilityScanNumber(0));
    }
  }

  @Test
  void selectsEverySecondFrameAndWarnsOnceForRepeatedCoordinates() throws IOException {
    final String xml = document(
        spectra(mobilitySpectrum(0, 1, 60, 10), mobilitySpectrum(1, 1, 60, 10),
            mobilitySpectrum(2, 1, 60, 20), mobilitySpectrum(3, 1, 120, 10),
            mobilitySpectrum(4, 1, 180, 10), mobilitySpectrum(5, 1, 180, 20)));
    final var filter = new ScanSelection(null, 2, null, null, PolarityType.ANY,
        MassSpectrumType.ANY, MsLevelFilter.ALL_LEVELS, null);
    final var config = new ScanImportProcessorConfig(filter,
        new MsProcessorList(List.of(new SortByMzMsProcessor())));
    final var file = Assertions.assertInstanceOf(IMSRawDataFile.class,
        importMzml(xml, true, config));
    Assertions.assertEquals(List.of(1, 3),
        file.getFrames().stream().map(Frame::getScanNumber).toList());
    final var first = file.getFrames().getFirst().getMobilityScans();
    assertPeaks(first.getFirst(), new double[]{100}, new double[]{0});
    assertPeaks(first.getLast(), new double[]{102}, new double[]{2});
    final var last = file.getFrames().getLast().getMobilityScans();
    assertPeaks(last.getFirst(), new double[]{104}, new double[]{4});
    assertPeaks(last.getLast(), new double[]{105}, new double[]{5});
    final var warnings = file.getWarnings().stream()
        .filter(w -> w.type() == RawFileWarningType.DUPLICATE_MOBILITY_SCANS).toList();
    Assertions.assertEquals(1, warnings.size());
    Assertions.assertTrue(warnings.getFirst().isCritical());
  }

  @Test
  void keepsFirstAcquiredSpectrumForNearlyEqualMobilities() throws IOException {
    final String xml = document(spectra(
        spectrum(0, "scan=1", 1, 60, driftTime(10.00001), "", new double[]{101}, new double[]{1}),
        spectrum(1, "scan=2", 1, 60, driftTime(10.0), "", new double[]{202}, new double[]{2})));
    final var file = Assertions.assertInstanceOf(IMSRawDataFile.class, importMzml(xml, true));
    final var bins = file.getFrames().getFirst().getMobilityScans();
    Assertions.assertEquals(1, bins.size());
    Assertions.assertEquals(10.00001, bins.getFirst().getMobility(), 1e-12);
    assertPeaks(bins.getFirst(), new double[]{101}, new double[]{1});
    Assertions.assertEquals(1, file.getWarnings().stream()
        .filter(w -> w.type() == RawFileWarningType.DUPLICATE_MOBILITY_SCANS).count());
  }

  @Test
  void usesTighterToleranceForCombinedTimsBins() throws IOException {
    final String spectrum = spectrum(0, "scan=1", 1, 60, "", "", new double[]{100, 200, 300},
        new double[]{1, 2, 3}).replace("binaryDataArrayList count=\"2\"",
        "binaryDataArrayList count=\"3\"").replace("</binaryDataArrayList>",
        binaryArray("MS:1003007", "MS:1002814", 1.0, 1.000001, 1.00001) + "</binaryDataArrayList>");
    final var file = Assertions.assertInstanceOf(IMSRawDataFile.class,
        importMzml(document(spectra(spectrum)), true));
    final var bins = file.getFrames().getFirst().getMobilityScans();
    Assertions.assertEquals(2, bins.size());
    Assertions.assertEquals(1.00001, bins.getFirst().getMobility(), 1e-12);
    Assertions.assertEquals(1.0, bins.getLast().getMobility(), 1e-12);
    assertPeaks(bins.getFirst(), new double[]{300}, new double[]{3});
    assertPeaks(bins.getLast(), new double[]{100}, new double[]{1});
    Assertions.assertEquals(1, file.getWarnings().stream()
        .filter(w -> w.type() == RawFileWarningType.DUPLICATE_MOBILITY_SCANS).count());
  }

  @Test
  void skipsBinaryDecodingForFramesRejectedByMetadata() throws IOException {
    final String ms2First = spectrum(0, "scan=1", 2, 60, driftTime(10), "", new double[]{100},
        new double[]{1});
    final String ms2Last = spectrum(1, "scan=2", 2, 60, driftTime(20), "", new double[]{200},
        new double[]{2});
    final String earlyCombined = spectrum(2, "scan=3", 1, 30, "", "", new double[]{300},
        new double[]{3}).replace("binaryDataArrayList count=\"2\"",
        "binaryDataArrayList count=\"3\"").replace("</binaryDataArrayList>",
        binaryArray("MS:1003007", "UO:0000028", 10) + "</binaryDataArrayList>");
    // Truncated doubles make an attempted decode fail even if the frame is discarded afterwards.
    final String xml = document(spectra(ms2First.replaceFirst("<binary>[^<]+", "<binary>AA=="),
        ms2Last.replaceFirst("<binary>[^<]+", "<binary>AA=="),
        earlyCombined.replaceFirst("<binary>[^<]+", "<binary>AA=="),
        mobilitySpectrum(3, 1, 120, 10), mobilitySpectrum(4, 1, 120, 20)));
    final var selection = new ScanSelection(Range.closed(1d, 2d), 1);
    final var config = new ScanImportProcessorConfig(selection,
        new MsProcessorList(List.of(new SortByMzMsProcessor())));
    final var file = Assertions.assertInstanceOf(IMSRawDataFile.class,
        importMzml(xml, true, config));
    Assertions.assertEquals(List.of(3),
        file.getFrames().stream().map(Frame::getScanNumber).toList());
    final var bins = file.getFrames().getFirst().getMobilityScans();
    Assertions.assertEquals(2, bins.size());
    assertPeaks(bins.getFirst(), new double[]{103}, new double[]{3});
    assertPeaks(bins.getLast(), new double[]{104}, new double[]{4});
  }

  @Test
  void retainsFinalRunOfCombinedMobilities() throws IOException {
    final String xml = spectrum(0, "scan=1", 1, 60, "", "", new double[]{120, 110, 111},
        new double[]{2, 3, 4}).replace("binaryDataArrayList count=\"2\"",
        "binaryDataArrayList count=\"3\"").replace("</binaryDataArrayList>",
        binaryArray("MS:1003007", "UO:0000028", 20, 10, 10) + "</binaryDataArrayList>");
    final var file = Assertions.assertInstanceOf(IMSRawDataFile.class,
        importMzml(document(spectra(xml)), true));
    final var bins = file.getFrames().getFirst().getMobilityScans();
    Assertions.assertEquals(List.of(10d, 20d),
        bins.stream().map(MobilityScan::getMobility).toList());
    assertPeaks(bins.getFirst(), new double[]{110, 111}, new double[]{3, 4});
    assertPeaks(bins.getLast(), new double[]{120}, new double[]{2});
  }

  @Test
  void removesOnlyRedundantZerosFromProfileScans() throws IOException {
    final double[] mzs = {100, 101, 102, 103, 104, 105, 106, 107, 108, 109};
    final double[] intensities = {0, 0, 5, 0, 0, 0, -2, 0, 0, -0d};
    final String profile = spectrum(0, "scan=1", 1, 60,
        "<scanWindowList count=\"1\"><scanWindow/></scanWindowList>", "", mzs, intensities).replace(
        "MS:1000127", "MS:1000128");
    final String centroid = spectrum(1, "scan=2", 1, 120, "", "", mzs, intensities);
    final RawDataFile file = importMzml(document(spectra(profile, centroid)), true);

    assertPeaks(file.getScan(0), new double[]{101, 102, 103, 105, 106, 107},
        new double[]{0, 5, 0, 0, -2, 0});
    Assertions.assertEquals(Range.closed(100d, 109d), file.getScan(0).getScanningMZRange());
    assertPeaks(file.getScan(1), mzs, intensities);
  }

  /**
   * @param combined whether MSConvert encodes the frame in one spectrum
   */
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void removesProfileZerosWithinEachStoredMobilityBin(final boolean combined) throws IOException {
    final String spectraXml;
    if (combined) {
      final String frame = spectrum(0, "scan=1", 1, 60, "", "",
          new double[]{100, 101, 102, 103, 104, 200, 201, 202},
          new double[]{0, 0, 3, 0, 5, 0, 0, 0}).replace("binaryDataArrayList count=\"2\"",
          "binaryDataArrayList count=\"3\"").replace("</binaryDataArrayList>",
          binaryArray("MS:1003007", "UO:0000028", 10, 10, 10, 10, 10, 20, 20, 20)
              + "</binaryDataArrayList>");
      spectraXml = spectra(frame);
    } else {
      spectraXml = spectra(
          spectrum(0, "scan=1", 1, 60, driftTime(10), "", new double[]{100, 101, 102, 103, 104},
              new double[]{0, 0, 3, 0, 5}),
          spectrum(1, "scan=2", 1, 60, driftTime(20), "", new double[]{200, 201, 202},
              new double[]{0, 0, 0}));
    }
    final var file = Assertions.assertInstanceOf(IMSRawDataFile.class,
        importMzml(document(spectraXml.replace("MS:1000127", "MS:1000128")), true));
    final List<MobilityScan> bins = file.getFrames().getFirst().getMobilityScans();
    Assertions.assertEquals(2, bins.size());
    assertPeaks(bins.getFirst(), new double[]{101, 102, 103, 104}, new double[]{0, 3, 0, 5});
    Assertions.assertEquals(104d, bins.getFirst().getBasePeakMz());
    Assertions.assertTrue(bins.getLast().isEmptyScan());
  }

  private static @NotNull String mobilitySpectrum(final int index, final int function,
      final int seconds, final int mobility) {
    return spectrum(index, "function=%d process=0 scan=%d".formatted(function, index + 1), 1,
        seconds, driftTime(mobility), "", new double[]{100 + index}, new double[]{index});
  }

  @Disabled("deferred: representing simultaneous MS2 targets requires a data model change")
  @Test
  void keepsMultiplexedMs2PrecursorsAtMsLevelTwo() throws IOException {
    final RawDataFile file = importMzml(document(spectra(spectrum(0, "scan=1", 2, 60, "",
        precursors(precursor("", 400, 1, "400", "", ""), precursor("", 500, 1, "500", "", "")),
        new double[]{100}, new double[]{1}))), true);

    final MsMsInfo info = file.getScan(0).getMsMsInfo();
    Assertions.assertNotNull(info);
    Assertions.assertFalse(info instanceof MSnInfoImpl);
    Assertions.assertEquals(2, info.getMsLevel());
  }

  @Test
  void convertsChromatogramTimeArrayInSeconds() throws IOException {
    final String trace = """
        <chromatogram id="TIC" index="0" defaultArrayLength="2">
          <cvParam accession="MS:1000235" value=""/>
          <binaryDataArrayList count="2">%s%s</binaryDataArrayList>
        </chromatogram>
        """.formatted(binaryArray("MS:1000595", "UO:0000010", 60, 120),
        binaryArray("MS:1000515", "MS:1000131", 10, 20));
    final RawDataFile file = importMzml(
        document("<chromatogramList count=\"1\">" + trace + "</chromatogramList>"), false);

    final var series = file.getOtherDataFiles().getFirst().getOtherTimeSeriesData().getRawTraces()
        .getFirst().getFeatureData();
    Assertions.assertEquals(1f, series.getRetentionTime(0));
    Assertions.assertEquals(2f, series.getRetentionTime(1));
  }

  /**
   * @param driftTimes drift times in document order, one single-peak spectrum each at RT 60 s
   * @return document with one drift-tube frame; each peak's m/z equals its drift time
   */
  private static @NotNull String driftFrame(final int @NotNull ... driftTimes) {
    final String[] spectra = new String[driftTimes.length];
    for (int i = 0; i < driftTimes.length; i++) {
      spectra[i] = spectrum(i, "scanId=" + (i + 1), 1, 60, driftTime(driftTimes[i]), "",
          new double[]{driftTimes[i]}, new double[]{1});
    }
    return document(spectra(spectra));
  }

  /**
   * Imports with the default processing config.
   *
   * @param xml    minimal mzML document
   * @param stream whether to use the converter-style entry point
   * @return imported data
   */
  private @NotNull RawDataFile importMzml(@NotNull final String xml, final boolean stream)
      throws IOException {
    return importMzml(xml, stream, ScanImportProcessorConfig.createDefault());
  }

  /**
   * Imports the same XML via a file or a non-seekable stream with a separate source identity.
   *
   * @param xml    minimal mzML document
   * @param stream whether to use the converter-style entry point
   * @param config scan filter and processing applied during import
   * @return imported data
   */
  private @NotNull RawDataFile importMzml(@NotNull final String xml, final boolean stream,
      @NotNull final ScanImportProcessorConfig config) throws IOException {
    final Path source = tempDir.resolve(stream ? "source.raw" : "synthetic.mzML");
    final var project = new MZmineProjectImpl();
    final var parameters = new MSDKmzMLImportParameters().cloneParameterSet();
    if (stream) {
      try (InputStream input = Channels.newInputStream(
          Channels.newChannel(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public synchronized int read(final byte @NotNull [] buffer, final int offset,
                final int length) {
              return super.read(buffer, offset, Math.min(3, length));
            }
          }))) {
        final var task = new MzMLImportTask(project, source.toFile(), input, config,
            MSDKmzMLImportModule.class, parameters, Instant.EPOCH, null);
        final RawDataFile file = task.importStreamOrFile();
        Assertions.assertNotNull(file, task::getErrorMessage);
        Assertions.assertEquals(source.toString(), file.getAbsolutePath());
        return file;
      }
    }

    Files.writeString(source, xml, StandardCharsets.UTF_8);
    final var task = new MzMLImportTask(project, source.toFile(), null, config,
        MSDKmzMLImportModule.class, parameters, Instant.EPOCH, null);
    task.run();
    Assertions.assertEquals(TaskStatus.FINISHED, task.getStatus(), task::getErrorMessage);
    Assertions.assertEquals(1, task.getImportedRawDataFiles().size());
    return task.getImportedRawDataFiles().getFirst();
  }

  /**
   * @param body XML fragment inside the run
   * @return minimal mzML document
   */
  private static @NotNull String document(@NotNull final String body) {
    return """
        <mzML xmlns="http://psi.hupo.org/ms/mzml" version="1.1.0">
          <run id="synthetic" defaultInstrumentConfigurationRef="IC1">
            %s
          </run>
        </mzML>
        """.formatted(body);
  }

  /**
   * @param spectra individual spectrum elements in document order
   * @return spectrum list fragment
   */
  private static @NotNull String spectra(@NotNull final String... spectra) {
    return """
        <spectrumList count="%d" defaultDataProcessingRef="DP1">
          %s
        </spectrumList>
        """.formatted(spectra.length, String.join("\n", spectra));
  }

  /**
   * @param index       zero-based document index
   * @param id          native spectrum identifier
   * @param level       MS level
   * @param seconds     retention time in seconds
   * @param scanParams  extra scan-level XML
   * @param precursors  precursor list XML, or an empty string
   * @param mzs         m/z values in file order
   * @param intensities matching intensity values
   * @return spectrum fragment
   */
  private static @NotNull String spectrum(final int index, @NotNull final String id,
      final int level, final int seconds, @NotNull final String scanParams,
      @NotNull final String precursors, final double @NotNull [] mzs,
      final double @NotNull [] intensities) {
    return """
        <spectrum index="%d" id="%s" defaultArrayLength="%d">
          <cvParam accession="MS:1000511" value="%d"/>
          <cvParam accession="MS:1000127" value=""/>
          <scanList count="1">
            <scan>
              <cvParam accession="MS:1000016" value="%d" unitAccession="UO:0000010"/>
              %s
            </scan>
          </scanList>
          %s
          <binaryDataArrayList count="2">%s%s</binaryDataArrayList>
        </spectrum>
        """.formatted(index, id, mzs.length, level, seconds, scanParams, precursors,
        binaryArray("MS:1000514", "MS:1000040", mzs),
        binaryArray("MS:1000515", "MS:1000131", intensities));
  }

  /**
   * @param milliseconds drift time
   * @return scan-level drift time parameter
   */
  private static @NotNull String driftTime(final double milliseconds) {
    return """
        <cvParam accession="MS:1002476" value="%s" unitAccession="UO:0000028"/>
        """.formatted(milliseconds);
  }

  /**
   * @param precursors precursor elements in document order
   * @return precursor list fragment
   */
  private static @NotNull String precursors(@NotNull final String... precursors) {
    return """
        <precursorList count="%d">%s</precursorList>
        """.formatted(precursors.length, String.join("\n", precursors));
  }

  /**
   * @param ref              original spectrum reference
   * @param targetMz         isolation target
   * @param upperOffset      upper isolation offset in Da
   * @param selectedMz       selected m/z as encoded in XML
   * @param activation       activation parameters
   * @param windowUserParams optional isolation-window parameters
   * @return precursor fragment
   */
  private static @NotNull String precursor(@NotNull final String ref, final int targetMz,
      final int upperOffset, @NotNull final String selectedMz, @NotNull final String activation,
      @NotNull final String windowUserParams) {
    return """
        <precursor spectrumRef="%s">
          <isolationWindow>
            <cvParam accession="MS:1000827" value="%d"/>
            <cvParam accession="MS:1000828" value="1"/>
            <cvParam accession="MS:1000829" value="%d"/>
            %s
          </isolationWindow>
          <selectedIonList count="1">
            <selectedIon>
              <cvParam accession="MS:1000744" value="%s"/>
              <cvParam accession="MS:1000041" value="2"/>
            </selectedIon>
          </selectedIonList>
          <activation>%s</activation>
        </precursor>
        """.formatted(ref, targetMz, upperOffset, windowUserParams, selectedMz, activation);
  }

  /**
   * @param accession array type
   * @param unit      unit accession
   * @param points    data in file order
   * @return independently encoded, uncompressed 64-bit binary array
   */
  private static @NotNull String binaryArray(@NotNull final String accession,
      @NotNull final String unit, final double @NotNull ... points) {
    final ByteBuffer buffer = ByteBuffer.allocate(points.length * Double.BYTES)
        .order(ByteOrder.LITTLE_ENDIAN);
    for (final double point : points) {
      buffer.putDouble(point);
    }
    final String encoded = Base64.getEncoder().encodeToString(buffer.array());
    return """
        <binaryDataArray encodedLength="%d">
          <cvParam accession="MS:1000523" value=""/>
          <cvParam accession="MS:1000576" value=""/>
          <cvParam accession="%s" unitAccession="%s" value=""/>
          <binary>%s</binary>
        </binaryDataArray>
        """.formatted(encoded.length(), accession, unit, encoded);
  }

  /**
   * @param spectrum    imported spectrum or mobility scan
   * @param mzs         expected m/z values
   * @param intensities expected matching intensity values
   */
  private static void assertPeaks(@NotNull final MassSpectrum spectrum,
      final double @NotNull [] mzs, final double @NotNull [] intensities) {
    Assertions.assertEquals(mzs.length, spectrum.getNumberOfDataPoints());
    Assertions.assertArrayEquals(mzs, spectrum.getMzValues(new double[mzs.length]));
    Assertions.assertArrayEquals(intensities,
        spectrum.getIntensityValues(new double[intensities.length]));
  }
}
