/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.datamodel.features.types.annotations;

import io.github.mzmine.datamodel.features.types.abstr.UrlType;
import org.jetbrains.annotations.NotNull;

/**
 * URL to MASST job on GNPS. MASST is the mass spectrometry search tool. e.g. <a
 * href="https://gnps.ucsd.edu/ProteoSAFe/status.jsp?task=fa0437e82d0a4a4493c8c2dcb4977c07">https://gnps.ucsd.edu/ProteoSAFe/status.jsp?task=fa0437e82d0a4a4493c8c2dcb4977c07</a>
 *
 * @author Robin Schmid (<a
 * href="https://github.com/robinschmid">https://github.com/robinschmid</a>)
 */
public class MasstUrlType extends UrlType {

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "masst_url";
  }

  @NotNull
  @Override
  public String getHeaderString() {
    return "MASST";
  }
}
