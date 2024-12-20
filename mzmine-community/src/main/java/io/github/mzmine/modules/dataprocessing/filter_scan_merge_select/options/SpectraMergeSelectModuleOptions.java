/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options;

import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.AdvancedSpectraMergeSelectModule;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.AdvancedSpectraMergeSelectParameters;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.InputSpectraSelectModule;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.InputSpectraSelectParameters;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.PresetAdvancedSpectraMergeSelectModule;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.PresetAdvancedSpectraMergeSelectParameters;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.PresetSimpleSpectraMergeSelectModule;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.PresetSimpleSpectraMergeSelectParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnum;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.FragmentScanSelection;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import io.github.mzmine.util.scans.merging.SpectraMerger;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Module options for merging and scan selection - each module comes with their own parameter set
 */
public enum SpectraMergeSelectModuleOptions implements ModuleOptionsEnum<MZmineModule> {

  SIMPLE_MERGED, PRESET_MERGED, SOURCE_SCANS, ADVANCED;

  public static SpectraMergeSelectModuleOptions[] defaultValuesNoAdvanced() {
    return new SpectraMergeSelectModuleOptions[]{SIMPLE_MERGED, PRESET_MERGED, SOURCE_SCANS};
  }


  @Override
  public Class<? extends MZmineModule> getModuleClass() {
    return switch (this) {
      case SIMPLE_MERGED -> PresetSimpleSpectraMergeSelectModule.class;
      case PRESET_MERGED -> PresetAdvancedSpectraMergeSelectModule.class;
      case SOURCE_SCANS -> InputSpectraSelectModule.class;
      case ADVANCED -> AdvancedSpectraMergeSelectModule.class;
    };
  }

  @Override
  public String toString() {
    return switch (this) {
      case SIMPLE_MERGED -> "Merged (simple)";
      case PRESET_MERGED -> "Merged (preset)";
      case SOURCE_SCANS -> "Source scans";
      case ADVANCED -> "Advanced";
    };
  }

  public boolean usesPreset() {
    return switch (this) {
      case SIMPLE_MERGED, PRESET_MERGED -> true;
      case SOURCE_SCANS, ADVANCED -> false;
    };
  }

  @Override
  public String getStableId() {
    return switch (this) {
      case SIMPLE_MERGED -> "simple_merged";
      case PRESET_MERGED -> "preset_merged";
      case SOURCE_SCANS -> "source_scans";
      case ADVANCED -> "advanced";
    };
  }

  /**
   * Creates the merger and filters needed in fragment scan selection
   *
   * @return a fragment scan selection either merging scans or just selecting source scans
   */
  @NotNull
  public FragmentScanSelection createFragmentScanSelection(final @Nullable MemoryMapStorage storage,
      @NotNull ParameterSet params) {
    params = createAdvancedSpectraMergeSelectParameters(params);

    List<MergedSpectraFinalSelectionTypes> finalScanSelection = params.getValue(
        AdvancedSpectraMergeSelectParameters.finalScanSelection);

    @Nullable SpectraMerger merger;
    if (this == SOURCE_SCANS) {
      merger = null;
    } else {
      // merging active
      var mzTol = params.getValue(AdvancedSpectraMergeSelectParameters.mergeMzTolerance);
      var intensityMergeType = params.getValue(
          AdvancedSpectraMergeSelectParameters.intensityMergeType);

      merger = new SpectraMerger(finalScanSelection, mzTol, intensityMergeType);
      merger.setStorage(storage);
    }

    return new FragmentScanSelection(storage, merger, finalScanSelection);
  }

  /**
   * translate into advanced parameters for to build selection and merger
   */
  @NotNull
  public ParameterSet createAdvancedSpectraMergeSelectParameters(
      final @NotNull ParameterSet params) {
    return switch (this) {
      case SIMPLE_MERGED -> {
        var preset = params.getValue(PresetSimpleSpectraMergeSelectParameters.preset);
        var mzTol = params.getValue(PresetSimpleSpectraMergeSelectParameters.mergeMzTolerance);
        yield AdvancedSpectraMergeSelectParameters.createParams(preset.listIncludedScanTypes(),
            mzTol, IntensityMergingType.MAXIMUM);
      }
      case PRESET_MERGED -> {
        var preset = params.getValue(PresetAdvancedSpectraMergeSelectParameters.preset);
        var mzTol = params.getValue(PresetAdvancedSpectraMergeSelectParameters.mergeMzTolerance);
        var intensityMergingType = params.getValue(
            PresetAdvancedSpectraMergeSelectParameters.intensityMergeType);
        yield AdvancedSpectraMergeSelectParameters.createParams(preset.listIncludedScanTypes(),
            mzTol, intensityMergingType);
      }
      case SOURCE_SCANS -> {
        MergedSpectraFinalSelectionTypes value = params.getValue(
            InputSpectraSelectParameters.sourceSelectionTypes).toFinalSelectionTypes();
        yield AdvancedSpectraMergeSelectParameters.createSourceScanParams(
            List.of(MergedSpectraFinalSelectionTypes.ACROSS_SAMPLES, value));
      }
      case ADVANCED -> params;
    };
  }
}
