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

package io.github.mzmine.datamodel.data_access;

import io.github.mzmine.gui.preferences.VendorImportParameters;
import org.jetbrains.annotations.NotNull;

/**
 * Helper to import raw data with an explicit vendor option, so tests do not depend on the current
 * default of {@link VendorImportParameters#applyTimsPressureCompensation}.
 */
public final class VendorImportParametersTestUtils {

  private VendorImportParametersTestUtils() {
  }

  /**
   * @param pressureCompensation Whether Bruker per-frame pressure compensation is applied on
   *                             import.
   * @return Default vendor import parameters with the pressure compensation set explicitly.
   */
  @NotNull
  public static VendorImportParameters withPressureCompensation(
      final boolean pressureCompensation) {
    final VendorImportParameters params = VendorImportParameters.createDefault();
    params.setParameter(VendorImportParameters.applyTimsPressureCompensation, pressureCompensation);
    return params;
  }
}
