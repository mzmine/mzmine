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

package io.github.mzmine.modules.tools.timstofmaldiacq;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class CeSteppingTables {

  private static final Logger logger = Logger.getLogger(CeSteppingTables.class.getName());

  private final List<CeSteppingTable> tables;

  public CeSteppingTables(List<Double> collisionEnergies, Double isolationWidth) {
    tables = collisionEnergies.stream().map(e -> new CeSteppingTable(e, isolationWidth)).toList();
  }

  public int getNumberOfCEs() {
    return tables.size();
  }

  public boolean writeCETable(int index, @NotNull final File file) {
    return tables.get(index).writeCETable(file);
  }

  public Double getCE(int index) {
    return tables.get(index).collisionEnergy();
  }

  public Map<Double, CeSteppingTable> asMap() {
    return tables.stream()
        .collect(HashMap::new, (map, table) -> map.put(table.collisionEnergy(), table),
            HashMap::putAll);
  }

  public List<CeSteppingTable> asList() {
//    return tables.stream().collect(
//        HashMap::new,  (map, table) -> map.put(table.collisionEnergy(), table),
//        HashMap::putAll);
    return List.copyOf(tables);
  }

  public record CeSteppingTable(Double collisionEnergy, Double isolationWidth) {

    public boolean writeCETable(@NotNull final File file) {
      var isoString = String.format("%.1f", this.isolationWidth);
      final String header = "mass,iso_width,ce,charge,type";
      final String line1 = "100.0," + isoString + "," + String.valueOf(collisionEnergy) + ",1,0";
      final String line2 = "500.0," + isoString + "," + String.valueOf(collisionEnergy) + ",1,0";
      final String line3 = "1000.0," + isoString + "," + String.valueOf(collisionEnergy) + ",1,0";

      try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {

        writer.write(header);
        writer.newLine();
        writer.write(line1);
        writer.newLine();
        writer.write(line2);
        writer.newLine();
        writer.write(line3);
        writer.newLine();
        writer.flush();
        writer.close();
      } catch (IOException e) {
        logger.log(Level.WARNING, e.getMessage(), e);
        return false;
      }
      return true;
    }
  }
}
