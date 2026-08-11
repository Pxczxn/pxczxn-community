package top.pxczxn.community.user.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.social.model.CommunityMoment;
import top.pxczxn.community.social.persistence.CommunityMomentMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class CommunityDataExportServiceImpl implements CommunityDataExportService {
    private final CommunityAuth communityAuth;
    private final CommunityUserMapper userMapper;
    private final BlogMapper blogMapper;
    private final ArticleMapper articleMapper;
    private final CommunityMomentMapper momentMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public CommunityDataExport exportMine() {
        Long userId = communityAuth.getLoginUserId();
        CommunityUser user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(401, "登录用户不存在");
        Blog blog = user.getPersonalBlogId() == null ? null : blogMapper.selectById(user.getPersonalBlogId());
        List<Article> articles = articleMapper.selectList(Wrappers.<Article>lambdaQuery()
                .eq(Article::getAuthorUserId, userId).isNull(Article::getDeletedAt));
        List<CommunityMoment> moments = momentMapper.selectList(Wrappers.<CommunityMoment>lambdaQuery()
                .eq(CommunityMoment::getActorUserId, userId).isNull(CommunityMoment::getDeletedAt));
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("exportedAt", Instant.now().toString());
        manifest.put("formatVersion", 1);
        manifest.put("includes", List.of("profile", "blog", "articles", "moments"));
        try (ByteArrayOutputStream output = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(output)) {
            write(zip, "manifest.json", manifest);
            write(zip, "profile.json", profile(user));
            write(zip, "blog.json", blog == null ? Map.of() : blog);
            write(zip, "articles.json", articles);
            write(zip, "moments.json", moments);
            zip.finish();
            return new CommunityDataExport("community-data-" + userId + ".zip", output.toByteArray());
        } catch (IOException exception) {
            throw new BusinessException(500, "导出社区数据失败");
        }
    }

    private void write(ZipOutputStream zip, String name, Object value) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(objectMapper.writeValueAsBytes(value));
        zip.closeEntry();
    }

    private static Map<String, Object> profile(CommunityUser user) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("userId", user.getId());
        profile.put("username", user.getUsername());
        profile.put("displayName", user.getDisplayName());
        profile.put("bio", user.getBio());
        profile.put("avatarFileId", user.getAvatarFileId());
        return profile;
    }
}
