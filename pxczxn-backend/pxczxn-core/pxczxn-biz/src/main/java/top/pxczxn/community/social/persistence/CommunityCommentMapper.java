package top.pxczxn.community.social.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.pxczxn.community.social.model.CommunityComment;

import java.util.List;

public interface CommunityCommentMapper extends BaseMapper<CommunityComment> {

    @Select("""
            SELECT root.*
            FROM community_comment root
            WHERE root.target_type = #{targetType}
              AND root.target_id = #{targetId}
              AND root.root_comment_id IS NULL
              AND root.parent_comment_id IS NULL
              AND (
                    root.status = 'PUBLISHED'
                    OR (
                        root.status = 'DELETED_BY_USER'
                        AND EXISTS (
                            SELECT 1
                            FROM community_comment reply
                            WHERE reply.root_comment_id = root.id
                              AND reply.status IN (
                                  'PUBLISHED', 'DELETED_BY_USER'
                              )
                        )
                    )
              )
            ORDER BY root.created_at DESC, root.id DESC
            """)
    List<CommunityComment> findPublicRoots(
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId
    );

    @Select("""
            SELECT *
            FROM community_comment
            WHERE root_comment_id = #{rootCommentId}
              AND status IN ('PUBLISHED', 'DELETED_BY_USER')
            ORDER BY created_at, id
            """)
    List<CommunityComment> findPublicReplies(
            @Param("rootCommentId") Long rootCommentId
    );

    @Select("""
            SELECT *
            FROM community_comment
            WHERE (
                    id = #{rootCommentId}
                    OR root_comment_id = #{rootCommentId}
                  )
              AND status = 'PUBLISHED'
            ORDER BY created_at, id
            """)
    List<CommunityComment> findPublishedThread(
            @Param("rootCommentId") Long rootCommentId
    );

    @Select("""
            SELECT *
            FROM community_comment
            WHERE (
                    id = #{rootCommentId}
                    OR root_comment_id = #{rootCommentId}
                  )
              AND status = 'TAKEN_DOWN'
              AND deleted_at IS NULL
            ORDER BY
              CASE WHEN id = #{rootCommentId} THEN 0 ELSE 1 END,
              created_at,
              id
            """)
    List<CommunityComment> findTakenDownThread(
            @Param("rootCommentId") Long rootCommentId
    );
}
