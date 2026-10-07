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

import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.addRow;
import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.createFeatureIntensityParametersAllSamples;
import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.createMainParameters;
import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.createRawFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AverageIntensityNormalizationTypeModuleTest {

  @Test
  void createReferenceFunctionsUsesAverageFeatureIntensity() {
    final FeatureIntensityNormalizationModule module = new FeatureIntensityNormalizationModule();
    final RawDataFileImpl fileA = createRawFile("file_a", LocalDateTime.of(2026, 1, 1, 10, 0));
    final RawDataFileImpl fileB = createRawFile("file_b", LocalDateTime.of(2026, 1, 1, 10, 5));

    final ModularFeatureList featureList = new ModularFeatureList("flist", null, fileA, fileB);
    addRow(featureList, 1, fileA, 2f, 2f, fileB, 1f, 1f);
    addRow(featureList, 2, fileA, 4f, 4f, fileB, 1f, 1f);

    final IntensityNormalizationSearchableSummary summary = new IntensityNormalizationSearchableSummary(
        featureList.getNumberOfRawDataFiles());
    module.createAllNormalizationFunctionsToSummary(summary, featureList,
        new SamplesBatch(featureList.getRawDataFiles(), null), new MetadataTable(false),
        createMainParameters(AbundanceMeasure.Height),
        createFeatureIntensityParametersAllSamples(FeatureIntensityNormalizationMode.AVERAGE));

    final FactorNormalizationFunction functionA = assertInstanceOf(
        FactorNormalizationFunction.class, summary.get(fileA));
    final FactorNormalizationFunction functionB = assertInstanceOf(
        FactorNormalizationFunction.class, summary.get(fileB));

    // Average(file_a)=3 and Average(file_b)=1 => median=2.
    assertEquals(2 / 3d, functionA.getNormalizationFactor(0d, 0f), 1e-12);
    assertEquals(2d, functionB.getNormalizationFactor(0d, 0f), 1e-12);
  }

  @Test
  void createReferenceFunctionsUsesSelectedFeatureMeasurementType() {
    final FeatureIntensityNormalizationModule module = new FeatureIntensityNormalizationModule();
    final RawDataFileImpl fileA = createRawFile("file_a", LocalDateTime.of(2026, 1, 1, 10, 0));
    final RawDataFileImpl fileB = createRawFile("file_b", LocalDateTime.of(2026, 1, 1, 10, 5));

    final ModularFeatureList featureList = new ModularFeatureList("flist", null, fileA, fileB);
    addRow(featureList, 1, fileA, 1f, 20f, fileB, 1f, 10f);
    addRow(featureList, 2, fileA, 1f, 10f, fileB, 1f, 10f);

    final IntensityNormalizationSearchableSummary summary = new IntensityNormalizationSearchableSummary(
        featureList.getNumberOfRawDataFiles());
    final Map<RawDataFile, NormalizationFunction> functions = module.createReferenceFunctions(
        summary, List.of(fileA, fileB), featureList,
        new SamplesBatch(featureList.getRawDataFiles(), null), new MetadataTable(false),
        createMainParameters(AbundanceMeasure.Area),
        createFeatureIntensityParametersAllSamples(FeatureIntensityNormalizationMode.AVERAGE));

    final FactorNormalizationFunction functionA = assertInstanceOf(
        FactorNormalizationFunction.class, functions.get(fileA));
    final FactorNormalizationFunction functionB = assertInstanceOf(
        FactorNormalizationFunction.class, functions.get(fileB));

    // Average area(file_a)=15 and Average area(file_b)=10 => median=12.5.
    assertEquals(12.5 / 15d, functionA.getNormalizationFactor(0d, 0f), 1e-12);
    assertEquals(12.5 / 10d, functionB.getNormalizationFactor(0d, 0f), 1e-12);
  }

  @Test
  void createInterpolatedFunctionInterpolatesFactors() {
    final FeatureIntensityNormalizationModule module = new FeatureIntensityNormalizationModule();
    final RawDataFileImpl prevFile = createRawFile("prev", LocalDateTime.of(2026, 1, 1, 10, 0));
    final RawDataFileImpl nextFile = createRawFile("next", LocalDateTime.of(2026, 1, 1, 10, 8));
    final RawDataFileImpl targetFile = createRawFile("target", LocalDateTime.of(2026, 1, 1, 10, 6));
    final ModularFeatureList featureList = new ModularFeatureList("flist", null, prevFile,
        targetFile, nextFile);

    final FactorNormalizationFunction prevFunction = new FactorNormalizationFunction(2d);
    final FactorNormalizationFunction nextFunction = new FactorNormalizationFunction(4d);

    final IntensityNormalizationSearchableSummary summary = new IntensityNormalizationSearchableSummary(
        featureList.getNumberOfRawDataFiles());
    summary.addMergeFunction(prevFile, prevFunction);
    summary.addMergeFunction(nextFile, nextFunction);

    // internally we never use summary.functions for interpolation because
    // interpolation should be based on a single steps functions not on the merged composite function
    Map<RawDataFile, NormalizationFunction> functions = Map.of(prevFile, prevFunction, nextFile,
        nextFunction);

    // apply interpolation in module
    module.interpolateAllFunctionsToSummary(summary, featureList,
        new SamplesBatch(featureList.getRawDataFiles()), new MetadataTable(false), functions,
        createMainParameters(AbundanceMeasure.Height),
        createFeatureIntensityParametersAllSamples(FeatureIntensityNormalizationMode.AVERAGE));

    // will check if interpolation is needed to then interpolate functions and save them to summary
    // should be the same as this
//    NormalizationFunctionUtils.interpolateLinearBinary(summary,
//        new SamplesBatch(List.of(prevFile, nextFile, targetFile)), summary.functions(),
//        new MetadataTable(false));

    final FactorNormalizationFunction interpolated = assertInstanceOf(
        FactorNormalizationFunction.class, summary.get(targetFile));
    assertNotNull(interpolated);
    // factor = nextFactor*nextRunWeight + prevFactor*previousWeight = 4*0.75 + 2*0.25 = 3.5
    assertEquals(3.5d, interpolated.getNormalizationFactor(0d, 0f), 1e-12);
  }
}
