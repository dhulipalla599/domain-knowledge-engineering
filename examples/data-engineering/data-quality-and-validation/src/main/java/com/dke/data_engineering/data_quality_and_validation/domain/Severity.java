package com.dke.data_engineering.data_quality_and_validation.domain;

/** ERROR rejects the row; WARN keeps the row but records the finding. */
public enum Severity { ERROR, WARN }
