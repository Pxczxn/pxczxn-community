package top.pxczxn.community.user.application;

import cn.hutool.crypto.digest.BCrypt;
import top.pxczxn.platform.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.sanction.application.CommunitySanctionService;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.model.CommunityUserLoginAccount;
import top.pxczxn.community.user.persistence.CommunityUserLoginAccountMapper;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommunitySessionServiceImplTest {

    private static final String PASSWORD_HASH = BCrypt.hashpw("correct-password");

    private CommunityUserMapper userMapper;
    private CommunityUserLoginAccountMapper loginAccountMapper;
    private BlogMapper blogMapper;
    private CommunityAuth communityAuth;
    private CommunitySessionServiceImpl service;

    @BeforeEach
    void setUp() {
        userMapper = mock(CommunityUserMapper.class);
        loginAccountMapper = mock(CommunityUserLoginAccountMapper.class);
        blogMapper = mock(BlogMapper.class);
        communityAuth = mock(CommunityAuth.class);
        service = new CommunitySessionServiceImpl(
                userMapper,
                loginAccountMapper,
                blogMapper,
                communityAuth,
                mock(CommunitySanctionService.class)
        );
    }

    @Test
    void loginIssuesIndependentCommunitySession() {
        CommunityUserLoginAccount account = account(100L, 200L, 0, null);
        CommunityUser user = user(200L, "NORMAL");
        when(loginAccountMapper.selectOne(any())).thenReturn(account);
        when(userMapper.selectById(200L)).thenReturn(user);
        when(loginAccountMapper.recordLoginSuccess(anyLong(), any())).thenReturn(1);
        when(userMapper.recordLoginSuccess(anyLong(), any())).thenReturn(1);
        when(communityAuth.getTokenValue()).thenReturn("community-token");
        when(communityAuth.getTokenTimeout()).thenReturn(604800L);

        CommunityLoginSession result = service.login(
                new CommunityLoginCommand(" Alice@Example.COM ", "correct-password")
        );

        assertThat(result.tokenName()).isEqualTo(CommunityAuth.TOKEN_NAME);
        assertThat(result.tokenValue()).isEqualTo("community-token");
        assertThat(result.userId()).isEqualTo(200L);
        verify(communityAuth).login(200L);
        verify(loginAccountMapper).recordLoginSuccess(anyLong(), any());
        verify(userMapper).recordLoginSuccess(anyLong(), any());
    }

    @Test
    void wrongPasswordRecordsFailureWithoutIssuingToken() {
        CommunityUserLoginAccount account = account(100L, 200L, 0, null);
        when(loginAccountMapper.selectOne(any())).thenReturn(account);
        when(userMapper.selectById(200L)).thenReturn(user(200L, "NORMAL"));
        when(loginAccountMapper.recordLoginFailure(anyLong(), anyInt(), any()))
                .thenReturn(1);

        assertThatThrownBy(() -> service.login(
                new CommunityLoginCommand("alice@example.com", "wrong-password")
        ))
                .isInstanceOf(InvalidCommunityCredentialsException.class)
                .hasMessage("邮箱或密码错误");

        verify(loginAccountMapper).recordLoginFailure(
                org.mockito.ArgumentMatchers.eq(100L),
                org.mockito.ArgumentMatchers.eq(5),
                any()
        );
        verify(communityAuth, never()).login(anyLong());
    }

    @Test
    void fifthWrongPasswordLocksAccount() {
        CommunityUserLoginAccount account = account(100L, 200L, 4, null);
        when(loginAccountMapper.selectOne(any())).thenReturn(account);
        when(userMapper.selectById(200L)).thenReturn(user(200L, "NORMAL"));
        when(loginAccountMapper.recordLoginFailure(anyLong(), anyInt(), any()))
                .thenReturn(1);

        assertThatThrownBy(() -> service.login(
                new CommunityLoginCommand("alice@example.com", "wrong-password")
        ))
                .isInstanceOf(CommunityLoginLockedException.class)
                .hasMessageContaining("15 分钟");
    }

    @Test
    void activeLockAndBannedStatusBothBlockLogin() {
        CommunityUserLoginAccount locked = account(
                100L,
                200L,
                5,
                LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10)
        );
        when(loginAccountMapper.selectOne(any())).thenReturn(locked);

        assertThatThrownBy(() -> service.login(
                new CommunityLoginCommand("alice@example.com", "correct-password")
        )).isInstanceOf(CommunityLoginLockedException.class);
        verify(userMapper, never()).selectById(anyLong());

        CommunityUserLoginAccount unlocked = account(101L, 201L, 0, null);
        when(loginAccountMapper.selectOne(any())).thenReturn(unlocked);
        when(userMapper.selectById(201L)).thenReturn(user(201L, "BANNED"));

        assertThatThrownBy(() -> service.login(
                new CommunityLoginCommand("other@example.com", "correct-password")
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("账号已封禁");
    }

    @Test
    void currentUserContainsPrivateAccountAndPersonalBlogData() {
        CommunityUser user = user(200L, "NORMAL");
        user.setDisplayName("Alice");
        user.setBio("Writer");
        user.setPersonalBlogId(300L);
        user.setVerificationStatus("UNVERIFIED");
        CommunityUserLoginAccount account = account(100L, 200L, 0, null);
        account.setNormalizedIdentifier("alice@example.com");
        Blog blog = new Blog();
        blog.setId(300L);
        blog.setName("Alice 的博客");
        blog.setSlug("alice");

        when(communityAuth.getLoginUserId()).thenReturn(200L);
        when(userMapper.selectById(200L)).thenReturn(user);
        when(loginAccountMapper.selectOne(any())).thenReturn(account);
        when(blogMapper.selectById(300L)).thenReturn(blog);

        CurrentCommunityUser result = service.getCurrentUser();

        assertThat(result.userId()).isEqualTo(200L);
        assertThat(result.email()).isEqualTo("alice@example.com");
        assertThat(result.personalBlogId()).isEqualTo(300L);
        assertThat(result.blogSlug()).isEqualTo("alice");
    }

    @Test
    void credentialExceptionsAreConfiguredToCommitFailureCounters() throws Exception {
        Transactional annotation = CommunitySessionServiceImpl.class
                .getMethod("login", CommunityLoginCommand.class)
                .getAnnotation(Transactional.class);

        assertThat(annotation).isNotNull();
        assertThat(Arrays.asList(annotation.noRollbackFor()))
                .contains(
                        InvalidCommunityCredentialsException.class,
                        CommunityLoginLockedException.class
                );
    }

    private static CommunityUserLoginAccount account(
            Long accountId,
            Long userId,
            int failedCount,
            LocalDateTime lockedUntil
    ) {
        CommunityUserLoginAccount account = new CommunityUserLoginAccount();
        account.setId(accountId);
        account.setUserId(userId);
        account.setLoginType("EMAIL");
        account.setNormalizedIdentifier("alice@example.com");
        account.setPasswordHash(PASSWORD_HASH);
        account.setFailedLoginCount(failedCount);
        account.setLockedUntil(lockedUntil);
        return account;
    }

    private static CommunityUser user(Long userId, String status) {
        CommunityUser user = new CommunityUser();
        user.setId(userId);
        user.setUsername("alice");
        user.setStatus(status);
        return user;
    }
}
