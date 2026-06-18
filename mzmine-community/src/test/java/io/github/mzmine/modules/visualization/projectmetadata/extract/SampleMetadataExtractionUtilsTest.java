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

package io.github.mzmine.modules.visualization.projectmetadata.extract;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.visualization.projectmetadata.extract.SampleMetadataExtractionUtils.GroupMatch;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SampleMetadataExtractionUtilsTest {

  private static MetadataRegexMapping mapping(final String regex, final String defaultValue,
      final boolean dropUnmapped, final List<MetadataValueMapping> valueMappings) {
    return new MetadataRegexMapping(RegexInputSource.FILE_NAME, "type", ExtractColumnType.AUTO,
        regex, defaultValue, dropUnmapped, valueMappings);
  }

  @Test
  void usesFirstCaptureGroup() {
    final MetadataRegexMapping m = mapping("_([A-Za-z]+)_", "", false, List.of());
    Assertions.assertEquals("QC",
        SampleMetadataExtractionUtils.extractValue(m, "20210610_QC_01.mzML"));
  }

  @Test
  void fallsBackToWholeMatchWithoutCapturingGroup() {
    final MetadataRegexMapping m = mapping("\\d{8}", "", false, List.of());
    Assertions.assertEquals("20210610",
        SampleMetadataExtractionUtils.extractValue(m, "20210610_QC_01.mzML"));
  }

  @Test
  void namedGroupIsExtracted() {
    final MetadataRegexMapping m = mapping("_(?<stype>[A-Za-z]+)_", "", false, List.of());
    Assertions.assertEquals("blank",
        SampleMetadataExtractionUtils.extractValue(m, "20210610_blank_01.mzML"));
  }

  @Test
  void regexMatchingIsCaseInsensitive() {
    // lowercase pattern matches the upper-case token in the file name
    final MetadataRegexMapping m = mapping("(qc)", "", false, List.of());
    Assertions.assertEquals("QC",
        SampleMetadataExtractionUtils.extractValue(m, "20210610_QC_01.mzML"));
  }

  @Test
  void valueMappingIsCaseInsensitive() {
    final MetadataRegexMapping m = mapping("_([A-Za-z]+)_", "", false,
        List.of(new MetadataValueMapping("media", "blank")));
    Assertions.assertEquals("blank",
        SampleMetadataExtractionUtils.extractValue(m, "20210610_Media_01.mzML"));
  }

  @Test
  void unmappedValuePassesThroughByDefault() {
    final MetadataRegexMapping m = mapping("_([A-Za-z]+)_", "", false,
        List.of(new MetadataValueMapping("media", "blank")));
    Assertions.assertEquals("QC",
        SampleMetadataExtractionUtils.extractValue(m, "20210610_QC_01.mzML"));
  }

  @Test
  void dropUnmappedReturnsNull() {
    final MetadataRegexMapping m = mapping("_([A-Za-z]+)_", "", true,
        List.of(new MetadataValueMapping("media", "blank")));
    Assertions.assertNull(SampleMetadataExtractionUtils.extractValue(m, "20210610_QC_01.mzML"));
  }

  @Test
  void defaultValueUsedWhenNoMatch() {
    final MetadataRegexMapping withDefault = mapping("QC_(\\d+)", "unknown", false, List.of());
    Assertions.assertEquals("unknown",
        SampleMetadataExtractionUtils.extractValue(withDefault, "20210610_blank_01.mzML"));

    final MetadataRegexMapping noDefault = mapping("QC_(\\d+)", "", false, List.of());
    Assertions.assertNull(
        SampleMetadataExtractionUtils.extractValue(noDefault, "20210610_blank_01.mzML"));
  }

  @Test
  void invalidRegexReturnsNoMatch() {
    final MetadataRegexMapping m = mapping("([unclosed", "", false, List.of());
    Assertions.assertNull(SampleMetadataExtractionUtils.firstGroupMatch(m, "anything"));
  }

  @Test
  void groupMatchReportsHighlightRange() {
    final MetadataRegexMapping m = mapping("_([A-Za-z]+)_", "", false, List.of());
    final String input = "20210610_QC_01.mzML";
    final GroupMatch match = SampleMetadataExtractionUtils.firstGroupMatch(m, input);
    Assertions.assertNotNull(match);
    Assertions.assertEquals("QC", match.value());
    Assertions.assertEquals("QC", input.substring(match.start(), match.end()));
  }

  @Test
  void distinctMatchedValuesDedupCaseInsensitively() {
    final MetadataRegexMapping m = mapping("_([A-Za-z]+)_", "", false, List.of());
    final List<RawDataFile> raws = List.of(new RawDataFilePlaceholder("x_QC_1.mzML", null),
        new RawDataFilePlaceholder("y_qc_2.mzML", null),
        new RawDataFilePlaceholder("z_blank_3.mzML", null));
    // "qc" dedupes with "QC" (case-insensitive), first-seen casing kept
    Assertions.assertEquals(List.of("QC", "blank"),
        SampleMetadataExtractionUtils.distinctMatchedValues(m, raws));
  }

  @Test
  void inputSourceExtraction() {
    final RawDataFilePlaceholder raw = new RawDataFilePlaceholder("sample_blank_01.mzML",
        "/data/run42/sample_blank_01.mzML");
    Assertions.assertEquals("sample_blank_01.mzML", RegexInputSource.FILE_NAME.extract(raw));
    Assertions.assertEquals("sample_blank_01",
        RegexInputSource.FILE_NAME_WITHOUT_EXTENSION.extract(raw));
    Assertions.assertEquals("/data/run42/sample_blank_01.mzML",
        RegexInputSource.ABSOLUTE_PATH.extract(raw));
    Assertions.assertEquals("run42", RegexInputSource.PARENT_FOLDER.extract(raw));
  }
}
