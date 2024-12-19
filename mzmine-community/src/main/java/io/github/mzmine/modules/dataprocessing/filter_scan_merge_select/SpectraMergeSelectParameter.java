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

package io.github.mzmine.modules.dataprocessing.filter_scan_merge_select;

import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.MergedSpectraFinalSelectionTypes;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.SpectraMergeSelectModuleOptions;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.SpectraMergeSelectPresets;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.SpectraMergeSelectPresetsParameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.ArrayUtils;
import io.github.mzmine.util.scans.FragmentScanSelection;
import io.github.mzmine.util.scans.merging.SpectraMerger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This parameter controls the merging and selection of fragment scans through the
 * {@link FragmentScanSelection} and {@link SpectraMerger}.
 * <p>
 * Either setup through presets: {@link PresetSimpleSpectraMergeSelectParameters}
 * {@link PresetAdvancedSpectraMergeSelectParameters}
 * <p>
 * Or select source scans without merging: {@link SourceSpectraSelectParameters}
 * <p>
 * Or advanced parameters for full control in {@link AdvancedSpectraMergeSelectParameters}
 */
public class SpectraMergeSelectParameter extends
    ModuleOptionsEnumComboParameter<SpectraMergeSelectModuleOptions> {

  public static final String DEFAULT_NAME = "Merge & select fragment scans";
  public static final String DEFAULT_DESCRIPTION = "Setup spectral merging and the selection of the final list of fragment scans. There are options for simple preset based setup or the advanced options.";

  public SpectraMergeSelectParameter() {
    this(SpectraMergeSelectModuleOptions.SIMPLE_MERGED);
  }

  public SpectraMergeSelectParameter(final @NotNull SpectraMergeSelectModuleOptions defaultValue) {
    this(SpectraMergeSelectModuleOptions.values(), defaultValue);
  }

  public SpectraMergeSelectParameter(final @NotNull SpectraMergeSelectModuleOptions[] options,
      final @NotNull SpectraMergeSelectModuleOptions defaultValue) {
    this(DEFAULT_NAME, DEFAULT_DESCRIPTION, options, defaultValue);
  }

  public SpectraMergeSelectParameter(final String name, final String description,
      final SpectraMergeSelectModuleOptions[] options,
      final SpectraMergeSelectModuleOptions active) {
    super(name, description, options, active);
  }


  public SpectraMergeSelectParameter(final SpectraMergeSelectPresets simplePreset) {
    this(SpectraMergeSelectModuleOptions.SIMPLE_MERGED);
    getEmbeddedParameters().setParameter(PresetSimpleSpectraMergeSelectParameters.preset,
        simplePreset);
  }

  public static SpectraMergeSelectParameter createSpectraLibrarySearchDefaultNoMSn() {
    return new Builder().createParameters();
  }

  public static SpectraMergeSelectParameter createSiriusExportAllDefault() {
    return new Builder().includeAdvanced(true).useExportAllSourceScans().createParameters();
  }

  /**
   * Useful methods to set specific simple presets in the wizard or other locations
   */
  public void setSimplePreset(final SpectraMergeSelectPresets preset,
      final MZTolerance mzTolScans) {
    setValue(SpectraMergeSelectModuleOptions.SIMPLE_MERGED);
    var embedded = getEmbeddedParameters();
    embedded.setParameter(PresetSimpleSpectraMergeSelectParameters.preset, preset);
    embedded.setParameter(PresetSimpleSpectraMergeSelectParameters.mergeMzTolerance, mzTolScans);
  }

  /**
   * Set all parameters so that source scans are used - either best or all scans.
   *
   * @param useAllScansOrBest true use all scans false use best
   */
  public void setUseSourceScans(final boolean useAllScansOrBest) {
    setValue(SpectraMergeSelectModuleOptions.SOURCE_SCANS);
    var embedded = getEmbeddedParameters();
    var selection = useAllScansOrBest ? MergedSpectraFinalSelectionTypes.ALL_SOURCE_SCANS
        : MergedSpectraFinalSelectionTypes.SINGLE_MOST_INTENSE_SOURCE_SCAN;
    embedded.setParameter(SourceSpectraSelectParameters.sourceSelectionTypes, selection);
  }

  /**
   * Used in clone paramer
   */
  private SpectraMergeSelectParameter(final String name, final String description,
      final SpectraMergeSelectModuleOptions selectedValue,
      final EnumMap<SpectraMergeSelectModuleOptions, ParameterSet> parameters) {
    super(name, description, selectedValue, parameters);
  }

  @Override
  public SpectraMergeSelectParameter cloneParameter() {
    EnumMap<SpectraMergeSelectModuleOptions, ParameterSet> copy = new EnumMap<>(parametersMap);
    for (final SpectraMergeSelectModuleOptions key : copy.keySet()) {
      var cloneParam = copy.get(key).cloneParameterSet();
      copy.put(key, cloneParam);
    }
    return new SpectraMergeSelectParameter(getName(), getDescription(), getValue(), copy);
  }

  /**
   * This builder comes with some defaults often used but allows configuration of this parameter for
   * different modules. Checkout the factory methods
   * {@link SpectraMergeSelectParameter#createSiriusExportAllDefault()} and others.
   */
  public static final class Builder {

    private String name = DEFAULT_NAME;
    private String description = DEFAULT_DESCRIPTION;
    private SpectraMergeSelectModuleOptions @NotNull [] options = SpectraMergeSelectModuleOptions.defaultValuesNoAdvanced();
    private @NotNull SpectraMergeSelectModuleOptions active = SpectraMergeSelectModuleOptions.SIMPLE_MERGED;
    private SpectraMergeSelectPresets @NotNull [] presetOptions = SpectraMergeSelectPresets.defaultNoMSnTrees();
    private @Nullable SpectraMergeSelectPresets preset = SpectraMergeSelectPresets.REPRESENTATIVE_SCANS;
    private boolean includeAllSourceScans = false;
    private boolean useExportAllSourceScans = false;
    // use optional to define that options should not be filtered but used as is
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private @NotNull Optional<Boolean> includeAdvanced = Optional.of(false);

    public SpectraMergeSelectParameter createParameters() {
      if (useExportAllSourceScans) {
        useExportAllSourceScans(); // make sure to set all related fields
      }

      includeAdvanced.ifPresent(advanced -> {
        var alreadyContains = ArrayUtils.contains(SpectraMergeSelectModuleOptions.ADVANCED,
            options);
        if (advanced && !alreadyContains) {
          // add
          options = Arrays.copyOf(options, options.length + 1);
          options[options.length - 1] = SpectraMergeSelectModuleOptions.ADVANCED;
        }
        if (!advanced && alreadyContains) {
          // remove
          options = Arrays.stream(options)
              .filter(o -> o != SpectraMergeSelectModuleOptions.ADVANCED)
              .toArray(SpectraMergeSelectModuleOptions[]::new);
        }
      });

      var param = new SpectraMergeSelectParameter(name, description, options, active);

      // limit choices of presets and set initial value
      // multiple module options use presets (Simple and AdvancedPreset)
      for (final SpectraMergeSelectModuleOptions option : options) {
        var embedded = param.getEmbeddedParameters(option);
        embedded.streamForClass(SpectraMergeSelectPresetsParameter.class)
            .forEach(preParam -> preParam.setChoices(presetOptions, preset));
      }

      // limit choice of all source scans - the default parameter shows this option so we only need to remove it
      var embedded = param.getEmbeddedParameters(SpectraMergeSelectModuleOptions.SOURCE_SCANS);
      if (embedded != null) {
        if (!includeAllSourceScans) {
          var singleBest = MergedSpectraFinalSelectionTypes.SINGLE_MOST_INTENSE_SOURCE_SCAN;
          embedded.getParameter(SourceSpectraSelectParameters.sourceSelectionTypes)
              .setChoices(new MergedSpectraFinalSelectionTypes[]{singleBest}, singleBest);
        } else if (useExportAllSourceScans) {
          embedded.getParameter(SourceSpectraSelectParameters.sourceSelectionTypes).setChoices(
              new MergedSpectraFinalSelectionTypes[]{
                  MergedSpectraFinalSelectionTypes.SINGLE_MOST_INTENSE_SOURCE_SCAN,
                  MergedSpectraFinalSelectionTypes.ALL_SOURCE_SCANS},
              MergedSpectraFinalSelectionTypes.ALL_SOURCE_SCANS);
        }
      }

      return param;
    }

    public Builder allPresets() {
      presetOptions = SpectraMergeSelectPresets.values();
      return this;
    }

    public Builder addMSnTreePreset() {
      return addPresets(SpectraMergeSelectPresets.MSn_TREE);
    }

    public Builder addPresets(final SpectraMergeSelectPresets... presets) {
      presetOptions = Stream.concat(Arrays.stream(presetOptions), Arrays.stream(presets)).distinct()
          .sorted(Comparator.naturalOrder()).toArray(SpectraMergeSelectPresets[]::new);
      return this;
    }

    public Builder removePresets(final SpectraMergeSelectPresets... presets) {
      var remove = Set.of(presets);
      presetOptions = Arrays.stream(presetOptions).filter(Predicate.not(remove::contains))
          .toArray(SpectraMergeSelectPresets[]::new);
      return this;
    }

    public Builder name(final @NotNull String name) {
      this.name = name;
      return this;
    }

    public Builder description(final @NotNull String description) {
      this.description = description;
      return this;
    }

    public Builder options(final SpectraMergeSelectModuleOptions @NotNull [] options) {
      this.options = options;
      return this;
    }

    public Builder active(final @NotNull SpectraMergeSelectModuleOptions active) {
      this.active = active;
      return this;
    }

    public Builder presetOptions(final SpectraMergeSelectPresets @NotNull [] presetOptions) {
      this.presetOptions = presetOptions;
      return this;
    }

    public Builder preset(final @Nullable SpectraMergeSelectPresets preset) {
      this.preset = preset;
      return this;
    }

    public Builder includeAllSourceScans(final boolean includeAllSourceScans) {
      this.includeAllSourceScans = includeAllSourceScans;
      return this;
    }

    public Builder useExportAllSourceScans() {
      active(SpectraMergeSelectModuleOptions.SOURCE_SCANS);
      includeAllSourceScans(true);
      this.useExportAllSourceScans = true;
      return this;
    }

    public Builder includeAdvanced(final @Nullable Boolean includeAdvanced) {
      this.includeAdvanced = Optional.ofNullable(includeAdvanced);
      return this;
    }
  }
}
