/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.id_localcsvsearch;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.parameters.parametertypes.combowithinput.ComboWithStringInputValue;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class LocalCSVDatabaseSearchParametersTest {

  @Test
  void ignoredAdditionalColumnsStayValidAfterCloning() {
    final LocalCSVDatabaseSearchParameters parameters = new LocalCSVDatabaseSearchParameters();
    parameters.setParameter(LocalCSVDatabaseSearchParameters.extraColumns,
        new ComboWithStringInputValue<>(HandleExtraColumnsOptions.IGNORE, null));

    assertIgnoredAdditionalColumnsAreValid(parameters);
    assertIgnoredAdditionalColumnsAreValid(
        (LocalCSVDatabaseSearchParameters) parameters.cloneParameterSet());
  }

  private static void assertIgnoredAdditionalColumnsAreValid(
      final LocalCSVDatabaseSearchParameters parameters) {
    final var additionalColumns = parameters.getParameter(
        LocalCSVDatabaseSearchParameters.extraColumns);
    final var errors = new ArrayList<String>();

    assertTrue(additionalColumns.checkValue(errors));
    assertTrue(errors.isEmpty());

    // The embedded string intentionally has no value for IGNORE. It must only be checked when
    // IMPORT_SPECIFIC is selected.
    assertFalse(additionalColumns.getEmbeddedParameter().checkValue(new ArrayList<>()));
  }
}
