package top.pxczxn.community.sanction.application;

import java.util.Set;

public enum SanctionType {
    WARNING, RATE_LIMIT, COMMENT_BAN, MOMENT_BAN, SUBMISSION_BAN, PUBLISH_SUSPEND, MESSAGE_BAN, LOGIN_SUSPEND, PERMANENT_BAN;
    public static final Set<SanctionType> TIMED = Set.of(RATE_LIMIT, COMMENT_BAN, MOMENT_BAN, SUBMISSION_BAN, PUBLISH_SUSPEND, MESSAGE_BAN, LOGIN_SUSPEND);
    public boolean requiresExpiry() { return TIMED.contains(this); }
}
