package top.pxczxn.community.admin.query;

import io.swagger.v3.oas.annotations.media.Schema;
import top.pxczxn.platform.common.result.Result;
import top.pxczxn.platform.common.result.PageResult;

/**
 * OpenAPI Schema Helper for AdminCommunityUserResponse PageResult
 *
 * 用于帮助 Springdoc 正确生成泛型类型的 OpenAPI Schema
 */
@Schema(description = "社区用户分页响应（用于OpenAPI文档生成）")
class AdminCommunityUserPageResult extends Result<PageResult<AdminCommunityUserResponse>> {
    // This class is only for OpenAPI documentation generation
    // It should never be instantiated
}
