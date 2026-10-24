INSERT INTO prompt_version (name, version_no, template, status, created_by, approved_by) VALUES
 ('claims-summary', 1, 'Summarise this insurance claim for an adjuster. Claim notes: {{notes}}', 'ACTIVE', 'priya.shah', 'marcus.lee'),
 ('claims-summary', 2, 'You are a claims assistant. Reply with three bullet points. Claim notes: {{notes}} Policy type: {{policyType}}', 'DRAFT', 'priya.shah', NULL),
 ('support-faq', 1, 'Answer the customer politely and briefly. Question: {{question}}', 'ACTIVE', 'dana.ortiz', 'marcus.lee');

INSERT INTO team_budget (team_id, monthly_token_limit, tokens_used) VALUES
 ('claims-team', 50000, 1200),
 ('marketing-team', 300, 250);
