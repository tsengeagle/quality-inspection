package com.tzuchi.his.quality.inspection.reporting;

public class DashboardSummary {

    private final long totalCases;
    private final long closedCases;
    private final long rereviewedCases;
    private final long overdueCases;
    private final double completionRate;
    private final double rereviewRate;
    private final double overdueRate;

    public DashboardSummary(long totalCases, long closedCases,
                             long rereviewedCases, long overdueCases) {
        this.totalCases = totalCases;
        this.closedCases = closedCases;
        this.rereviewedCases = rereviewedCases;
        this.overdueCases = overdueCases;
        this.completionRate = totalCases > 0 ? (double) closedCases / totalCases : 0.0;
        this.rereviewRate = totalCases > 0 ? (double) rereviewedCases / totalCases : 0.0;
        this.overdueRate = totalCases > 0 ? (double) overdueCases / totalCases : 0.0;
    }

    public long getTotalCases() { return totalCases; }
    public long getClosedCases() { return closedCases; }
    public long getRereviewedCases() { return rereviewedCases; }
    public long getOverdueCases() { return overdueCases; }
    public double getCompletionRate() { return completionRate; }
    public double getRereviewRate() { return rereviewRate; }
    public double getOverdueRate() { return overdueRate; }
}
