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

package datamodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListScans;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FeatureListScansTest {

  private static final ScanSelection POS = new ScanSelection(1, PolarityType.POSITIVE);
  private static final ScanSelection NEG = new ScanSelection(1, PolarityType.NEGATIVE);

  // distinct (by identity) scan lists so assertSame can tell them apart
  private static List<Scan> newScans() {
    return new ArrayList<>();
  }

  @Test
  void singleSelectionResolvesWithoutSelectionKey() {
    final FeatureListScans scans = new FeatureListScans();
    final RawDataFile file = RawDataFile.createDummyFile();
    final List<Scan> list = newScans();
    scans.setScans(POS, file, list);

    // a row with unknown selection still resolves to the file's sole selection
    assertSame(list, scans.getScans(null, file));
    assertSame(list, scans.getScansForFile(file));
    assertSame(list, scans.getScans(POS, file));
  }

  @Test
  void twoSelectionsResolveByExactKeyAndPolarity() {
    final FeatureListScans scans = new FeatureListScans();
    final RawDataFile file = RawDataFile.createDummyFile();
    final List<Scan> posList = newScans();
    final List<Scan> negList = newScans();
    scans.setScans(POS, file, posList);
    scans.setScans(NEG, file, negList);

    // exact key
    assertSame(posList, scans.getScans(POS, file));
    assertSame(negList, scans.getScans(NEG, file));

    // file-only is ambiguous with two selections
    assertNull(scans.getScansForFile(file));

    // exact miss but same polarity -> resolves by polarity. Different ms level so not equal to POS.
    final ScanSelection otherPositive = new ScanSelection(2, PolarityType.POSITIVE);
    assertSame(posList, scans.getScans(otherPositive, file));
  }

  @Test
  void stableRegistryIndexing() {
    final FeatureListScans scans = new FeatureListScans();
    final RawDataFile file = RawDataFile.createDummyFile();
    scans.setScans(POS, file, newScans());
    scans.setScans(NEG, file, newScans());

    assertEquals(0, scans.indexOf(POS));
    assertEquals(1, scans.indexOf(NEG));
    assertEquals(POS, scans.getSelectionByIndex(0));
    assertEquals(NEG, scans.getSelectionByIndex(1));
    assertEquals(-1, scans.indexOf(new ScanSelection(3, PolarityType.POSITIVE)));
    assertNull(scans.getSelectionByIndex(5));
  }

  @Test
  void largestScanCountAcrossSelections() {
    final FeatureListScans scans = new FeatureListScans();
    final RawDataFile fileA = RawDataFile.createDummyFile();
    final RawDataFile fileB = RawDataFile.createDummyFile();
    final List<Scan> three = new ArrayList<>();
    three.add(null);
    three.add(null);
    three.add(null);
    final List<Scan> one = new ArrayList<>();
    one.add(null);
    scans.setScans(POS, fileA, three);
    scans.setScans(POS, fileB, one);

    assertEquals(3, scans.largestScanCount(fileA));
    assertEquals(1, scans.largestScanCount(fileB));
    assertEquals(3, scans.largestScanCount(null));
  }

  @Test
  void rowSelectionDrivesScanResolutionInFeatureList() {
    final RawDataFile file = RawDataFile.createDummyFile();
    final ModularFeatureList flist = new ModularFeatureList("test", null, file);
    final List<Scan> posList = newScans();
    final List<Scan> negList = newScans();
    flist.setSelectedScans(file, POS, posList);
    flist.setSelectedScans(file, NEG, negList);

    final ModularFeatureListRow posRow = new ModularFeatureListRow(flist, 1);
    posRow.setScanSelection(POS);
    final ModularFeatureListRow negRow = new ModularFeatureListRow(flist, 2);
    negRow.setScanSelection(NEG);

    // the row's selection (auto-stored as a hidden row type) resolves the right scan list per file
    assertSame(POS, posRow.getScanSelection());
    assertSame(NEG, negRow.getScanSelection());
    assertSame(posList, flist.getScans(posRow.getScanSelection(), file));
    assertSame(negList, flist.getScans(negRow.getScanSelection(), file));
  }

  @Test
  void replaceRawDataFileKeepsSelections() {
    final FeatureListScans scans = new FeatureListScans();
    final RawDataFile oldFile = RawDataFile.createDummyFile();
    final RawDataFile newFile = RawDataFile.createDummyFile();
    final List<Scan> list = newScans();
    scans.setScans(POS, oldFile, list);

    final List<Scan> mapped = newScans();
    scans.replaceRawDataFile(oldFile, newFile, _ -> mapped);

    assertNull(scans.getScans(POS, oldFile));
    assertSame(mapped, scans.getScans(POS, newFile));
    assertEquals(0, scans.indexOf(POS)); // selection registry unchanged
  }
}
