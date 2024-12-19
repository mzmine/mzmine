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

package io.github.mzmine.util.scans.merging;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A node in the MSn or a singular node for an MS2 precursor
 *
 * @param scanByEnergy    map of energy groups to scan
 * @param allEnergiesScan all merged across energies
 */
public record SpectraMergingResultsNode(@NotNull Map<FloatGrouping, @NotNull Scan> scanByEnergy,
                                        @Nullable Scan allEnergiesScan) {

  /**
   * Map a single scan to its collision energy group
   *
   * @return the map contains one scan, allEnergiesScan is null
   */
  @NotNull
  public static SpectraMergingResultsNode ofSingleScan(@NotNull Scan scan) {
    Map<FloatGrouping, Scan> scanByEnergy = Map.of(
        FloatGrouping.of(ScanUtils.extractCollisionEnergies(scan)), scan);
    return new SpectraMergingResultsNode(scanByEnergy, null);
  }

  /**
   * Collect all scans of the same energy in a map
   *
   * @param nodes results for each MSn or MS2 node
   * @return mapping of energy group to list of scans extracted from all nodes
   */
  public static @NotNull Map<FloatGrouping, @NotNull List<Scan>> groupScansByEnergy(
      List<SpectraMergingResultsNode> nodes) {
    Map<FloatGrouping, List<Scan>> map = new HashMap<>();

    for (final SpectraMergingResultsNode node : nodes) {
      for (final Entry<FloatGrouping, Scan> entry : node.scanByEnergy.entrySet()) {
        var scans = map.computeIfAbsent(entry.getKey(), k -> new ArrayList<>());
        scans.add(entry.getValue());
      }
    }
    return map;
  }

  /**
   * @return the ALL_ENERGIES scan merged across energies or a SINGLE_ENERGY scan if there was only
   * one energy
   */
  public @NotNull Scan getAcrossEnergiesOrSingleScan() {
    return requireNonNullElse(allEnergiesScan, scanByEnergy.values().stream().findFirst().get());
  }

  public Stream<Scan> streamScanByEnergy() {
    return scanByEnergy.values().stream();
  }
}
