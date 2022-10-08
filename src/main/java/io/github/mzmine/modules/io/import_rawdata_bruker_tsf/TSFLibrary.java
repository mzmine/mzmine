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

package io.github.mzmine.modules.io.import_rawdata_bruker_tsf;

import com.sun.jna.Library;

/**
 * Inteface for Java Native Access for Bruker Daltonic's tdf data format.
 * <p>
 * Javadoc added according to documentation in timsdata-2.8.7.1-win32-vc141
 *
 * @author Bruker Daltonik GmbH - copied from the SDK by SteffenHeu.
 */
public interface TSFLibrary extends Library {

  /**
   * @param analysis_directory_name analysis_directory_name the name of the directory in the file
   *                                system that contains the analysis data, in UTF-8 encoding.
   * @param use_recalibrated_state  if non-zero, use the most recent recalibrated state of the
   *                                analysis, if there is one; if zero, use the original "raw"
   *                                calibration written during acquisition time.
   * @return On success, returns a non-zero instance handle that needs to be passed to subsequent
   * API calls, in particular to the required call to tims_close(). On failure, returns 0, and you
   * can use tims_get_last_error_string() to obtain a string describing the problem.
   */
  long tsf_open(String analysis_directory_name, long use_recalibrated_state);

  /**
   * @param handle obtained by tsf_open(); passing 0 is ok and has no effect.
   */
  void tsf_close(long handle);

  /**
   * @param error pointer to a buffer into which the error string will be written.
   * @param len   length of the buffer
   * @return the actual length of the error message (including the final zero byte). If this is
   * longer than the input parameter 'len', you know that the returned error string was truncated to
   * fit in the provided buffer.
   */
  long tsf_get_last_error_string(byte[] error, long len);

  /**
   * @param handle
   * @returnReturns 1 if the raw data have been recalibrated after acquisition, e.g. in the
   * DataAnalysis software. Note that masses and 1/K0 values in the raw-data SQLite file are always
   * in the raw calibration state, not the recalibrated state.
   */
  long tsf_has_recalibrated_state(long handle);

  /**
   * Read a line spectrum. Fails if no line spectrum is contained (GlobalMetatdata.HasLineSpectra ==
   * 0)
   * <p>
   * Note: different threads must not read spectra from the same storage handle concurrently.
   *
   * @param handle          [in] handle the handle used for reading
   * @param spectrum_id     [in] spectrum_id from .tsf SQLite: Frames.Id
   * @param index_array     [out] index_array index values as double array
   * @param intensity_array [out] intensity_array intensity values as float array
   * @param length          [in] length the length of the provided arrays
   * @return 0 on error, otherwise the number of entries necessary for the output arrays of this
   * call (if this is larger than the provided output array length, the result is not complete).
   */
  long tsf_read_line_spectrum(long handle, long spectrum_id, double[] index_array,
      float[] intensity_array, long length);

  /**
   * Read a line spectrum. Fails if no line spectrum or no peak width is contained
   * (GlobalMetatdata.HasLineSpectra == 0 or GlobalMetatdata.HasLineSpectraPeakWidth == 0) Note:
   * different threads must not read spectra from the same storage handle concurrently.
   *
   * @param handle          [in] handle the handle used for reading
   * @param spectrum_id     [in] spectrum_id from .tsf SQLite: Frames.Id
   * @param index_array     [out] index_array index values as double array
   * @param intensity_array [out] intensity_array intensity values as float array
   * @param width_array     [out] width_array width values as float array
   * @param length          [in] length the length of the provided arrays
   * @return 0 on error, otherwise the number of entries necessary for the output arrays of this
   * call (if this is larger than the provided output array length, the result is not complete).
   */
  long tsf_read_line_spectrum_with_width(long handle, long spectrum_id, double[] index_array,
      float[] intensity_array, float[] width_array, long length);

  /**
   * Read a profile spectrum. Fails if no profile is contained (GlobalMetatdata.HasProfileSpectra ==
   * 0) Note: different threads must not read spectra from the same storage handle concurrently.
   *
   * @param handle        [in] handle the handle used for reading
   * @param spectrum_id   [in] spectrum_id from .tsf SQLite: Frames.Id
   * @param profile_array [out] profile_array intensity values as uint32_t array, position in the
   *                      array is the index
   * @param len           [in] length the length of the provided array
   * @return 0 on error, otherwise the number of entries necessary for the output array of this call
   * (if this is larger than the provided output array length, the result is not complete).
   */
  long tsf_read_profile_spectrum(long handle, long spectrum_id, byte[] profile_array, long len);

  /// -----------------------------------------------------------------------------------
  ///
  /// Conversion functions coming up. All these functions share the same signature (see
  /// typedef 'BdalTimsConversionFunction'). They all return 1 on success, 0 on failure.
  ///
  /// -----------------------------------------------------------------------------------
  /// typedef uint32_t BdalTimsConversionFunction (
  ///     uint64_t handle,
  ///     int64_t frame_id,      //< from .tdf SQLite: Frames.Id
  ///       const double *index,   //<  in: array of values
  ///     double *mz,            //< out: array of values
  ///     uint32_t cnt           //< number of values to convert (arrays must have
  ///     //< corresponding size)
  ///  );

  /**
   * @param handle
   * @param frame_id from .tdf SQLite: Frames.Id
   * @param index    in: array of values
   * @param mz       out: array of values
   * @param cnt      number of values to convert (arrays must have corresponding size
   * @return
   */
  long tsf_index_to_mz(long handle, long frame_id, double[] index, double[] mz, long cnt);

  long tsf_mz_to_index(long handle, long frame_id, double[] index, double[] mz, long cnt);

  long tsf_set_num_threads(int numThreads);
}
