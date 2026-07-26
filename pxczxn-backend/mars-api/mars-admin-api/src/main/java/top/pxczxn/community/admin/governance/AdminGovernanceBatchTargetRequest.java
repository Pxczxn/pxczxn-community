package top.pxczxn.community.admin.governance;

public record AdminGovernanceBatchTargetRequest(
        String id,
        Integer expectedLockVersion
) {
}
