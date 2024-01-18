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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.exception.CDKException;

class FragmentedStructureParserTest {

  private static final Logger logger = Logger.getLogger(
      FragmentedStructureParserTest.class.getName());
  File file = new File(
      "D:\\git\\annotation_validation\\metadata_validator\\metadata\\fragments_nih.tsv");


  @Test
  public void testParseList() {
    String list = "['COC(=O)C1C(O)CCC2CN3CCc4c([nH]c5ccccc45)C3CC21.[CH3+]', 'C=CCCC(O)C(/C=C/C1NCCc2c1[nH]c1ccccc21)C(=[O+])OC', 'C=CCCC(O)C([CH][CH+]C1NCCc2c1[nH]c1ccccc21)C(=O)OC']";

    var empty = list.replaceAll("\\['", "") //
        .replaceAll("']", "") //
        .replaceAll("', '", ",") //
        .split(",");
    logger.info(String.join("\n", empty));
    assertEquals(3, empty.length);
  }

  @Test
  void parseFile() throws CDKException, IOException {
    FragmentedStructureParser parser = new FragmentedStructureParser();
    Map<String, FragmentedStructure> map = parser.parseFile(file).createInchiKeyMap();

    logger.info("Elements: " + map.size());
    assertEquals(5, map.size());
    assertTrue(map.containsKey("XGPBRZDOJDLKOT-NXIDYTHLSA-N"));
    assertEquals(7860, map.get("XGPBRZDOJDLKOT-NXIDYTHLSA-N").fragments().size());
  }
}