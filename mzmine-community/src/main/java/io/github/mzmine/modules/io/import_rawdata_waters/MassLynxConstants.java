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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.io.import_rawdata_waters;

import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.nio.file.Path;

public class MassLynxConstants {

  public static final float DEFAULT_FLOAT = -1.0f;
  public static final int DEFAULT_INT = -1;

  public static final float NO_PRECURSOR = DEFAULT_FLOAT;
  public static final float NO_QUAD_ISOLATION = DEFAULT_FLOAT;
  public static final float NO_COLLISION_ENERGY = DEFAULT_FLOAT;
  public static final float NO_MOBILITY = DEFAULT_FLOAT;
  public static final int NO_DRIFT_SCANS = DEFAULT_INT;
  public static final int NO_LOCKMASS_FUNCTION = DEFAULT_INT;
  public static final float NO_RT = DEFAULT_FLOAT;

}
