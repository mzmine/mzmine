package net.sf.mzmine.modules.visualization.peaklist;

import java.util.logging.Logger;

import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.ConstructionPeak;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ScanUtils;

class ManuallyDefinePeakTask implements Task {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    private TaskStatus status;
    private String errorMessage;

    private int processedScans;
    private int totalScans;
    
    private PeakListRow selectedRow;
    private RawDataFile selectedFile;
    private float minRT, maxRT, minMZ, maxMZ;
    
    ManuallyDefinePeakTask(PeakListRow selectedRow,
            RawDataFile selectedFile, float minRT, float maxRT, float minMZ,
            float maxMZ) {
        
        status = TaskStatus.WAITING;
        
        this.selectedRow = selectedRow;
        this.selectedFile = selectedFile;
        
        this.minRT = minRT;
        this.maxRT = maxRT;
        this.minMZ = minMZ;
        this.maxMZ = maxMZ;

    }

    public void cancel() {
        status = TaskStatus.CANCELED;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public float getFinishedPercentage() {
        if (totalScans == 0)
            return 0.0f;
        return (float) processedScans / totalScans;
    }

    public Object getResult() {
        return null;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getTaskDescription() {
        return "Manually picking a peak from " + selectedFile;
    }

    public void run() {
        
        status = TaskStatus.PROCESSING;
        
        logger.finest("Starting manual peak picker, RT: " + minRT + " - " + maxRT + ", m/z: " + minMZ + " - " + maxMZ);

        int[] scanNumbers = selectedFile.getScanNumbers(1, minRT, maxRT);
        totalScans = scanNumbers.length;
        
        ConstructionPeak ucPeak = new ConstructionPeak(selectedFile);
        
        for (int i = 0; i < totalScans; i++) {

            if (status == TaskStatus.CANCELED)
                return;

            // Get next scan
            Scan scan = selectedFile.getScan(scanNumbers[i]);
            
            float basePeak[] = ScanUtils.findBasePeak(scan, minMZ, maxMZ);
            ucPeak.addDatapoint(scan.getScanNumber(), basePeak[0],
                    scan.getRetentionTime(), basePeak[1]);
            
            processedScans++;
            
        }
        
        ucPeak.finalizedAddingDatapoints();
        
        selectedRow.addPeak(selectedFile, ucPeak, ucPeak);

        logger.finest("Finished manual peak picker, " + processedScans + " scans processed");

        status = TaskStatus.FINISHED;
        
    }

}
