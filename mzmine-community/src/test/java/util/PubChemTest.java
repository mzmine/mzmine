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

import com.google.common.collect.Lists;
import io.github.mzmine.modules.dataprocessing.id_pubchemsearch.CompoundData;
import io.github.mzmine.modules.dataprocessing.id_pubchemsearch.PubChemApiClient;
import io.github.mzmine.modules.dataprocessing.id_pubchemsearch.PubChemApiClient.PubChemApiException;
import io.github.mzmine.modules.dataprocessing.id_pubchemsearch.PubChemSearch;
import io.github.mzmine.modules.dataprocessing.id_pubchemsearch.PubChemSearchResult;
import io.github.mzmine.taskcontrol.TaskService;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.IOException;
import java.util.List;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.Test;

public class PubChemTest {

  @Test
  void testFormulaSearch() throws PubChemApiException, IOException, InterruptedException {

    try (PubChemApiClient client = new PubChemApiClient()) {

      final PubChemSearch search = PubChemSearch.byFormula("H2O");
      final List<String> cids = client.findCids(search);
      final List<List<String>> chunked = Lists.partition(cids,
          PubChemApiClient.DEFAULT_CID_CHUNK_SIZE);
      List<CompoundData> s = chunked.stream().map(chunk -> {
        try {
          return client.fetchPropertiesForChunk(search, chunk);
        } catch (PubChemApiException e) {
          throw new RuntimeException(e);
        } catch (IOException e) {
          throw new RuntimeException(e);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }).flatMap(List::stream).toList();

      System.out.println(s);
    }
  }

  @Test
  void testMassSearch() {

    final PubChemSearchResult result = PubChemApiClient.runAsync(
        PubChemSearch.byMassRange(18.01, 18.02), TaskService.getController().getExecutor());

    while(result.status().getValue() != TaskStatus.FINISHED) {
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      System.out.println(result.status().getValue());
    }

    System.out.println(result.results());
  }
}
