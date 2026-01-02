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

package io.github.mzmine.datamodel.identities;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default internal {@link IonLibrary} that may not be changed by the user. The only option is to
 * duplicate the libraries and work on a copy.
 */
public class IonLibraries {

  /**
   * A default library with both positive and negative ions
   */
  public static final @NotNull IonLibrary MZMINE_DEFAULT_DUAL_POLARITY_FULL = new SimpleIonLibrary(
      "mzmine default comprehensive (+/-)", IonTypes.DEFAULT_BOTH_POLARITIES_FULL);
  public static final @NotNull IonLibrary MZMINE_DEFAULT_POS_FULL = new SimpleIonLibrary(
      "mzmine default comprehensive (+)", IonTypes.DEFAULT_POSITIVE_FULL);
  public static final @NotNull IonLibrary MZMINE_DEFAULT_NEG_FULL = new SimpleIonLibrary(
      "mzmine default comprehensive (-)", IonTypes.DEFAULT_NEGATIVE_FULL);

  public static final @NotNull IonLibrary MZMINE_DEFAULT_DUAL_POLARITY_MAIN = new SimpleIonLibrary(
      "mzmine default main ions (+/-)", IonTypes.DEFAULT_BOTH_POLARITIES_MAIN);
  public static final @NotNull IonLibrary MZMINE_DEFAULT_POS_MAIN = new SimpleIonLibrary(
      "mzmine default main ions (+)", IonTypes.DEFAULT_POSITIVE_MAIN);
  public static final @NotNull IonLibrary MZMINE_DEFAULT_NEG_MAIN = new SimpleIonLibrary(
      "mzmine default main ions (-)", IonTypes.DEFAULT_NEGATIVE_MAIN);


  public static final @NotNull IonLibrary MZMINE_DEFAULT_DUAL_POLARITY_SMALLEST = new SimpleIonLibrary(
      "mzmine default most common ions (+/-)", IonTypes.DEFAULT_BOTH_POLARITIES_SMALLEST);

  /**
   * Already searchable library only for the main default both polarity library with polarity filter
   * activated
   */
  public static final @NotNull SearchableIonLibrary MZMINE_DEFAULT_DUAL_POLARITY_MAIN_SEARCHABLE = MZMINE_DEFAULT_DUAL_POLARITY_MAIN.toSearchableLibrary(
      true);

  /**
   * Neutral modifications library
   */
  public static final @NotNull IonLibrary MZMINE_DEFAULT_NEUTRAL_MODIFICATIONS = new SimpleIonLibrary(
      "mzmine default neutral modifications", IonTypes.DEFAULT_NEUTRAL_MODIFICATIONS);

  /**
   *
   * @return a new modifiable list of default libraries
   */
  public static List<IonLibrary> createDefaultLibrariesModifiable() {
    return new ArrayList<IonLibrary>(List.of(
        // full
        MZMINE_DEFAULT_DUAL_POLARITY_FULL, MZMINE_DEFAULT_POS_FULL, MZMINE_DEFAULT_NEG_FULL,
        // main
        MZMINE_DEFAULT_DUAL_POLARITY_MAIN, MZMINE_DEFAULT_POS_MAIN, MZMINE_DEFAULT_NEG_MAIN,
        // smallest, used for other tools where less ions are better like formula prediction
        MZMINE_DEFAULT_DUAL_POLARITY_SMALLEST,
        // NEUTRAL modifications
        MZMINE_DEFAULT_NEUTRAL_MODIFICATIONS));
  }

  /**
   * @return true if input is an internal library that may never be changed
   */
  public static boolean isInternalLibrary(@Nullable IonLibrary library) {
    return library != null && isInternalLibrary(library.name());
  }

  /**
   * @return true if input is an internal library that may never be changed
   */
  public static boolean isInternalLibrary(@Nullable String name) {
    return name != null && (name.toLowerCase().contains("mzmine default"));
  }
}
