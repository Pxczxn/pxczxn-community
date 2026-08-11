package top.pxczxn.community.user.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.model.CommunityUserPreference;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
import top.pxczxn.community.user.persistence.CommunityUserPreferenceMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CommunityPreferenceServiceImpl implements CommunityPreferenceService {

    private static final int MAX_SETTINGS_BYTES = 16 * 1024;
    private static final Set<String> ALLOWED_KEYS = Set.of(
            "dndMode", "emailFrequency", "notificationMatrix",
            "bookmarksPrivacy", "followingPrivacy", "readingStatusPrivacy",
            "whoCanMessage", "whoCanMention", "allowSearchIndex", "allowRecommendation",
            "fontSize", "uiDensity", "codeTheme", "reduceMotion",
            "defaultHomeFeed", "defaultArticleSort", "defaultPostVisibility",
            "defaultCommentScope", "allowRepost", "mutedKeywords"
    );

    private final CommunityUserPreferenceMapper preferenceMapper;
    private final CommunityUserMapper userMapper;
    private final CommunityAuth communityAuth;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public CommunityPreferenceSettings mine() {
        return new CommunityPreferenceSettings(readSettings(requirePreference(requireActorId())));
    }

    @Override
    @Transactional
    public CommunityPreferenceSettings update(Map<String, Object> settings) {
        Long userId = requireActorId();
        Map<String, Object> normalized = normalize(settings);
        String serialized;
        try {
            serialized = objectMapper.writeValueAsString(normalized);
        } catch (Exception exception) {
            throw new BusinessException(400, "偏好设置格式无效");
        }
        if (serialized.length() > MAX_SETTINGS_BYTES) {
            throw new BusinessException(400, "偏好设置过大");
        }
        if (preferenceMapper.update(null, Wrappers.<CommunityUserPreference>update()
                .eq("user_id", userId)
                .set("settings_json", serialized)) != 1) {
            throw new BusinessException(409, "偏好设置已发生变化，请重试");
        }
        return new CommunityPreferenceSettings(normalized);
    }

    private Long requireActorId() {
        Long userId = communityAuth.getLoginUserId();
        CommunityUser user = userMapper.selectById(userId);
        if (user == null || !("NORMAL".equals(user.getStatus()) || "LIMITED".equals(user.getStatus()))) {
            throw new BusinessException(403, "当前账号不能管理偏好设置");
        }
        return userId;
    }

    private CommunityUserPreference requirePreference(Long userId) {
        CommunityUserPreference preference = preferenceMapper.selectOne(Wrappers.<CommunityUserPreference>query()
                .eq("user_id", userId).last("LIMIT 1"));
        if (preference == null) {
            throw new BusinessException(500, "用户偏好不存在");
        }
        return preference;
    }

    private Map<String, Object> readSettings(CommunityUserPreference preference) {
        if (preference.getSettingsJson() == null || preference.getSettingsJson().isBlank()) {
            return Map.of();
        }
        try {
            return normalize(objectMapper.readValue(preference.getSettingsJson(), new TypeReference<>() { }));
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private static Map<String, Object> normalize(Map<String, Object> settings) {
        if (settings == null) {
            throw new BusinessException(400, "偏好设置不能为空");
        }
        if (settings.size() > ALLOWED_KEYS.size() || !ALLOWED_KEYS.containsAll(settings.keySet())) {
            throw new BusinessException(400, "偏好设置包含不支持的字段");
        }
        return new LinkedHashMap<>(settings);
    }
}
