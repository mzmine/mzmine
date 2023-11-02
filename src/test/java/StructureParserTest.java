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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mzmine.util.structure.StructureInputType;
import io.github.mzmine.util.structure.StructureParser;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.exception.CDKException;

public class StructureParserTest {

  @Test
  public void testConvertSmilesToInChIKey() throws CDKException {
    // Define a SMILES representation
    String smiles = "OC(=O)CC(O)(C(=O)O)CC(=O)O";
    var parser = new StructureParser();
    var inchiKey = parser.getInChIKey(smiles, StructureInputType.SMILES);

    // Define the expected InChI Key
    String expectedInchiKey = "KRKNYBCHXYNGOX-UHFFFAOYSA-N"; // Replace with the actual expected InChI Key

    // Assert that the generated InChI Key matches the expected value
    assertEquals(expectedInchiKey, inchiKey);
    assertNull(parser.getInChIKey(null, StructureInputType.SMILES));
    assertNull(parser.getInChIKey("Hello", StructureInputType.SMILES));
  }

  @Test
  public void testConvertInChIKeyToSmiles() throws CDKException {
    // Define the expected InChI Key
    String InchiKey = "KRKNYBCHXYNGOX-UHFFFAOYSA-N";
    var parser = new StructureParser();
    var smiles = parser.getInChIKey(InchiKey, StructureInputType.INCHI);

    // Define a SMILES representation
    String expectedSmiles = "OC(=O)CC(O)(C(=O)O)CC(=O)O";

    // Assert that the generated smile matches the expected value
    assertEquals(expectedSmiles, smiles);
    assertNull(parser.getInChIKey(null, StructureInputType.INCHI));
    assertNull(parser.getInChIKey("Hello", StructureInputType.INCHI));
  }
}

