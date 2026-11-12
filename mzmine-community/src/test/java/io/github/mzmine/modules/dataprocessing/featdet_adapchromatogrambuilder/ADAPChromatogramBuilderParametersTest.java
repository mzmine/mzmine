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

package io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder;

import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder.ChromatogramBuilderSensitivity;
import io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder.FastAutoChromatogramBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder.FastChromatogramBuilderParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.combowithinput.MZToleranceOrAuto;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class ADAPChromatogramBuilderParametersTest {

  // a batch step of mzmine before the algorithm selection, from gc_tof.mzbatch
  private static final String VERSION_1_STEP = """
      <batchstep method="io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderModule"
          module_name="Chromatogram builder" parameter_version="1">
          <parameter name="Raw data files" type="BATCH_LAST_FILES"/>
          <parameter name="Scan filters" selected="true">
              <parameter name="Scan number"/>
              <parameter name="Base Filtering Integer"/>
              <parameter name="Retention time">
                  <min>0.3</min>
                  <max>30.0</max>
              </parameter>
              <parameter name="Mobility"/>
              <parameter name="MS level filter" selected="MS1, level = 1">1</parameter>
              <parameter name="Scan definition"/>
              <parameter name="Polarity">Any</parameter>
              <parameter name="Spectrum type">ANY</parameter>
          </parameter>
          <parameter name="Minimum consecutive scans">7</parameter>
          <parameter name="Minimum intensity for consecutive scans">1234.0</parameter>
          <parameter name="Minimum absolute height">5678.0</parameter>
          <parameter name="m/z tolerance (scan-to-scan)">
              <absolutetolerance>0.005</absolutetolerance>
              <ppmtolerance>20.0</ppmtolerance>
          </parameter>
          <parameter name="Suffix">eics</parameter>
          <parameter name="Clear previous RT corrections">false</parameter>
      </batchstep>""";

  // the names of older versions that were renamed before version 1
  private static final String OLD_NAMES_STEP = """
      <batchstep method="io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderModule">
          <parameter name="Raw data files" type="BATCH_LAST_FILES"/>
          <parameter name="Min group size in # of scans">6</parameter>
          <parameter name="Group intensity threshold">300.0</parameter>
          <parameter name="Min highest intensity">900.0</parameter>
          <parameter name="Scan to scan accuracy (m/z)">
              <absolutetolerance>0.002</absolutetolerance>
              <ppmtolerance>8.0</ppmtolerance>
          </parameter>
          <parameter name="Suffix">chroms</parameter>
      </batchstep>""";

  @Test
  void version1StepLoadsAsLegacyAlgorithmWithItsValues() throws Exception {
    final ADAPChromatogramBuilderParameters parameters = load(VERSION_1_STEP);

    Assertions.assertEquals(ChromatogramBuilderAlgorithms.LEGACY_ADAP,
        parameters.getValue(ADAPChromatogramBuilderParameters.algorithm));
    final ParameterSet legacy = parameters.getEmbeddedParameterValue(
        ADAPChromatogramBuilderParameters.algorithm);
    Assertions.assertEquals(7,
        legacy.getValue(LegacyAdapChromatogramBuilderParameters.minimumConsecutiveScans));
    Assertions.assertEquals(1234d,
        legacy.getValue(LegacyAdapChromatogramBuilderParameters.minGroupIntensity));
    Assertions.assertEquals(5678d,
        legacy.getValue(LegacyAdapChromatogramBuilderParameters.minHighestPoint));
    Assertions.assertEquals(new MZTolerance(0.005, 20),
        legacy.getValue(LegacyAdapChromatogramBuilderParameters.mzTolerance));
    Assertions.assertEquals("eics", parameters.getValue(ADAPChromatogramBuilderParameters.suffix));
    Assertions.assertFalse(parameters.getValue(ADAPChromatogramBuilderParameters.clearRtCorrection));
    Assertions.assertEquals(1, parameters.getValue(ADAPChromatogramBuilderParameters.scanSelection)
        .getMsLevelFilter().getSingleMsLevelOrNull());

    final ChromatogramBuilderSettings settings = ADAPChromatogramBuilderParameters.getAppliedSettings(
        parameters).orElseThrow();
    Assertions.assertEquals(
        new ChromatogramBuilderSettings(7, 1234d, 5678d, new MZTolerance(0.005, 20)), settings);
    // the user sees which algorithm the old step uses
    Assertions.assertFalse(parameters.getLoadingVersionMessages().isBlank());
  }

  @Test
  void oldParameterNamesLoadIntoTheLegacyAlgorithm() throws Exception {
    final ADAPChromatogramBuilderParameters parameters = load(OLD_NAMES_STEP);

    Assertions.assertEquals(ChromatogramBuilderAlgorithms.LEGACY_ADAP,
        parameters.getValue(ADAPChromatogramBuilderParameters.algorithm));
    Assertions.assertEquals(
        new ChromatogramBuilderSettings(6, 300d, 900d, new MZTolerance(0.002, 8)),
        ADAPChromatogramBuilderParameters.getAppliedSettings(parameters).orElseThrow());
    // missing in old steps, enabled by default
    Assertions.assertTrue(parameters.getValue(ADAPChromatogramBuilderParameters.clearRtCorrection));
  }

  @Test
  void newStepsKeepTheirAlgorithm() throws Exception {
    final RawDataFilesSelection files = new RawDataFilesSelection(
        RawDataFilesSelectionType.BATCH_LAST_FILES);
    final ADAPChromatogramBuilderParameters fastAuto = ADAPChromatogramBuilderParameters.createFastAuto(
        files, new ScanSelection(1), ChromatogramBuilderSensitivity.ABUNDANT, "auto", true);
    final ADAPChromatogramBuilderParameters loadedAuto = load(save(fastAuto));
    Assertions.assertEquals(ChromatogramBuilderAlgorithms.FAST_AUTO,
        loadedAuto.getValue(ADAPChromatogramBuilderParameters.algorithm));
    Assertions.assertEquals(ChromatogramBuilderSensitivity.ABUNDANT,
        loadedAuto.getEmbeddedParameterValue(ADAPChromatogramBuilderParameters.algorithm)
            .getValue(FastAutoChromatogramBuilderParameters.sensitivity));
    Assertions.assertTrue(loadedAuto.getLoadingVersionMessages().isBlank());
    // not applied, no determined values
    Assertions.assertTrue(
        ADAPChromatogramBuilderParameters.getAppliedSettings(loadedAuto).isEmpty());

    // the determined values of an applied method survive saving, e.g., in a project
    final ParameterSet autoParameters = fastAuto.getEmbeddedParameterValue(
        ADAPChromatogramBuilderParameters.algorithm);
    autoParameters.setParameter(FastAutoChromatogramBuilderParameters.determinedMinConsecutiveScans,
        4);
    autoParameters.setParameter(FastAutoChromatogramBuilderParameters.determinedMinGroupIntensity,
        1200d);
    autoParameters.setParameter(FastAutoChromatogramBuilderParameters.determinedMinHeight, 5100d);
    autoParameters.setParameter(FastAutoChromatogramBuilderParameters.determinedMzTolerance,
        new MZTolerance(0.0007, 9.3));
    Assertions.assertEquals(
        new ChromatogramBuilderSettings(4, 1200d, 5100d, new MZTolerance(0.0007, 9.3)),
        ADAPChromatogramBuilderParameters.getAppliedSettings(load(save(fastAuto))).orElseThrow());

    final MZToleranceOrAuto tolerance = MZToleranceOrAuto.auto(new MZTolerance(0.003, 7));
    final ADAPChromatogramBuilderParameters fast = ADAPChromatogramBuilderParameters.createFast(
        files, new ScanSelection(1), 5, tolerance, "fast", 1E3, 1E4, false);
    final ADAPChromatogramBuilderParameters loadedFast = load(save(fast));
    Assertions.assertEquals(ChromatogramBuilderAlgorithms.FAST,
        loadedFast.getValue(ADAPChromatogramBuilderParameters.algorithm));
    final ParameterSet fastParameters = loadedFast.getEmbeddedParameterValue(
        ADAPChromatogramBuilderParameters.algorithm);
    Assertions.assertEquals(5,
        fastParameters.getValue(FastChromatogramBuilderParameters.minimumConsecutiveScans));
    Assertions.assertEquals(tolerance,
        fastParameters.getValue(FastChromatogramBuilderParameters.mzTolerance));
    Assertions.assertEquals(
        new ChromatogramBuilderSettings(5, 1E3, 1E4, new MZTolerance(0.003, 7)),
        ADAPChromatogramBuilderParameters.getAppliedSettings(loadedFast).orElseThrow());
  }

  @NotNull
  private static ADAPChromatogramBuilderParameters load(@NotNull String xml) throws Exception {
    final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    final var parameters = (ADAPChromatogramBuilderParameters) new ADAPChromatogramBuilderParameters().cloneParameterSet();
    parameters.loadValuesFromXML(document.getDocumentElement());
    return parameters;
  }

  @NotNull
  private static String save(@NotNull ParameterSet parameters) throws Exception {
    final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .newDocument();
    final Element step = document.createElement("batchstep");
    step.setAttribute(BatchQueue.MODULE_VERSION_ATTR, String.valueOf(parameters.getVersion()));
    document.appendChild(step);
    parameters.saveValuesToXML(step);
    final Transformer transformer = TransformerFactory.newInstance().newTransformer();
    final StringWriter writer = new StringWriter();
    transformer.transform(new DOMSource(document), new StreamResult(writer));
    return writer.toString();
  }
}
