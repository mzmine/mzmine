package io.github.mzmine.modules.io.tdfimport;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Paths;


public class TDFExample {

	/*
	 * You need a JDBC driver for sqlite. I used sqlite-jdbc-3.8.11.1.jar. This driver uses the native shared libraries for
	 * the different OSs and should be fairly fast.
	 */
	public static void main(String[] args) throws ClassNotFoundException {

		if (1 != args.length) {
			System.out.println("enter path of .d directory");
			return;
		}

		// load the sqlite-JDBC driver using the current class loader
		Class.forName("org.sqlite.JDBC");

		Connection connection = null;
		try {
			// create a database connection
			String tdf_path = Paths.get(args[0], "analysis.tdf").toString();
			connection = DriverManager.getConnection("jdbc:sqlite:" + tdf_path);
			// the jdbc driver has a strange behaviour when the sqlite file cannot be found: It throws an exception with 
			// message: [SQLITE_CANTOPEN]  Unable to open the database file (out of memory) ...
			// so if you get that exception check your path, filename, rights ...
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30); // set timeout to 30 sec.

			// Example: read frame parameter
			ResultSet rsFrames = statement.executeQuery("SELECT COUNT(*) FROM Frames");
			rsFrames.next();
			int numFrames = rsFrames.getInt(1);
			rsFrames.close();
			System.out.printf("number of TIMS frames: %d", numFrames).println();

			// Example: check used scan modes
			rsFrames = statement.executeQuery("SELECT DISTINCT ScanMode FROM Frames");
			while (rsFrames.next()) {
				int scanmode = rsFrames.getInt(1);
				System.out.printf("scan mode: %d", scanmode).println();
			}

			rsFrames.close();

			// If you want to read a frame you need just need to know the frame id and the number of scans. The scans are then
			// read using the timsdata shared library (.dll or .so)
			int frameId = 23;
			rsFrames = statement.executeQuery(String
					.format("SELECT NumScans, NumPeaks FROM Frames WHERE Id BETWEEN 20 AND 30", frameId));
			while (rsFrames.next()) {
				int num_scans = rsFrames.getInt(1);
				int num_peaks = rsFrames.getInt(2);
				System.out.printf("frame %d, number of scans=%d, number of peaks=%d", frameId, num_scans,
						num_peaks).println();
			}
			rsFrames.close();
			System.out.println("---------------------------------------------------------");

			// if you want to read a value, use the Properties view
			rsFrames = statement.executeQuery(
					"SELECT Frame, Value FROM Properties WHERE Property=(SELECT Id FROM PropertyDefinitions WHERE PermanentName='TOF_DeviceTempCurrentValue1') AND Frame BETWEEN 20 AND 30");
			while (rsFrames.next()) {
				frameId = rsFrames.getInt(1);
				double temp = rsFrames.getDouble(2);
				System.out.printf("frame %d TOF_DeviceTempCurrentValue1: %f", frameId, temp).println();
			}

			rsFrames.close();

			statement.close();
		} catch (SQLException e) {
			// if the error message is "out of memory",
			// it probably means no database file is found
			System.err.println(e.getMessage());
		} finally {
			try {
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException e) {
				// connection close failed.
				System.err.println(e);
			}
		}
	}

//	private static int getTableCount() {
//		
//	}

}
