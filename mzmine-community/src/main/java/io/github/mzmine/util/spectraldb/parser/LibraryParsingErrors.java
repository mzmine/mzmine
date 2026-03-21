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

package io.github.mzmine.util.spectraldb.parser;

import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class LibraryParsingErrors {

  private final String library;
  private final int maxErrors;
  /**
   * counts how often a key was found but is unknown to mzmine
   */
  private final Map<String, Integer> unknownKeys = new HashMap<>();
  private final Map<DBEntryField, LibraryValueError> valueErrors = new HashMap<>();
  /**
   * count exceptions with the same message (often different if there is a specific word in there)
   */
  private final Map<String, Integer> unknownExceptions = new HashMap<>();

  public LibraryParsingErrors(@NotNull String library) {
    this(library, 500);
  }

  public LibraryParsingErrors(@NotNull String library, int maxErrors) {
    this.library = library;
    this.maxErrors = maxErrors;
  }

  public int addUnknownKey(String key) {
    // if max errors reached only count already known errors
    // otherwise the log and error list may overflow
    if (unknownKeys.size() >= maxErrors && !unknownKeys.containsKey(key)) {
      return unknownKeys.size();
    }

    return unknownKeys.compute(key, (_, counter) -> counter == null ? 1 : counter + 1);
  }

  public int addUnknownException(String message) {
    // if max errors reached only count already known errors
    // otherwise the log and error list may overflow
    if (unknownExceptions.size() >= maxErrors && !unknownExceptions.containsKey(message)) {
      return unknownExceptions.size();
    }

    return unknownExceptions.compute(message, (_, counter) -> counter == null ? 1 : counter + 1);
  }

  /**
   *
   * @param field      the field
   * @param fieldKey   the field key
   * @param valueError the value that created the parsing error
   * @return number of errors for this key including the current error
   */
  public int addValueParsingError(@NotNull DBEntryField field, @NotNull String fieldKey,
      @NotNull String valueError) {
    final LibraryValueError errorCollector = valueErrors.computeIfAbsent(field,
        _ -> new LibraryValueError(field, fieldKey));

    // only add a 10th of value parsing errors as there are many fields with each potential parsing issues
    errorCollector.addError(maxErrors / 10, valueError);
    return errorCollector.getTotalErrors();
  }

  @Override
  public String toString() {
    final String valueParsingErrors = valueErrors.entrySet().stream().sorted(Entry.comparingByKey())
        .map(Entry::getValue).map(LibraryValueError::toString).collect(Collectors.joining("\n"));

    // sorted alphabetically to spot typos
    final String unknownKeysString = unknownKeys.entrySet().stream().sorted(Entry.comparingByKey())
        .map(e -> "'%s' (%d)".formatted(e.getKey(), e.getValue()))
        .collect(Collectors.joining("\n"));

    final String unknownExceptionsString = unknownExceptions.entrySet().stream()
        .sorted(Entry.comparingByKey()).map(e -> "'%s' (%d)".formatted(e.getKey(), e.getValue()))
        .collect(Collectors.joining("\n"));

    String message = """
        Library "%s" parsing results and summary:
        """.formatted(library);

    List<String> summary = new ArrayList<>(3);

    if (!unknownKeys.isEmpty()) {
      summary.add("""
          Unknown keys %d:
          %s""".formatted(unknownKeys.size(), unknownKeysString));
    }
    if (!valueErrors.isEmpty()) {
      summary.add("""
          Metadata value parsing errors %d:
          %s""".formatted(valueErrors.size(), valueParsingErrors));
    }
    if (!valueErrors.isEmpty()) {
      summary.add("""
          Unknown errors %d:
          %s""".formatted(unknownExceptions.size(), unknownExceptionsString));
    }
    if (summary.isEmpty()) {
      return message + "All parsed without issues";
    } else {
      return message + String.join("\n", summary);
    }
  }

  public String toStringShort() {
    // sorted alphabetically to spot typos
    final String unknownKeysString = unknownKeys.entrySet().stream().sorted(Entry.comparingByKey())
        .map(e -> "'%s' (%d)".formatted(e.getKey(), e.getValue()))
        .collect(Collectors.joining("\n"));

    return """ 
        Library "%s" parsing results and summary:
        Metadata value parsing errors %d.
        Unknown errors %d.
        Unknown keys %d:
        %s""".formatted(library, valueErrors.size(), unknownExceptions.size(), unknownKeys.size(),
        unknownKeysString);

  }

}
