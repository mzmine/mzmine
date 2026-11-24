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

package io.github.mzmine.modules.io.import_rawdata_mzml.header;

import io.github.mzmine.modules.io.import_rawdata_mzml.MzMLXml;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLCVParam;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLTags;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Collects the file level metadata of the mzML header (everything before the run element):
 * referenceableParamGroups, sourceFiles, software, instrumentConfigurations, and samples. Only used
 * during import. Errors in the header are logged and never stop the import.
 */
public class MzMLHeaderMetadata {

  private final @NotNull Map<String, MzMLParamContainer> paramGroups = new LinkedHashMap<>();
  private final @NotNull Map<String, MzMLParamContainer> sourceFiles = new LinkedHashMap<>();
  private final @NotNull Map<String, MzMLSoftware> software = new LinkedHashMap<>();
  private final @NotNull Map<String, MzMLInstrumentConfiguration> instrumentConfigurations = new LinkedHashMap<>();
  private final @NotNull Map<String, MzMLParamContainer> samples = new LinkedHashMap<>();

  private @Nullable String defaultInstrumentConfigurationRef;
  private @Nullable String defaultSourceFileRef;
  private @Nullable String sampleRef;

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
   * Reads a direct child of mzML preceding the run. The caller advances to the next sibling.
   *
   * @param xml cursor on a header section
   */
  public void readSection(@NotNull final XMLStreamReader xml) throws XMLStreamException {
    switch (xml.getLocalName()) {
      case MzMLTags.TAG_FILE_DESCRIPTION -> {
        while (nextChild(xml, MzMLTags.TAG_SOURCE_FILE_LIST)) {
          while (nextChild(xml, MzMLTags.TAG_SOURCE_FILE)) {
            readParams(xml, putNew(sourceFiles, new MzMLParamContainer(id(xml))));
          }
        }
      }
      case MzMLTags.TAG_REF_PARAM_GROUP_LIST -> {
        while (nextChild(xml, MzMLTags.TAG_REF_PARAM_GROUP)) {
          final String id = MzMLXml.required(xml, MzMLTags.ATTR_ID);
          readParams(xml, putNew(paramGroups, new MzMLParamContainer(id)));
        }
      }
      case MzMLTags.TAG_SOFTWARE_LIST -> {
        while (nextChild(xml, MzMLTags.TAG_SOFTWARE)) {
          readParams(xml, putNew(software,
              new MzMLSoftware(id(xml), xml.getAttributeValue(null, MzMLTags.ATTR_VERSION))));
        }
      }
      case MzMLTags.TAG_INSTRUMENT_CONFIGURATION_LIST -> {
        while (nextChild(xml, MzMLTags.TAG_INSTRUMENT_CONFIGURATION)) {
          readParams(xml,
              putNew(instrumentConfigurations, new MzMLInstrumentConfiguration(id(xml))));
        }
      }
      case MzMLTags.TAG_SAMPLE_LIST -> {
        while (nextChild(xml, MzMLTags.TAG_SAMPLE)) {
          final var sample = putNew(samples, new MzMLParamContainer(id(xml)));
          final String name = xml.getAttributeValue(null, MzMLTags.ATTR_NAME);
          if (name != null && !name.isBlank()) {
            sample.addUserParam(new MzMLUserParam(MzMLTags.ATTR_NAME, name));
          }
          readParams(xml, sample);
        }
      }
      default -> MzMLXml.skipElement(xml);
    }
  }

  /**
   * @return whether the cursor reached the next named child before the end of its parent
   */
  private static boolean nextChild(@NotNull final XMLStreamReader xml, @NotNull final String name)
      throws XMLStreamException {
    while (xml.nextTag() == XMLStreamConstants.START_ELEMENT) {
      if (xml.getLocalName().equals(name)) {
        return true;
      }
      MzMLXml.skipElement(xml);
    }
    return false;
  }

  private void readParams(@NotNull final XMLStreamReader xml,
      @NotNull final MzMLParamContainer target) throws XMLStreamException {
    while (xml.nextTag() == XMLStreamConstants.START_ELEMENT) {
      switch (xml.getLocalName()) {
        case MzMLTags.TAG_COMPONENT_LIST -> {
          if (target instanceof MzMLInstrumentConfiguration config) {
            while (xml.nextTag() == XMLStreamConstants.START_ELEMENT) {
              switch (xml.getLocalName()) {
                case MzMLTags.TAG_SOURCE, MzMLTags.TAG_ANALYZER, MzMLTags.TAG_DETECTOR ->
                    readParams(xml, addComponent(config, xml.getLocalName(), xml));
                default -> MzMLXml.skipElement(xml);
              }
            }
            continue;
          }
        }
        case MzMLTags.TAG_SOFTWARE_REF -> {
          if (target instanceof MzMLInstrumentConfiguration config) {
            config.setSoftwareRef(xml.getAttributeValue(null, MzMLTags.ATTR_REF));
          }
        }
        case MzMLTags.TAG_REF_PARAM_GROUP_REF -> {
          final String ref = xml.getAttributeValue(null, MzMLTags.ATTR_REF);
          if (ref != null) {
            target.addParamGroupRef(ref);
          }
        }
        case MzMLTags.TAG_CV_PARAM -> {
          // decision: optional malformed header metadata must not prevent spectrum import.
          final String accession = xml.getAttributeValue(null, MzMLTags.ATTR_ACCESSION);
          if (accession != null && !accession.isEmpty()) {
            target.addCvParam(MzMLCVParam.read(xml));
          }
        }
        case MzMLTags.TAG_USER_PARAM -> {
          final String name = xml.getAttributeValue(null, MzMLTags.ATTR_NAME);
          final String value = xml.getAttributeValue(null, MzMLTags.ATTR_VALUE);
          if (name != null) {
            target.addUserParam(
                new MzMLUserParam(name, value == null || value.isBlank() ? null : value));
          }
        }
      }
      MzMLXml.skipElement(xml);
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

  /**
   * @param id reference from a spectrum, chromatogram or one of its child elements
   * @return the declared parameter group, if present
   */
  public @Nullable MzMLParamContainer getParamGroup(@NotNull final String id) {
    return paramGroups.get(id);
  }
}
