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

package io.github.mzmine.modules.io.spectral_library_from_matches;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.io.File;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public class MatchesSubsetLibraryTask extends AbstractFeatureListTask {

  private final FeatureList[] featureLists;
  private final String libName;

  public MatchesSubsetLibraryTask(@NotNull ParameterSet parameters,
      @NotNull Instant moduleCallDate, @NotNull FeatureList[] featureLists) {
    super(null, moduleCallDate, parameters, MatchesSubsetLibraryModule.class);
    this.featureLists = featureLists;
    libName = FileAndPathUtil.safePathEncode(
        parameters.getValue(MatchesSubsetLibraryParameters.libraryName));
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(featureLists);
  }

  @Override
  protected void process() {
    Set<SpectralLibraryEntry> unique = new HashSet<>();

    for (FeatureList flist : featureLists) {
      for (FeatureListRow row : flist.getRows()) {
        for (SpectralDBAnnotation match : row.getSpectralLibraryMatches()) {
          unique.add(match.getEntry());
        }
      }
    }
    final List<SpectralLibraryEntry> allEntries = unique.stream()
        .sorted(Comparator.comparingDouble(m -> m.getOrElse(DBEntryField.PRECURSOR_MZ, 0d)))
        .toList();

    final SpectralLibrary library = new SpectralLibrary(null, libName, new File(""));
    library.addEntries(allEntries);
    ProjectService.getProject().addSpectralLibrary(library);
  }

  @Override
  public String getTaskDescription() {
    return "Subsetting spectral library matches from %d feature lists to spectral library.".formatted(featureLists.length);
  }
}
