package com.dke.ai_engineering.llm_gateway_and_prompt_management.domain;

import jakarta.persistence.*;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name", "version_no"}))
public class PromptVersion {

    private static final Pattern VARIABLE = Pattern.compile("\\{\\{(\\w+)}}");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Column(nullable = false, length = 2000)
    private String template;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromptStatus status = PromptStatus.DRAFT;

    @Column(nullable = false)
    private String createdBy;

    private String approvedBy;

    protected PromptVersion() {
    }

    public PromptVersion(String name, int versionNo, String template, String createdBy) {
        this.name = name;
        this.versionNo = versionNo;
        this.template = template;
        this.createdBy = createdBy;
    }

    public void approve(String approver) {
        moveTo(PromptStatus.APPROVED);
        this.approvedBy = approver;
    }

    public void activate() {
        moveTo(PromptStatus.ACTIVE);
    }

    public void retire() {
        moveTo(PromptStatus.RETIRED);
    }

    private void moveTo(PromptStatus next) {
        if (!status.canMoveTo(next)) {
            throw new IllegalStateException(
                    "Prompt %s v%d is %s and cannot become %s".formatted(name, versionNo, status, next));
        }
        this.status = next;
    }

    /** Fills {{variable}} placeholders; a missing value is a caller error, never an empty string. */
    public String render(Map<String, String> values) {
        Matcher m = VARIABLE.matcher(template);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String value = values.get(m.group(1));
            if (value == null) {
                throw new IllegalArgumentException("Missing variable: " + m.group(1));
            }
            m.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        m.appendTail(out);
        return out.toString();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public int getVersionNo() { return versionNo; }
    public String getTemplate() { return template; }
    public PromptStatus getStatus() { return status; }
    public String getCreatedBy() { return createdBy; }
    public String getApprovedBy() { return approvedBy; }
}
