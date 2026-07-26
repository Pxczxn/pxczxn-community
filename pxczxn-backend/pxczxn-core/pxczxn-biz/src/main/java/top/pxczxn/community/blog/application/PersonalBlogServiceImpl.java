package top.pxczxn.community.blog.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import top.pxczxn.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.model.BlogSetting;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.blog.persistence.BlogSettingMapper;
import top.pxczxn.community.file.application.CommunityFileService;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PersonalBlogServiceImpl implements PersonalBlogService {

    private static final Pattern SLUG_PATTERN =
            Pattern.compile("[a-z0-9]+(?:[-_][a-z0-9]+)*");
    private static final Set<String> COMMENT_SCOPES =
            Set.of(
                    "ALL_LOGGED_IN",
                    "FOLLOWERS_ONLY",
                    "MUTUAL_ONLY",
                    "BLOGGER_FOLLOWING",
                    "DISABLED"
            );
    private static final Set<String> VISIBILITIES =
            Set.of("PUBLIC", "PRIVATE", "FOLLOWERS_ONLY", "UNLISTED");
    private static final Set<String> REPOST_POLICIES =
            Set.of("ALLOW", "APPROVAL_REQUIRED", "DISALLOW");
    private static final Set<String> THEMES =
            Set.of("light", "dark", "starry");

    private final BlogMapper blogMapper;
    private final BlogSettingMapper settingMapper;
    private final CommunityUserMapper userMapper;
    private final CommunityAuth communityAuth;
    private final CommunityFileService fileService;
    private final ArticleMapper articleMapper;

    @Override
    @Transactional(readOnly = true)
    public PersonalBlogProfile getMine() {
        Blog blog = requireOwnedPersonalBlog();
        return toPersonalProfile(blog, requireSettings(blog.getId()));
    }

    @Override
    @Transactional
    public PersonalBlogProfile updateMine(UpdatePersonalBlogCommand command) {
        if (command == null) {
            throw new BusinessException(400, "博客资料不能为空");
        }
        Blog blog = requireOwnedPersonalBlog();
        assertEditable(blog);

        var update = Wrappers.<Blog>lambdaUpdate()
                .eq(Blog::getId, blog.getId())
                .eq(Blog::getOwnerUserId, communityAuth.getLoginUserId())
                .eq(Blog::getBlogType, "PERSONAL")
                .isNull(Blog::getDeletedAt);
        boolean changed = false;

        if (command.name() != null) {
            String name = normalizeText(command.name(), "博客名称", 1, 120);
            update.set(Blog::getName, name);
            changed = true;
        }
        if (command.slug() != null) {
            String slug = normalizeSlug(command.slug());
            if (!slug.equals(blog.getSlug())) {
                throw new BusinessException(400, "博客地址不可在资料编辑中修改");
            }
        }
        if (command.summary() != null) {
            String summary = normalizeOptionalText(command.summary(), "博客简介", 500);
            update.set(Blog::getSummary, summary);
            changed = true;
        }
        if (command.clearAvatar()) {
            fileService.clearReference("BLOG", blog.getId(), "BLOG_AVATAR");
            update.set(Blog::getAvatarFileId, null);
            changed = true;
        } else if (command.avatarFileId() != null) {
            Long avatarFileId = positiveFileId(command.avatarFileId(), "头像文件");
            fileService.linkOwnedFile(
                    avatarFileId,
                    "BLOG",
                    blog.getId(),
                    "BLOG_AVATAR"
            );
            update.set(Blog::getAvatarFileId, avatarFileId);
            changed = true;
        }
        if (command.clearBackground()) {
            fileService.clearReference("BLOG", blog.getId(), "BLOG_BACKGROUND");
            update.set(Blog::getBackgroundFileId, null);
            changed = true;
        } else if (command.backgroundFileId() != null) {
            Long backgroundFileId = positiveFileId(
                    command.backgroundFileId(), "背景文件"
            );
            fileService.linkOwnedFile(
                    backgroundFileId,
                    "BLOG",
                    blog.getId(),
                    "BLOG_BACKGROUND"
            );
            update.set(Blog::getBackgroundFileId, backgroundFileId);
            changed = true;
        }

        if (changed) {
            try {
                if (blogMapper.update(null, update) != 1) {
                    throw new BusinessException(409, "博客资料已发生变化，请刷新后重试");
                }
            } catch (DuplicateKeyException exception) {
                throw new BusinessException(409, "博客地址已被占用");
            }
        }
        Blog refreshed = blogMapper.selectById(blog.getId());
        return toPersonalProfile(refreshed, requireSettings(blog.getId()));
    }

    @Override
    @Transactional
    public BlogSettingsView updateMySettings(UpdateBlogSettingsCommand command) {
        if (command == null) {
            throw new BusinessException(400, "博客设置不能为空");
        }
        Blog blog = requireOwnedPersonalBlog();
        assertEditable(blog);
        BlogSetting current = requireSettings(blog.getId());
        var update = Wrappers.<BlogSetting>lambdaUpdate()
                .eq(BlogSetting::getId, current.getId())
                .eq(BlogSetting::getBlogId, blog.getId());
        boolean changed = false;

        if (command.commentScope() != null) {
            update.set(BlogSetting::getCommentScope, enumValue(
                    command.commentScope(), COMMENT_SCOPES, "评论范围"
            ));
            changed = true;
        }
        if (command.defaultVisibility() != null) {
            update.set(BlogSetting::getDefaultVisibility, enumValue(
                    command.defaultVisibility(), VISIBILITIES, "默认可见性"
            ));
            changed = true;
        }
        if (command.allowRepost() != null) {
            update.set(BlogSetting::getAllowRepost, enumValue(
                    command.allowRepost(), REPOST_POLICIES, "转载策略"
            ));
            changed = true;
        }
        if (command.themeKey() != null) {
            String theme = Normalizer.normalize(
                    command.themeKey().trim(), Normalizer.Form.NFKC
            ).toLowerCase(Locale.ROOT);
            if (!THEMES.contains(theme)) {
                throw new BusinessException(400, "主题仅支持 light、dark 或 starry");
            }
            update.set(BlogSetting::getThemeKey, theme);
            changed = true;
        }
        if (command.seoTitle() != null) {
            update.set(BlogSetting::getSeoTitle, normalizeOptionalText(
                    command.seoTitle(), "SEO 标题", 160
            ));
            changed = true;
        }
        if (command.seoDescription() != null) {
            update.set(BlogSetting::getSeoDescription, normalizeOptionalText(
                    command.seoDescription(), "SEO 描述", 300
            ));
            changed = true;
        }
        if (changed && settingMapper.update(null, update) != 1) {
            throw new BusinessException(409, "博客设置已发生变化，请刷新后重试");
        }
        return toSettings(settingMapper.selectById(current.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public PublicBlogProfile getPublicBySlug(String blogSlug) {
        String slug = normalizeSlug(blogSlug);
        Blog blog = blogMapper.selectOne(
                Wrappers.<Blog>lambdaQuery()
                        .eq(Blog::getSlug, slug)
                        .eq(Blog::getStatus, "ACTIVE")
                        .isNull(Blog::getDeletedAt)
                        .last("LIMIT 1")
        );
        if (blog == null) {
            throw new BusinessException(404, "博客不存在");
        }
        CommunityUser owner = userMapper.selectById(blog.getOwnerUserId());
        if (owner == null || !Set.of("NORMAL", "LIMITED").contains(owner.getStatus())) {
            throw new BusinessException(404, "博客不存在");
        }
        BlogSetting setting = settingMapper.selectOne(
                Wrappers.<BlogSetting>lambdaQuery()
                        .eq(BlogSetting::getBlogId, blog.getId())
                        .last("LIMIT 1")
        );
        return new PublicBlogProfile(
                blog.getId(),
                blog.getBlogType(),
                blog.getName(),
                blog.getSlug(),
                blog.getSummary(),
                blog.getAvatarFileId(),
                blog.getBackgroundFileId(),
                articleMapper.countPublicByBlog(blog.getId()),
                longValue(blog.getFollowerCount()),
                owner.getUsername(),
                owner.getDisplayName(),
                owner.getBio(),
                owner.getAvatarFileId(),
                setting == null || setting.getThemeKey() == null
                        ? "light"
                        : setting.getThemeKey(),
                setting == null ? null : setting.getThemeConfigJson(),
                setting == null || setting.getSeoTitle() == null
                        || setting.getSeoTitle().isBlank()
                        ? blog.getName()
                        : setting.getSeoTitle(),
                setting == null || setting.getSeoDescription() == null
                        || setting.getSeoDescription().isBlank()
                        ? blog.getSummary()
                        : setting.getSeoDescription()
        );
    }

    private Blog requireOwnedPersonalBlog() {
        Long userId = communityAuth.getLoginUserId();
        CommunityUser user = userMapper.selectById(userId);
        if (user == null || user.getPersonalBlogId() == null) {
            throw new BusinessException(404, "个人博客不存在");
        }
        Blog blog = blogMapper.selectById(user.getPersonalBlogId());
        if (blog == null
                || !userId.equals(blog.getOwnerUserId())
                || !"PERSONAL".equals(blog.getBlogType())
                || blog.getDeletedAt() != null) {
            throw new BusinessException(403, "无权访问该博客");
        }
        return blog;
    }

    private BlogSetting requireSettings(Long blogId) {
        BlogSetting setting = settingMapper.selectOne(
                Wrappers.<BlogSetting>lambdaQuery()
                        .eq(BlogSetting::getBlogId, blogId)
                        .last("LIMIT 1")
        );
        if (setting == null) {
            throw new BusinessException(500, "博客设置缺失");
        }
        return setting;
    }

    private static void assertEditable(Blog blog) {
        if (!Set.of("ACTIVE", "HIDDEN").contains(blog.getStatus())) {
            throw new BusinessException(403, "当前博客状态不允许修改");
        }
    }

    private static PersonalBlogProfile toPersonalProfile(
            Blog blog,
            BlogSetting settings
    ) {
        return new PersonalBlogProfile(
                blog.getId(),
                blog.getName(),
                blog.getSlug(),
                blog.getSummary(),
                blog.getAvatarFileId(),
                blog.getBackgroundFileId(),
                blog.getStatus(),
                longValue(blog.getArticleCount()),
                longValue(blog.getFollowerCount()),
                toSettings(settings)
        );
    }

    private static BlogSettingsView toSettings(BlogSetting setting) {
        return new BlogSettingsView(
                setting.getCommentScope(),
                setting.getDefaultVisibility(),
                setting.getAllowRepost(),
                setting.getThemeKey(),
                setting.getSeoTitle(),
                setting.getSeoDescription()
        );
    }

    private static String normalizeSlug(String raw) {
        String slug = Normalizer.normalize(raw == null ? "" : raw.trim(), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        if (slug.length() < 3 || slug.length() > 80 || !SLUG_PATTERN.matcher(slug).matches()) {
            throw new BusinessException(
                    400,
                    "博客地址须为 3-80 位小写字母、数字、下划线或单连字符"
            );
        }
        return slug;
    }

    private static String normalizeText(String raw, String label, int min, int max) {
        String value = Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC);
        if (value.length() < min || value.length() > max) {
            throw new BusinessException(400, label + "长度须为 " + min + "-" + max + " 个字符");
        }
        return value;
    }

    private static String normalizeOptionalText(String raw, String label, int max) {
        String value = Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC);
        if (value.length() > max) {
            throw new BusinessException(400, label + "不能超过 " + max + " 个字符");
        }
        return value.isEmpty() ? null : value;
    }

    private static Long positiveFileId(Long id, String label) {
        if (id <= 0) {
            throw new BusinessException(400, label + " ID 无效");
        }
        return id;
    }

    private static String enumValue(String raw, Set<String> allowed, String label) {
        String value = Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC)
                .toUpperCase(Locale.ROOT);
        if (!allowed.contains(value)) {
            throw new BusinessException(400, label + "无效");
        }
        return value;
    }

    private static long longValue(Long value) {
        return value == null ? 0L : value;
    }
}
