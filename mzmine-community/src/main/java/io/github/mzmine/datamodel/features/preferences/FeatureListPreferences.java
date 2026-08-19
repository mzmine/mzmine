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

package io.github.mzmine.datamodel.features.preferences;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.types.numbers.abstr.AbstractRsdType;
import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.StringUtils;
import io.github.mzmine.util.concurrent.CloseableReentrantReadWriteLock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * User defined preferences of a single {@link FeatureList} that are not tied to a specific
 * processing step but influence how values are derived from the feature list, e.g., the sample
 * types used by {@link AbstractRsdType}.
 * <p>
 * A default instance is always created in the feature list constructor and may be replaced by the
 * "Redefine feature list preferences" module. Preferences are persisted with the project.
 * <p>
 * The preferences themselves are immutable, only the internal cache of resolved raw data files is
 * mutable and lazily populated.
 */
public final class FeatureListPreferences {

  private static final Logger logger = Logger.getLogger(FeatureListPreferences.class.getName());
  private static final String XML_QC_RSD_SAMPLE_TYPES_ATTR = "qc_rsd_sample_types";

  private final @NotNull SampleTypeFilter rsdSampleTypeFilter;

  /**
   * Guards all cache fields below. The resolved files are requested for every cell of the RSD
   * columns, therefore the result is cached until either the source files or the project metadata
   * change.
   */
  private final CloseableReentrantReadWriteLock cacheLock = new CloseableReentrantReadWriteLock();
  private @Nullable List<RawDataFile> cachedRsdFiles;
  private @Nullable List<RawDataFile> cachedSourceFiles;
  private @Nullable MetadataTable cachedMetadata;
  private long cachedMetadataVersion;

  public FeatureListPreferences(@NotNull final SampleTypeFilter rsdSampleTypeFilter) {
    this.rsdSampleTypeFilter = rsdSampleTypeFilter;
  }

  /**
   * @return the default preferences as created in the {@link FeatureList} constructor
   */
  public static @NotNull FeatureListPreferences createDefault() {
    return new FeatureListPreferences(SampleTypeFilter.qc());
  }

  /**
   * @return the sample types used to calculate the RSD in {@link AbstractRsdType}
   */
  public @NotNull SampleTypeFilter getQcRsdSampleTypeFilter() {
    return rsdSampleTypeFilter;
  }

  /**
   * Resolves the raw data files used for the RSD calculation. The result is cached and only
   * recomputed if the source files or the project metadata changed, see
   * {@link MetadataTable#getVersion()}.
   *
   * @param sourceFiles all raw data files of the feature list, usually
   *                    {@link FeatureList#getRawDataFiles()}
   * @return the filtered files, may be empty
   */
  public @NotNull List<RawDataFile> getRsdFiles(@NotNull final List<RawDataFile> sourceFiles) {
    final MetadataTable metadata = ProjectService.getMetadata();
    final long metadataVersion = metadata.getVersion();

    try (var _ = cacheLock.lockRead()) {
      if (isCacheValid(metadata, metadataVersion, sourceFiles)) {
        return Objects.requireNonNull(cachedRsdFiles);
      }
    }

    try (var _ = cacheLock.lockWrite()) {
      // another thread may have populated the cache while waiting for the write lock
      if (isCacheValid(metadata, metadataVersion, sourceFiles)) {
        return Objects.requireNonNull(cachedRsdFiles);
      }

      // make sure its the right version
      cachedMetadataVersion = metadata.getVersion();
      final List<RawDataFile> filtered = rsdSampleTypeFilter.filterFiles(sourceFiles);
      cachedRsdFiles = filtered;
      cachedSourceFiles = sourceFiles;
      cachedMetadata = metadata;
      return filtered;
    }
  }

  /**
   * Requires the read or write lock of {@link #cacheLock}.
   *
   * @return true if the cached files still match the requested files and the current metadata
   */
  private boolean isCacheValid(@NotNull final MetadataTable metadata, final long metadataVersion,
      @NotNull final List<RawDataFile> sourceFiles) {
    // assumption: the metadata instance is exchanged on project load, therefore compare identity
    // as well because the version counter restarts with a new table
    return cachedRsdFiles != null && cachedMetadata == metadata
        && cachedMetadataVersion == metadataVersion && sourceFiles.equals(cachedSourceFiles);
  }

  public @NotNull FeatureListPreferences withRsdSampleTypeFilter(
      @NotNull final SampleTypeFilter filter) {
    return new FeatureListPreferences(filter);
  }

  /**
   * @return a copy without the internal cache
   */
  public @NotNull FeatureListPreferences copy() {
    return new FeatureListPreferences(rsdSampleTypeFilter);
  }

  public void saveToXML(@NotNull final Element element) {
    element.setAttribute(XML_QC_RSD_SAMPLE_TYPES_ATTR,
        rsdSampleTypeFilter.getTypes().stream().map(SampleType::name)
            .collect(Collectors.joining(",")));
  }

  /**
   * @param element the preferences element or null if the project was saved before preferences were
   *                introduced
   * @return the loaded preferences or null if missing
   */
  @Nullable
  public static FeatureListPreferences loadFromXML(@Nullable final Element element) {
    if (element == null || !element.hasAttribute(XML_QC_RSD_SAMPLE_TYPES_ATTR)) {
      return null;
    }

    final String types = element.getAttribute(XML_QC_RSD_SAMPLE_TYPES_ATTR);
    // an empty attribute is a valid empty filter, only unknown values fall back to the default
    if (StringUtils.isBlank(types)) {
      return new FeatureListPreferences(SampleTypeFilter.of(List.of()));
    }

    final List<SampleType> parsed = new ArrayList<>();
    for (final String type : types.split(",")) {
      try {
        parsed.add(SampleType.valueOf(type.trim()));
      } catch (IllegalArgumentException e) {
        logger.log(Level.WARNING,
            "Unknown sample type %s in feature list preferences. Skipping this type.".formatted(
                type), e);
      }
    }
    return new FeatureListPreferences(SampleTypeFilter.of(parsed));
  }

  // SampleTypeFilter has no equals, therefore compare the types it allows
  @Override
  public boolean equals(final Object o) {
    return o instanceof FeatureListPreferences other && rsdSampleTypeFilter.getTypes()
        .equals(other.rsdSampleTypeFilter.getTypes());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(rsdSampleTypeFilter.getTypes());
  }

  @Override
  public String toString() {
    return "FeatureListPreferences{rsdSampleTypeFilter=" + rsdSampleTypeFilter + '}';
  }
}
