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

import io.github.mzmine.datamodel.RawDataFile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shared extraction logic used by both the editor preview and the processing task so that the
 * highlighted preview always reflects what would actually be stored.
 */
public class SampleMetadataExtractionUtils {

  private SampleMetadataExtractionUtils() {
  }

  /**
   * The matched capture group inside an input string.
   *
   * @param start the start index of the group inside the input string (inclusive)
   * @param end   the end index of the group inside the input string (exclusive)
   * @param value the extracted group value
   */
  public record GroupMatch(int start, int end, @NotNull String value) {

  }

  /**
   * @param raw     the raw data file
   * @param mapping the mapping defining the input source
   * @return the input string used for regex matching, never null
   */
  public static @NotNull String inputString(@NotNull final RawDataFile raw,
      @NotNull final MetadataRegexMapping mapping) {
    return mapping.inputSource().extract(raw);
  }

  /**
   * Finds the first (case-insensitive) regex match in the input and extracts its group.
   *
   * @param mapping the mapping with the regex
   * @param input   the input string
   * @return the matched group with its position, or {@code null} if the regex does not match, is
   * invalid, or the group did not participate in the match. The first capture group is used if the
   * regex defines one, otherwise the whole match (group 0).
   */
  public static @Nullable GroupMatch firstGroupMatch(@NotNull final MetadataRegexMapping mapping,
      @Nullable final String input) {
    if (input == null || mapping.regex().isBlank()) {
      return null;
    }
    final Pattern pattern = tryCompile(mapping.regex());
    if (pattern == null) {
      return null;
    }
    final Matcher matcher = pattern.matcher(input);
    if (!matcher.find()) {
      return null;
    }
    // use the first capture group if the regex defines one, otherwise the whole match
    final int group = matcher.groupCount() >= 1 ? 1 : 0;
    return toMatch(matcher, group);
  }

  /**
   * Collects all distinct group values matched across the given files, preserving the first-seen
   * casing and order. Used to pre-fill the value mapping list.
   *
   * @param mapping the mapping with the regex and input source
   * @param raws    the raw data files to scan
   * @return distinct matched values (case-insensitive distinction)
   */
  public static @NotNull List<String> distinctMatchedValues(
      @NotNull final MetadataRegexMapping mapping, @NotNull final List<RawDataFile> raws) {
    final LinkedHashMap<String, String> seen = new LinkedHashMap<>();
    for (final RawDataFile raw : raws) {
      final GroupMatch match = firstGroupMatch(mapping, inputString(raw, mapping));
      if (match != null) {
        seen.putIfAbsent(match.value().toLowerCase(), match.value());
      }
    }
    return new ArrayList<>(seen.values());
  }

  private static @Nullable GroupMatch toMatch(@NotNull final Matcher matcher, final int group) {
    final String value = matcher.group(group);
    if (value == null) {
      return null;
    }
    return new GroupMatch(matcher.start(group), matcher.end(group), value);
  }

  /**
   * Extracts the final value that would be stored for this file: applies the capture group, the
   * case-insensitive value mappings, the drop-unmapped option, and the no-match default.
   *
   * @param mapping the mapping
   * @param input   the input string
   * @return the value to store, or {@code null} if the cell should be left empty
   */
  public static @Nullable String extractValue(@NotNull final MetadataRegexMapping mapping,
      @Nullable final String input) {
    final GroupMatch match = firstGroupMatch(mapping, input);
    if (match == null) {
      // no regex match -> use the per-row default value (blank = leave empty)
      return blankToNull(mapping.defaultValue());
    }

    final String captured = match.value();
    for (final MetadataValueMapping vm : mapping.valueMappings()) {
      if (vm.matches(captured)) {
        return blankToNull(vm.to());
      }
    }

    // unmapped value: optionally drop it when mappings are defined
    if (mapping.dropUnmapped() && !mapping.activeValueMappings().isEmpty()) {
      return null;
    }
    return blankToNull(captured);
  }

  /**
   * @param regex the regular expression
   * @return the compiled pattern or {@code null} if the regex is invalid or blank
   */
  public static @Nullable Pattern tryCompile(@Nullable final String regex) {
    if (regex == null || regex.isBlank()) {
      return null;
    }
    try {
      // decision: regex matching is case-insensitive (incl. unicode) per user request
      return Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    } catch (final PatternSyntaxException ex) {
      return null;
    }
  }

  private static @Nullable String blankToNull(@Nullable final String s) {
    return s == null || s.isBlank() ? null : s;
  }
}
