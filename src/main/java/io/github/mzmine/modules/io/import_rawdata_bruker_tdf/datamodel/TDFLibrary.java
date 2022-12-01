/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel;

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.callbacks.CentroidCallback;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.callbacks.CentroidData;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.callbacks.MultipleCentroidData;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.callbacks.MultipleProfileData;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.callbacks.ProfileCallback;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.callbacks.ProfileData;

/**
 * Inteface for Java Native Access for Bruker Daltonic's tdf data format.
 * <p>
 * Javadoc added according to documentation in timsdata-2.8.7.1-win32-vc141
 *
 * @author Bruker Daltonik GmbH - copied from the SDK by SteffenHeu.
 */
public interface TDFLibrary extends Library {

  /**
   * Open data set.
   * <p>
   * On success, returns a non-zero instance handle that needs to be passed to subsequent API calls,
   * in particular to the required call to tims_close().
   *
   * @param analysis_dir the name of the directory in the file system that * contains the analysis
   *                     data, in UTF-8 encoding.
   * @param use_recalib  if non-zero, use the most recent recalibrated state * of the analysis, if
   *                     there is one; if zero, use the original "raw" calibration * written during
   *                     acquisition time.
   * @return On failure, returns 0, and you can use tims_get_last_error_string() to obtain a string
   * describing the problem.
   */
  long tims_open(String analysis_dir, long use_recalib);

  /**
   * Open data set.
   *
   * @param analysis_dir         the name of the directory in the file system that contains the
   *                             analysis data, in UTF-8 encoding.
   * @param use_recalib          if non-zero, use the most recent recalibrated state of the
   *                             analysis, if there is one; if zero, use the original "raw"
   *                             calibration written during acquisition time.
   * @param pressureCompensation the pressure compensation strategy. 0 = NoPressureCompensation,
   *                             1=AnalyisGlobalPressureCompensation,
   *                             2=PerFramePressureCompensation
   * @return On success, returns a non-zero instance handle that needs to be passed to subsequent
   * API calls, in particular to the required call to tims_close(). On failure, returns 0, and you
   * can use tims_get_last_error_string() to obtain a string describing the problem.
   */
  long tims_open_v2(String analysis_dir, long use_recalib, int pressureCompensation);

  /**
   * Close data set.
   *
   * @param handle btained by tims_open(); passing 0 is ok and has no effect.
   * @return not documented
   */
  void tims_close(long handle);

  /**
   * Return the last error as a string (thread-local).
   * <p>
   *
   * @param error pointer to a buffer into which the error string will be written.
   * @param len   length of the buffer
   * @return the actual length of the error message (including the final zero byte). If this is
   * longer than the input parameter 'len', you know that the returned error string was truncated to
   * fit in the provided buffer.
   */
  long tims_get_last_error_string(byte[] error, long len);

  /**
   * TODO: Check if this export exists
   *
   * @param handle see {@link TDFLibrary#tims_open(String, long)}.
   * @return Returns 1 if the raw data have been recalibrated after acquisition, e.g. in the
   * DataAnalysis software. Note that masses and 1/K0 values in the raw-data SQLite file are always
   * in the raw calibration state, not the recalibrated state.
   */
  long tims_has_recalibrated_state(long handle);

  /**
   * Read a range of scans from a single frame.
   * <p>
   * Output layout: (N = scan_end - scan_begin = number of requested scans) N x uint32_t: number of
   * peaks in each of the N requested scans N x (two uint32_t arrays: first indices, then
   * intensities)
   * <p>
   * Note: different threads must not read scans from the same storage handle concurrently.
   *
   * @param handle     see {@link TDFLibrary#tims_open(String, long)}.
   * @param frameId    from .tdf SQLite: Frames.Id
   * @param scanBegin  first scan number to read (inclusive)
   * @param scanEnd    last scan number (exclusive)
   * @param scanBuffer destination buffer allocated by user
   * @param len        length of buffer (in bytes)
   * @return 0 on error, otherwise the number of buffer bytes necessary for the output of this call
   * (if this is larger than the provided buffer length, the result is not complete).
   */
  long tims_read_scans_v2(long handle, long frameId, long scanBegin, long scanEnd,
      byte[] scanBuffer, long len);

  /**
   * Read peak-picked MS/MS spectra for a list of PASEF precursors.
   * <p>
   * Given a list of PASEF precursor IDs, this function reads all necessary PASEF frames, sums up
   * the corresponding scan-number ranges into synthetic profile spectra for each precursor,
   * performs centroiding using an algorithm and parameters suggested by Bruker, and returns the
   * resulting MS/MS spectra (one for each precursor ID).
   * <p>
   * Note: the order of the returned MS/MS spectra does not necessarily match the order in the
   * specified precursor ID list. The parameter id in the callback is the precursor ID.
   * <p>
   * Note: different threads must not read scans from the same storage handle concurrently.
   *
   * @param handle         see {@link TDFLibrary#tims_open(String, long)}.
   * @param precursors     list of PASEF precursor IDs; the returned spectra may be in different
   *                       order
   * @param num_precursors number of requested spectra, must be >= 1
   * @param callback       callback accepting the MS/MS spectra
   * @return 0 on error
   */
  long tims_read_pasef_msms(long handle, long[] precursors, long num_precursors,
      CentroidCallback callback);

  long tims_read_pasef_msms_v2(long handle, long[] precursors, long num_precursors,
      CentroidData callback, Pointer user_data);

