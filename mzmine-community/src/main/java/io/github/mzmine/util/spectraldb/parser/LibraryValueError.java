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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public record LibraryValueError(DBEntryField field, String fieldKey, Set<String> uniqueErrorValues,
                                AtomicInteger totalErrors) {

  public LibraryValueError(DBEntryField field, String fieldKey) {
    this(field, fieldKey, new HashSet<>(), new AtomicInteger(0));
  }

  public int getTotalErrors() {
    return totalErrors.get();
  }

  public int getUniqueErrors() {
    return uniqueErrorValues.size();
  }

  public void addError(int maxErrors, @NotNull String valueError) {
    totalErrors.incrementAndGet();
    if (uniqueErrorValues.size() < maxErrors) {
      uniqueErrorValues.add(valueError);
    }
  }

  @Override
  public @NotNull String toString() {
    final String uniqueErrorsString = uniqueErrorValues.stream().map(s -> "'" + s + "'")
        .collect(Collectors.joining(", "));
    return "%s as '%s' had %d (%d unique) value parsing errors (%s)".formatted(field, fieldKey,
        getTotalErrors(), getUniqueErrors(), uniqueErrorsString);
  }
}
