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

package io.github.mzmine.datamodel.features.rawfiletypes;

import io.github.mzmine.datamodel.features.types.DataType;
import org.jetbrains.annotations.NotNull;

/**
 * Tags a {@link DataType} with the controlled vocabulary term (PSI-MS by default) it represents,
 * e.g., instrument serial number MS:1000529. The value of the data type is stored as a plain value
 * (String, Double, ...), not as a CV term. Not every raw file type needs to be a CV term type.
 */
public interface CvTermType {

  /**
   * @return the accession, e.g. MS:1000529
   */
  @NotNull String getCvAccession();

  /**
   * @return the name of the term in the ontology, e.g. instrument serial number
   */
  @NotNull String getCvName();

  /**
   * @return the id of the controlled vocabulary as used in mzML cvRef
   */
  default @NotNull String getCvRef() {
    return "MS";
  }
}
