package top.pxczxn.community.blog.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import top.pxczxn.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.model.BlogCategory;
import top.pxczxn.community.blog.persistence.BlogCategoryMapper;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BlogCategoryService {

    private static final int MAX_CATEGORIES = 100;
    private static final Pattern SLUG_PATTERN =
            Pattern.compile("[a-z0-9]+(?:[-_][a-z0-9]+)*");

    private final BlogCategoryMapper categoryMapper;
    private final BlogMapper blogMapper;
    private final ArticleMapper articleMapper;
    private final CommunityUserMapper userMapper;
    private final CommunityAuth communityAuth;

    @Transactional(readOnly = true)
    public List<BlogCategoryView> listMine() {
        Blog blog = requireOwnedBlog();
        return categoryMapper.selectList(
                        Wrappers.<BlogCategory>lambdaQuery()
                                .eq(BlogCategory::getBlogId, blog.getId())
                                .isNull(BlogCategory::getDeletedAt)
                                .orderByAsc(BlogCategory::getSortOrder)
                                .orderByAsc(BlogCategory::getId)
                )
                .stream()
                .map(BlogCategoryService::toView)
                .toList();
    }

    @Transactional
    public BlogCategoryView create(CreateBlogCategoryCommand command) {
        if (command == null) {
            throw new BusinessException(400, "分类信息不能为空");
        }
        Blog blog = requireOwnedBlog();
        Long count = categoryMapper.selectCount(
                Wrappers.<BlogCategory>lambdaQuery()
                        .eq(BlogCategory::getBlogId, blog.getId())
                        .isNull(BlogCategory::getDeletedAt)
        );
        if (count != null && count >= MAX_CATEGORIES) {
            throw new BusinessException(400, "每个博客最多创建 100 个分类");
        }
        BlogCategory category = new BlogCategory();
        category.setId(IdWorker.getId());
        category.setBlogId(blog.getId());
        category.setName(requiredText(command.name(), "分类名称", 80));
        category.setSlug(normalizeSlug(command.slug()));
        category.setDescription(optionalText(command.description(), "分类描述", 300));
        category.setSortOrder(sortOrder(command.sortOrder()));
        category.setIsDefault(0);
        category.setArticleCount(0L);
        category.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        try {
            if (categoryMapper.insert(category) != 1) {
                throw new BusinessException(500, "创建分类失败");
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(409, "分类地址已存在");
        }
        return toView(category);
    }

    @Transactional
    public BlogCategoryView update(Long categoryId, UpdateBlogCategoryCommand command) {
        if (command == null) {
            throw new BusinessException(400, "分类信息不能为空");
        }
        Blog blog = requireOwnedBlog();
        BlogCategory category = requireOwnedCategory(blog.getId(), categoryId);
        var update = Wrappers.<BlogCategory>update()
                .eq("id", category.getId())
                .eq("blog_id", blog.getId())
                .isNull("deleted_at");
        boolean changed = false;
        if (command.name() != null) {
            update.set("name", requiredText(
                    command.name(), "分类名称", 80
            ));
            changed = true;
        }
        if (command.slug() != null) {
            update.set("slug", normalizeSlug(command.slug()));
            changed = true;
        }
        if (command.description() != null) {
            update.set("description", optionalText(
                    command.description(), "分类描述", 300
            ));
            changed = true;
        }
        if (command.sortOrder() != null) {
            update.set("sort_order", sortOrder(command.sortOrder()));
            changed = true;
        }
        if (changed) {
            try {
                if (categoryMapper.update(null, update) != 1) {
                    throw new BusinessException(409, "分类已发生变化，请刷新后重试");
                }
            } catch (DuplicateKeyException exception) {
                throw new BusinessException(409, "分类地址已存在");
            }
        }
        return toView(categoryMapper.selectById(category.getId()));
    }

    @Transactional
    public void delete(Long categoryId) {
        Blog blog = requireOwnedBlog();
        BlogCategory category = requireOwnedCategory(blog.getId(), categoryId);
        if (Integer.valueOf(1).equals(category.getIsDefault())) {
            throw new BusinessException(409, "默认分类不可删除");
        }
        BlogCategory defaultCategory = categoryMapper.selectOne(
                Wrappers.<BlogCategory>lambdaQuery()
                        .eq(BlogCategory::getBlogId, blog.getId())
                        .eq(BlogCategory::getIsDefault, 1)
                        .isNull(BlogCategory::getDeletedAt)
                        .last("LIMIT 1")
        );
        if (defaultCategory == null) {
            throw new BusinessException(500, "默认分类缺失");
        }
        articleMapper.update(
                null,
                Wrappers.<Article>update()
                        .eq("blog_id", blog.getId())
                        .eq("category_id", category.getId())
                        .isNull("deleted_at")
                        .set("category_id", defaultCategory.getId())
        );
        categoryMapper.update(
                null,
                Wrappers.<BlogCategory>update()
                        .eq("id", defaultCategory.getId())
                        .setSql(
                                "article_count = (SELECT COUNT(*) FROM article "
                                        + "WHERE category_id = "
                                        + defaultCategory.getId()
                                        + " AND deleted_at IS NULL)"
                        )
        );
        if (categoryMapper.update(
                null,
                Wrappers.<BlogCategory>update()
                        .eq("id", category.getId())
                        .eq("blog_id", blog.getId())
                        .isNull("deleted_at")
                        .set("article_count", 0)
                        .set("deleted_at", LocalDateTime.now(ZoneOffset.UTC))
        ) != 1) {
            throw new BusinessException(409, "分类已发生变化，请刷新后重试");
        }
    }

    private Blog requireOwnedBlog() {
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
            throw new BusinessException(403, "无权管理该博客分类");
        }
        if (!List.of("ACTIVE", "HIDDEN").contains(blog.getStatus())) {
            throw new BusinessException(403, "当前博客状态不允许管理分类");
        }
        return blog;
    }

    private BlogCategory requireOwnedCategory(Long blogId, Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            throw new BusinessException(400, "分类 ID 无效");
        }
        BlogCategory category = categoryMapper.selectOne(
                Wrappers.<BlogCategory>lambdaQuery()
                        .eq(BlogCategory::getId, categoryId)
                        .eq(BlogCategory::getBlogId, blogId)
                        .isNull(BlogCategory::getDeletedAt)
                        .last("LIMIT 1")
        );
        if (category == null) {
            throw new BusinessException(404, "分类不存在");
        }
        return category;
    }

    private static BlogCategoryView toView(BlogCategory category) {
        return new BlogCategoryView(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getSortOrder() == null ? 0 : category.getSortOrder(),
                Integer.valueOf(1).equals(category.getIsDefault()),
                category.getArticleCount() == null ? 0 : category.getArticleCount()
        );
    }

    private static String requiredText(String raw, String label, int max) {
        if (raw == null) {
            throw new BusinessException(400, label + "不能为空");
        }
        String value = Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC);
        if (value.isEmpty() || value.length() > max) {
            throw new BusinessException(400, label + "长度须为 1-" + max + " 个字符");
        }
        return value;
    }

    private static String optionalText(String raw, String label, int max) {
        if (raw == null) {
            return null;
        }
        String value = Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC);
        if (value.length() > max) {
            throw new BusinessException(400, label + "不能超过 " + max + " 个字符");
        }
        return value.isEmpty() ? null : value;
    }

    private static String normalizeSlug(String raw) {
        String slug = raw == null
                ? ""
                : Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC)
                        .toLowerCase(Locale.ROOT);
        if (slug.length() < 1 || slug.length() > 80 || !SLUG_PATTERN.matcher(slug).matches()) {
            throw new BusinessException(400, "分类地址须为 1-80 位小写字母、数字、下划线或连字符");
        }
        return slug;
    }

    private static int sortOrder(Integer value) {
        int sort = value == null ? 0 : value;
        if (sort < -10000 || sort > 10000) {
            throw new BusinessException(400, "分类排序值须在 -10000 到 10000 之间");
        }
        return sort;
    }
}
