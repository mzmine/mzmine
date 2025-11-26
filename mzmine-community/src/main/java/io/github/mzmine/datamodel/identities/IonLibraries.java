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
 * Default internal {@link IonLibrary} that may not be changed by the user. The only option is to
 * duplicate the libraries and work on a copy.
 */
public class IonLibraries {

  public static final @NotNull String MZMINE_DEFAULT_NAME_DUAL_POLARITIES = "mzmine default (+/-)";
  /**
   * A default library with both positive and negative ions
   */
  public static final @NotNull IonLibrary MZMINE_DEFAULT_DUAL_POLARITY = new SimpleIonLibrary(
      MZMINE_DEFAULT_NAME_DUAL_POLARITIES, IonTypes.DEFAULT_VALUES_BOTH_POLARITIES);
  public static final @NotNull String MZMINE_DEFAULT_NAME_POS = "mzmine default (+)";
  public static final @NotNull IonLibrary MZMINE_DEFAULT_POS = new SimpleIonLibrary(
      MZMINE_DEFAULT_NAME_POS, IonTypes.DEFAULT_VALUES_POSITIVE);
  public static final @NotNull String MZMINE_DEFAULT_NAME_NEG = "mzmine default (-)";
  public static final @NotNull IonLibrary MZMINE_DEFAULT_NEG = new SimpleIonLibrary(
      MZMINE_DEFAULT_NAME_NEG, IonTypes.DEFAULT_VALUES_NEGATIVE);
  /**
   * Library names are reserved as they are mzmine internal libraries. Those libraries may change
   * between versions to provide the best possible defaults. For reproducibility, old batches would
   * still use the old library version as they save the library to xml.
   */
  public static final @NotNull List<String> RESERVED_LIBRARY_NAMES = List.of(
      MZMINE_DEFAULT_NAME_DUAL_POLARITIES, MZMINE_DEFAULT_NAME_POS, MZMINE_DEFAULT_NAME_NEG);
}
