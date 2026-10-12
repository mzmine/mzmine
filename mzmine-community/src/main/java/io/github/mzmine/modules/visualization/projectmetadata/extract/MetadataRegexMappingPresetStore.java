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

import static io.github.mzmine.modules.visualization.projectmetadata.extract.MetadataRegexMappingPreset.createMappingPreset;

import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.util.presets.AbstractJsonPresetStore;
import io.github.mzmine.util.presets.FxPresetEditor;
import io.github.mzmine.util.presets.KnownPresetGroup;
import io.github.mzmine.util.presets.PresetCategory;
import io.github.mzmine.util.presets.PresetGroup;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stores {@link MetadataRegexMappingPreset}s - single ready-made mapping rows that are added to the
 * {@link MetadataRegexExtractionComponent} grid by its presets button.
 */
public class MetadataRegexMappingPresetStore extends
    AbstractJsonPresetStore<MetadataRegexMappingPreset> {

  public static final String SAMPLE_TYPE_PRESET_NAME = "More sample types";

  @Override
  public @NotNull PresetCategory getPresetCategory() {
    return PresetCategory.OTHER;
  }

  @Override
  public @NotNull PresetGroup getPresetGroup() {
    return KnownPresetGroup.METADATA_REGEX_MAPPING_PRESET;
  }

  @Override
  public @NotNull List<MetadataRegexMappingPreset> createDefaults() {
    return List.of( //
        MetadataRegexMappingPreset.createExactValuePreset("Extract number", "c",
            ExtractColumnType.NUMBER, "_([0-9]+[.]?[0-9]*)[_.]"), //
        MetadataRegexMappingPreset.createMapOneValuePreset("Pooled QC",
            MetadataColumn.SAMPLE_TYPE_HEADER, ExtractColumnType.TEXT,
            ".*(?<![a-zA-Z])(pool(?:ed)?(?:[_-]?qc)?)(?![a-zA-Z]).*", "pooled_qc"), //
        // showcases the value mappings and the remaining-values fallback: everything that is not
        // recognized as blank, QC, ... becomes a regular sample
        createMappingPreset(SAMPLE_TYPE_PRESET_NAME, MetadataColumn.SAMPLE_TYPE_HEADER,
            ExtractColumnType.TEXT,
            ".*(?<![a-zA-Z])(media(?:[_-]?blank)?|blank|blk|pool(?:ed)?(?:[_-]?qc)?|(?<!pooled[_-]?)qc|sst|cal|calibration)(?![a-zA-Z]).*",
            DropUnmappedMode.MAP_UNMAPPED, "sample", "sample", List.of(
                // media
                new MetadataValueMapping("media", "media"),
                new MetadataValueMapping("mediablank", "media"),
                new MetadataValueMapping("media_blank", "media"),
                new MetadataValueMapping("media-blank", "media"),
                // blanks
                new MetadataValueMapping("blank", "blank"),
                new MetadataValueMapping("blk", "blank"),
                // pooled_qc
                new MetadataValueMapping("pooled", "pooled_qc"),
                new MetadataValueMapping("pooledqc", "pooled_qc"),
                new MetadataValueMapping("pooled_qc", "pooled_qc"),
                new MetadataValueMapping("pooled-qc", "pooled_qc"),
                // sst
                new MetadataValueMapping("sst", "sst"),
                // calibration
                new MetadataValueMapping("cal", "calibration"),
                new MetadataValueMapping("calibration", "calibration"),
                // qc
                new MetadataValueMapping("qc", "qc"))) //
    );
  }

  @Override
  public @Nullable FxPresetEditor createPresetEditor() {
    // mappings are edited in the extraction dialog itself, not in the manage-presets tab
    return null;
  }
}
