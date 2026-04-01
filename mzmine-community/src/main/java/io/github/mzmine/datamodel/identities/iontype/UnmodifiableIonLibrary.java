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

package io.github.mzmine.datamodel.identities.iontype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * A simple unmodifiable ion library used by the parameter. Use
 * {@link #toSearchableLibrary(boolean)} for an optimized version for searches.
 * <p>
 * The ions are in an unmodifiable list.
 */
public class UnmodifiableIonLibrary implements IonLibrary {

  private static final Logger logger = Logger.getLogger(UnmodifiableIonLibrary.class.getName());
  private final @NotNull String name;
  private final @NotNull List<IonType> ions;

  /**
   * @param skipNameCheck name check can only be skipped by classes in this package to create
   *                      default libraries. Outside, there will always be a name check.
   */
  UnmodifiableIonLibrary(boolean skipNameCheck, @NotNull String name, @NotNull List<IonType> ions) {
    if (!skipNameCheck && IonLibraries.isInternalLibrary(name)) {
      // use try catch to get stack trace
      // users might load a library with mzmine default in name
      // maybe even old default libraries from former versions that are selected in the parameterset
      try {
        throw new IllegalArgumentException(
            "The chosen name '%s' contains the part '%s', which is reserved for internal default libraries. Will use 'Unnamed' instead.".formatted(
                name, IonLibraries.RESERVED_NAME));
      } catch (Exception e) {
        logger.log(Level.WARNING, e.getMessage(), e);
      }
      name = "unnamed library";
    }
    this.name = name;
    List<IonType> sorted = new ArrayList<>(ions);
    sorted.sort(IonTypeSorting.MOLECULES_THEN_CHARGE_THEN_MASS.getComparator());
    this.ions = Collections.unmodifiableList(sorted);
  }

  /**
   * Create a new library and check if name is valid
   */
  public UnmodifiableIonLibrary(@NotNull String name, @NotNull List<IonType> ions) {
    this(false, name, ions);
  }

  /**
   * Option to create internal default libraries within this package
   */
  static UnmodifiableIonLibrary createInternal(@NotNull String name, @NotNull List<IonType> ions) {
    return new UnmodifiableIonLibrary(true, name, ions);
  }


  @Override
  @NotNull
  public List<IonType> ions() {
    return ions;
  }

  @Override
  public @NotNull IonLibrary copy() {
    return new UnmodifiableIonLibrary(true, name, ions);
  }

  @Override
  @NotNull
  public String toString() {
    return "%s (%d ions)".formatted(name, ions.size());
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof UnmodifiableIonLibrary o)) {
      return false;
    }

    return name.equals(o.name) && equalIons(o);
  }

  @Override
  public @NotNull String name() {
    return name;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, ions);
  }


}
