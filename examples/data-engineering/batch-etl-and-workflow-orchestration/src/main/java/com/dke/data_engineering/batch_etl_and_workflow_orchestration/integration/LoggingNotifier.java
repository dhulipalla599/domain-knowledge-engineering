package com.dke.data_engineering.batch_etl_and_workflow_orchestration.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotifier.class);

    @Override
    public void alert(String message) {
        log.warn("ALERT: {}", message);
    }
}
