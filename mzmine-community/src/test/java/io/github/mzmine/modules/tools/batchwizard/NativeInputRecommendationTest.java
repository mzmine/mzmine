package io.github.mzmine.modules.tools.batchwizard;

import static org.junit.jupiter.api.Assertions.*;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.RawDataPreparation;
import java.io.File;
import java.util.Arrays;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class NativeInputRecommendationTest {
  @Test void reusesNativeQcRecommendationWithoutRestrictingInputArray() {
    final File[] dataset = IntStream.range(0, 25).mapToObj(i -> new File(
        (i < 5 ? "QC_" : "sample_") + i + ".mzML")).toArray(File[]::new);
    final File[] selected = RawDataPreparation.selectOptimizerInputFiles(dataset);
    assertEquals(5, selected.length);
    assertTrue(Arrays.stream(selected).allMatch(file -> file.getName().startsWith("QC_")));
    assertEquals(25, dataset.length);
  }
  @Test void emptyAndSmallDataSetsNeedNoFallbackLimits() {
    assertEquals(0, RawDataPreparation.selectOptimizerInputFiles(new File[0]).length);
    assertEquals(2, RawDataPreparation.selectOptimizerInputFiles(new File[]{new File("sample1"), new File("sample2")}).length);
  }
}
