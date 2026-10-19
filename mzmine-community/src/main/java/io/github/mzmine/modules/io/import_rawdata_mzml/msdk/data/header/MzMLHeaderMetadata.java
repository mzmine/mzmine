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

import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLCVParam;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLParser;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLTags;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.util.TagTracker;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Collects the file level metadata of the mzML header (everything before the run element):
 * referenceableParamGroups, sourceFiles, software, instrumentConfigurations, and samples. Only used
 * during import. Errors in the header are logged and never stop the import.
 */
public class MzMLHeaderMetadata {

  private static final Logger logger = Logger.getLogger(MzMLHeaderMetadata.class.getName());

  private final @NotNull Map<String, MzMLParamContainer> paramGroups = new LinkedHashMap<>();
  private final @NotNull Map<String, MzMLParamContainer> sourceFiles = new LinkedHashMap<>();
  private final @NotNull Map<String, MzMLSoftware> software = new LinkedHashMap<>();
  private final @NotNull Map<String, MzMLInstrumentConfiguration> instrumentConfigurations = new LinkedHashMap<>();
  private final @NotNull Map<String, MzMLParamContainer> samples = new LinkedHashMap<>();

  private @Nullable String defaultInstrumentConfigurationRef;
  private @Nullable String defaultSourceFileRef;
  private @Nullable String sampleRef;

  // parsing state: the element that receives cvParams and userParams
  private @Nullable MzMLParamContainer current;
  private @Nullable MzMLInstrumentConfiguration currentConfiguration;
  private boolean loggedError = false;

  private static @NotNull String id(@NotNull final XMLStreamReader reader) {
    final String id = reader.getAttributeValue(null, MzMLTags.ATTR_ID);
    return id == null ? "" : id;
  }

  private static <T extends MzMLParamContainer> @NotNull T putNew(
      @NotNull final Map<String, ? super T> map, @NotNull final T element) {
    map.put(element.getId(), element);
    return element;
  }

  private static @NotNull MzMLParamContainer addComponent(
      @NotNull final MzMLInstrumentConfiguration configuration, @NotNull final String tag,
      @NotNull final XMLStreamReader reader) {
    final String order = reader.getAttributeValue(null, MzMLTags.ATTR_ORDER);
    final MzMLParamContainer component = new MzMLParamContainer(order == null ? tag : tag + order);
    // assumption: components are written in their order, as done by all common converters
    switch (tag) {
      case MzMLTags.TAG_SOURCE -> configuration.getSources().add(component);
      case MzMLTags.TAG_ANALYZER -> configuration.getAnalyzers().add(component);
      case MzMLTags.TAG_DETECTOR -> configuration.getDetectors().add(component);
      default -> throw new IllegalArgumentException("Not a component " + tag);
    }
    return component;
  }

  private static <T> @Nullable T getOrFirst(@NotNull final Map<String, T> map,
      @Nullable final String ref) {
    if (ref != null) {
      final T value = map.get(ref);
      if (value != null) {
        return value;
      }
    }
    return map.values().stream().findFirst().orElse(null);
  }

