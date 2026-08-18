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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.mzmine.util.io.JsonUtils;
import io.github.mzmine.util.presets.Preset;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MetadataRegexMappingPresetTest {

  private static MetadataRegexMappingPreset preset() {
    return new MetadataRegexMappingPreset("Blank or sample",
        new MetadataRegexMapping(RegexInputSource.FILE_NAME_WITHOUT_EXTENSION, "mzmine_sample_type",
            ExtractColumnType.TEXT, "_([A-Za-z]+)_", "unknown", DropUnmappedMode.MAP_UNMAPPED,
            "sample", List.of(new MetadataValueMapping("media", "blank"))));
  }

  @Test
  void jsonRoundTripKeepsMapping() throws JsonProcessingException {
    final MetadataRegexMappingPreset original = preset();
    // presets are written and read as the Preset supertype (see the JsonSubTypes on Preset)
    final String json = JsonUtils.MAPPER.writeValueAsString(original);
    final Preset loaded = JsonUtils.MAPPER.readValue(json, Preset.class);

    Assertions.assertInstanceOf(MetadataRegexMappingPreset.class, loaded);
    Assertions.assertEquals(original, loaded);
  }

  @Test
  void withNameKeepsContent() {
    final MetadataRegexMappingPreset renamed = preset().withName("Other name");
    Assertions.assertEquals("Other name", renamed.name());
    Assertions.assertTrue(renamed.equalsByContent(preset()));
  }

  @Test
  void defaultPresetsAreValidAndSerializable() throws JsonProcessingException {
    final List<MetadataRegexMappingPreset> defaults = new MetadataRegexMappingPresetStore().createDefaults();
    Assertions.assertFalse(defaults.isEmpty());

    for (final MetadataRegexMappingPreset preset : defaults) {
      final MetadataRegexMapping mapping = preset.mapping();
      Assertions.assertFalse(preset.name().isBlank());
      Assertions.assertFalse(mapping.columnName().isBlank());
      // every default must define a compilable regex
      Assertions.assertNotNull(SampleMetadataExtractionUtils.tryCompile(mapping.regex()),
          () -> "invalid regex in preset " + preset.name());

      final Preset loaded = JsonUtils.MAPPER.readValue(JsonUtils.MAPPER.writeValueAsString(preset),
          Preset.class);
      Assertions.assertEquals(preset, loaded);
    }
  }

  @Test
  void blankQcOrSampleDefaultMapsRemainingValues() {
    final MetadataRegexMapping mapping = new MetadataRegexMappingPresetStore().createDefaults()
        .stream().filter(p -> p.name().equals("Blank, QC, or sample")).findFirst().orElseThrow()
        .mapping();

    Assertions.assertEquals("blank",
        SampleMetadataExtractionUtils.extractValue(mapping, "20210610_Media_01.mzML"));
    Assertions.assertEquals("qc",
        SampleMetadataExtractionUtils.extractValue(mapping, "20210610_pooled_QC_01.mzML"));
    // "sample" is not in the mapping list, so the remaining-values fallback applies
    Assertions.assertEquals("sample",
        SampleMetadataExtractionUtils.extractValue(mapping, "20210610_sample_01.mzML"));
    // no match at all leaves the cell empty (no default value)
    Assertions.assertNull(
        SampleMetadataExtractionUtils.extractValue(mapping, "20210610_std_01.mzML"));
  }
}
