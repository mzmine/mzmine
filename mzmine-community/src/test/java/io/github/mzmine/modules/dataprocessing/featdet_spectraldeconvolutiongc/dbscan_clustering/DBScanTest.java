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

package io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.dbscan_clustering;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class DBScanTest {

  @Test
  void testCluster() {
    // 5 should go in second group because it is denser with 6.8 because 3.75 only has 2 neighbors
    // 17 is not included in cluster because it only connects to 15.9 which only has 2 neighbors
    // requires 3 neighbors at least
    double[] rawData = {1.2, 1.5, 1.5, 1.5, 1.7, 3.65, 5, 6.8, 6.8, 6.9, 9.1, 9.2, 9.2, 9.3, 10.1,
        12, 13, 15.9, 17, 20.5};
    final List<TestValue> list = Arrays.stream(rawData).mapToObj(TestValue::new).toList();

    DBScan<TestValue> dbscan = new DBScan<>(2.0, 3, TestValue::value);
    final List<List<TestValue>> clusters = dbscan.clusterSorted(list);

    assertEquals(3, clusters.size());
    assertEquals(6, clusters.get(0).size());
    assertEquals(4, clusters.get(1).size());
    assertEquals(6, clusters.get(2).size());

    assertEquals(list.subList(0, 6), clusters.get(0));
    assertEquals(list.subList(6, 10), clusters.get(1));
    assertEquals(list.subList(10, 16), clusters.get(2));

    for (List<TestValue> cluster : clusters) {
      System.out.println(
          "Cluster: " + cluster.stream().map(Objects::toString).collect(Collectors.joining(", ")));
    }
  }

  record TestValue(double value) {

    @Override
    public String toString() {
      return value + "";
    }
  }
}