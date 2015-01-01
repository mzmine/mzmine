/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection;

import org.rosuda.JRI.Rengine;
import net.sf.mzmine.util.RUtilities;

import java.util.logging.Logger;

/**
 * @description TODO
 * @author Gauthier Boaglio
 * @date Nov 19, 2014
 */
public class RSession {

    // Logger.
    private static final Logger LOG = Logger
	    .getLogger(RSession.class.getName());

    public enum RengineType {

	JRIengine("JRIengine - mono-instance engine"), RCaller(
		"RCaller - multi-instance engine");

	private String type;

	RengineType(String type) {
	    this.type = type;
	}

	public String toString() {
	    return type;
	}

    }

    private RengineType rEngineType;
    private Object rEngine = null;
    private String[] reqPackages;

    // private RCode rcallerCode;

    public RSession(RengineType type, String[] reqPackages) {
	this.rEngineType = type;
	this.reqPackages = reqPackages;
    }

    // // Since the batch launcher already added the correct paths
    // private void setExecutablePaths() {
    // Globals.R_Windows = RUtilities.getRexecutablePath(); //"R.exe";
    // Globals.R_Linux = RUtilities.getRexecutablePath();//"/usr/bin/R";
    // Globals.RScript_Windows = Globals.R_Windows.substring(0,
    // Globals.R_Windows.length()-5) + "Rscript.exe";//"Rscript.exe";
    // Globals.RScript_Linux = Globals.R_Linux.substring(0,
    // Globals.R_Linux.length()-1) + "Rscript";//"/usr/bin/Rscript";
    // Globals.detect_current_rscript();
    // }

    private void getRengineInstance() {

	LOG.info("getRengineInstance() for: " + this.rEngineType.toString());

	try {
	    if (this.rEngineType == RengineType.JRIengine) {
		// Get JRI engine unique instance.
		this.rEngine = RUtilities.getREngine();
		// Quick test
		LOG.info(((Rengine) this.rEngine).eval("R.version.string")
			.asString());
	    } else {
		// if (this.rEngine == null) {
		// // Create RCaller new instance for this task.
		//
		// LOG.info("PATHS: R=" + Globals.R_Linux + "  |  " +
		// Globals.RScript_Linux);
		// LOG.info("PATHS: R=" + Globals.R_current + "  |  " +
		// Globals.Rscript_current);
		//
		// RCaller rcaller = new RCaller();
		// this.setExecutablePaths();
		// rcaller.setRExecutable(Globals.R_current);
		// rcaller.setRscriptExecutable(Globals.Rscript_current);
		// this.rcallerCode = new RCode();
		// rcaller.setRCode(this.rcallerCode);
		// this.rEngine = rcaller;
		// }
	    }
	} catch (Throwable t) {
	    t.printStackTrace();
	    throw new IllegalStateException(
		    "This feature requires R but it couldn't be loaded ("
			    + t.getMessage() + ')');
	}
    }

    public RengineType getRengineType() {
	return this.rEngineType;
    }

    // TODO: fix testing packages with RCaller
    public void loadPackage(String packageName) {

	String loadCode = "library(" + packageName + ", logical.return = TRUE)";
	String errorMsg = "The \"" + packageName
		+ "\" R package couldn't be loaded - is it installed in R?";

	if (this.rEngineType == RengineType.JRIengine) {
	    synchronized (RUtilities.R_SEMAPHORE) {
		if (((Rengine) this.rEngine).eval(loadCode).asBool().isFALSE()) {
		    throw new IllegalStateException(errorMsg);
		}
	    }
	} else {
	    // RCaller rcaller = ((RCaller) this.rEngine);
	    // String logicalRet = "pkgOK";
	    // this.rcallerCode.addRCode(logicalRet + " <- " + loadCode);
	    // rcaller.runAndReturnResultOnline(logicalRet);
	    // //this.rcallerCode.clear();
	    // if (rcaller.getParser().getAsLogicalArray(logicalRet)[0]) {
	    // throw new IllegalStateException(errorMsg);
	    // }
	    // this.rcallerCode.R_require(packageName);
	}
    }

    public String loadRequiredPackages() {

	String reqPackage = null;
	try {
	    for (int i = 0; i < this.reqPackages.length; ++i) {
		reqPackage = this.reqPackages[i];
		this.loadPackage(this.reqPackages[i]);
		LOG.info("Loaded package: " + reqPackage + "'.");
	    }
	    return null;
	} catch (Exception e) {
	    return reqPackage;
	}
    }

    // TODO: templatize: assignDoubleArray<T>(String objName, T obj)
    public void assignDoubleArray(String objName, double[] dArray) {
	// LOG.info("Assign '" + dArray + "' array to object '" + objName +
	// "'.");
	if (this.rEngineType == RengineType.JRIengine) {
	    // synchronized (RUtilities.R_SEMAPHORE) {
	    ((Rengine) this.rEngine).assign(objName, dArray);
	    // }
	} else {
	    // //synchronized (RUtilities.R_SEMAPHORE) {
	    // this.rcallerCode.addDoubleArray(objName, dArray);
	    // //}
	}
	// LOG.info("Assign '" + dArray + "' array to object '" + objName +
	// "' DONE!");
    }

    public void eval(String rCode) {
	// LOG.info("Eval: " + rCode);
	if (this.rEngineType == RengineType.JRIengine) {
	    // synchronized (RUtilities.R_SEMAPHORE) {
	    ((Rengine) this.rEngine).eval(rCode);
	    // }
	} else {
	    // this.rcallerCode.addRCode(rCode);
	    // //LOG.info("Eval code: " +
	    // this.rcallerCode.getCode().toString());
	}
    }

    // TODO: templatize: T collectDoubleArray(String objName)
    public double[] collectDoubleArray(String objName) {
	if (this.rEngineType == RengineType.JRIengine) {
	    // synchronized (RUtilities.R_SEMAPHORE) {
	    return ((Rengine) this.rEngine).eval(objName).asDoubleArray();
	    // }
	} else {
	    // RCaller rcaller = ((RCaller) this.rEngine);
	    // rcaller.runAndReturnResultOnline(objName);
	    // double[] dArray = rcaller.getParser().getAsDoubleArray(objName);
	    // //LOG.info("BEFORE: " + this.rcallerCode);
	    // this.rcallerCode.clearOnline();
	    // return dArray;
	}
	return null;
    }

    public void open() {
	// Load engine
	getRengineInstance();
	// // Check & load packages
	// this.loadRequiredPackages();
    }

    public void close() {

	// if (this.rEngineType == RengineType.RCaller) {
	// RCaller rcaller = ((RCaller) this.rEngine);
	// rcaller.stopStreamConsumers();
	// rcaller.StopRCallerOnline();
	// }

    }

}
