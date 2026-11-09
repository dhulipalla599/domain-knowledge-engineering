package com.dke.data_engineering.batch_etl_and_workflow_orchestration.integration;

/** Alerting port. Production adapter: SNS topic to PagerDuty/Slack. */
public interface Notifier {

    void alert(String message);
}
