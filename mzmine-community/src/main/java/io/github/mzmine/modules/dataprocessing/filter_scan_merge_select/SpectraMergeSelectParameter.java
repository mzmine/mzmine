/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.InputSpectraSelectParameters.SelectInputScans;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.MergedSpectraFinalSelectionTypes;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.SpectraMergeSelectModuleOptions;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.SpectraMergeSelectPresets;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.SpectraMergeSelectPresetsParameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.ArrayUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.FragmentScanSelection;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import io.github.mzmine.util.scans.merging.SpectraMerger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
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
 * Use the factory methods or the {@link Builder} for setup of this parameter to limit options.
 * <p>
 * Either setup through presets: {@link PresetSimpleSpectraMergeSelectParameters}
 * {@link PresetAdvancedSpectraMergeSelectParameters}
 * <p>
 * Or select input scans without merging: {@link InputSpectraSelectParameters}
 * <p>
 * Or advanced parameters for full control in {@link AdvancedSpectraMergeSelectParameters}
 */
public class SpectraMergeSelectParameter extends
    ModuleOptionsEnumComboParameter<SpectraMergeSelectModuleOptions> {

  public static final String DEFAULT_NAME = "Merge & select fragment scans";
  public static final String DEFAULT_DESCRIPTION = "Setup spectral merging and the selection of the final list of fragment scans. There are options for simple preset based setup or the advanced options.";

  protected SpectraMergeSelectParameter(
      final @NotNull SpectraMergeSelectModuleOptions defaultValue) {
    this(SpectraMergeSelectModuleOptions.values(), defaultValue);
  }

  protected SpectraMergeSelectParameter(final @NotNull SpectraMergeSelectModuleOptions[] options,
      final @NotNull SpectraMergeSelectModuleOptions defaultValue) {
    this(DEFAULT_NAME, DEFAULT_DESCRIPTION, options, defaultValue);
  }

  protected SpectraMergeSelectParameter(final String name, final String description,
      final SpectraMergeSelectModuleOptions[] options,
      final SpectraMergeSelectModuleOptions active) {
    super(name, description, options, active);
  }

  /**
   * Used in clone parameter
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
   * No MSn for now - need to implement first - default selection should be representative scans so
   * each energy and across energy
   */
  @NotNull
  public static SpectraMergeSelectParameter createSpectraLibrarySearchDefaultNoMSn() {
    return new Builder().preset(SpectraMergeSelectPresets.REPRESENTATIVE_SCANS).createParameters();
  }

  /**
   * Options used for SIRIUS export: Advanced options available, no MSn, and use ALL_INPUT_SCANS
   * initially
   */
  @NotNull
  public static SpectraMergeSelectParameter createSiriusExportAllDefault() {
    return new Builder().includeAdvanced(true).useExportAllInputScans().createParameters();
  }

  /**
   * GNPS allows single scan per row so also limit the options to either single merged or single
   * best input scan
   */
  public static SpectraMergeSelectParameter createGnpsSingleScanDefault() {
    return new Builder().limitToSingleScan().preset(SpectraMergeSelectPresets.SINGLE_MERGED_SCAN)
        .createParameters();
  }

  /**
   * Full choice of configuration with a simple preset preselected in
   * {@link PresetSimpleSpectraMergeSelectParameters}
   */
  @NotNull
  public static SpectraMergeSelectParameter createFullSetupWithSimplePreset(
      final SpectraMergeSelectPresets simplePreset) {
    // FULL setup
    var param = new SpectraMergeSelectParameter(SpectraMergeSelectModuleOptions.SIMPLE_MERGED);
    param.getEmbeddedParameters()
        .setParameter(PresetSimpleSpectraMergeSelectParameters.preset, simplePreset);
    return param;
  }

  /**
   * Use single merged scan across samples as default to reduce processing speed.
   * {@link SpectraMergeSelectPresets#REPRESENTATIVE_SCANS} may be good as well but this will slow
   * down the networking and make it even more complex to look at.
   * <p>
   * Currently no MSn, no advanced setup - too many scans as option.
   */
  public static SpectraMergeSelectParameter createMolecularNetworkingDefault() {
    return new Builder().preset(SpectraMergeSelectPresets.SINGLE_MERGED_SCAN).createParameters();
  }

  /**
   * Single merged scan in each sample and across all samples
   */
  public static SpectraMergeSelectParameter createEachSampleAcrossEnergies() {
    return new Builder().preset(SpectraMergeSelectPresets.SINGLE_MERGED_SCAN)
        .active(SpectraMergeSelectModuleOptions.PRESET_MERGED).presetAlsoEachSample(true)
        .createParameters();
  }

  public static SpectraMergeSelectParameter createLipidSearchAllSpectraDefault() {
    return new Builder().useExportAllInputScans().createParameters();
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
   * Useful methods to set specific advanced presets in the wizard or other locations
   */
  public void setAdvancedPreset(final SpectraMergeSelectPresets preset,
      final MZTolerance mzTolScans, final IntensityMergingType intensityMergingType,
      final List<MergedSpectraFinalSelectionTypes> sampleHandling) {
    setValue(SpectraMergeSelectModuleOptions.PRESET_MERGED);
    var embedded = getEmbeddedParameters();
    embedded.setParameter(PresetAdvancedSpectraMergeSelectParameters.preset, preset);
    embedded.setParameter(PresetAdvancedSpectraMergeSelectParameters.mergeMzTolerance, mzTolScans);
    embedded.setParameter(PresetAdvancedSpectraMergeSelectParameters.intensityMergeType,
        intensityMergingType);
    embedded.setParameter(PresetAdvancedSpectraMergeSelectParameters.sampleHandling,
        sampleHandling);
  }

  /**
   * Set all parameters so that input scans are used - either best or all scans.
   *
   * @param option use all scans or use best
   */
  public void setUseInputScans(final SelectInputScans option) {
    setValue(SpectraMergeSelectModuleOptions.INPUT_SCANS);
    var embedded = getEmbeddedParameters();
    embedded.setParameter(InputSpectraSelectParameters.inputSelectionType, option);
  }

  /**
   * Creates the merger and filters needed in fragment scan selection
   *
   * @return a fragment scan selection either merging scans or just selecting input scans
   */
  @NotNull
  public FragmentScanSelection createFragmentScanSelection(
      final @Nullable MemoryMapStorage storage) {
    var value = getValue();
    return value.createFragmentScanSelection(storage, getEmbeddedParameters(value));
  }

  /**
   * Set all mz tolerances for merging in all embedded parameter sets
   *
   * @param mzTol the new value
   */
  public void setMzTolerance(final MZTolerance mzTol) {
    for (final ParameterSet params : parametersMap.values()) {
      params.streamForClass(MZToleranceParameter.class)
          .forEach(mzTolParam -> mzTolParam.setValue(mzTol));
    }
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

    // input scans selection
    private SelectInputScans @NotNull [] inputScansOptions = SelectInputScans.valuesExcludingNone();
    private SelectInputScans inputSelection = SelectInputScans.MOST_INTENSE_ACROSS_SAMPLES;

    // use optional to define that options should not be filtered but used as is
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private @NotNull Optional<Boolean> includeAdvanced = Optional.of(false);

    // advanced preset {@link PresetAdvancedSpectraMergeSelectParameters}
    private boolean presetAlsoEachSample = false;


    public SpectraMergeSelectParameter createParameters() {
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

      // limit choice of all input scans - the default parameter shows this option so we only need to remove it
      var embedded = param.getEmbeddedParameters(SpectraMergeSelectModuleOptions.INPUT_SCANS);
      if (embedded != null) {
        embedded.getParameter(InputSpectraSelectParameters.inputSelectionType)
            .setChoices(inputScansOptions, inputSelection);
      }

      // advanced preset also use for each sample? otherwise only across samples
      var presetAdvanced = param.getEmbeddedParameters(
          SpectraMergeSelectModuleOptions.PRESET_MERGED);
      if (presetAdvanced != null) {
        List<MergedSpectraFinalSelectionTypes> sampleHandling = new ArrayList<>();
        sampleHandling.add(MergedSpectraFinalSelectionTypes.ACROSS_SAMPLES);
        if (presetAlsoEachSample) {
          sampleHandling.add(MergedSpectraFinalSelectionTypes.EACH_SAMPLE);
        }
        presetAdvanced.setParameter(PresetAdvancedSpectraMergeSelectParameters.sampleHandling,
            sampleHandling);
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
      if (!ArrayUtils.contains(preset, presetOptions)) {
        preset = presetOptions[0];
      }
      return this;
    }

    public Builder preset(final @Nullable SpectraMergeSelectPresets preset) {
      this.preset = preset;
      return this;
    }

    public Builder useExportAllInputScans() {
      active(SpectraMergeSelectModuleOptions.INPUT_SCANS);
      inputSelection = SelectInputScans.MOST_INTENSE_ACROSS_SAMPLES;
      return this;
    }

    public Builder includeAdvanced(final @Nullable Boolean includeAdvanced) {
      this.includeAdvanced = Optional.ofNullable(includeAdvanced);
      return this;
    }

    public Builder limitToSingleScan() {
      inputScansOptions = Arrays.stream(inputScansOptions).filter(SelectInputScans::isSingleScan)
          .toArray(SelectInputScans[]::new);
      inputSelection = SelectInputScans.MOST_INTENSE_ACROSS_SAMPLES;

      presetOptions(new SpectraMergeSelectPresets[]{SpectraMergeSelectPresets.SINGLE_MERGED_SCAN});
      preset(SpectraMergeSelectPresets.SINGLE_MERGED_SCAN);
      includeAdvanced(false);
      return this;
    }

    public Builder presetAlsoEachSample(final boolean presetAlsoEachSample) {
      this.presetAlsoEachSample = presetAlsoEachSample;
      return this;
    }
  }
}
