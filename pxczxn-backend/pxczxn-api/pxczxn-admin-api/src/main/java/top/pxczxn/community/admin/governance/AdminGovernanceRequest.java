package top.pxczxn.community.admin.governance;

public record AdminGovernanceRequest(
        Integer expectedLockVersion,
        String reason
) {
}
