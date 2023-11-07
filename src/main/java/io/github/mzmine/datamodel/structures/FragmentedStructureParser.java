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

package io.github.mzmine.datamodel.structures;

import io.github.mzmine.util.StringUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.exception.CDKException;

public class FragmentedStructureParser {

  @Nullable
  private static MolecularStructure parseSmiles(final StructureParser parser, final String smiles) {
    SimpleMolecularStructure struc = parser.parseStructure(smiles, StructureInputType.SMILES);
    return struc != null ? struc.precomputeValues(parser) : null;
  }

  public FragmentedStructureLibrary parseFile(File file) throws CDKException, IOException {
    final StructureParser parser = new StructureParser(true);

    // format has three columns and a header
    // "smiles", "inchikey", "fragments"
    try (Stream<String> lines = Files.lines(file.toPath())) {
      FragmentedStructure[] structures = lines.skip(1).map(line -> {
        String[] split = line.split("\t");
        if (split.length < 3) {
          return null;
        }

        String smiles = split[0];
//            String inchikey = split[1];
        String fragments = split[2];

        return parseStructure(parser, smiles, fragments);
      }).filter(Objects::nonNull).toArray(FragmentedStructure[]::new);

      return new FragmentedStructureLibrary(structures);
    }
  }

  private FragmentedStructure parseStructure(final StructureParser parser, final String smiles,
      final String fragmentsListString) {
    // try parse smiles
    var structure = parseSmiles(parser, smiles);
    if (structure == null) {
      return null;
    }

    var fragments = parseFragmentsList(parser, fragmentsListString);
    return new FragmentedStructure(structure, fragments);
  }

  private List<MolecularStructure> parseFragmentsList(final StructureParser parser,
      String fragmentsListString) {
    return StringUtils.streamListString(fragmentsListString)
        .map(smiles -> parseSmiles(parser, smiles)).filter(Objects::nonNull)
        .sorted(Comparator.comparingDouble(MolecularStructure::getMonoIsotopicMass)).toList();
  }


}
