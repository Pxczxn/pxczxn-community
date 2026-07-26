package top.pxczxn.community.file.application;

import com.mars.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.permission.ArticleAction;
import top.pxczxn.community.article.permission.ArticlePermissionService;
import top.pxczxn.community.article.permission.ArticlePublicAccess;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.file.model.CommunityFileReference;
import top.pxczxn.community.file.model.FileObject;
import top.pxczxn.community.file.persistence.CommunityFileReferenceMapper;
import top.pxczxn.community.file.persistence.FileObjectMapper;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommunityFileServiceTest {

    private FileObjectMapper fileMapper;
    private CommunityFileReferenceMapper referenceMapper;
    private CommunityUserMapper userMapper;
    private CommunityAuth communityAuth;
    private CommunityObjectStorage storage;
    private BlogMapper blogMapper;
    private ArticleMapper articleMapper;
    private ArticlePermissionService articlePermissionService;
    private CommunityFileService service;

    @BeforeEach
    void setUp() {
        fileMapper = mock(FileObjectMapper.class);
        referenceMapper = mock(CommunityFileReferenceMapper.class);
        userMapper = mock(CommunityUserMapper.class);
        communityAuth = mock(CommunityAuth.class);
        storage = mock(CommunityObjectStorage.class);
        blogMapper = mock(BlogMapper.class);
        articleMapper = mock(ArticleMapper.class);
        articlePermissionService = mock(ArticlePermissionService.class);
        service = new CommunityFileService(
                fileMapper,
                referenceMapper,
                userMapper,
                communityAuth,
                storage,
                blogMapper,
                articleMapper,
                articlePermissionService
        );
    }

    @Test
    void uploadUsesInspectedTypeAndRecordsOwnerAndHash() {
        arrangeUser(100L);
        when(storage.provider()).thenReturn("local");
        when(fileMapper.insert(any())).thenReturn(1);
        byte[] png = {
                (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'
        };

        CommunityFileInfo info = service.upload(new UploadCommunityFileCommand(
                "../avatar.png",
                "video/mp4",
                png
        ));

        assertThat(info.originalName()).isEqualTo("avatar.png");
        assertThat(info.mimeType()).isEqualTo("image/png");
        assertThat(info.ownerUserId()).isEqualTo(100L);
        assertThat(info.sha256()).hasSize(64);
        verify(storage).upload(any(), any());
    }

    @Test
    void otherUsersFileCannotBeLinked() {
        arrangeUser(100L);
        FileObject file = file(200L, 999L);
        when(fileMapper.selectById(200L)).thenReturn(file);

        assertThatThrownBy(() -> service.linkOwnedFile(
                200L, "BLOG", 300L, "BLOG_AVATAR"
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("无权使用该文件");

        verify(referenceMapper, never()).insert(any());
    }

    @Test
    void referencedFileCannotBeDeleted() {
        arrangeUser(100L);
        when(fileMapper.selectById(200L)).thenReturn(file(200L, 100L));
        when(referenceMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteMine(200L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仍被业务内容引用");

        verify(fileMapper, never()).update(any(), any());
        verify(storage, never()).delete(any());
    }

    @Test
    void unreferencedFileIsNotPublic() {
        when(fileMapper.selectById(200L)).thenReturn(file(200L, 100L));
        when(referenceMapper.selectList(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.readPublic(200L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文件不存在");

        verify(storage, never()).read(any());
    }

    @Test
    void publishedArticleCoverCanBeReadPublicly() {
        when(fileMapper.selectById(200L)).thenReturn(file(200L, 100L));
        when(referenceMapper.selectList(any())).thenReturn(List.of(
                reference(200L, "ARTICLE", 300L, "ARTICLE_COVER")
        ));
        when(articlePermissionService.requirePublicArticle(
                300L,
                ArticleAction.VIEW_DETAIL
        )).thenReturn(mock(ArticlePublicAccess.class));
        when(storage.read(any())).thenReturn(new byte[]{1, 2, 3});

        CommunityFileContent content = service.readPublic(200L);

        assertThat(content.content()).containsExactly(1, 2, 3);
    }

    @Test
    void onlyCurrentPublishedVersionCanExposeContentImage() {
        when(fileMapper.selectById(200L)).thenReturn(file(200L, 100L));
        when(referenceMapper.selectList(any())).thenReturn(List.of(
                reference(
                        200L,
                        "ARTICLE_VERSION",
                        500L,
                        "CONTENT_IMAGE"
                )
        ));
        Article article = new Article();
        article.setId(300L);
        article.setPublishedVersionId(500L);
        when(articleMapper.selectOne(any())).thenReturn(article);
        when(articlePermissionService.requirePublicArticle(
                300L,
                ArticleAction.VIEW_DETAIL
        )).thenReturn(mock(ArticlePublicAccess.class));
        when(storage.read(any())).thenReturn(new byte[]{4, 5});

        CommunityFileContent content = service.readPublic(200L);

        assertThat(content.content()).containsExactly(4, 5);
    }

    @Test
    void draftVersionContentImageRemainsPrivate() {
        when(fileMapper.selectById(200L)).thenReturn(file(200L, 100L));
        when(referenceMapper.selectList(any())).thenReturn(List.of(
                reference(
                        200L,
                        "ARTICLE_VERSION",
                        501L,
                        "CONTENT_IMAGE"
                )
        ));
        when(articleMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.readPublic(200L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(404);
        verify(storage, never()).read(any());
    }

    @Test
    void inactiveBlogAvatarIsNotPublic() {
        when(fileMapper.selectById(200L)).thenReturn(file(200L, 100L));
        when(referenceMapper.selectList(any())).thenReturn(List.of(
                reference(200L, "BLOG", 300L, "BLOG_AVATAR")
        ));
        Blog blog = new Blog();
        blog.setId(300L);
        blog.setOwnerUserId(100L);
        blog.setStatus("HIDDEN");
        when(blogMapper.selectById(300L)).thenReturn(blog);

        assertThatThrownBy(() -> service.readPublic(200L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(404);
    }

    @Test
    void contentVersionCanReferenceMultipleOwnedFiles() {
        arrangeUser(100L);
        when(fileMapper.selectById(201L)).thenReturn(file(201L, 100L));
        when(fileMapper.selectById(202L)).thenReturn(file(202L, 100L));
        when(referenceMapper.selectOne(any())).thenReturn(null);
        when(referenceMapper.insert(any())).thenReturn(1);

        service.replaceOwnedFileReferences(
                List.of(201L, 202L),
                "ARTICLE_VERSION",
                300L,
                "CONTENT_IMAGE"
        );

        verify(referenceMapper, times(2)).insert(any());
    }

    @Test
    void contentVersionCannotReferenceMoreThanOneHundredFiles() {
        arrangeUser(100L);
        List<Long> ids = LongStream.rangeClosed(1, 101).boxed().toList();

        assertThatThrownBy(() -> service.replaceOwnedFileReferences(
                ids,
                "ARTICLE_VERSION",
                300L,
                "CONTENT_IMAGE"
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("最多引用 100");

        verify(fileMapper, never()).selectById(any());
    }

    private void arrangeUser(Long userId) {
        when(communityAuth.getLoginUserId()).thenReturn(userId);
        CommunityUser user = new CommunityUser();
        user.setId(userId);
        user.setStatus("NORMAL");
        when(userMapper.selectById(userId)).thenReturn(user);
    }

    private static FileObject file(Long fileId, Long ownerId) {
        FileObject file = new FileObject();
        file.setId(fileId);
        file.setCreatedByUserId(ownerId);
        file.setStatus("ACTIVE");
        file.setObjectKey("community/test/file.png");
        file.setOriginalName("file.png");
        file.setMimeType("image/png");
        file.setSizeBytes(8L);
        file.setSha256("0".repeat(64));
        return file;
    }

    private static CommunityFileReference reference(
            Long fileId,
            String targetType,
            Long targetId,
            String usageType
    ) {
        CommunityFileReference reference = new CommunityFileReference();
        reference.setFileId(fileId);
        reference.setTargetType(targetType);
        reference.setTargetId(targetId);
        reference.setUsageType(usageType);
        return reference;
    }
}
