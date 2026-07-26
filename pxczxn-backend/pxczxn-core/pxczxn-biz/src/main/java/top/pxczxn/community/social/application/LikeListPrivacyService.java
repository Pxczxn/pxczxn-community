package top.pxczxn.community.social.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mars.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.social.persistence.CommunityFollowMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.model.CommunityUserPreference;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
import top.pxczxn.community.user.persistence.CommunityUserPreferenceMapper;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LikeListPrivacyService {

    private static final Set<String> ACTIVE_USER_STATUSES =
            Set.of("NORMAL", "LIMITED");
    private static final Set<String> VISIBILITIES = Set.of(
            "PRIVATE", "PUBLIC", "FOLLOWERS_ONLY", "MUTUAL_ONLY"
    );

    private final CommunityUserPreferenceMapper preferenceMapper;
    private final CommunityUserMapper userMapper;
    private final BlogMapper blogMapper;
    private final CommunityFollowMapper followMapper;
    private final CommunityAuth communityAuth;

    @Transactional(readOnly = true)
    public LikeListPrivacyView mine() {
        Long actorId = requireActorId();
        return view(actorId, requirePreference(actorId));
    }

    @Transactional
    public LikeListPrivacyView update(String rawVisibility) {
        Long actorId = requireActorId();
        String visibility = normalize(rawVisibility);
        CommunityUserPreference preference = requirePreference(actorId);
        if (visibility.equals(preference.getLikesVisibility())) {
            return view(actorId, preference);
        }
        if (preferenceMapper.update(
                null,
                Wrappers.<CommunityUserPreference>update()
                        .eq("user_id", actorId)
                        .set("likes_visibility", visibility)
        ) != 1) {
            throw new BusinessException(409, "喜欢列表设置已发生变化，请重试");
        }
        preference.setLikesVisibility(visibility);
        return view(actorId, preference);
    }

    @Transactional(readOnly = true)
    public LikeListPrivacyView requireCanView(Long ownerUserId) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw notFound();
        }
        CommunityUser owner = userMapper.selectById(ownerUserId);
        if (owner == null || !ACTIVE_USER_STATUSES.contains(owner.getStatus())) {
            throw notFound();
        }
        CommunityUserPreference preference = requirePreference(ownerUserId);
        String visibility = effectiveVisibility(preference);
        Long viewerId = communityAuth.getOptionalLoginUserId();
        if (Objects.equals(viewerId, ownerUserId)) {
            return new LikeListPrivacyView(ownerUserId, visibility);
        }
        if ("PUBLIC".equals(visibility)) {
            return new LikeListPrivacyView(ownerUserId, visibility);
        }
        if (viewerId == null) {
            throw notFound();
        }
        CommunityUser viewer = userMapper.selectById(viewerId);
        if (viewer == null || !ACTIVE_USER_STATUSES.contains(viewer.getStatus())) {
            throw notFound();
        }
        Blog ownerBlog = publicPersonalBlog(owner);
        boolean follower = followMapper.findRelation(
                viewerId, "BLOG", ownerBlog.getId()
        ) != null;
        if ("FOLLOWERS_ONLY".equals(visibility) && follower) {
            return new LikeListPrivacyView(ownerUserId, visibility);
        }
        boolean mutual = follower
                && viewer.getPersonalBlogId() != null
                && followMapper.findRelation(
                        ownerUserId, "BLOG", viewer.getPersonalBlogId()
                ) != null;
        if ("MUTUAL_ONLY".equals(visibility) && mutual) {
            return new LikeListPrivacyView(ownerUserId, visibility);
        }
        throw notFound();
    }

    private Long requireActorId() {
        Long actorId = communityAuth.getLoginUserId();
        CommunityUser actor = userMapper.selectById(actorId);
        if (actor == null || !ACTIVE_USER_STATUSES.contains(actor.getStatus())) {
            throw new BusinessException(403, "当前账号不能管理喜欢列表");
        }
        return actorId;
    }

    private CommunityUserPreference requirePreference(Long userId) {
        CommunityUserPreference preference = preferenceMapper.selectOne(
                Wrappers.<CommunityUserPreference>query()
                        .eq("user_id", userId)
                        .last("LIMIT 1")
        );
        if (preference == null) {
            throw new BusinessException(500, "用户偏好不存在");
        }
        return preference;
    }

    private Blog publicPersonalBlog(CommunityUser owner) {
        if (owner.getPersonalBlogId() == null) {
            throw notFound();
        }
        Blog blog = blogMapper.selectById(owner.getPersonalBlogId());
        if (blog == null
                || blog.getDeletedAt() != null
                || !"PERSONAL".equals(blog.getBlogType())
                || !"ACTIVE".equals(blog.getStatus())
                || !Objects.equals(blog.getOwnerUserId(), owner.getId())) {
            throw notFound();
        }
        return blog;
    }

    private static LikeListPrivacyView view(
            Long userId,
            CommunityUserPreference preference
    ) {
        return new LikeListPrivacyView(
                userId, effectiveVisibility(preference)
        );
    }

    private static String effectiveVisibility(
            CommunityUserPreference preference
    ) {
        return preference.getLikesVisibility() == null
                ? "PRIVATE"
                : normalize(preference.getLikesVisibility());
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(400, "喜欢列表公开范围不能为空");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!VISIBILITIES.contains(normalized)) {
            throw new BusinessException(400, "喜欢列表公开范围无效");
        }
        return normalized;
    }

    private static BusinessException notFound() {
        return new BusinessException(404, "喜欢列表不存在");
    }
}
