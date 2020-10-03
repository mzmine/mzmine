/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.io.tdfimport.datamodel;

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import io.github.mzmine.modules.io.tdfimport.datamodel.callbacks.MsMsCallbackV2;
import io.github.mzmine.modules.io.tdfimport.datamodel.callbacks.ProfileMsMsCallback;

/**
 * Inteface for Java Native Access for Bruker Daltonic's tdf data format.
 * <p>
 * Javadoc added according to documentation in tdf-sdk-2.8.7_pre
 *
 * @author Bruker Daltonik GmbH - Basically copied from Bruker's java example by SteffenHeu.
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
   * Close data set.
   *
   * @param handle btained by tims_open(); passing 0 is ok and has no effect.
   * @return not documented
   */
  long tims_close(long handle);

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
//  long tims_read_pasef_msms(long handle, long[] precursors, long num_precursors,
//      MsMsCallback my_callback);

  long tims_read_pasef_msms_v2(long handle, long[] precursors, long num_precursors,
      MsMsCallbackV2 callback, Pointer user_data);

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
//  long tims_read_pasef_msms_for_frame(long handle, long frameId,
//      MsMsCallback my_callback);

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
   *                   different order" - I doubt that ~SteffenHeu
   * @param scan_begin first scan number to read (inclusive)
   * @param scan_end   last scan number (exclusive)
   * @param callback   Bruker: "callback accepting the MS/MS spectra" - sounds more like MS1 to me?
   *                   ~SteffenHeu
   * @param user_data  ?????
   * @return 0 on error
   */
  long tims_extract_centroided_spectrum_for_frame(long handle, long frame_id, long scan_begin,
      long scan_end, MsMsCallbackV2 callback, Pointer user_data);

  long tims_extract_profile_for_frame(long handle, long frame_id, long scan_begin, long scan_end,
      ProfileMsMsCallback callback, Pointer userData);

  /**
   * TODO: functions in timsdata.h
   *
   * /// Function type that takes a centroided peak list.
   *     typedef void(msms_spectrum_function)(
   *         int64_t id,                //< the id of the precursor or frame
   *         uint32_t num_peaks,        //< the number of peaks in the MS/MS spectrum
   *         const double *mz_values,   //< all peak m/z values
   *         const float *area_values,  //< all peak areas
   *         void *user_data
   *     );
   *
   *     /// Function type that takes a (non-centroided) profile spectrum.
   *     typedef void(msms_profile_spectrum_function)(
   *         int64_t id,                      //< the id of the precursor or frame
   *         uint32_t num_points,             //< number of entries in the profile spectrum
   *         const int32_t *intensity_values, //< the "quasi profile"
   *         void *user_data
   *     );
   *
   * BdalTimsdataDllSpec uint32_t tims_read_pasef_msms_v2(
   *         uint64_t handle,
   *         const int64_t *precursors,        //< list of PASEF precursor IDs; the returned spectra may be in different order
   *         uint32_t num_precursors,          //< number of requested spectra, must be >= 1
   *         msms_spectrum_function *callback, //< callback accepting the MS/MS spectra
   *         void *user_data                   //< will be passed to callback
   *     );
   * BdalTimsdataDllSpec uint32_t tims_read_pasef_msms_for_frame_v2(
   *         uint64_t handle,
   *         int64_t frame_id,                 //< frame id
   *         msms_spectrum_function *callback, //< callback accepting the MS/MS spectra
   *         void *user_data                   //< will be passed to callback
   *     );
   * BdalTimsdataDllSpec uint32_t tims_read_pasef_profile_msms_v2(
   *         uint64_t handle,
   *         const int64_t *precursors,                //< list of PASEF precursor IDs; the returned spectra may be in different order
   *         uint32_t num_precursors,                  //< number of requested spectra, must be >= 1
   *         msms_profile_spectrum_function *callback, //< callback accepting profile MS/MS spectra
   *         void *user_data                           //< will be passed to callback
   *     );
   * BdalTimsdataDllSpec uint32_t tims_read_pasef_profile_msms_for_frame_v2(
   *         uint64_t handle,
   *         int64_t frame_id,                         //< frame id
   *         msms_profile_spectrum_function *callback, //< callback accepting profile MS/MS spectra
   *         void *user_data                           //< will be passed to callback
   *     );
   *
   * BdalTimsdataDllSpec uint32_t tims_extract_profile_for_frame(
   *         uint64_t handle,
   *         int64_t frame_id,                         //< frame id
   *         uint32_t scan_begin,                      //< first scan number to read (inclusive)
   *         uint32_t scan_end,                        //< last scan number (exclusive)
   *         msms_profile_spectrum_function *callback, //< callback accepting profile MS/MS spectra
   *         void *user_data                           //< will be passed to callback
   *     );
   *
   *
   * typedef uint32_t BdalTimsConversionFunction (
   *         uint64_t handle,
   *         int64_t frame_id,      //< from .tdf SQLite: Frames.Id
   *         const double *index,   //<  in: array of values
   *         double *mz,            //< out: array of values
   *         uint32_t cnt           //< number of values to convert (arrays must have
   *                                //< corresponding size)
   *         );
   *
   *     tims_index_to_mz
   *     tims_mz_to_index
   *
   *     tims_scannum_to_oneoverk0
   *     tims_oneoverk0_to_scannum
   *
   *     tims_scannum_to_voltage
   *     tims_voltage_to_scannum
   *
   * BdalTimsdataDllSpec void tims_set_num_threads (uint32_t n);
   *
   * BdalTimsdataDllSpec double tims_oneoverk0_to_ccs_for_mz(
   *         const double ook0,
   *         const int charge,
   *         const double mz
   *     );
   * BdalTimsdataDllSpec double tims_ccs_to_oneoverk0_for_mz(
   *         const double ccs,
   *         const int charge,
   *         const double mz
   *     );
   */

};
