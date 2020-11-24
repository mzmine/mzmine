package io.github.mzmine.modules.io.rawdataimport.fileformats.tdfimport.datamodel.callbacks;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;

public interface ProfileCallback extends Callback {

  void invoke(long id, long num_points, Pointer intensity_values, Pointer userData);
}
