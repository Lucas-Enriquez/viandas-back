CREATE TABLE global_invitation (
    id UUID PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    max_uses INTEGER,
    used_count INTEGER NOT NULL DEFAULT 0,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_global_invitation_max_uses_positive CHECK (max_uses IS NULL OR max_uses > 0),
    CONSTRAINT ck_global_invitation_used_count_non_negative CHECK (used_count >= 0)
);

CREATE INDEX idx_global_invitation_company_active ON global_invitation(company_id, active);
