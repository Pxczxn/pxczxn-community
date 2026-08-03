package top.pxczxn.community.user.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import top.pxczxn.community.user.model.CommunityUserLoginAccount;

import java.time.LocalDateTime;

public interface CommunityUserLoginAccountMapper extends BaseMapper<CommunityUserLoginAccount> {

    @Update("""
            UPDATE community_user_login_account
            SET locked_until = CASE
                    WHEN failed_login_count + 1 >= #{maximumAttempts}
                    THEN #{lockedUntil}
                    ELSE locked_until
                END,
                failed_login_count = failed_login_count + 1
            WHERE id = #{accountId}
            """)
    int recordLoginFailure(
            @Param("accountId") Long accountId,
            @Param("maximumAttempts") int maximumAttempts,
            @Param("lockedUntil") LocalDateTime lockedUntil
    );

    @Update("""
            UPDATE community_user_login_account
            SET failed_login_count = 0,
                locked_until = NULL
            WHERE id = #{accountId}
            """)
    int clearLoginFailures(@Param("accountId") Long accountId);

    @Update("""
            UPDATE community_user_login_account
            SET failed_login_count = 0,
                locked_until = #{lockedUntil}
            WHERE id = #{accountId}
            """)
    int lockAccount(@Param("accountId") Long accountId, @Param("lockedUntil") LocalDateTime lockedUntil);

    @Update("""
            UPDATE community_user_login_account
            SET failed_login_count = 0,
                locked_until = NULL,
                last_login_at = #{loginAt}
            WHERE id = #{accountId}
            """)
    int recordLoginSuccess(
            @Param("accountId") Long accountId,
            @Param("loginAt") LocalDateTime loginAt
    );
}
