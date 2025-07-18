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

package io.github.mzmine.modules.dataanalysis.significance;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.modules.dataanalysis.significance.ttest.TTestSamplingConfig;
import java.util.List;
import org.apache.commons.math3.stat.inference.MannWhitneyUTest;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Enum representing the specific statistical test algorithm. This corresponds to the second,
 * conditional dropdown in the UI.
 */
public enum SignificanceTests implements UniqueIdSupplier, SignificanceTest {

  /**
   * <b>Type:</b> Parametric<br>
   * <b>Use Case:</b> Compares the means of two INDEPENDENT groups.<br>
   * <b>Assumptions:</b> Data in both groups are normally distributed. Heteroscedastic: Variances
   * of the groups may be unequal.
   */
  WELCHS_T_TEST,

  /**
   * <b>Type:</b> Parametric<br>
   * <b>Use Case:</b> Compares the means of two INDEPENDENT groups.<br>
   * <b>Assumptions:</b> Data in both groups is normally distributed. Homoscedastic: Variances of
   * the two groups are approximately equal.
   */
  STUDENTS_T_TEST,

  /**
   * <b>Type:</b> Parametric<br>
   * <b>Use Case:</b> Compares the means of two RELATED/PAIRED groups (e.g., before and after
   * treatment on the same subjects).<br>
   * <b>Assumptions:</b> The *differences* between the paired samples are normally distributed.
   */
  PAIRED_T_TEST,

  /**
   * <b>Type:</b> Non-Parametric<br>
   * <b>Use Case:</b> Compares the medians/distributions of two INDEPENDENT groups. It is the
   * non-parametric equivalent of the Student's t-test.<br>
   * <b>Assumptions:</b> None regarding data distribution. Robust to outliers and small sample
   * sizes.
   */
  MANN_WHITNEY_U_TEST,

  // Note: Wilcoxon Signed-Rank Test (non-parametric paired test) is not available in Apache Commons Math

// multi variate tests of multiple groups
  /**
   * <b>Type:</b> Parametric<br>
   * <b>Use Case:</b> Compares the means of MORE THAN TWO independent groups to see if at least one
   * group is different from the others.<br>
   * <b>Assumptions:</b> Data in all groups is normally distributed. Variances are equal across all
   * groups (homoscedasticity).
   */
  ONE_WAY_ANOVA
  // PERMANOVA?

//  /**
//   * <b>Type:</b> Non-Parametric<br>
//   * <b>Use Case:</b> Compares the medians/distributions of MORE THAN TWO independent groups. It is
//   * the non-parametric equivalent of ANOVA.<br>
//   * <b>Assumptions:</b> None regarding data distribution.
//   */
//  KRUSKAL_WALLIS_TEST,
  ;


  private static final SignificanceTests[] univariateTests = {WELCHS_T_TEST, STUDENTS_T_TEST,
      PAIRED_T_TEST};
  private static final SignificanceTests[] multivariateTests = {ONE_WAY_ANOVA};

  public static final MannWhitneyUTest MANN_WHITNEY_U_TEST_INST = new MannWhitneyUTest();

  /**
   * Also checks the previously used TTestSamplingConfig
   */
  public static SignificanceTests parseOrDefault(String toParse, SignificanceTests defaultValue) {
    if (toParse == null) {
      return defaultValue;
    }
    for (final var type : values()) {
      if (type.getUniqueID().equalsIgnoreCase(toParse)) {
        return type;
      }
    }

    // might also match to the sometimes used other class
    for (final var type : TTestSamplingConfig.values()) {
      if (type.getUniqueID().equalsIgnoreCase(toParse)) {
        return type.toSignificanceTest();
      }
    }

    return defaultValue;
  }

