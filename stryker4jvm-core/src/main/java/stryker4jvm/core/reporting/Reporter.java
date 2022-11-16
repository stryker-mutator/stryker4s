package stryker4jvm.core.reporting;

import stryker4jvm.core.reporting.events.FinishedRunEvent;
import stryker4jvm.core.reporting.events.MutantTestedEvent;

public abstract class Reporter {
    public abstract void mutantTested(MutantTestedEvent event);
    public abstract void onRunFinished(FinishedRunEvent event);
}
