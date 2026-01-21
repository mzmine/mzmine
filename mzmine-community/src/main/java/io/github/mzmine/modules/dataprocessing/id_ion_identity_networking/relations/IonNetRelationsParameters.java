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

package io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.relations;

import io.github.mzmine.datamodel.identities.iontype.IonLibraries;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ionidentity.IonLibraryParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import org.jetbrains.annotations.Nullable;

public class IonNetRelationsParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final MZToleranceParameter mzTol = new MZToleranceParameter();

  public static final IonLibraryParameter ionLibrary = new IonLibraryParameter(
      "Neutral modifications", """
      A library of neutral modifications between two 'molecules' found by Ion Identity Networking.
      Only neutral definitions will be used and charged ions will be skipped.""",
      IonLibraries.MZMINE_DEFAULT_NEUTRAL_MODIFICATIONS);


  public static final BooleanParameter searchCondensedMultimer = new BooleanParameter(
      "Search condensed multimer",
      "Searches for condensed structures (loss of water) with regards to possible structure modifications",
      true);
  public static final BooleanParameter searchCondensedHeteroMultimer = new BooleanParameter(
      "Search condensed hetero",
      "Searches for condensed structures (loss of water) of two different neutral modifications",
      true);

  public IonNetRelationsParameters() {
    super(
        new Parameter[]{featureLists, mzTol, searchCondensedMultimer, searchCondensedHeteroMultimer,
            ionLibrary});
  }

  @Override
  public int getVersion() {
    return 2;
  }

  @Override
  public @Nullable String getVersionMessage(int version) {
    return switch (version) {
      case 2 ->
          "Changed to the new ion library based definition of neutral modifications. Define libraries and use them here to search for neutral modifications.";
      default -> null;
    };
  }
}
