package io.github.mzmine.modules.io.tdfimport.datamodel.callbacks;

import com.sun.jna.Pointer;
import javax.security.auth.callback.Callback;

public interface ProfileMsMsCallback extends Callback {

  void invoke(long id, long num_points, Pointer intensity_values, Pointer userData);
}