  /**
   * Call after the tracker entered the tag. Only header tags are handled, all others are ignored.
   */
  public void processOpeningTag(@NotNull final TagTracker tracker,
      @NotNull final XMLStreamReader reader, @NotNull final String tag) {
    try {
      switch (tag) {
        case MzMLTags.TAG_REF_PARAM_GROUP -> {
          if (tracker.inside(MzMLTags.TAG_REF_PARAM_GROUP_LIST)) {
            current = putNew(paramGroups, new MzMLParamContainer(id(reader)));
          }
        }
        case MzMLTags.TAG_SOURCE_FILE -> {
          if (tracker.inside(MzMLTags.TAG_SOURCE_FILE_LIST)) {
            current = putNew(sourceFiles, new MzMLParamContainer(id(reader)));
          }
        }
        case MzMLTags.TAG_SOFTWARE -> {
          if (tracker.inside(MzMLTags.TAG_SOFTWARE_LIST)) {
            current = putNew(software, new MzMLSoftware(id(reader),
                reader.getAttributeValue(null, MzMLTags.ATTR_VERSION)));
          }
        }
        case MzMLTags.TAG_INSTRUMENT_CONFIGURATION -> {
          if (tracker.inside(MzMLTags.TAG_INSTRUMENT_CONFIGURATION_LIST)) {
            currentConfiguration = putNew(instrumentConfigurations,
                new MzMLInstrumentConfiguration(id(reader)));
            current = currentConfiguration;
          }
        }
        case MzMLTags.TAG_SOURCE, MzMLTags.TAG_ANALYZER, MzMLTags.TAG_DETECTOR -> {
          if (currentConfiguration != null && tracker.inside(MzMLTags.TAG_COMPONENT_LIST)) {
            current = addComponent(currentConfiguration, tag, reader);
          }
        }
        case MzMLTags.TAG_SOFTWARE_REF -> {
          if (currentConfiguration != null) {
            currentConfiguration.setSoftwareRef(reader.getAttributeValue(null, MzMLTags.ATTR_REF));
          }
        }
        case MzMLTags.TAG_SAMPLE -> {
          if (tracker.inside(MzMLTags.TAG_SAMPLE_LIST)) {
            final MzMLParamContainer sample = putNew(samples, new MzMLParamContainer(id(reader)));
            // the sample name attribute is optional and often empty
            final String name = reader.getAttributeValue(null, MzMLTags.ATTR_NAME);
            if (name != null && !name.isBlank()) {
              sample.addUserParam(new MzMLUserParam(MzMLTags.ATTR_NAME, name));
            }
            current = sample;
          }
        }
        case MzMLTags.TAG_REF_PARAM_GROUP_REF -> {
          final String ref = reader.getAttributeValue(null, MzMLTags.ATTR_REF);
          if (current != null && ref != null) {
            current.addParamGroupRef(ref);
          }
        }
        case MzMLTags.TAG_CV_PARAM -> {
          if (current != null) {
            current.addCvParam(MzMLParser.createMzMLCVParam(reader));
          }
        }
        case MzMLTags.TAG_USER_PARAM -> {
          final String name = reader.getAttributeValue(null, MzMLTags.ATTR_NAME);
          if (current != null && name != null) {
            final String value = reader.getAttributeValue(null, MzMLTags.ATTR_VALUE);
            current.addUserParam(
                new MzMLUserParam(name, value == null || value.isBlank() ? null : value));
          }
        }
        default -> {
        }
      }
    } catch (RuntimeException e) {
      // decision: invalid header metadata must never stop the import of the spectra
      if (!loggedError) {
        loggedError = true;
        logger.log(Level.WARNING,
            "Cannot parse mzML header element %s: %s".formatted(tag, e.getMessage()), e);
      }
    }
  }

  /**
   * Call after the tracker exited the tag.
   */
  public void processClosingTag(@NotNull final String tag) {
    switch (tag) {
      // back to the instrument configuration after a component
      case MzMLTags.TAG_SOURCE, MzMLTags.TAG_ANALYZER, MzMLTags.TAG_DETECTOR -> {
        if (currentConfiguration != null) {
          current = currentConfiguration;
        }
      }
      case MzMLTags.TAG_INSTRUMENT_CONFIGURATION -> {
        currentConfiguration = null;
        current = null;
      }
      case MzMLTags.TAG_REF_PARAM_GROUP, MzMLTags.TAG_SOURCE_FILE, MzMLTags.TAG_SOFTWARE,
           MzMLTags.TAG_SAMPLE -> current = null;
      default -> {
      }
    }
  }

  /**
   * References of the run element to the header elements
   */
  public void setRunReferences(@Nullable final String defaultInstrumentConfigurationRef,
      @Nullable final String defaultSourceFileRef, @Nullable final String sampleRef) {
    this.defaultInstrumentConfigurationRef = defaultInstrumentConfigurationRef;
    this.defaultSourceFileRef = defaultSourceFileRef;
    this.sampleRef = sampleRef;
  }

  /**
   * @return the cvParams of this element including the ones of referenced param groups
   */
  public @NotNull List<MzMLCVParam> resolveCvParams(@NotNull final MzMLParamContainer element) {
    final List<MzMLCVParam> params = new ArrayList<>(element.getCvParams());
    for (final String ref : element.getParamGroupRefs()) {
      final MzMLParamContainer group = paramGroups.get(ref);
      if (group != null) {
        params.addAll(group.getCvParams());
      }
    }
    return params;
  }

  /**
   * @return the userParams of this element including the ones of referenced param groups
   */
  public @NotNull List<MzMLUserParam> resolveUserParams(@NotNull final MzMLParamContainer element) {
    final List<MzMLUserParam> params = new ArrayList<>(element.getUserParams());
    for (final String ref : element.getParamGroupRefs()) {
      final MzMLParamContainer group = paramGroups.get(ref);
      if (group != null) {
        params.addAll(group.getUserParams());
      }
    }
    return params;
  }

  /**
   * @return the instrument configuration referenced by the run or the first one
   */
  public @Nullable MzMLInstrumentConfiguration getDefaultInstrumentConfiguration() {
    return getOrFirst(instrumentConfigurations, defaultInstrumentConfigurationRef);
  }

  /**
   * @return the source file referenced by the run or the first one
   */
  public @Nullable MzMLParamContainer getDefaultSourceFile() {
    return getOrFirst(sourceFiles, defaultSourceFileRef);
  }

  /**
   * @return the sample referenced by the run or the first one
   */
  public @Nullable MzMLParamContainer getSample() {
    return getOrFirst(samples, sampleRef);
  }

  public @Nullable MzMLSoftware getSoftware(@Nullable final String id) {
    return id == null ? null : software.get(id);
  }
}
