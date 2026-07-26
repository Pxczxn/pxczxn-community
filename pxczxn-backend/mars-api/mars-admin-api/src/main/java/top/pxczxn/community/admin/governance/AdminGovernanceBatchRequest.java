package top.pxczxn.community.admin.governance;

import java.util.List;

public record AdminGovernanceBatchRequest(
        List<AdminGovernanceBatchTargetRequest> targets,
        String reason
) {
}
