package io.github.mzmine.datamodel.identities.fx;

import io.github.mzmine.parameters.impl.CurrentProjectNoDialogParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryParameter;

public class IonTypeCreatorParameters extends CurrentProjectNoDialogParameterSet {

  public static final DirectoryParameter otherDirectory = new DirectoryParameter(
      "Additional ion libraries directory", """
      Defines an additional directory to search for ion libraries, for example on a shared drive.
      This allows sharing of ion libraries. If conflicts arise between this and the local user directory - the additional lists will overrule the local definitions.""");

  public IonTypeCreatorParameters() {
    super(otherDirectory);
  }
}
