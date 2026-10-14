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

package io.github.mzmine.modules.tools.tools_autoparam.optimizer;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
class BenchmarkCampaignTest {

  @Test
  void generatedIdIdentifiesTheRelevantConfiguration() {
    final BenchmarkCampaign campaign = BenchmarkCampaign.create("MOEAD",
        "Isotope ratio consistency score",
        "SOBOL", 80, List.of(42L, 7L), "thermo-20y-qc, zenotof-feces-pos", null);

    Assertions.assertEquals(
        "moead-isotope-ratio-consistency-score-sobol-b80-s42-7-dthermo-20y-qc-zenotof-feces-pos",
        campaign.id());
    Assertions.assertTrue(campaign.outputFile("trajectory").getName().startsWith("trajectory-"));
  }

  @Test
  void explicitCampaignIdReplacesTheGeneratedSuffix() {
    final BenchmarkCampaign campaign = BenchmarkCampaign.create("ignored", "ignored", "ignored", 30,
        List.of(42L), null, " GP screening #1 ");

    Assertions.assertEquals("gp-screening-1", campaign.id());
    Assertions.assertEquals("summary-gp-screening-1.csv", campaign.outputFile("summary").getName());
  }

  @Test
  void emptySanitizedCampaignIdIsRejected() {
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> BenchmarkCampaign.create("ignored", "ignored", "ignored", 30, List.of(42L), null,
            "---"));
  }
}
