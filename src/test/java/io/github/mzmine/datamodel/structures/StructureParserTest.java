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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.exception.CDKException;

public class StructureParserTest {

  @Test
  public void testConvertSmilesToInChIKey() throws CDKException {
    // Define a SMILES representation
    String smiles = "OC(=O)CC(O)(C(=O)O)CC(=O)O";
    var parser = new StructureParser(true);
    var inchiKey = parser.parseStructure(smiles, StructureInputType.SMILES).getInChIKey(parser);

    // Define the expected InChI Key
    String expectedInchiKey = "KRKNYBCHXYNGOX-UHFFFAOYSA-N"; // Replace with the actual expected InChI Key

    // Assert that the generated InChI Key matches the expected value
    assertEquals(expectedInchiKey, inchiKey);
    assertNull(parser.parseStructure(null, StructureInputType.SMILES));
    assertNull(parser.parseStructure("Hello", StructureInputType.SMILES));
  }

  @Test
  public void testConvertInChIKeyToSmiles() throws CDKException {
    // Define the expected InChI Key
    String inchi = "InChI=1S/C24H40O5/c1-13(4-7-21(28)29)16-5-6-17-22-18(12-20(27)24(16,17)3)23(2)9-8-15(25)10-14(23)11-19(22)26/h13-20,22,25-27H,4-12H2,1-3H3,(H,28,29)/t13-,14+,15-,16-,17+,18+,19-,20+,22+,23+,24-/m1/s1";
    var parser = new StructureParser(true);
    var inchikey = parser.parseStructure(inchi, StructureInputType.INCHI).getInChIKey(parser);

    // Define a SMILES representation
    String expectedInchikey = "BHQCQFFYRZLCQQ-OELDTZBJSA-N";

    // Assert that the generated smile matches the expected value
    assertEquals(expectedInchikey, inchikey);
    assertNull(parser.parseStructure(null, StructureInputType.INCHI));
    assertNull(parser.parseStructure("Hello", StructureInputType.INCHI));
  }
}

