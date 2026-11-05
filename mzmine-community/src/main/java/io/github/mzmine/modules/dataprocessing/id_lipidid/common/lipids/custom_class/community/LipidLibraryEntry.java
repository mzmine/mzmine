package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.community;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.Semver.SemverType;
import com.vdurmont.semver4j.SemverException;
import io.github.mzmine.util.io.SemverVersionReader;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One lipid class in the catalog (index.json) of the community lipid library.
 *
 * @param source "community" or "builtin"
 * @param file   path of the mzmine importable class file relative to the library base URL
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LipidLibraryEntry(@NotNull String id, @Nullable String source, @NotNull String name,
                                @NotNull String abbr, @Nullable String coreClass,
                                @Nullable String mainClass, @Nullable String backBoneFormula,
                                @Nullable List<String> chainTypes,
                                @Nullable List<String> polarities, @Nullable String description,
                                @Nullable List<LipidLibraryAuthor> authors,
                                @Nullable List<String> tags, @Nullable String instrument,
                                @Nullable String minMzmineVersion, @Nullable String updated,
                                @Nullable String rightsHolder, @Nullable String license,
                                @Nullable String licenseUrl, @NotNull String file) {

  /**
   * @return true for classes shared by the community, false for the built-in mzmine classes
   */
  public boolean isCommunity() {
    return "community".equals(source);
  }

  /**
   * @return comma separated author names
   */
  public @NotNull String authorNames() {
    return authors == null ? "" : authors.stream().map(LipidLibraryAuthor::name)
        .filter(Objects::nonNull).collect(Collectors.joining(", "));
  }

  /**
   * @return true if the running mzmine version is too old for this class
   */
  public boolean requiresNewerMzmine() {
    if (minMzmineVersion == null || minMzmineVersion.isBlank()) {
      return false;
    }
    try {
      final Semver current = SemverVersionReader.getMZmineVersion();
      return current.isLowerThan(new Semver(minMzmineVersion, SemverType.LOOSE));
    } catch (SemverException | NullPointerException e) {
      return false;
    }
  }

  /**
   * @return lower case text used for searching
   */
  public @NotNull String searchText() {
    return String.join(" ", abbr, name, Objects.requireNonNullElse(description, ""),
        Objects.requireNonNullElse(coreClass, ""), Objects.requireNonNullElse(mainClass, ""),
        Objects.requireNonNullElse(backBoneFormula, ""), authorNames(),
        tags == null ? "" : String.join(" ", tags)).toLowerCase();
  }
}
