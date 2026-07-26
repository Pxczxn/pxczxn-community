package top.pxczxn.community.admin.application;

public record AdminGovernanceTarget(
        Long id,
        Integer expectedLockVersion
) {
}
