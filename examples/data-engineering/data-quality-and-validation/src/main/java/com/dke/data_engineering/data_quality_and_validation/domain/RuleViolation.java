package com.dke.data_engineering.data_quality_and_validation.domain;

import jakarta.persistence.*;

/** A failed check on one row. ERROR rows are kept here (quarantine) and never loaded. */
@Entity
@Table(name = "rule_violation")
public class RuleViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long runId;

    private int lineNumber;

    @Column(nullable = false)
    private String ruleCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    private String message;

    @Column(length = 1000)
    private String rawLine;

    protected RuleViolation() { }

    public RuleViolation(Long runId, int lineNumber, String ruleCode, Severity severity, String message, String rawLine) {
        this.runId = runId;
        this.lineNumber = lineNumber;
        this.ruleCode = ruleCode;
        this.severity = severity;
        this.message = message;
        this.rawLine = rawLine;
    }

    public Long getId() { return id; }
    public Long getRunId() { return runId; }
    public int getLineNumber() { return lineNumber; }
    public String getRuleCode() { return ruleCode; }
    public Severity getSeverity() { return severity; }
    public String getMessage() { return message; }
    public String getRawLine() { return rawLine; }
}
