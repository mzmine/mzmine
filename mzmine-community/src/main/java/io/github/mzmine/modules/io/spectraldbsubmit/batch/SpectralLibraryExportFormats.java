/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.io.spectraldbsubmit.batch;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import org.jetbrains.annotations.NotNull;

public enum SpectralLibraryExportFormats implements UniqueIdSupplier {
  json_mzmine, msp, mgf;

  @Override
  public String toString() {
    return switch (this) {
      case json_mzmine -> "mzmine json (recommended)";
      case msp -> "NIST msp";
      case mgf -> "mgf";
    };
  }

  public String getExtension() {
    return switch (this) {
      case json_mzmine -> "json";
      case msp -> "msp";
      case mgf -> "mgf";
    };
  }

  @Override
  public @NotNull String getUniqueID() {
    // do not change these values are used for import export
    return switch (this) {
      case json_mzmine -> "MZmine json (recommended)"; // this was the initial name
      case msp -> "msp";
      case mgf -> "mgf";
    };
  }
}
