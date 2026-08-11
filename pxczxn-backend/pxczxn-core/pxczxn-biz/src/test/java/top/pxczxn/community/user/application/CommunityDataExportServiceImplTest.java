package top.pxczxn.community.user.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.social.model.CommunityMoment;
import top.pxczxn.community.social.persistence.CommunityMomentMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommunityDataExportServiceImplTest {

    @Test
    void exportsCurrentUsersProfileBlogArticlesAndMomentsAsZipEntries() throws Exception {
        CommunityAuth auth = mock(CommunityAuth.class);
        CommunityUserMapper users = mock(CommunityUserMapper.class);
        BlogMapper blogs = mock(BlogMapper.class);
        ArticleMapper articles = mock(ArticleMapper.class);
        CommunityMomentMapper moments = mock(CommunityMomentMapper.class);
        CommunityDataExportService service = new CommunityDataExportServiceImpl(
                auth, users, blogs, articles, moments, new ObjectMapper()
        );
        CommunityUser user = new CommunityUser();
        user.setId(7L); user.setUsername("exporter"); user.setDisplayName("Export User"); user.setPersonalBlogId(9L);
        Blog blog = new Blog(); blog.setId(9L); blog.setName("Export Blog");
        Article article = new Article(); article.setId(11L); article.setTitle("Export Article");
        CommunityMoment moment = new CommunityMoment(); moment.setId(13L); moment.setTextContent("Export Moment");
        when(auth.getLoginUserId()).thenReturn(7L);
        when(users.selectById(7L)).thenReturn(user);
        when(blogs.selectById(9L)).thenReturn(blog);
        when(articles.selectList(any())).thenReturn(List.of(article));
        when(moments.selectList(any())).thenReturn(List.of(moment));

        CommunityDataExport result = service.exportMine();

        assertThat(result.filename()).isEqualTo("community-data-7.zip");
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(result.content()))) {
            assertThat(zip.getNextEntry().getName()).isEqualTo("manifest.json");
            assertThat(new String(zip.readAllBytes(), StandardCharsets.UTF_8)).contains("formatVersion");
            assertThat(zip.getNextEntry().getName()).isEqualTo("profile.json");
            assertThat(new String(zip.readAllBytes(), StandardCharsets.UTF_8)).contains("exporter");
            assertThat(zip.getNextEntry().getName()).isEqualTo("blog.json");
            assertThat(new String(zip.readAllBytes(), StandardCharsets.UTF_8)).contains("Export Blog");
            assertThat(zip.getNextEntry().getName()).isEqualTo("articles.json");
            assertThat(new String(zip.readAllBytes(), StandardCharsets.UTF_8)).contains("Export Article");
            assertThat(zip.getNextEntry().getName()).isEqualTo("moments.json");
            assertThat(new String(zip.readAllBytes(), StandardCharsets.UTF_8)).contains("Export Moment");
        }
    }
}
