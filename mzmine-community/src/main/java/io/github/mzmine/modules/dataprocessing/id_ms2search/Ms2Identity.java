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

package io.github.mzmine.modules.dataprocessing.id_ms2search;

import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.impl.SimpleFeatureIdentity;
import io.github.mzmine.main.MZmineCore;

public class Ms2Identity extends SimpleFeatureIdentity {

  public Ms2Identity(final Feature featureA, final Feature featureB, Ms2SearchResult searchResult) {

    super("MS2similarity" + " m/z:"
        + MZmineCore.getConfiguration().getMZFormat().format(featureB.getMZ()) + " RT:"
        + MZmineCore.getConfiguration().getRTFormat().format(featureB.getRT()) + " Score:"
        + String.format("%3.1e", searchResult.getScore()) + " NumIonsMatched:"
        + searchResult.getNumIonsMatched() + " MatchedIons:"
        + searchResult.getMatchedIonsAsString());

    setPropertyValue(PROPERTY_METHOD, "MS2 search");
  }
}
