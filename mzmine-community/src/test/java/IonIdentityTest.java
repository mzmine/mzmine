/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
import io.github.mzmine.datamodel.features.correlation.RowGroup;
import io.github.mzmine.datamodel.features.correlation.RowGroupFull;
import io.github.mzmine.datamodel.features.types.FeatureGroupType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonIdentityListType;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.identities.IonType;
import io.github.mzmine.datamodel.identities.IonTypes;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonNetworkLogic;
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

  IonType hAdduct = IonTypes.H.asIonType();
  IonType naAdduct = IonTypes.NA.asIonType();


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
    doReturn(mz + hAdduct.totalMass()).when(rowProtonated).getAverageMZ();
    doReturn(mz + naAdduct.totalMass()).when(rowSodiated).getAverageMZ();

    // add rows
    flist.addRow(rowProtonated);
    flist.addRow(rowSodiated);

    List<RowGroup> groups = new ArrayList<>();
    RowGroup group = new RowGroupFull(List.of(raw), 0);
    groups.add(group);
    group.add(rowProtonated);
    group.add(rowSodiated);
    flist.setGroups(groups);

    // add ions to rows - not really done much as the tasks handle this
    final IonNetwork network = new IonNetwork(MZTolerance.FIFTEEN_PPM_OR_FIVE_MDA, 1);
    final IonIdentity ionH = new IonIdentity(hAdduct);
    ionH.setNetwork(network);
    rowProtonated.addIonIdentity(ionH);
    final IonIdentity ionNa = new IonIdentity(naAdduct);
    ionNa.setNetwork(network);
    rowSodiated.addIonIdentity(ionNa);

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
    IonNetworkLogic.removeEmptyNetworks(flist);
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
    ResultFormula best = ion.getBestMolFormula().get();
    assertEquals(0.9f, best.getIsotopeScore());
    assertEquals(4.5f, best.getRDBE());

    // setting the formula should have updated the sub properties
    // test properties in ion identity
    best = rowProtonated.get(IonIdentityListType.class).get(0).getBestMolFormula().get();
    assertEquals(4.5f, best.getRDBE(),
        "Cannot access formula specific types from sub types of IonIdentityModularType.class");
    assertEquals(0.9f, best.getIsotopeScore(),
        "Cannot access formula specific types from sub types of IonIdentityModularType.class");

  }
}
