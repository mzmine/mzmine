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

package util;

import io.github.mzmine.modules.dataprocessing.id_pubchemsearch.CompoundData;
import io.github.mzmine.modules.dataprocessing.id_pubchemsearch.PubChemApiClient;
import io.github.mzmine.modules.dataprocessing.id_pubchemsearch.PubChemSearch;
import io.github.mzmine.modules.dataprocessing.id_pubchemsearch.PubChemSearchResult;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.Test;

public class PubChemTest {

  @Test
  void testFormulaSearch() {
    final PubChemSearchResult result = PubChemApiClient.executeSearch(PubChemSearch.byFormula("H2O"));
    System.out.println(result.results());
  }

  @Test
  void testMassSearch() {
    final ObservableList<CompoundData> s = PubChemApiClient.executeSearch(PubChemSearch.byMassRange(18.01, 18.02)).results();
    System.out.println(s);
  }
}
