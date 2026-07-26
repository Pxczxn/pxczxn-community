package top.pxczxn.community.user.application;

import cn.hutool.core.lang.Validator;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import top.pxczxn.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.model.BlogCategory;
import top.pxczxn.community.blog.model.BlogSetting;
import top.pxczxn.community.blog.persistence.BlogCategoryMapper;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.blog.persistence.BlogSettingMapper;
import top.pxczxn.community.social.model.FavoriteFolder;
import top.pxczxn.community.social.persistence.FavoriteFolderMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.model.CommunityUserLoginAccount;
import top.pxczxn.community.user.model.CommunityUserPreference;
import top.pxczxn.community.user.persistence.CommunityUserLoginAccountMapper;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
import top.pxczxn.community.user.persistence.CommunityUserPreferenceMapper;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityRegistrationServiceImpl implements CommunityRegistrationService {

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-z0-9][a-z0-9_-]{2,31}$");
    private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;
    private static final String DEFAULT_CATEGORY_NAME = "未分类";
    private static final String DEFAULT_CATEGORY_SLUG = "uncategorized";
    private static final String DEFAULT_FAVORITE_FOLDER_NAME = "全部收藏";

    private final CommunityUserMapper userMapper;
    private final CommunityUserLoginAccountMapper loginAccountMapper;
    private final CommunityUserPreferenceMapper preferenceMapper;
    private final BlogMapper blogMapper;
    private final BlogSettingMapper blogSettingMapper;
    private final BlogCategoryMapper blogCategoryMapper;
    private final FavoriteFolderMapper favoriteFolderMapper;

    @Override
    public boolean isUsernameAvailable(String username) {
        String normalizedUsername = normalizeUsername(username);
        return userMapper.selectCount(Wrappers.<CommunityUser>lambdaQuery()
                .eq(CommunityUser::getUsername, normalizedUsername)) == 0;
    }

    @Override
    public boolean isEmailAvailable(String email) {
        String normalizedEmail = normalizeEmail(email);
        return loginAccountMapper.selectCount(
                Wrappers.<CommunityUserLoginAccount>lambdaQuery()
                        .eq(CommunityUserLoginAccount::getLoginType, "EMAIL")
                        .eq(CommunityUserLoginAccount::getNormalizedIdentifier, normalizedEmail)
        ) == 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegisteredCommunityUser register(RegisterCommunityUserCommand command) {
        if (command == null) {
            throw new BusinessException(400, "注册信息不能为空");
        }

        String username = normalizeUsername(command.username());
        String email = normalizeEmail(command.email());
        String displayName = normalizeDisplayName(command.displayName(), username);
        validatePassword(command.password());

        if (!isUsernameAvailable(username)) {
            throw RegistrationConflictException.username();
        }
        if (!isEmailAvailable(email)) {
            throw RegistrationConflictException.email();
        }

        Long userId = IdWorker.getId();
        Long loginAccountId = IdWorker.getId();
        Long preferenceId = IdWorker.getId();
        Long blogId = IdWorker.getId();
        Long blogSettingId = IdWorker.getId();
        Long categoryId = IdWorker.getId();
        Long favoriteFolderId = IdWorker.getId();

        try {
            insertRequired(userMapper.insert(newUser(userId, username, displayName)), "创建社区用户");
            insertRequired(
                    loginAccountMapper.insert(newLoginAccount(
                            loginAccountId, userId, email, BCrypt.hashpw(command.password())
                    )),
                    "创建登录账号"
            );
            insertRequired(
                    preferenceMapper.insert(newPreference(preferenceId, userId)),
                    "创建用户偏好"
            );
            insertRequired(
                    favoriteFolderMapper.insert(newDefaultFavoriteFolder(
                            favoriteFolderId, userId
                    )),
                    "创建默认收藏夹"
            );
            insertRequired(blogMapper.insert(newPersonalBlog(blogId, userId, displayName, username)),
                    "创建个人博客");
            insertRequired(blogSettingMapper.insert(newBlogSetting(blogSettingId, blogId)),
                    "创建博客设置");
            insertRequired(blogCategoryMapper.insert(newDefaultCategory(categoryId, blogId)),
                    "创建默认分类");

            CommunityUser personalBlogLink = new CommunityUser();
            personalBlogLink.setId(userId);
            personalBlogLink.setPersonalBlogId(blogId);
            insertRequired(userMapper.updateById(personalBlogLink), "回写个人博客");
        } catch (DuplicateKeyException exception) {
            throw mapDuplicateConflict(exception);
        }

        log.info("社区用户注册完成, userId={}, blogId={}", userId, blogId);
        return new RegisteredCommunityUser(userId, blogId, username, username);
    }

    private static CommunityUser newUser(Long userId, String username, String displayName) {
        CommunityUser user = new CommunityUser();
        user.setId(userId);
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setStatus("NORMAL");
        user.setVerificationStatus("UNVERIFIED");
        return user;
    }

    private static CommunityUserLoginAccount newLoginAccount(
            Long loginAccountId,
            Long userId,
            String normalizedEmail,
            String passwordHash
    ) {
        CommunityUserLoginAccount account = new CommunityUserLoginAccount();
        account.setId(loginAccountId);
        account.setUserId(userId);
        account.setLoginType("EMAIL");
        account.setNormalizedIdentifier(normalizedEmail);
        account.setPasswordHash(passwordHash);
        return account;
    }

    private static CommunityUserPreference newPreference(Long preferenceId, Long userId) {
        CommunityUserPreference preference = new CommunityUserPreference();
        preference.setId(preferenceId);
        preference.setUserId(userId);
        return preference;
    }

    private static FavoriteFolder newDefaultFavoriteFolder(
            Long folderId,
            Long userId
    ) {
        FavoriteFolder folder = new FavoriteFolder();
        folder.setId(folderId);
        folder.setOwnerUserId(userId);
        folder.setName(DEFAULT_FAVORITE_FOLDER_NAME);
        folder.setDescription("所有收藏内容");
        folder.setVisibility("PRIVATE");
        folder.setIsDefault(1);
        folder.setItemCount(0L);
        folder.setSortOrder(0);
        return folder;
    }

    private static Blog newPersonalBlog(
            Long blogId,
            Long userId,
            String displayName,
            String slug
    ) {
        Blog blog = new Blog();
        blog.setId(blogId);
        blog.setBlogType("PERSONAL");
        blog.setOwnerUserId(userId);
        blog.setName(displayName + " 的博客");
        blog.setSlug(slug);
        blog.setStatus("ACTIVE");
        return blog;
    }

    private static BlogSetting newBlogSetting(Long settingId, Long blogId) {
        BlogSetting setting = new BlogSetting();
        setting.setId(settingId);
        setting.setBlogId(blogId);
        return setting;
    }

    private static BlogCategory newDefaultCategory(Long categoryId, Long blogId) {
        BlogCategory category = new BlogCategory();
        category.setId(categoryId);
        category.setBlogId(blogId);
        category.setName(DEFAULT_CATEGORY_NAME);
        category.setSlug(DEFAULT_CATEGORY_SLUG);
        category.setSortOrder(0);
        category.setIsDefault(1);
        return category;
    }

    private static String normalizeUsername(String rawUsername) {
        if (rawUsername == null) {
            throw new BusinessException(400, "用户名不能为空");
        }
        String username = Normalizer.normalize(rawUsername.trim(), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new BusinessException(400, "用户名须为 3-32 位字母、数字、下划线或连字符");
        }
        return username;
    }

    private static String normalizeEmail(String rawEmail) {
        if (rawEmail == null) {
            throw new BusinessException(400, "邮箱不能为空");
        }
        String email = Normalizer.normalize(rawEmail.trim(), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        if (email.length() > 320 || !Validator.isEmail(email)) {
            throw new BusinessException(400, "邮箱格式不正确");
        }
        return email;
    }

    private static String normalizeDisplayName(String rawDisplayName, String username) {
        if (rawDisplayName == null || rawDisplayName.isBlank()) {
            return username;
        }
        String displayName = Normalizer.normalize(rawDisplayName.trim(), Normalizer.Form.NFKC);
        if (displayName.length() > 80) {
            throw new BusinessException(400, "显示名称不能超过 80 个字符");
        }
        return displayName;
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new BusinessException(400, "密码至少需要 8 个字符");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES) {
            throw new BusinessException(400, "密码 UTF-8 编码后不能超过 72 字节");
        }
    }

    private static void insertRequired(int affectedRows, String operation) {
        if (affectedRows != 1) {
            throw new BusinessException(500, operation + "失败");
        }
    }

    private static RegistrationConflictException mapDuplicateConflict(
            DuplicateKeyException exception
    ) {
        String causeMessage = exception.getMostSpecificCause().getMessage();
        String message = causeMessage == null ? "" : causeMessage.toLowerCase(Locale.ROOT);
        if (message.contains("uk_community_user_username")) {
            return RegistrationConflictException.username();
        }
        if (message.contains("uk_login_account_type_identifier")
                || message.contains("uk_login_account_user_type")) {
            return RegistrationConflictException.email();
        }
        if (message.contains("uk_blog_slug") || message.contains("uk_blog_personal_owner")) {
            return RegistrationConflictException.blogSlug();
        }
        return RegistrationConflictException.generic();
    }
}
