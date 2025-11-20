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

package io.github.mzmine.datamodel.identities;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Use {@link #toSearchableLibrary(boolean)} for optimized version for searches
 */
public class SimpleIonLibrary implements IonLibrary {

  private final @NotNull String name;
  private final List<IonType> ions;
  private final List<IonPart> parts;

  public SimpleIonLibrary(@NotNull String name, @NotNull List<IonType> ions,
      @NotNull List<IonPart> parts) {
    this.name = name;
    this.ions = ions;
    this.parts = parts;
  }

  public SimpleIonLibrary(@NotNull String name, @NotNull List<IonType> ions) {
    this(name, ions, IonTypeUtils.extractUniqueParts(ions));
  }

  @Override
  public @NotNull String getName() {
    return name;
  }

  @Override
  @NotNull
  public List<IonType> getIons() {
    return ions;
  }

  @Override
  @NotNull
  public List<IonPart> getParts() {
    return parts;
  }

  @Override
  public String toString() {
    return "%s (%d ions)".formatted(name, ions.size());
  }
}
