/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package modules.fraggraph;

import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.impl.masslist.SimpleMassList;
import java.util.logging.Logger;

public class FragmentGraphTest {

  private static final Logger logger = Logger.getLogger(FragmentGraphTest.class.getName());

  private final double[] caffeineMzs = new double[]{42.03426, 69.04623, 83.06062, 108.05574,
      110.06946, 122.07037, 123.04293, 138.06653, 194.49059, 195.08994};
  private final double[] caffeineIntensities = new double[]{26.287885, 7.195664, 3.313599, 1.74958,
      24.935937, 1.416748, 7.110247, 93.499455, 1.125151, 100};

  private final MassList caffeineSpectrum = new SimpleMassList(null, caffeineMzs,
      caffeineIntensities);


//  @Test
//  void testFormulaGeneration() {
//
//    FragGraphPrecursorFormulaTask formulaTask = new FragGraphPrecursorFormulaTask(new FragDashboardModel(), 195.08994,
//        PolarityType.POSITIVE, 1,
//        List.of(new IonType(IonModification.H), new IonType(IonModification.NA)), null,
//        new MZTolerance(0.005, 10), true, true);
//
//    final MolecularFormulaGenerator generator = formulaTask.setUpFormulaGenerator();
//    formulaTask.generateFormulae(false, generator);
//
//    final ConcurrentLinkedQueue<IMolecularFormula> formulae = formulaTask.get();
//
//    final Optional<IMolecularFormula> caffeineOptional = formulae.stream()
//        .filter(f -> MolecularFormulaManipulator.getString(f).equals("[C8H11N4O2]+")).findAny();
//    Assertions.assertTrue(caffeineOptional.isPresent());
//    final IMolecularFormula caf = caffeineOptional.get();
//
//    final List<SignalWithFormulae> peaksWithFormulae = FragmentUtils.getPeaksWithFormulae(caf,
//        caffeineSpectrum, new SpectralSignalFilter(true, 10, 50, 100, 0.98),
//        new MZTolerance(0.005, 10));
//
//    for (SignalWithFormulae pair : peaksWithFormulae) {
//      logger.info(pair.toString());
//    }
//
//    FragmentGraphGenerator builder = new FragmentGraphGenerator("caffeine", peaksWithFormulae,
//        new FormulaWithExactMz(caf, FormulaUtils.calculateMzRatio(caf)));
//  }

}
