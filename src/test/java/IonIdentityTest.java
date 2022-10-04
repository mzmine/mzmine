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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.RowGroup;
import io.github.mzmine.datamodel.features.types.FeatureGroupType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonIdentityListType;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonNetworkLogic;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.project.ProjectManager;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test ion identity networking
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
@ExtendWith(MockitoExtension.class)
public class IonIdentityTest {

  private static final Logger logger = Logger.getLogger(IonIdentityTest.class.getName());

  @Mock
  RawDataFile raw;

  @Mock
  ProjectManager projectManager;

  @Mock
  ResultFormula formula;

  IonType hAdduct = new IonType(IonModification.H);
  IonType naAdduct = new IonType(IonModification.NA);


  @Test
  void ionIdentityTest() {
      // create feature list need project manager and project
      ModularFeatureList flist = new ModularFeatureList("A", null, raw);

      flist.addRowType(new IDType());
      flist.addRowType(new RTType());
      flist.addRowType(new MZType());
      flist.addRowType(new IonIdentityListType());
      flist.addRowType(new FeatureGroupType());

      ModularFeatureListRow rowProtonated = new ModularFeatureListRow(flist, 1);
      ModularFeatureListRow rowSodiated = new ModularFeatureListRow(flist, 2);
      // create spy instances to return mz and rt values
      double mz = 200;
      rowProtonated = spy(rowProtonated);
      rowSodiated = spy(rowSodiated);
      // return some fixed mz and rt
      doReturn(mz + hAdduct.getMassDifference()).when(rowProtonated).getAverageMZ();
      doReturn(mz + naAdduct.getMassDifference()).when(rowSodiated).getAverageMZ();

      // add rows
      flist.addRow(rowProtonated);
      flist.addRow(rowSodiated);

      List<RowGroup> groups = new ArrayList<>();
      RowGroup group = new RowGroup(List.of(raw), 0);
      groups.add(group);
      group.add(rowProtonated);
      group.add(rowSodiated);
      flist.setGroups(groups);

      // add ions to rows
      IonIdentity.addAdductIdentityToRow(new MZTolerance(1, 10), rowProtonated,
          hAdduct, rowSodiated, naAdduct);

      assertNotNull(rowProtonated.get(new IonIdentityListType()));
      assertNotNull(rowProtonated.get(IonIdentityListType.class));
      assertNotNull(rowProtonated.getIonIdentities());

      assertEquals(1, rowProtonated.getIonIdentities().size());
      assertEquals(1, rowSodiated.getIonIdentities().size());
      assertNotNull(rowProtonated.getBestIonIdentity());
      assertNotNull(rowSodiated.getBestIonIdentity());

      List<IonNetwork> nets = IonNetworkLogic.streamNetworks(flist).collect(Collectors.toList());
      assertEquals(1, nets.size());
      assertEquals(2, nets.get(0).size());

      IonNetworkLogic.renumberNetworks(flist);
      nets = IonNetworkLogic.streamNetworks(flist).collect(Collectors.toList());
      assertEquals(1, nets.size());
      assertEquals(2, nets.get(0).size());

      // recalc annotation networks
      IonNetworkLogic.recalcAllAnnotationNetworks(flist, true);
      nets = IonNetworkLogic.streamNetworks(flist).collect(Collectors.toList());
      assertEquals(1, nets.size());
      assertEquals(2, nets.get(0).size());

      // show all annotations with the highest count of links
      IonNetworkLogic.sortIonIdentities(flist, true);
      nets = IonNetworkLogic.streamNetworks(flist).collect(Collectors.toList());
      assertEquals(1, nets.size());
      assertEquals(2, nets.get(0).size());

      // test add formula
      when(formula.getIsotopeScore()).thenReturn(0.9f);
      when(formula.getRDBE()).thenReturn(4.5f);
      IonIdentity ion = rowProtonated.getBestIonIdentity();
      ion.addMolFormula(formula);
      assertEquals(0.9f, ion.getBestMolFormula().getIsotopeScore());
      assertEquals(4.5f, ion.getBestMolFormula().getRDBE());

      // setting the formula should have updated the sub properties
      // test properties in ion identity
      assertEquals(4.5f,
          rowProtonated.get(IonIdentityListType.class).get(0).getBestMolFormula().getRDBE(),
          "Cannot access formula specific types from sub types of IonIdentityModularType.class");
      assertEquals(0.9f,
          rowProtonated.get(IonIdentityListType.class).get(0).getBestMolFormula().getIsotopeScore(),
          "Cannot access formula specific types from sub types of IonIdentityModularType.class");

  }
}
