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
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.DoubleMetadataColumn;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class MetadataColumnNormalizationTypeModuleTest {

  @Test
  void createReferenceFunctionsUsesMetadataValues() {
    final MetadataColumnNormalizationTypeModule module = new MetadataColumnNormalizationTypeModule();
    final RawDataFileImpl fileA = createRawFile("file_a", LocalDateTime.of(2026, 1, 1, 10, 0));
    final RawDataFileImpl fileB = createRawFile("file_b", LocalDateTime.of(2026, 1, 1, 10, 5));
    final RawDataFileImpl fileC = createRawFile("file_c", LocalDateTime.of(2026, 1, 1, 10, 10));

    final ModularFeatureList featureList = new ModularFeatureList("flist", null, fileA, fileB,
        fileC);
    final MetadataTable metadata = new MetadataTable(false);
    final DoubleMetadataColumn concentrationColumn = new DoubleMetadataColumn("concentration");
    metadata.setValue(concentrationColumn, fileA, 10d);
    metadata.setValue(concentrationColumn, fileB, 5d);
    metadata.setValue(concentrationColumn, fileC, 2d);

    final MetadataColumnNormalizationTypeParameters moduleParameters = new MetadataColumnNormalizationTypeParameters();
    moduleParameters.setParameter(MetadataColumnNormalizationTypeParameters.metadataColumn,
        concentrationColumn.getTitle());

    final List<RawDataFile> referenceFiles = module.getReferenceSamples(featureList,
        moduleParameters);
    final Map<RawDataFile, NormalizationFunction> functions = module.createReferenceFunctions(
        referenceFiles, featureList, metadata, new IntensityNormalizerParameters(),
        moduleParameters);

    final FactorNormalizationFunction functionA = assertInstanceOf(
        FactorNormalizationFunction.class, functions.get(fileA));
    final FactorNormalizationFunction functionB = assertInstanceOf(
        FactorNormalizationFunction.class, functions.get(fileB));
    final FactorNormalizationFunction functionC = assertInstanceOf(
        FactorNormalizationFunction.class, functions.get(fileC));

    assertEquals(3, functions.size());
    assertEquals(1d, functionA.getNormalizationFactor(0d, 0f), 1e-12);
    assertEquals(2d, functionB.getNormalizationFactor(0d, 0f), 1e-12);
    assertEquals(5d, functionC.getNormalizationFactor(0d, 0f), 1e-12);
  }

  @Test
  void createReferenceFunctionsThrowsOnMissingMetadataValue() {
    final MetadataColumnNormalizationTypeModule module = new MetadataColumnNormalizationTypeModule();
    final RawDataFileImpl fileA = createRawFile("file_a", LocalDateTime.of(2026, 1, 1, 10, 0));
    final RawDataFileImpl fileB = createRawFile("file_b", LocalDateTime.of(2026, 1, 1, 10, 5));

    final ModularFeatureList featureList = new ModularFeatureList("flist", null, fileA, fileB);
    final MetadataTable metadata = new MetadataTable(false);
    final DoubleMetadataColumn concentrationColumn = new DoubleMetadataColumn("concentration");
    metadata.setValue(concentrationColumn, fileA, 10d);

    final MetadataColumnNormalizationTypeParameters moduleParameters = new MetadataColumnNormalizationTypeParameters();
    moduleParameters.setParameter(MetadataColumnNormalizationTypeParameters.metadataColumn,
        concentrationColumn.getTitle());

    final IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> module.createReferenceFunctions(List.of(fileA, fileB), featureList, metadata,
            new IntensityNormalizerParameters(), moduleParameters));

    assertEquals("Invalid metadata value in column 'concentration' for file 'file_b': null",
        exception.getMessage());
  }

  @Test
  void createReferenceFunctionsThrowsOnNegativeMetadataValue() {
    final MetadataColumnNormalizationTypeModule module = new MetadataColumnNormalizationTypeModule();
    final RawDataFileImpl fileA = createRawFile("file_a", LocalDateTime.of(2026, 1, 1, 10, 0));
    final RawDataFileImpl fileB = createRawFile("file_b", LocalDateTime.of(2026, 1, 1, 10, 5));

    final ModularFeatureList featureList = new ModularFeatureList("flist", null, fileA, fileB);
    final MetadataTable metadata = new MetadataTable(false);
    final DoubleMetadataColumn concentrationColumn = new DoubleMetadataColumn("concentration");
    metadata.setValue(concentrationColumn, fileA, 10d);
    metadata.setValue(concentrationColumn, fileB, -1d);

    final MetadataColumnNormalizationTypeParameters moduleParameters = new MetadataColumnNormalizationTypeParameters();
    moduleParameters.setParameter(MetadataColumnNormalizationTypeParameters.metadataColumn,
        concentrationColumn.getTitle());

    final IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> module.createReferenceFunctions(List.of(fileA, fileB), featureList, metadata,
            new IntensityNormalizerParameters(), moduleParameters));

    assertEquals("Invalid metadata value in column 'concentration' for file 'file_b': -1.0",
        exception.getMessage());
  }

  private static @NotNull RawDataFileImpl createRawFile(final @NotNull String name,
      final @NotNull LocalDateTime timestamp) {
    final RawDataFileImpl rawDataFile = new RawDataFileImpl(name, null, null);
    rawDataFile.setStartTimeStamp(timestamp);
    return rawDataFile;
  }
}
