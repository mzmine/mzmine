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

package io.github.mzmine.util.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import fxinitializer.InitJavaFX;
import io.github.mzmine.util.FormulaStringFlavor;
import io.github.mzmine.util.FormulaUtils;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.interfaces.IMolecularFormula;

/**
 * Tests the bidirectional text <-> formula synchronization of {@link FormulaTextField}. All
 * interactions run on the JavaFX application thread because the constructor loads an icon font.
 */
class FormulaTextFieldTest {

  private static final FormulaStringFlavor FLAVOR = FormulaStringFlavor.DEFAULT_CHARGED;

  @BeforeAll
  static void initFx() {
    InitJavaFX.init();
  }

  @Test
  void typingParsesFormulaButDoesNotReformatText() throws Exception {
    runOnFxAndWait(() -> {
      final FormulaTextField field = new FormulaTextField(FLAVOR, false);
      // reordered, non-canonical input
      field.setText("O6C6H12");

      // text must stay exactly as typed
      assertEquals("O6C6H12", field.getText());
      // but the formula must be parsed to the correct composition
      assertNotNull(field.getFormula());
      assertEquals(canonical("C6H12O6"), FormulaUtils.getFormulaString(field.getFormula(), FLAVOR));
    });
  }

  @Test
  void settingFormulaExternallyUpdatesText() throws Exception {
    runOnFxAndWait(() -> {
      final FormulaTextField field = new FormulaTextField(FLAVOR, false);
      final IMolecularFormula ethanol = FormulaUtils.createMajorIsotopeMolFormulaWithCharge(
          "C2H6O");

      field.setFormula(ethanol);

      assertEquals(FormulaUtils.getFormulaString(ethanol, FLAVOR), field.getText());
      assertEquals(ethanol, field.getFormula());
    });
  }

  @Test
  void invalidTextKeepsLastFormulaAndExternalSetOverwritesIt() throws Exception {
    runOnFxAndWait(() -> {
      final FormulaTextField field = new FormulaTextField(FLAVOR, false);
      field.setText("C6H12O6");
      assertNotNull(field.getFormula());

      // transiently invalid input (e.g. half-typed brackets) must not clear the last valid formula
      field.setText("[C6H12");
      assertEquals("[C6H12", field.getText());
      assertEquals(canonical("C6H12O6"), FormulaUtils.getFormulaString(field.getFormula(), FLAVOR));

      // an externally set formula wins over the invalid text (guard case)
      final IMolecularFormula ethanol = FormulaUtils.createMajorIsotopeMolFormulaWithCharge(
          "C2H6O");
      field.setFormula(ethanol);
      assertEquals(FormulaUtils.getFormulaString(ethanol, FLAVOR), field.getText());
      assertEquals(ethanol, field.getFormula());
    });
  }

  @Test
  void blankTextClearsFormula() throws Exception {
    runOnFxAndWait(() -> {
      final FormulaTextField field = new FormulaTextField(FLAVOR, false);
      field.setText("C2H6O");
      assertNotNull(field.getFormula());

      field.setText("");
      assertNull(field.getFormula());
    });
  }

  @Test
  void bidirectionalBindingSyncsBothDirections() throws Exception {
    runOnFxAndWait(() -> {
      final FormulaTextField field = new FormulaTextField(FLAVOR, false);
      final ObjectProperty<IMolecularFormula> external = new SimpleObjectProperty<>();
      field.formulaProperty().bindBidirectional(external);

      // external -> field text
      final IMolecularFormula water = FormulaUtils.createMajorIsotopeMolFormulaWithCharge("H2O");
      external.set(water);
      assertEquals(FormulaUtils.getFormulaString(water, FLAVOR), field.getText());
      assertEquals(water, field.getFormula());

      // field text -> external formula
      field.setText("CO2");
      assertNotNull(external.get());
      assertEquals(canonical("CO2"), FormulaUtils.getFormulaString(external.get(), FLAVOR));
    });
  }

  private static @Nullable String canonical(@NotNull final String formula) {
    return FormulaUtils.getFormulaString(
        FormulaUtils.createMajorIsotopeMolFormulaWithCharge(formula), FLAVOR);
  }

  /**
   * Runs the given action on the JavaFX application thread and blocks until it finished, rethrowing
   * any error (including assertion failures) on the calling thread.
   */
  private static void runOnFxAndWait(@NotNull final Runnable action) throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    final Throwable[] error = new Throwable[1];
    final Runnable wrapped = () -> {
      try {
        action.run();
      } catch (Throwable t) {
        error[0] = t;
      } finally {
        latch.countDown();
      }
    };

    // the toolkit may still be starting up right after InitJavaFX.init(), so retry until runLater
    // is accepted instead of failing on a premature IllegalStateException
    final long deadline = System.currentTimeMillis() + 15_000;
    while (true) {
      try {
        Platform.runLater(wrapped);
        break;
      } catch (IllegalStateException toolkitNotReady) {
        if (System.currentTimeMillis() > deadline) {
          throw toolkitNotReady;
        }
        Thread.sleep(50);
      }
    }

    if (!latch.await(30, TimeUnit.SECONDS)) {
      throw new AssertionError("Timed out waiting for JavaFX task to complete");
    }
    if (error[0] instanceof AssertionError ae) {
      throw ae;
    }
    if (error[0] != null) {
      throw new RuntimeException(error[0]);
    }
  }
}
