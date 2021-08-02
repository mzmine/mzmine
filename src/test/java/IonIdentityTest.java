/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
import io.github.mzmine.datamodel.features.types.annotations.iin.IonIdentityModularType;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.datamodel.features.types.numbers.IsotopePatternScoreType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.features.types.annotations.RdbeType;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonNetworkLogic;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
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
  ResultFormula formula;

  IonType hAdduct = new IonType(IonModification.H);
  IonType naAdduct = new IonType(IonModification.NA);

  @Test
  void ionIdentityTest() {
    ModularFeatureList flist = new ModularFeatureList("A", null, raw);

    flist.addRowType(new IDType());
    flist.addRowType(new RTType());
    flist.addRowType(new MZType());
    flist.addRowType(new IonIdentityModularType());
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
    doReturn(1f).when(rowProtonated).getAverageRT();
    doReturn(1f).when(rowSodiated).getAverageRT();

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

    assertNotNull(rowProtonated.get(new IonIdentityModularType()));
    assertNotNull(rowProtonated.get(IonIdentityModularType.class));
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
    when(formula.getIsotopeScore()).thenReturn(0.9);
    when(formula.getRDBE()).thenReturn(4.5);
    IonIdentity ion = rowProtonated.getBestIonIdentity();
    ion.addMolFormula(formula);
    assertEquals(0.9, ion.getBestMolFormula().getIsotopeScore());
    assertEquals(4.5, ion.getBestMolFormula().getRDBE());

    // setting the formula should have updated the sub properties
    // test properties in ion identity
    assertEquals(4.5f,
        rowProtonated.get(IonIdentityModularType.class).get(RdbeType.class).getValue(),
        "Cannot access formula specific types from sub types of IonIdentityModularType.class");
    assertEquals(0.9f,
        rowProtonated.get(IonIdentityModularType.class).get(IsotopePatternScoreType.class)
            .getValue(),
        "Cannot access formula specific types from sub types of IonIdentityModularType.class");

  }

}
