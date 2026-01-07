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

package io.github.mzmine.modules.dataprocessing.id_online_reactivity;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.dataprocessing.id_online_reactivity.OnlineReaction.Type;
import io.github.mzmine.util.io.CsvReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OnlineReactionJsonWriterTest {

  private static final Logger logger = Logger.getLogger(
      OnlineReactionJsonWriterTest.class.getName());
  @Mock
  FeatureListRow row1;
  @Mock
  FeatureListRow row2;
  @Mock
  FeatureListRow row3;

  @BeforeEach
  void setUp() {
    Mockito.when(row1.getID()).thenReturn(1);
    Mockito.when(row2.getID()).thenReturn(2);
    Mockito.when(row3.getID()).thenReturn(3);
  }

  @Test
  void createReactivityString() throws IOException {

    File file = new File(
        getClass().getClassLoader().getResource("modules/id_online_rections/reactions_smarts.csv")
            .getFile());
    List<OnlineReaction> list = CsvReader.readToList(file, OnlineReaction.class);

    List<OnlineReactionMatch> matches = new ArrayList<>();
    for (final OnlineReaction reaction : list) {
      // just for testing row 1 will be both a product and an educt
      matches.add(new OnlineReactionMatch(row1, row2, reaction, Type.Educt));
      matches.add(new OnlineReactionMatch(row1, row3, reaction, Type.Product));
    }

    OnlineReactionJsonWriter writer = new OnlineReactionJsonWriter(false);
    String reactivityString = writer.createReactivityString(row1, matches);

    logger.info(reactivityString);

    // format is required by SIRIUS and GNPS mgf
    String result = "[{\"compound_type\":\"Educt\",\"reaction_name\":\"Hydroxylamine\",\"filename_contains\":\"Hydroxylamine\",\"educt_smarts\":\"O=[CH1]-[CH2]C([C,H])[C,H]\",\"reaction_smarts\":\"O=[CH1]-[CH2]C([C,H])[C,H].NO>>[#1,#6]C([#1,#6])C/C=N/O.O\",\"delta_mz\":15.0109,\"linked_ids\":[2,3]},{\"compound_type\":\"Educt\",\"reaction_name\":\"L-cysteine\",\"filename_contains\":\"cysteine\",\"educt_smarts\":\"[C]C(C([C,H,O])=C([C,H,O])[C,H,O])=O\",\"reaction_smarts\":\"[C]C(C([C,H,O])=C([C,H,O])[C,H,O])=O.N[C@@H](CS)C(O)=O>>[C]C(C(C([#1,#6,#8])(SC[C@@H](C(O)=O)N)[#1,#6,#8])[#1,#6,#8])=O\",\"delta_mz\":121.01975,\"linked_ids\":[2,3]},{\"compound_type\":\"Educt\",\"reaction_name\":\"L-cysteine\",\"filename_contains\":\"cysteine\",\"educt_smarts\":\"O=C1OC([*])C1[*]\",\"reaction_smarts\":\"O=C1OC([*])C1[*].N[C@@H](CS)C(O)=O>>O=C(SC[C@@H](C(O)=O)N)C([*])C([*])O\",\"delta_mz\":121.01975,\"linked_ids\":[2,3]},{\"compound_type\":\"Educt\",\"reaction_name\":\"L-cysteine\",\"filename_contains\":\"cysteine\",\"educt_smarts\":\"[CH2]=C([C,c,H])[C,c,H]\",\"reaction_smarts\":\"[CH2]=C([C,c,H])[C,c,H].N[C@@H](CS)C(O)=O>>[#1,#6]C(CSC[C@H](N)C(O)=O)[#1,#6]\",\"delta_mz\":121.01975,\"linked_ids\":[2,3]},{\"compound_type\":\"Educt\",\"reaction_name\":\"L-cysteine\",\"filename_contains\":\"cysteine\",\"educt_smarts\":\"c12c([*])c([*])c([*])c([*])c1C(=O)c3c([*])c([*])c([*])c([*])c3C2=O\",\"reaction_smarts\":\"c12c([*])c([*])c([*])c([*])c1C(=O)c3c([*])c([*])c([*])c([*])c3C2=O.N[C@@H](CS)C(O)=O>>[*]c1c([*])(SC[C@@H](C(O)=O)N)c([*])c([*])c(C(c2c3c([*])c([*])c([*])c2[*])=O)c1C3=O\",\"delta_mz\":121.01975,\"linked_ids\":[2,3]},{\"compound_type\":\"Educt\",\"reaction_name\":\"L-cysteine\",\"filename_contains\":\"cysteine\",\"educt_smarts\":\"O=C-1-C([C,H,O])=C([C,H,O])-C(=[O])-c:2:c([*]):c([*]):c([*]):c([*]):c-1:2\",\"reaction_smarts\":\"O=C-1-C([C,H,O])=C([C,H,O])-C(=[O])-c:2:c([*]):c([*]):c([*]):c([*]):c-1:2.N[C@@H](CS)C(O)=O>>O=C1C(C(C(c2c1c([*])c([*])c([*])c2[*])=O)[#1,#6,#8])(SC[C@@H](C(O)=O)N)[#1,#6,#8]\",\"delta_mz\":121.01975,\"linked_ids\":[2,3]},{\"compound_type\":\"Educt\",\"reaction_name\":\"L-cysteine\",\"filename_contains\":\"cysteine\",\"educt_smarts\":\"O=C(O[C,H])C([C,H,O])=C([C,H,O])[C,H,O]\",\"reaction_smarts\":\"O=C(O[C,H])C([C,H,O])=C([C,H,O])[C,H,O].N[C@@H](CS)C(O)=O>>O=C(C(C([#1,#6,#8])(SC[C@@H](C(O)=O)N)[#1,#6,#8])[#1,#6,#8])O[#1,#6]\",\"delta_mz\":121.01975,\"linked_ids\":[2,3]},{\"compound_type\":\"Educt\",\"reaction_name\":\"AQC\",\"filename_contains\":\"AQC\",\"educt_smarts\":\"[C]N([H])[C,H]\",\"reaction_smarts\":\"[C]N([H])[C,H].O=C(N1OC(NC2=CC3=C(N=CC=C3)C=C2)=O)CCC1=O>>[C]N(C(NC1=CC2=C(C=C1)N=CC=C2)=O)[#1,#6].O=C(N1O)CCC1=O\",\"delta_mz\":170.0481,\"linked_ids\":[2,3]},{\"compound_type\":\"Educt\",\"reaction_name\":\"AQC\",\"filename_contains\":\"AQC\",\"educt_smarts\":\"[C]N(O([H]))C([C])=O\",\"reaction_smarts\":\"[C]N(O([H]))C([C])=O.O=C(N1OC(NC2=CC3=C(N=CC=C3)C=C2)=O)CCC1=O>>[C]N(C([C])=O)OC(NC1=CC2=C(C=C1)N=CC=C2)=O.O=C(N1O)CCC1=O\",\"delta_mz\":170.0481,\"linked_ids\":[2,3]},{\"compound_type\":\"Educt\",\"reaction_name\":\"AQC\",\"filename_contains\":\"AQC\",\"educt_smarts\":\"O([H])n1c([*])c([*])c([*])c1([*])\",\"reaction_smarts\":\"O([H])n1c([*])c([*])c([*])c1([*]).O=C(N1OC(NC2=CC3=C(N=CC=C3)C=C2)=O)CCC1=O>>O=C(On1c([*])c([*])c([*])c1[*])NC2=CC3=C(C=C2)N=CC=C3.O=C(N1O)CCC1=O\",\"delta_mz\":170.0481,\"linked_ids\":[2,3]}]";
    Assertions.assertEquals(result, reactivityString);
  }
}