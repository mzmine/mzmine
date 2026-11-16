package io.github.mzmine.modules.tools.batch_resource_estimation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResourceSubsetSelectorTest {

  private static List<EstimationInputFile> files(final SampleType type, final int count) {
    final List<EstimationInputFile> files = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      files.add(new EstimationInputFile(new File(type + "_" + i + ".mzML"), type, (i + 1) * 100L));
    }
    return files;
  }

  @Test
  void prefixesFollowSampleTypeProportions() {
    final List<EstimationInputFile> all = new ArrayList<>();
    all.addAll(files(SampleType.QC, 5));
    all.addAll(files(SampleType.SAMPLE, 20));
    all.addAll(files(SampleType.BLANK, 2));

    final List<EstimationInputFile> order = ResourceSubsetSelector.orderForSubsets(all);
    assertEquals(all.size(), order.stream().distinct().count());

    // 20:5:2 - the QC is not preferred, the first QC comes after four samples
    final List<SampleType> firstTen = order.subList(0, 10).stream()
        .map(EstimationInputFile::sampleType).toList();
    assertEquals(8, firstTen.stream().filter(t -> t == SampleType.SAMPLE).count());
    assertEquals(2, firstTen.stream().filter(t -> t == SampleType.QC).count());
    assertEquals(SampleType.SAMPLE, order.getFirst().sampleType());
  }

  @Test
  void spreadsFileSizes() {
    final List<EstimationInputFile> spread = ResourceSubsetSelector.spreadBySize(
        files(SampleType.SAMPLE, 7));
    // sizes 100..700: median first, then the medians of both halves
    assertEquals(List.of(400L, 200L, 600L, 100L, 300L, 500L, 700L),
        spread.stream().map(EstimationInputFile::sizeBytes).toList());
  }

  @Test
  void requiredFilesCoverLargestSubsetAndValidation() {
    assertEquals(32, ResourceSubsetSelector.requiredFiles(4, 3));
    assertEquals(16, ResourceSubsetSelector.requiredFiles(4, 2));
  }

  @Test
  void subsetSizesDoubleAndAreCapped() {
    assertEquals(List.of(4, 8, 16), ResourceSubsetSelector.subsetSizes(100, 4, 3));
    assertEquals(List.of(4, 8, 10), ResourceSubsetSelector.subsetSizes(10, 4, 3));
    assertEquals(List.of(3), ResourceSubsetSelector.subsetSizes(3, 4, 3));
  }
}
