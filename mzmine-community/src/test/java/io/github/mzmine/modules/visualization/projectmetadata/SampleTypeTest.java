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

package io.github.mzmine.modules.visualization.projectmetadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pins the file name heuristic that seeds the {@code mzmine_sample_type} metadata column on
 * import.
 */
class SampleTypeTest {

  @ParameterizedTest
  @CsvSource({
      // plain keywords, with and without separators or digits around them
      "qc, QC", "QC, QC", "'  qc  ', QC", "20240101_qc_03.mzML, QC", "sample-QC-1.raw, QC",
      "qc3, QC", "3qc, QC",
      // concatenated spellings that are explicitly known
      "pooled_qc, QC", "pooledqc, QC", "PooledQC_07.mzML, QC", "qcpool.raw, QC",
      "qc_pooled_2.mzML, QC", "qcpooled.raw, QC",
      // blanks, media stays a blank
      "blank, BLANK", "media, BLANK", "media_blank, BLANK", "mediablank.mzML, BLANK",
      "blk_02.raw, BLANK", "20240101_Blank_01.mzML, BLANK",
      // calibration - the long spellings only matched before because the lookarounds were no-ops
      "cal, CALIBRATION", "cal_5.mzML, CALIBRATION", "calibration.raw, CALIBRATION",
      "20240101_calibration_02.mzML, CALIBRATION", "quant_01.mzML, CALIBRATION",
      "calibrant-3.raw, CALIBRATION",
      // system suitability, and the standard mix spellings with and without separator
      "sst, SST", "SST_01.mzML, SST", "system_suitability.raw, SST", "systemsuitability.raw, SST",
      "sst-3.raw, SST", "std_mix_1.raw, SST", "stdmix.raw, SST", "standards_mix.mzML, SST",
      "standard_mix-02.raw, SST",
      // anything else is a sample
      "sample, SAMPLE", "plasma_01.mzML, SAMPLE", "'', SAMPLE"})
  void guessesTypeFromFileName(String name, SampleType expected) {
    assertEquals(expected, SampleType.guessFromName(name));
  }

  /**
   * The previous implementation quantified its lookarounds ({@code (?<![a-z])*}), which made them
   * no-ops and turned the patterns into plain substring matches. These names must never be anything
   * but a plain sample.
   */
  @ParameterizedTest
  @ValueSource(strings = {"chemical", "chemical_standard_1.mzML", "typical_run.raw", "local_02",
      "physical.mzML", "multimedia.raw", "assessment_1.mzML", "acquisition.raw",
      "calcium_serum.raw", "medial_tissue.mzML", "blanket.raw", "quantile_norm.raw", "qcheck.mzML",
      "standardization.raw"})
  void doesNotMatchKeywordsInsideLongerWords(String name) {
    assertEquals(SampleType.SAMPLE, SampleType.guessFromName(name),
        "'%s' must not be classified by a keyword hidden inside a longer word".formatted(name));
  }

  @Test
  void nullNameIsSample() {
    assertEquals(SampleType.SAMPLE, SampleType.guessFromName(null));
  }

  @Test
  void multimediaIsSampleButMediaBlankIsBlank() {
    // guards the exact pair that the old blank regex confused
    assertEquals(SampleType.SAMPLE, SampleType.guessFromName("multimedia_01.mzML"));
    assertEquals(SampleType.BLANK, SampleType.guessFromName("media_01.mzML"));
  }

  @ParameterizedTest
  @CsvSource({"qc, QC", "QC, QC", "'  Blank ', BLANK", "sst, SST", "calibration, CALIBRATION",
      "sample, SAMPLE"})
  void parsesExactValuesIgnoringCase(String value, SampleType expected) {
    assertEquals(expected, SampleType.ofExactValue(value));
  }

  @ParameterizedTest
  @ValueSource(strings = {"pooled_qc", "media_blank", "my custom group", "", "  "})
  void customValuesHaveNoPredefinedType(String value) {
    // custom group names are valid sample types, they just are not one of the predefined constants
    assertNull(SampleType.ofExactValue(value));
  }

  @Test
  void toStringValuesAreStableWireFormat() {
    // these strings end up in metadata files and batch xml, they must not drift
    assertEquals("blank", SampleType.BLANK.toString());
    assertEquals("sample", SampleType.SAMPLE.toString());
    assertEquals("qc", SampleType.QC.toString());
    assertEquals("calibration", SampleType.CALIBRATION.toString());
    assertEquals("sst", SampleType.SST.toString());
  }
}
