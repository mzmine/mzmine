package io.github.mzmine.modules.io.tdfimport.datamodel;

import com.sun.jna.Library;
import com.sun.jna.Native;
import io.github.mzmine.modules.io.tdfimport.TDFBinExample;

public interface TDFLibrary extends Library {

  TDFBinExample.TDFLibrary INSTANCE = (TDFBinExample.TDFLibrary) Native
      .load("timsdata", TDFBinExample.TDFLibrary.class);

  long tims_open(String analysis_dir, long use_recalib);

  long tims_close(long handle);

  long tims_get_last_error_string(byte[] error, long len);

  long tims_read_scans_v2(long handle, long frameId, long scanBegin, long scanEnd,
      byte[] scanBuffer, long len);

  long tims_index_to_mz(long handle, long frameId, double[] index, double[] mz, long len);

  long tims_scannum_to_oneoverk0(long handle, long frameId, double[] scannum, double[] oneOverK0,
      long len);


  long tims_read_pasef_msms(long handle, long[] precursors, long num_precursors,
      TDFBinExample.TDFLibrary.MsMsCallback my_callback);

  long tims_read_pasef_msms_for_frame(long handle, long frameId,
      TDFBinExample.TDFLibrary.MsMsCallback my_callback);

  // I know this is really clumsy, I wish I could use Arrays.stream(int[]).asDoubleStream().toArray();
  public static double[] copyFromIntArray(int[] source) {
    double[] dest = new double[source.length];
    for (int i = 0; i < source.length; i++) {
      dest[i] = source[i];
    }
    return dest;
  }

};