  public static @NotNull SignificanceTests[] univariateValues() {
    return univariateTests;
  }


  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case WELCHS_T_TEST -> "WELCHS_T_TEST";
      case STUDENTS_T_TEST -> "STUDENTS_T_TEST";
      case PAIRED_T_TEST -> "PAIRED_T_TEST";
      case MANN_WHITNEY_U_TEST -> "MANN_WHITNEY_U_TEST";
      case ONE_WAY_ANOVA -> "ONE_WAY_ANOVA";
    };
  }

  @Override
  public String toString() {
    return switch (this) {
      case WELCHS_T_TEST -> "Welch's t-test (unpaired)";
      case STUDENTS_T_TEST -> "Student's t-test (unpaired)";
      case PAIRED_T_TEST -> "t-test (paired)";
      case MANN_WHITNEY_U_TEST -> "Mann-Whitney U test (unpaired)";
      case ONE_WAY_ANOVA -> "One-way ANOVA";
    };
  }

  /**
   * @return A string describing the assumptions for the data
   */
  public String describeAssumptions() {
    return switch (this) {
      case STUDENTS_T_TEST, ONE_WAY_ANOVA ->
          "Parametric; assumes normally distributed data; homoscedasticity, similar variances in all groups.";
      case PAIRED_T_TEST ->
          "Parametric; assumes normally distributed difference between grouped data.";
      case WELCHS_T_TEST ->
          "Parametric; assumes normally distributed data; heteroscedasticity, unequal variances in all groups.";
      case MANN_WHITNEY_U_TEST -> "Non-parametric; robust to outliers and small sample sizes.";
    };
  }

  /**
   * @return true if the test is parametric (makes assumptions for the data distribution etc.)
   */
  public boolean isParametric() {
    return switch (this) {
      case STUDENTS_T_TEST, PAIRED_T_TEST, ONE_WAY_ANOVA, WELCHS_T_TEST -> true;
      case MANN_WHITNEY_U_TEST -> false;
    };
  }

  /**
   * @return PAIRED or UNPAIRED (independent samples)
   */
  public @NotNull TTestSamplingConfig getSamplingConfig() {
    return switch (this) {
      case STUDENTS_T_TEST, MANN_WHITNEY_U_TEST, ONE_WAY_ANOVA, WELCHS_T_TEST ->
          TTestSamplingConfig.UNPAIRED;
      case PAIRED_T_TEST -> TTestSamplingConfig.PAIRED;
    };
  }

  public String getDescription() {
    return """
        %s: %s
        %s""".formatted( //
        toString(), describeAssumptions(), //
        getSamplingConfig().getDescription() //
    );
  }

  // the calculation of p values
  @Override
  public double test(List<double @NotNull []> data) {
    return switch (this) {
      // univariate tests
      case WELCHS_T_TEST -> TestUtils.tTest(data.getFirst(), data.get(1));
      case PAIRED_T_TEST -> TestUtils.pairedTTest(data.getFirst(), data.get(1));
      case STUDENTS_T_TEST -> TestUtils.homoscedasticTTest(data.getFirst(), data.get(1));
      case MANN_WHITNEY_U_TEST ->
          MANN_WHITNEY_U_TEST_INST.mannWhitneyUTest(data.getFirst(), data.get(1));
      // multi variate
      case ONE_WAY_ANOVA -> TestUtils.oneWayAnovaPValue(data);
    };
  }

  @Override
  public void applyPreChecks(List<double[]> data) {
    boolean valid = switch (this) {
      case STUDENTS_T_TEST, PAIRED_T_TEST, MANN_WHITNEY_U_TEST, WELCHS_T_TEST -> {
        if (data.size() != 2) {
          throw new IllegalArgumentException(
              "Groups need to be exactly 2 but is %d".formatted(data.size()));
        }

        final int samplesA = data.getFirst().length;
        final int samplesB = data.get(1).length;
        if (this.getSamplingConfig() == TTestSamplingConfig.PAIRED && samplesA != samplesB) {
          throw new IllegalArgumentException(
              "Paired test requires the same number of features in group 1 and group 2. Found %d in group 1 and %d in group 2".formatted(
                  samplesA, samplesB));
        }
        yield true;
      }
      case ONE_WAY_ANOVA -> {
        if (data.size() < 3) {
          throw new IllegalArgumentException(
              "At least 3 groups required for ANOVA but is %d".formatted(data.size()));
        }
        yield true;
      }
    };
  }
}
