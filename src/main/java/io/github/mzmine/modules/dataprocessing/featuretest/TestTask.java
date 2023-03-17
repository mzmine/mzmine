package io.github.mzmine.modules.dataprocessing.featuretest;

import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.exceptions.MissingMassListException;

import java.time.Instant;

public class TestTask extends AbstractTask {
    private final Integer minSignals;
    private final ModularFeatureList flists;

    public TestTask(ModularFeatureList flists, ParameterSet parameters, Instant moduleCallDate) {
        super(null, moduleCallDate);


        this.flists = flists;
        minSignals = parameters.getValue(TestParameters.minSignals);
    }

    @Override
    public String getTaskDescription() {
        return "Filter feature list by MS2";
    }

    @Override
    public double getFinishedPercentage() {
        return 0;
    }

    @Override
    public void run() {
        // logic
        setStatus(TaskStatus.PROCESSING);

        flists.getRows().removeIf(row -> checkMinSignals(row));

        setStatus(TaskStatus.FINISHED);
    }

    private boolean checkMinSignals(FeatureListRow row) {
        Scan scan = row.getMostIntenseFragmentScan();
        if (scan == null) {
            return false;
        }
        MassList massList = scan.getMassList();
        if (massList == null) {
            setStatus(TaskStatus.ERROR);
            throw new MissingMassListException(scan);
        }

       return massList.getNumberOfDataPoints() < minSignals;
    }
}