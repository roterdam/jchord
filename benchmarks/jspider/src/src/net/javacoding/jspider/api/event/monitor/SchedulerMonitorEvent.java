package net.javacoding.jspider.api.event.monitor;

/**
 * $Id: SchedulerMonitorEvent.java,v 1.4 2003/04/03 15:57:14 vanrogu Exp $
 */
public class SchedulerMonitorEvent extends MonitorEvent {

    protected int jobCount;
    protected int spiderJobsCount;
    protected int thinkerJobsCount;
    protected int jobsDone;
    protected int spiderJobsDone;
    protected int thinkerJobsDone;
    protected int blockedCount;
    protected int assignedCount;

    public SchedulerMonitorEvent ( int jobCount, int spiderJobsCount, int thinkerJobsCount, int jobsDone, int spiderJobsDone, int thinkerJobsDone, int blockedCount, int assignedCount ) {
        this.jobCount= jobCount;
        this.spiderJobsCount= spiderJobsCount;
        this.thinkerJobsCount= thinkerJobsCount;
        this.jobsDone = jobsDone;
        this.spiderJobsDone = spiderJobsDone;
        this.thinkerJobsDone = thinkerJobsDone;
        this.blockedCount = blockedCount;
        this.assignedCount = assignedCount;
    }

    public String toString ( ) {
        int pctDone = 0;
        if ( getJobCount() != 0 ) {
            pctDone = ((getJobsDone()*100)/getJobCount());
        }
        int spidersPctDone = 0;
        if ( getSpiderJobsCount() != 0) {
            spidersPctDone = ((getSpiderJobsDone()*100)/getSpiderJobsCount());
        }
        int thinkersPctDone = 0;
        if ( getThinkerJobsCount() != 0) {
            thinkersPctDone = ((getThinkerJobsDone()*100)/getThinkerJobsCount());
        }
        return "Job monitor: " + pctDone + "% (" + getJobsDone() + "/" + getJobCount() + ")  [S:" + spidersPctDone + "% (" + getSpiderJobsDone() + "/" + getSpiderJobsCount() + ") | T:" + thinkersPctDone + "% (" + getThinkerJobsDone() + "/" + getThinkerJobsCount() + ")] [blocked:" + blockedCount + "] [assigned:" + assignedCount + "]";
    }

    public String getComment() {
        return toString ( );
    }

    public int getJobCount() {
        return jobCount;
    }

    public int getSpiderJobsCount() {
        return spiderJobsCount;
    }

    public int getThinkerJobsCount() {
        return thinkerJobsCount;
    }

    public int getJobsDone() {
        return jobsDone;
    }

    public int getSpiderJobsDone() {
        return spiderJobsDone;
    }

    public int getThinkerJobsDone() {
        return thinkerJobsDone;
    }

    public int getBlockedCount ( ) {
        return blockedCount;
    }

    public int getAssignedCount ( ){
        return assignedCount;
    }

}