  /**
   * <p>
   * Read peak-picked MS/MS spectra for all PASEF precursors from a given frame.
   * <p>
   * Given a frame id, this function reads all contained PASEF precursors the necessary PASEF frames
   * in the same way as tims_read_pasef_msms.
   * <p>
   * Note: the order of the returned MS/MS spectra does not necessarily match the order in the
   * specified precursor ID list. The parameter id in the callback is the precursor ID.
   * <p>
   * Note: different threads must not read scans from the same storage handle concurrently.
   *
   * @param handle      see {@link TDFLibrary#tims_open(String, long)}.
   * @param frameId     frame id
   * @param my_callback callback accepting the MS/MS spectra
   * @return 0 on error
   */
  long tims_read_pasef_msms_for_frame_v2(long handle, long frameId,
      MultipleCentroidData my_callback);

  /**
   * TODO test
   *
   * @param handle
   * @param precursors     list of PASEF precursor IDs; the returned spectra may be in different
   *                       order
   * @param num_precursors number of requested spectra, must be >= 1
   * @param callback       callback accepting profile MS/MS spectra
   * @param user_data      will be passed to callback
   * @return 0 on error
   */
  long tims_read_pasef_profile_msms_v2(long handle, long[] precursors, long num_precursors,
      ProfileData callback, Pointer user_data);

  /**
   * TODO test
   * <p>
   * Read "quasi profile" MS/MS spectra for all PASEF precursors from a given frame.
   * <p>
   * Given a frame id, this function reads for all contained PASEF precursors the necessary PASEF
   * frames in the same way as tims_read_pasef_profile_msms.
   * <p>
   * Note: the order of the returned MS/MS spectra does not necessarily match the order in the
   * specified precursor ID list. The parameter id in the callback is the precursor ID.
   * <p>
   * Note: different threads must not read scans from the same storage handle concurrently.
   *
   * @param handle
   * @param frame_id
   * @param callback
   * @param user_data
   * @return 0 non error
   */
  long tims_read_pasef_profile_msms_for_frame_v2(long handle, long frame_id,
      MultipleProfileData callback, Pointer user_data);

  /**
   * @param handle  see {@link TDFLibrary#tims_open(String, long)}.
   * @param frameId from .tdf SQLite: Frames.Id
   * @param index   in: array of values
   * @param mz      out: array of values
   * @param len     number of values to convert (arrays must have corresponding size)
   * @return 1 on success, 0 on failure
   */
  long tims_index_to_mz(long handle, long frameId, double[] index, double[] mz, long len);

  /**
   * mobility transformation: convert back and forth between (possibly non-integer) scan numbers and
   * 1/K0 values.
   *
   * @param handle    see {@link TDFLibrary#tims_open(String, long)}.
   * @param frameId   from .tdf SQLite: Frames.Id
   * @param scannum   in: array of values
   * @param oneOverK0 out: array of values
   * @param len       number of values to convert (arrays must have corresponding size)
   * @return 1 on success, 0 on failure
   */
  long tims_scannum_to_oneoverk0(long handle, long frameId, double[] scannum, double[] oneOverK0,
      long len);

  /**
   * Converts the 1/K0 value to CCS (in Angstrom^2) using the Mason-Shamp equation
   *
   * @param ook0   the 1/K0 value in Vs/cm2
   * @param charge the charge
   * @param mz     the mz of the ion
   * @return the CCS value in Angstrom^2
   */
  double tims_oneoverk0_to_ccs_for_mz(double ook0, long charge, double mz);

  /**
   * Converts the CCS (in Angstrom^2) to 1/K0 using the Mason-Shamp equation
   *
   * @param ccs    the ccs value in Angstrom^2
   * @param charge the charge
   * @param mz     the mz of the ion
   * @return the 1/K0 value in Vs/cm2
   **/
  double tims_ccs_to_oneoverk0_for_mz(double ccs, long charge, double mz);


  /**
   * Read peak-picked spectra for a tims frame.
   * <p>
   * Given a frame ID, this function reads the frame, sums up the corresponding scan-number ranges
   * into a synthetic profile spectrum, performs centroiding using an algorithm and parameters
   * suggested by Bruker, and returns the resulting spectrum (exactly one for the frame ID).
   * <p>
   * Note: Result callback identical to the tims_read_pasef_msms_v2 methods, but only returns a
   * single result and the parameter id is the frame_id
   * <p>
   * Note: different threads must not read scans from the same storage handle concurrently.
   *
   * @param handle     see {@link TDFLibrary#tims_open(String, long)}.
   * @param frame_id   Bruker: "list of PASEF precursor IDs; the returned spectra may be in
   *                   different order"
   * @param scan_begin first scan number to read (inclusive)
   * @param scan_end   last scan number (exclusive)
   * @param callback   callback accepting the spectra
   * @param user_data  will be passed to callback
   * @return 0 on error
   */
  long tims_extract_centroided_spectrum_for_frame_v2(long handle, long frame_id, long scan_begin,
      long scan_end, CentroidCallback callback, Pointer user_data);

  /**
   * TODO test
   *
   * @param handle
   * @param frame_id
   * @param scan_begin
   * @param scan_end
   * @param callback
   * @param userData
   * @return
   */
  long tims_extract_profile_for_frame(long handle, long frame_id, long scan_begin, long scan_end,
      ProfileCallback callback, Pointer userData);

  /**
   * @param numThreads The number of threads used to read one file. >= 1.
   */
  void tims_set_num_threads(int numThreads);
};
