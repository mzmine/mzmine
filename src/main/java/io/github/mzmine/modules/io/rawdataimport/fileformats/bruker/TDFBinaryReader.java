package io.github.mzmine.modules.io.rawdataimport.fileformats.bruker;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TDFBinaryReader {

  public interface TDFLibrary extends Library {

    String libraryPath = System.getProperty("user.dir") + File.separator + "lib" + File.separator
        + "vendor_lib" + File.separator + "bruker" + File.separator + "timsdata.dll";

    //    TDFLibrary INSTANCE = (TDFLibrary) Native.load(libraryPath.toString(), TDFLibrary.class);
    TDFLibrary INSTANCE = (TDFLibrary) Native.load("timsdata", TDFLibrary.class);

    long tims_open(String analysis_dir, long use_recalib);

    long tims_close(long handle);

    long tims_get_last_error_string(byte[] error, long len);

    long tims_read_scans_v2(long handle, long frameId, long scanBegin, long scanEnd,
        byte[] scanBuffer, long len);

    long tims_index_to_mz(long handle, long frameId, double[] index, double[] mz, long len);

    long tims_scannum_to_oneoverk0(long handle, long frameId, double[] scannum, double[] oneOverK0,
        long len);

    interface MsMsCallback extends Callback {

      void invoke(long precursor_id, int num_peaks, Pointer pMz, Pointer pIntensites);
    }

    long tims_read_pasef_msms(long handle, long[] precursors, long num_precursors,
        MsMsCallback my_callback);

    long tims_read_pasef_msms_for_frame(long handle, long frameId, MsMsCallback my_callback);
  }

  public static class MsMsData implements TDFLibrary.MsMsCallback {

    public long precursorId = 0;
    public int numPeaks = 0;
    public double[] mz_values;
    public float[] intensity_values;

    @Override
    public void invoke(long precursor_id, int num_peaks, Pointer pMz, Pointer pIntensites) {
      precursorId = precursor_id;
      numPeaks = num_peaks;
      mz_values = pMz.getDoubleArray(0, num_peaks);
      intensity_values = pIntensites.getFloatArray(0, num_peaks);
    }

  }

  public static class MsMsDataFrame implements TDFLibrary.MsMsCallback {

    public class MsMsSpectrum {

      public double[] mz_values;
      public float[] intensity_values;
    }

    Map<Long, MsMsSpectrum> msmsSpectra = new HashMap<Long, MsMsSpectrum>();
    public long precursorId = 0;

    @Override
    public void invoke(long precursor_id, int num_peaks, Pointer pMz, Pointer pIntensites) {
      MsMsSpectrum msms_spectrum = new MsMsSpectrum();
      msms_spectrum.mz_values = pMz.getDoubleArray(0, num_peaks);
      msms_spectrum.intensity_values = pIntensites.getFloatArray(0, num_peaks);
      msmsSpectra.put(precursor_id, msms_spectrum);
    }

  }

  // I know this is really clumsy, I wish I could use Arrays.stream(int[]).asDoubleStream().toArray();
  public static double[] copyFromIntArray(int[] source) {
    double[] dest = new double[source.length];
    for (int i = 0; i < source.length; i++) {
      dest[i] = source[i];
    }
    return dest;
  }

  public static void main(String[] args) throws UnsupportedEncodingException {

//		if (1!= args.length) {
//			System.out.println("enter path of .d directory");
//			return;
//		}

    long handle = TDFLibrary.INSTANCE
        .tims_open("/home/knakul853/Desktop/gsocData/200ngHeLaPASEF_2min_compressed.d",
            0);
    System.out.printf("Open tims_bin, handle: %d", handle).println();

    byte[] errorBuffer = new byte[64];
    long len = TDFLibrary.INSTANCE.tims_get_last_error_string(errorBuffer, errorBuffer.length);
    String errorMessage = new String(errorBuffer, "UTF-8");
    System.out.printf("Last error: %s, length %d", errorMessage, len).println();

    if (0 != handle) {
      // just pick frame with id 1 as an example. Frame ids start with 1. Usually you have to query the number of
      // scans from analysis.tdf sqlite db. Here we just take the first 100 as an example
      long frameId = 1;
      int numScans = 50;
      byte[] buffer = new byte[200000];
      long buf_len = TDFLibrary.INSTANCE
          .tims_read_scans_v2(handle, frameId, 0, numScans, buffer, buffer.length);

      if (buf_len == 0) {
        System.out.printf("Error reading scans for frame %d", frameId).println();
      }

      System.out.printf("reading frame %d, buffer length: %d", frameId, buf_len).println();
      IntBuffer intBuf = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
      int[] scanBuffer = new int[intBuf.remaining()];
      intBuf.get(scanBuffer);
      System.out.println(intBuf.toString());
      // check out the layout of scanBuffer:
      // - the first numScan integers specify the number of peaks for each scan
      // - the next integers are pairs of (x,y) values for the scans. The x values are not masses but index values
      double[][] masses = new double[numScans][];
      int[][] intensities = new int[numScans][];

      long error_stat = 0;
      int d = numScans;
      for (int i = 0; i < numScans; ++i) {
        int numberPeaks = scanBuffer[i];
        int[] indices = Arrays.copyOfRange(scanBuffer, d, d + numberPeaks);
        d += numberPeaks;
        intensities[i] = Arrays.copyOfRange(scanBuffer, d, d + numberPeaks);
        d += numberPeaks;

        // you are not really interested in index values for x-axis but masses
        masses[i] = new double[indices.length];
        error_stat = TDFLibrary.INSTANCE
            .tims_index_to_mz(handle, frameId, copyFromIntArray(indices), masses[i],
                masses[i].length);
        if (0 == error_stat) {
          System.out.println("could not convert indices to masses");
        }
        System.out.printf("scan %d has %d peaks", i, numberPeaks).println();
        System.out.println(Arrays.toString(masses[i]));
        System.out.println(Arrays.toString(intensities[i]));
      }

      System.out.println("---------------------------------------------------------");

      // correct axis for mobilogram is 1/K0. Use tims transformation function for that
      double[] scannums = new double[numScans];
      double[] oneOverK0 = new double[numScans];
      for (int i = 0; i < numScans; ++i) {
        scannums[i] = numScans - 1 - i;
      } // unfortunately cannot use java 8 here ...

      error_stat = TDFLibrary.INSTANCE
          .tims_scannum_to_oneoverk0(handle, frameId, scannums, oneOverK0, numScans);
      if (0 == error_stat) {
        System.out.println("could not convert scan numbers to 1/K0");
      }

      System.out.println(Arrays.toString(oneOverK0));

      System.out.println("---------------------------------------------------------");

      // get msms spectrum for precursor_id
      TDFBinaryReader.MsMsData msmsData = new TDFBinaryReader.MsMsData();

      long precursor_id = 1;
      error_stat = TDFLibrary.INSTANCE
          .tims_read_pasef_msms(handle, new long[]{precursor_id}, 1, msmsData);
      if (0 == error_stat) {
        System.out.printf("could not get msms spectrum for precursor id %d", precursor_id);
      }

      System.out.printf("msms data, precursor_id %d, number of peaks %d", msmsData.precursorId,
          msmsData.numPeaks).println();
      System.out.println(Arrays.toString(msmsData.mz_values));
      System.out.println(Arrays.toString(msmsData.intensity_values));

      // get all msms spectra for whole frame
      TDFBinaryReader.MsMsDataFrame msmsDataFrame = new TDFBinaryReader.MsMsDataFrame();
      error_stat = TDFLibrary.INSTANCE
          .tims_read_pasef_msms_for_frame(handle, frameId, msmsDataFrame);
      if (0 == error_stat) {
        System.out.printf("could not get msms spectra for frame %d", frameId);
      }

      // print out all msms spectra
      for (Map.Entry<Long, TDFBinaryReader.MsMsDataFrame.MsMsSpectrum> msmsEntry : msmsDataFrame.msmsSpectra
          .entrySet()) {
        System.out.printf("msms data, precursor_id %d", msmsEntry.getKey()).println();
        System.out.println(Arrays.toString(msmsEntry.getValue().mz_values));
        System.out.println(Arrays.toString(msmsEntry.getValue().intensity_values));
      }
    }

    handle = TDFLibrary.INSTANCE.tims_close(handle);
    System.out.println("---------------------------------------------------------");
    System.out.printf("Open tims_bin, handle: %d", handle).println();
  }
}