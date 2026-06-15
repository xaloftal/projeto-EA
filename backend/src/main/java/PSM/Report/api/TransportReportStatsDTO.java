package PSM.Report.api;

import java.util.Map;

public class TransportReportStatsDTO {
    private long totalValidations;
    private long successfulValidations;
    private long failedValidations;
    private Map<String, Long> validationsByDayOfWeek;
    private Map<Integer, Long> validationsByHour;
    private Map<String, Long> validationsByStop;

    public TransportReportStatsDTO() {}

    public TransportReportStatsDTO(long totalValidations, long successfulValidations, long failedValidations,
                                   Map<String, Long> validationsByDayOfWeek, Map<Integer, Long> validationsByHour,
                                   Map<String, Long> validationsByStop) {
        this.totalValidations = totalValidations;
        this.successfulValidations = successfulValidations;
        this.failedValidations = failedValidations;
        this.validationsByDayOfWeek = validationsByDayOfWeek;
        this.validationsByHour = validationsByHour;
        this.validationsByStop = validationsByStop;
    }

    public long getTotalValidations() { return totalValidations; }
    public void setTotalValidations(long totalValidations) { this.totalValidations = totalValidations; }

    public long getSuccessfulValidations() { return successfulValidations; }
    public void setSuccessfulValidations(long successfulValidations) { this.successfulValidations = successfulValidations; }

    public long getFailedValidations() { return failedValidations; }
    public void setFailedValidations(long failedValidations) { this.failedValidations = failedValidations; }

    public Map<String, Long> getValidationsByDayOfWeek() { return validationsByDayOfWeek; }
    public void setValidationsByDayOfWeek(Map<String, Long> validationsByDayOfWeek) { this.validationsByDayOfWeek = validationsByDayOfWeek; }

    public Map<Integer, Long> getValidationsByHour() { return validationsByHour; }
    public void setValidationsByHour(Map<Integer, Long> validationsByHour) { this.validationsByHour = validationsByHour; }

    public Map<String, Long> getValidationsByStop() { return validationsByStop; }
    public void setValidationsByStop(Map<String, Long> validationsByStop) { this.validationsByStop = validationsByStop; }
}
