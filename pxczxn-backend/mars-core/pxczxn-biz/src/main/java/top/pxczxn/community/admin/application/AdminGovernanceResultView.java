package top.pxczxn.community.admin.application;

import java.util.List;

public record AdminGovernanceResultView(
        Long id,
        String status,
        int lockVersion,
        boolean replay,
        int affectedCount,
        List<Long> affectedIds
) {
}
