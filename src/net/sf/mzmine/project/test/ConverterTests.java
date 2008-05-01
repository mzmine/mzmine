package net.sf.mzmine.project.test;
import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)                           
@SuiteClasses( { 

	RawDataFileConverter_NormalModeTest.class,
	RawDataFileConverter_SimpleModeTest.class,
	PeakListConverter_SimplePeakListTest.class,
	SimpleDataPointConverterTest.class
	})
public class ConverterTests {
	public static void main(String[] args) {
		JUnitCore.main(ConverterTests.class.getName());
	}
}
