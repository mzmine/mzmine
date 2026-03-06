/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NormalizationFunctionsParameterTest {

  @TempDir
  Path tempDir;

  @Test
  void saveLoadRoundtripKeepsAllFunctionTypes() {
    final LocalDateTime factorTimestamp = LocalDateTime.of(2026, 1, 1, 10, 0);
    final LocalDateTime standardTimestamp = LocalDateTime.of(2026, 1, 1, 10, 10);
    final LocalDateTime interpolatedTimestamp = LocalDateTime.of(2026, 1, 1, 10, 5);

    final FactorNormalizationFunction factorFunction = new FactorNormalizationFunction(
        new RawDataFilePlaceholder("factor_file", tempDir.resolve("factor.mzML").toString(), 11),
        factorTimestamp, 2d);
    final StandardCompoundNormalizationFunction standardFunction = new StandardCompoundNormalizationFunction(
        new RawDataFilePlaceholder("standard_file", tempDir.resolve("standard.mzML").toString(),
            12), standardTimestamp, StandardUsageType.Nearest, 1d,
        List.of(new StandardCompoundReferencePoint(100d, 5f, 200d, false)));
    final InterpolatedNormalizationFunction interpolatedFunction = new InterpolatedNormalizationFunction(
        new RawDataFilePlaceholder("target_file", tempDir.resolve("target.mzML").toString(), 13),
        interpolatedTimestamp, factorFunction, 0.25d, standardFunction, 0.75d);

    final NormalizationFunctionsParameter parameter = new NormalizationFunctionsParameter();
    parameter.setValue(List.of(factorFunction, standardFunction, interpolatedFunction));

    final String xml = ParameterUtils.saveParameterToXMLString(parameter);
    final NormalizationFunctionsParameter loadedParameter = new NormalizationFunctionsParameter();
    ParameterUtils.loadParameterFromString(loadedParameter, xml);

    final List<NormalizationFunction> loadedFunctions = loadedParameter.getValue();
    assertEquals(3, loadedFunctions.size());

    final FactorNormalizationFunction loadedFactor = assertInstanceOf(
        FactorNormalizationFunction.class, loadedFunctions.get(0));
    assertEquals("factor_file", loadedFactor.rawDataFilePlaceholder().getName());
    assertEquals(factorTimestamp, loadedFactor.acquisitionTimestamp());
    assertEquals(2d, loadedFactor.getConstantFactor(), 1e-12);

    final StandardCompoundNormalizationFunction loadedStandard = assertInstanceOf(
        StandardCompoundNormalizationFunction.class, loadedFunctions.get(1));
    assertEquals("standard_file", loadedStandard.rawDataFilePlaceholder().getName());
    assertEquals(standardTimestamp, loadedStandard.acquisitionTimestamp());
    assertEquals(StandardUsageType.Nearest, loadedStandard.usageType());
    assertEquals(1, loadedStandard.referencePoints().size());
    assertEquals(0.5d, loadedStandard.getFactor(100d, 5f), 1e-12);

    final InterpolatedNormalizationFunction loadedInterpolated = assertInstanceOf(
        InterpolatedNormalizationFunction.class, loadedFunctions.get(2));
    assertEquals("target_file", loadedInterpolated.rawDataFilePlaceholder().getName());
    assertEquals(interpolatedTimestamp, loadedInterpolated.acquisitionTimestamp());
    assertEquals(0.25d, loadedInterpolated.getPreviousWeight(), 1e-12);
    assertEquals(0.75d, loadedInterpolated.getNextWeight(), 1e-12);
    assertEquals(0.875d, loadedInterpolated.getFactor(100d, 5f), 1e-12);
  }

  @Test
  void hiddenParameterInNormalizerParameterSetPersistsFunctions() throws Exception {
    final IntensityNormalizerParameters parameters = new IntensityNormalizerParameters();
    final LocalDateTime timestamp = LocalDateTime.of(2026, 1, 2, 10, 0);
    final List<NormalizationFunction> functions = List.of(new FactorNormalizationFunction(
        new RawDataFilePlaceholder("file_a", tempDir.resolve("a.mzML").toString(), 42), timestamp,
        1.25d));
    parameters.setParameter(IntensityNormalizerParameters.normalizationFunctions, functions);

    final String xml = ParameterUtils.saveValuesToXMLString(parameters);
    final IntensityNormalizerParameters loaded = new IntensityNormalizerParameters();
    ParameterUtils.loadValuesFromXMLString(loaded, xml);

    final List<NormalizationFunction> loadedFunctions = loaded.getValue(
        IntensityNormalizerParameters.normalizationFunctions);
    assertEquals(1, loadedFunctions.size());
    final FactorNormalizationFunction loadedFactor = assertInstanceOf(
        FactorNormalizationFunction.class, loadedFunctions.getFirst());
    assertEquals("file_a", loadedFactor.rawDataFilePlaceholder().getName());
    assertEquals(timestamp, loadedFactor.acquisitionTimestamp());
    assertEquals(1.25d, loadedFactor.getConstantFactor(), 1e-12);
  }

  @Test
  void extractionUsesLatestIntensityNormalizerAppliedMethod() {
    final RawDataFile rawDataFile = RawDataFile.createDummyFile();
    final ModularFeatureList featureList = new ModularFeatureList("flist", null, rawDataFile);

    final IntensityNormalizerParameters olderParameters = new IntensityNormalizerParameters();
    olderParameters.setParameter(IntensityNormalizerParameters.normalizationFunctions, List.of(
        new FactorNormalizationFunction(new RawDataFilePlaceholder(rawDataFile),
            LocalDateTime.of(2026, 1, 1, 9, 0), 2d)));
    featureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(IntensityNormalizerModule.class, olderParameters,
            Instant.parse("2026-01-01T09:30:00Z")));

    final IntensityNormalizerParameters latestParameters = new IntensityNormalizerParameters();
    latestParameters.setParameter(IntensityNormalizerParameters.normalizationFunctions, List.of(
        new FactorNormalizationFunction(new RawDataFilePlaceholder(rawDataFile),
            LocalDateTime.of(2026, 1, 1, 11, 0), 3d)));
    featureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(IntensityNormalizerModule.class, latestParameters,
            Instant.parse("2026-01-01T11:30:00Z")));

    final List<NormalizationFunction> latestFunctions = IntensityNormalizerModule.getNormalizationFunctionsOfLatestCall(
        featureList);
    assertEquals(1, latestFunctions.size());
    assertEquals(3d, latestFunctions.getFirst().getFactor(100d, 5f), 1e-12);

    final NormalizationFunction latestForFile = IntensityNormalizerModule.getNormalizationFunctionOfLatestCallForFile(
        featureList, rawDataFile);
    assertNotNull(latestForFile);
    assertEquals(3d, latestForFile.getFactor(100d, 5f), 1e-12);

    final NormalizationFunction missingFile = IntensityNormalizerModule.getNormalizationFunctionOfLatestCallForFile(
        featureList,
        new RawDataFilePlaceholder("unknown", tempDir.resolve("unknown.mzML").toString(), 999));
    assertNull(missingFile);
  }
}
