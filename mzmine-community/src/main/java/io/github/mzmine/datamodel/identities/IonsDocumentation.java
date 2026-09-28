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

/**
 * Links into the online documentation page that describes ion libraries, ion types, and ion
 * building blocks. Kept in one place so that the anchors stay in sync with the documentation page.
 */
public final class IonsDocumentation {

  /**
   * The full ion types and libraries page
   */
  public static final String IONS_PAGE = "https://mzmine.github.io/mzmine_documentation/ions/ions.html";

  /**
   * Notation used to write molecular formulas and ion types
   */
  public static final String NOTATION = IONS_PAGE + "#notation";

  /**
   * Manage ion libraries: remove, copy, create, import, export
   */
  public static final String DEFINE_LIBRARIES = IONS_PAGE + "#define-libraries";

  /**
   * Create and edit a library and define its ion types
   */
  public static final String DEFINE_TYPES = IONS_PAGE + "#define-types";

  /**
   * Define ion building blocks
   */
  public static final String DEFINE_PARTS = IONS_PAGE + "#define-parts";

  /**
   * Select an ion library in a module parameter
   */
  public static final String LIBRARY_PARAMETER = IONS_PAGE + "#ion-library-parameter";

  private IonsDocumentation() {
  }
}
