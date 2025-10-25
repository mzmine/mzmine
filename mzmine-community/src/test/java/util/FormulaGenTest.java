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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package util;

import com.google.common.collect.Range;
import io.github.mzmine.taskcontrol.AllTasksFinishedListener;
import io.github.mzmine.taskcontrol.SimpleRunnableTask;
import io.github.mzmine.taskcontrol.TaskService;
import io.github.mzmine.taskcontrol.utils.TaskUtils;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.RangeUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.formula.MolecularFormulaGenerator;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import testutils.MZmineTestUtil;

public class FormulaGenTest {

  private static final Logger logger = Logger.getLogger(FormulaGenTest.class.getName());

  @Test
  void testConcurrentGeneration() throws InterruptedException {
    Locale.setDefault(Locale.ENGLISH);
    MolecularFormulaRange r = new MolecularFormulaRange();
    IsotopeFactory iFac = null;
    try {
      iFac = Isotopes.getInstance();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    r.addIsotope(iFac.getMajorIsotope("C"), 0, 200);
    r.addIsotope(iFac.getMajorIsotope("H"), 0, 300);
    r.addIsotope(iFac.getMajorIsotope("N"), 0, 15);
    r.addIsotope(iFac.getMajorIsotope("O"), 0, 25);
    r.addIsotope(iFac.getMajorIsotope("P"), 0, 2);
    r.addIsotope(iFac.getMajorIsotope("S"), 0, 2);

    final double[] masses = new double[]{286.1756, 332.2179, 215.1389, 575.3848, 247.1109, 696.4530,
        261.1681, 233.1494, 195.1127, 258.1487, 313.1544, 261.1443, 461.3328, 548.3800, 189.1232,
        350.1739, 461.3330, 327.0623, 344.2545, 189.1233, 322.1870, 233.1493, 203.1389, 231.1702};

    final List<Runnable> runnables = new ArrayList<>();
    for (int i = 0; i < masses.length; i++) {
      final Range<Double> mzRange = RangeUtils.rangeAround(masses[i], 0.01);
      MolecularFormulaGenerator g = new MolecularFormulaGenerator(
          SilentChemObjectBuilder.getInstance(), mzRange.lowerEndpoint(), mzRange.upperEndpoint(),
          r);
      int finalI = i;
      Runnable runnable = () -> {
        IMolecularFormula f = null;
        while ((f = g.getNextFormula()) != null) {
          final double mz = FormulaUtils.calculateMzRatio(f);
          logger.info("\t%d\t%.4f\t%s\t%.4f".formatted(finalI, masses[finalI],
              MolecularFormulaManipulator.getString(f), mz));
          if (!mzRange.contains(mz)) {
            throw new RuntimeException(mz + " not within range " + mzRange.toString());
          }
        }
      };
      runnables.add(runnable);
    }

    TaskService.init(masses.length);
    runnables.forEach(run -> TaskService.getController().addTask(new SimpleRunnableTask(run)));
    AtomicBoolean b = new AtomicBoolean(false);
    AllTasksFinishedListener l = new AllTasksFinishedListener(
        TaskService.getController().getReadOnlyTasksSnapshot(), _ -> {
      b.set(true);
    });

    while (!b.get()) {
      TimeUnit.MILLISECONDS.sleep(100);
    }
  }

}
