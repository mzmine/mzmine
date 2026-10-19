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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter.Mode;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The filter works on the free text values of the sample type metadata column: exact, but ignoring
 * case and surrounding whitespace. It deliberately does not apply the file name heuristics of
 * {@link SampleType#guessFromName(String)}.
 * <p>
 * It is also the parameter value, so the normalized value set and its display forms are part of the
 * contract - see
 * {@link io.github.mzmine.parameters.parametertypes.metadata.SampleTypeFilterParameter}.
 */
class SampleTypeFilterTest {

  @ParameterizedTest
  @ValueSource(strings = {"qc", "QC", "Qc", " qc ", "\tQC\n"})
  void matchesValuesIgnoringCaseAndWhitespace(String value) {
    assertTrue(SampleTypeFilter.qc().matchesValue(value));
  }

  @Test
  void doesNotApplyFileNameHeuristicsToValues() {
    final SampleTypeFilter qc = SampleTypeFilter.qc();
    // the column value is authoritative - "pooled_qc" is its own group, not a qc
    assertFalse(qc.matchesValue("pooled_qc"));
    assertFalse(qc.matchesValue("qc_pooled"));
    assertFalse(SampleTypeFilter.blank().matchesValue("media"));
    // ... unless the user selected exactly that group
    assertTrue(SampleTypeFilter.ofValues("pooled_qc").matchesValue("Pooled_QC"));
    assertTrue(SampleTypeFilter.ofValues("media").matchesValue("media"));
  }

  @Test
  void matchesCustomUserDefinedGroups() {
    final SampleTypeFilter filter = SampleTypeFilter.ofValues("sst", "my Custom Group");
    assertTrue(filter.matchesValue("SST"));
    assertTrue(filter.matchesValue("my custom group"));
    assertFalse(filter.matchesValue("other"));
  }

  @Test
  void allMatchesEverythingIncludingUnknownTypes() {
    final SampleTypeFilter all = SampleTypeFilter.all();
    assertTrue(all.matchesValue("qc"));
    assertTrue(all.matchesValue("a type nobody has seen before"));
    // an open ended filter must also accept files without a value, they are still samples
    assertTrue(all.matchesValue(null));
    assertFalse(all.isEmpty());
    assertEquals(Mode.ALL, all.getMode());
  }

  @Test
  void noneMatchesNothing() {
    final SampleTypeFilter none = SampleTypeFilter.none();
    assertFalse(none.matchesValue("qc"));
    assertFalse(none.matchesValue(null));
    assertTrue(none.isEmpty());
    assertEquals(Mode.NONE, none.getMode());
  }

  @Test
  void emptyListMatchesNothing() {
    final SampleTypeFilter empty = SampleTypeFilter.ofValues(List.of());
    assertFalse(empty.matchesValue("qc"));
    assertTrue(empty.isEmpty());
  }

  @Test
  void nullAndBlankValuesNeverMatchAListFilter() {
    final SampleTypeFilter filter = SampleTypeFilter.qc();
    assertFalse(filter.matchesValue(null));
    assertFalse(filter.matchesValue(""));
    assertFalse(filter.matchesValue("   "));
  }

  @Test
  void storesNormalizedValuesAndDropsBlanks() {
    final SampleTypeFilter filter = SampleTypeFilter.ofValues(" QC ", "Blank", "", "  ");
    assertEquals(Set.of("qc", "blank"), filter.getValues());
  }

  @Test
  void collapsesValuesThatOnlyDifferInCaseOrWhitespace() {
    // "Sample" and "sample" are one and the same group - that is what the user gets to pick
    final SampleTypeFilter filter = SampleTypeFilter.ofValues("QC", " qc ", "Qc", "blank");
    assertEquals(List.of("blank", "qc"), List.copyOf(filter.getValues()));
  }

  @Test
  void valuesAreSortedSoEqualFiltersSerializeIdentically() {
    final SampleTypeFilter filter = SampleTypeFilter.ofValues("qc", "Blank", "sst", "media_blank");
    assertEquals(List.of("blank", "media_blank", "qc", "sst"), List.copyOf(filter.getValues()));
    // order of construction must not matter for equality
    assertEquals(SampleTypeFilter.ofValues("blank", "qc"),
        SampleTypeFilter.ofValues("QC", "blank"));
  }

  @Test
  void enumFactoriesUseTheStringValues() {
    assertEquals(Set.of("qc"), SampleTypeFilter.qc().getValues());
    assertEquals(Set.of("blank"), SampleTypeFilter.blank().getValues());
    assertEquals(Set.of("sample"), SampleTypeFilter.sample().getValues());
    assertEquals(Set.of("calibration"), SampleTypeFilter.calibration().getValues());
    assertEquals(Set.of("sst"), SampleTypeFilter.sst().getValues());
    assertTrue(SampleTypeFilter.of(SampleType.QC, SampleType.BLANK).matches(SampleType.BLANK));
  }

  @Test
  void customValuesExcludePredefinedTypes() {
    final SampleTypeFilter filter = SampleTypeFilter.ofValues("qc", "QC ", "pooled_qc", "my group");
    assertEquals(Set.of("pooled_qc", "my group"), filter.customValues());
  }

  @Test
  void toStringIsSortedAndCommaSeparated() {
    assertEquals("blank, qc", SampleTypeFilter.ofValues("qc", "blank").toString());
    assertEquals("All", SampleTypeFilter.all().toString());
    assertEquals("None", SampleTypeFilter.none().toString());
    assertEquals("None", SampleTypeFilter.ofValues(List.of()).toString());
  }

  @Test
  void shortStringNeverCutsInsideAValueAndReportsHiddenCount() {
    final SampleTypeFilter filter = SampleTypeFilter.ofValues("blank", "calibration", "qc",
        "sample", "sst");

    assertEquals("blank, calibration, qc, sample, sst", filter.toShortString(100));
    // "blank, calibration" is 18 chars, adding ", qc" would exceed 20
    assertEquals("blank, calibration (+3)", filter.toShortString(20));
    // a single value that does not fit is still shown in full rather than truncated mid word
    assertEquals("blank (+4)", filter.toShortString(1));
  }

  @Test
  void shortStringUsesTheModeNameForAllAndNone() {
    assertEquals("All", SampleTypeFilter.all().toShortString(5));
    assertEquals("None", SampleTypeFilter.none().toShortString(5));
  }
}
