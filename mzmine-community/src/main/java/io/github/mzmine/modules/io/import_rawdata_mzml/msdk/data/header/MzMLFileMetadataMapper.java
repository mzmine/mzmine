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

package io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.header;

import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.rawfiletypes.AcquisitionSoftwareType;
import io.github.mzmine.datamodel.features.rawfiletypes.AcquisitionSoftwareVersionType;
import io.github.mzmine.datamodel.features.rawfiletypes.DetectorsType;
import io.github.mzmine.datamodel.features.rawfiletypes.InstrumentModelType;
import io.github.mzmine.datamodel.features.rawfiletypes.IonSourcesType;
import io.github.mzmine.datamodel.features.rawfiletypes.MassAnalyzersType;
import io.github.mzmine.datamodel.features.rawfiletypes.RawFileMetadataTypes;
import io.github.mzmine.datamodel.features.rawfiletypes.SampleNameType;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.abstr.StringListType;
import io.github.mzmine.datamodel.features.types.abstr.StringType;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLCVParam;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLTags;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Maps the mzML header to the file metadata of a raw data file. The instrument model and components
 * are child terms in the PSI-MS ontology. Without the ontology, they are recognized by their
 * position in the mzML and stored by their name.
 */
public final class MzMLFileMetadataMapper {

  private static final String USER_PARAM_INSTRUMENT_MODEL = "instrument model";
  private static final String USER_PARAM_SOFTWARE_NAME = "software name";

  private MzMLFileMetadataMapper() {
  }

  public static void apply(@NotNull final MzMLHeaderMetadata header,
      @NotNull final ModularDataModel metadata) {
    final MzMLInstrumentConfiguration configuration = header.getDefaultInstrumentConfiguration();
    if (configuration != null) {
      final List<MzMLCVParam> cvParams = header.resolveCvParams(configuration);
      setByAccession(cvParams, metadata);
      setIfAbsent(metadata, InstrumentModelType.class,
          findInstrumentModel(cvParams, header.resolveUserParams(configuration)));

      setIfAbsent(metadata, IonSourcesType.class,
          componentNames(header, configuration.getSources()));
      setIfAbsent(metadata, MassAnalyzersType.class,
          componentNames(header, configuration.getAnalyzers()));
      setIfAbsent(metadata, DetectorsType.class,
          componentNames(header, configuration.getDetectors()));

      // decision: only the software referenced by the instrument configuration is the acquisition
      // software. Other entries are often converters like ProteoWizard.
      final MzMLSoftware software = header.getSoftware(configuration.getSoftwareRef());
      if (software != null) {
        setIfAbsent(metadata, AcquisitionSoftwareType.class, findSoftwareName(header, software));
        setIfAbsent(metadata, AcquisitionSoftwareVersionType.class, software.getVersion());
      }
    }

    final MzMLParamContainer sourceFile = header.getDefaultSourceFile();
    if (sourceFile != null) {
      setByAccession(header.resolveCvParams(sourceFile), metadata);
    }

    final MzMLParamContainer sample = header.getSample();
    if (sample != null) {
      setByAccession(header.resolveCvParams(sample), metadata);
      // fallback to the name attribute of the sample element
      setIfAbsent(metadata, SampleNameType.class,
          MzMLParamContainer.findUserParamValue(sample.getUserParams(), MzMLTags.ATTR_NAME));
    }
  }

  /**
   * Sets all cvParams with a value whose accession is defined by a raw file string type, e.g.,
   * instrument serial number. The first value wins.
   */
  private static void setByAccession(@NotNull final List<MzMLCVParam> params,
      @NotNull final ModularDataModel metadata) {
    for (final MzMLCVParam param : params) {
      final String value = param.getValue().orElse(null);
      if (value == null || value.isBlank()) {
        continue;
      }
      final DataType<?> type = RawFileMetadataTypes.forAccession(param.getAccession());
      if (type instanceof StringType stringType && metadata.get(stringType) == null) {
        metadata.set(stringType, value.strip());
      }
    }
  }

  /**
   * The instrument model is a child term of MS:1000031 without value. Specific model terms (Q
   * Exactive) are preferred over a user param, which is preferred over generic vendor terms (Waters
   * instrument model).
   */
  private static @Nullable String findInstrumentModel(@NotNull final List<MzMLCVParam> cvParams,
      @NotNull final List<MzMLUserParam> userParams) {
    String generic = null;
    for (final MzMLCVParam param : cvParams) {
      final String name = param.getName().orElse(null);
      // assumption: value less terms that are not claimed by another type describe the model
      if (name == null || param.getValue().isPresent()
          || RawFileMetadataTypes.forAccession(param.getAccession()) != null) {
        continue;
      }
      if (name.toLowerCase().endsWith(USER_PARAM_INSTRUMENT_MODEL)) {
        if (generic == null) {
          generic = name;
        }
        continue;
      }
      return name;
    }
    final String userModel = MzMLParamContainer.findUserParamValue(userParams,
        USER_PARAM_INSTRUMENT_MODEL);
    return userModel != null ? userModel : generic;
  }

  private static @Nullable String findSoftwareName(@NotNull final MzMLHeaderMetadata header,
      @NotNull final MzMLSoftware software) {
    // user param is more specific, e.g., TIMS SDK instead of Bruker software
    final String userName = MzMLParamContainer.findUserParamValue(
        header.resolveUserParams(software), USER_PARAM_SOFTWARE_NAME);
    if (userName != null) {
      return userName;
    }
    for (final MzMLCVParam param : header.resolveCvParams(software)) {
      final String name = param.getName().orElse(null);
      if (name != null) {
        // custom unreleased software tool carries the name as value
        return param.getValue().orElse(name);
      }
    }
    return software.getId().isBlank() ? null : software.getId();
  }

  /**
   * @return names of all value less cvParams of all components in order, or null if empty
   */
  private static @Nullable List<String> componentNames(@NotNull final MzMLHeaderMetadata header,
      @NotNull final List<MzMLParamContainer> components) {
    final List<String> names = new ArrayList<>();
    for (final MzMLParamContainer component : components) {
      for (final MzMLCVParam param : header.resolveCvParams(component)) {
        if (param.getValue().isEmpty()) {
          param.getName().ifPresent(names::add);
        }
      }
    }
    return names.isEmpty() ? null : List.copyOf(names);
  }

  private static void setIfAbsent(@NotNull final ModularDataModel metadata,
      @NotNull final Class<? extends StringType> type, @Nullable final String value) {
    if (value != null && !value.isBlank() && metadata.get(type) == null) {
      metadata.set(type, value.strip());
    }
  }

  private static void setIfAbsent(@NotNull final ModularDataModel metadata,
      @NotNull final Class<? extends StringListType> type, @Nullable final List<String> value) {
    if (value != null && !value.isEmpty() && metadata.get(type) == null) {
      metadata.set(type, value);
    }
  }
}
