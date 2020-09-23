package io.github.mzmine.modules.io.tdfimport.datamodel;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;

interface MsMsCallback extends Callback {

  void invoke(long precursor_id, int num_peaks, Pointer pMz, Pointer pIntensites);
}
