/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_fragment_structure_annotation;

import io.github.mzmine.datamodel.structures.FragmentedStructure;
import io.github.mzmine.datamodel.structures.FragmentedStructureParser;
import io.github.mzmine.datamodel.structures.MolecularStructure;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.exception.CDKException;

class SpectraFragmentStructureAnnotatorTest {

  File file = new File(
      "D:\\git\\annotation_validation\\metadata_validator\\metadata\\fragments_nih.tsv");


  @Test
  void annotateSpectralSignals() throws CDKException, IOException {
    FragmentedStructureParser parser = new FragmentedStructureParser();
    Map<String, FragmentedStructure> map = parser.parseFile(file).createInchiKeyMap();

    FragmentedStructure structure = map.get("XGPBRZDOJDLKOT-NXIDYTHLSA-N");

    List<MolecularStructure> frags = structure.fragments();
    double[] mzs = Stream.of(frags.get(10), frags.get(55), frags.get(120), frags.get(150),
            frags.get(350), frags.get(900), frags.get(1560), frags.get(2000), frags.get(2500),
            frags.get(5000), frags.get(7400)).mapToDouble(MolecularStructure::getMonoIsotopicMass)
        .toArray();

    SpectraFragmentStructureAnnotator annotator = new SpectraFragmentStructureAnnotator(
        new MZTolerance(0.008, 20));
    Map<Integer, MolecularStructure> annotations = annotator.annotateSpectralSignals(map, mzs,
        "XGPBRZDOJDLKOT-NXIDYTHLSA-N");

    Assertions.assertEquals(mzs.length, annotations.size());

    List<Double> added = new ArrayList<>();
  }
}