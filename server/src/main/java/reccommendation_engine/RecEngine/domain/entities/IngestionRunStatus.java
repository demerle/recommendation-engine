package reccommendation_engine.RecEngine.domain.entities;

public enum IngestionRunStatus {
    QUEUED,
    RUNNING,
    PARTIAL,
    FAILED,
    SUCCEEDED,
    RETRIED
}
