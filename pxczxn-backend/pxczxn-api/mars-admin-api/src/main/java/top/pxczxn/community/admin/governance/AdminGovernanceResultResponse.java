package top.pxczxn.community.admin.governance;

import top.pxczxn.community.admin.application.AdminGovernanceResultView;

import java.util.List;

public record AdminGovernanceResultResponse(
        String id,
        String status,
        int lockVersion,
        boolean replay,
        int affectedCount,
        List<String> affectedIds
) {

    static AdminGovernanceResultResponse from(
            AdminGovernanceResultView view
    ) {
        return new AdminGovernanceResultResponse(
                id(view.id()),
                view.status(),
                view.lockVersion(),
                view.replay(),
                view.affectedCount(),
                view.affectedIds().stream()
                        .map(AdminGovernanceResultResponse::id)
                        .toList()
        );
    }

    private static String id(Long value) {
        return value == null ? null : value.toString();
    }
}
