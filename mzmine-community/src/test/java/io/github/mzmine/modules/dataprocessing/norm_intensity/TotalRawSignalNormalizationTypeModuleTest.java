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

import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.addScan;
import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.createFactorParameters;
import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.createMainParameters;
import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.createRawFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TotalRawSignalNormalizationTypeModuleTest {

  @Test
  void createReferenceFunctionsUsesMs1TicSum() {
    final TotalRawSignalNormalizationTypeModule module = new TotalRawSignalNormalizationTypeModule();
    final RawDataFileImpl fileA = createRawFile("file_a", LocalDateTime.of(2026, 1, 1, 10, 0));
    final RawDataFileImpl fileB = createRawFile("file_b", LocalDateTime.of(2026, 1, 1, 10, 5));

    addScan(fileA, 1, 1, 1f, new double[]{100d, 101d}, new double[]{10d, 20d});
    addScan(fileA, 2, 2, 2f, new double[]{100d}, new double[]{1000d});
    addScan(fileB, 1, 1, 1f, new double[]{100d, 101d}, new double[]{5d, 10d});

    final ModularFeatureList featureList = new ModularFeatureList("flist", null, fileA, fileB);
    final Map<RawDataFile, NormalizationFunction> functions = module.createReferenceFunctions(
        List.of(fileA, fileB), featureList, new MetadataTable(false),
        createMainParameters(AbundanceMeasure.Height), createFactorParameters());

    final FactorNormalizationFunction functionA = assertInstanceOf(
        FactorNormalizationFunction.class, functions.get(fileA));
    final FactorNormalizationFunction functionB = assertInstanceOf(
        FactorNormalizationFunction.class, functions.get(fileB));

    // TIC(file_a)=30 from MS1 scans only, TIC(file_b)=15 => maxMetric=30.
    assertEquals(1d, functionA.getNormalizationFactor(0d, 0f), 1e-12);
    assertEquals(2d, functionB.getNormalizationFactor(0d, 0f), 1e-12);
  }

  @Test
  void createReferenceFunctionsThrowsIfNoMs1TicFound() {
    final TotalRawSignalNormalizationTypeModule module = new TotalRawSignalNormalizationTypeModule();
    final RawDataFileImpl file = createRawFile("no_tic", LocalDateTime.of(2026, 1, 1, 10, 0));
    addScan(file, 1, 2, 1f, new double[]{100d}, new double[]{100d});

    final ModularFeatureList featureList = new ModularFeatureList("flist", null, file);

    final IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> module.createReferenceFunctions(List.of(file), featureList, new MetadataTable(false),
            createMainParameters(AbundanceMeasure.Height), createFactorParameters()));

    assertEquals("No TIC found for file: no_tic", exception.getMessage());
  }
}
