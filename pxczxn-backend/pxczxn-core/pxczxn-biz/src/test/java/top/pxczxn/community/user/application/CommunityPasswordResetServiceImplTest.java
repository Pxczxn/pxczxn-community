package top.pxczxn.community.user.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import top.pxczxn.community.abuse.application.CommunityAbuseGuard;
import top.pxczxn.community.user.model.CommunityUserLoginAccount;
import top.pxczxn.community.user.persistence.CommunityUserLoginAccountMapper;
import top.pxczxn.platform.common.exception.BusinessException;
import top.pxczxn.platform.mail.EmailService;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommunityPasswordResetServiceImplTest {

    private static final String EMAIL = "alice@example.com";
    private static final String VALID_PASSWORD = "NewPassw0rd!";

    private CommunityUserLoginAccountMapper loginAccountMapper;
    private CommunityAbuseGuard abuseGuard;
    private EmailService emailService;
    private CommunitySessionService sessionService;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private CommunityPasswordResetServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        loginAccountMapper = mock(CommunityUserLoginAccountMapper.class);
        abuseGuard = mock(CommunityAbuseGuard.class);
        emailService = mock(EmailService.class);
        sessionService = mock(CommunitySessionService.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new CommunityPasswordResetServiceImpl(
                loginAccountMapper,
                abuseGuard,
                emailService,
                sessionService,
                redisTemplate
        );
    }

    @Test
    void sendResetCodeStoresSixDigitCodeAndSendsMailForRegisteredEmail() {
        when(loginAccountMapper.selectOne(any())).thenReturn(account());

        service.sendResetCode(" Alice@Example.COM ");

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(
                eq("community:pwdreset:code:alice@example.com"),
                codeCaptor.capture(),
                eq(Duration.ofMinutes(10))
        );
        assertThat(codeCaptor.getValue()).matches("^\\d{6}$");
        verify(valueOperations).set(
                eq("community:pwdreset:attempts:alice@example.com"),
                eq("5"),
                eq(Duration.ofMinutes(10))
        );
        verify(emailService).sendResetPassword("alice@example.com", codeCaptor.getValue(), 10);
    }

    @Test
    void sendResetCodeDoesNotSendMailForUnknownEmail() {
        when(loginAccountMapper.selectOne(any())).thenReturn(null);

        service.sendResetCode(EMAIL);

        verify(emailService, never()).sendResetPassword(anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt());
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void sendResetCodeCleansUpWhenMailDeliveryFails() {
        when(loginAccountMapper.selectOne(any())).thenReturn(account());
        doThrow(new RuntimeException("smtp down"))
                .when(emailService).sendResetPassword(anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt());

        assertThatThrownBy(() -> service.sendResetCode(EMAIL))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("验证码发送失败");

        verify(redisTemplate).delete("community:pwdreset:code:" + EMAIL);
        verify(redisTemplate).delete("community:pwdreset:attempts:" + EMAIL);
    }

    @Test
    void resetPasswordRejectsWrongCodeWithoutTouchingSessionService() {
        when(loginAccountMapper.selectOne(any())).thenReturn(account());
        when(valueOperations.get("community:pwdreset:code:" + EMAIL)).thenReturn("123456");

        assertThatThrownBy(() -> service.resetPassword(EMAIL, "000000", VALID_PASSWORD))
                .isInstanceOf(BusinessException.class)
                .hasMessage("验证码错误或已过期");

        verify(sessionService, never()).resetPassword(anyString(), anyString());
    }

    @Test
    void resetPasswordRejectsExpiredCode() {
        when(loginAccountMapper.selectOne(any())).thenReturn(account());
        when(valueOperations.get("community:pwdreset:code:" + EMAIL)).thenReturn(null);

        assertThatThrownBy(() -> service.resetPassword(EMAIL, "123456", VALID_PASSWORD))
                .isInstanceOf(BusinessException.class)
                .hasMessage("验证码错误或已过期");

        verify(sessionService, never()).resetPassword(anyString(), anyString());
    }

    @Test
    void resetPasswordConsumesCodeAndDelegatesToSessionServiceOnSuccess() {
        when(loginAccountMapper.selectOne(any())).thenReturn(account());
        when(valueOperations.get("community:pwdreset:code:" + EMAIL)).thenReturn("123456");

        service.resetPassword(EMAIL, " 123456 ", VALID_PASSWORD);

        // 先完成密码更新，成功后才消费验证码
        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(sessionService, redisTemplate);
        inOrder.verify(sessionService).resetPassword(EMAIL, VALID_PASSWORD);
        inOrder.verify(redisTemplate).delete("community:pwdreset:code:" + EMAIL);
        inOrder.verify(redisTemplate).delete("community:pwdreset:attempts:" + EMAIL);
    }

    @Test
    void resetPasswordKeepsCodeWhenPasswordUpdateFails() {
        when(loginAccountMapper.selectOne(any())).thenReturn(account());
        when(valueOperations.get("community:pwdreset:code:" + EMAIL)).thenReturn("123456");
        org.mockito.Mockito.doThrow(new RuntimeException("db down"))
                .when(sessionService).resetPassword(anyString(), anyString());

        assertThatThrownBy(() -> service.resetPassword(EMAIL, "123456", VALID_PASSWORD))
                .isInstanceOf(RuntimeException.class);

        // 更新失败时验证码未被消费，用户可重试
        verify(redisTemplate, never()).delete("community:pwdreset:code:" + EMAIL);
        verify(redisTemplate, never()).delete("community:pwdreset:attempts:" + EMAIL);
    }

    @Test
    void resetPasswordInvalidatesCodeAfterFiveFailedAttempts() {
        when(loginAccountMapper.selectOne(any())).thenReturn(account());
        when(valueOperations.get("community:pwdreset:code:" + EMAIL)).thenReturn("123456");
        java.util.concurrent.atomic.AtomicInteger remainingAttempts =
                new java.util.concurrent.atomic.AtomicInteger(CommunityPasswordResetServiceImpl.MAX_ATTEMPTS);
        when(valueOperations.get("community:pwdreset:attempts:" + EMAIL)).thenAnswer(invocation ->
                String.valueOf(remainingAttempts.getAndDecrement()));

        for (int i = 0; i < CommunityPasswordResetServiceImpl.MAX_ATTEMPTS; i++) {
            assertThatThrownBy(() -> service.resetPassword(EMAIL, "000000", VALID_PASSWORD))
                    .isInstanceOf(BusinessException.class);
        }

        // 第 5 次失败后验证码与剩余次数均被清除
        verify(redisTemplate).delete("community:pwdreset:code:" + EMAIL);
        verify(redisTemplate).delete("community:pwdreset:attempts:" + EMAIL);
        verify(sessionService, never()).resetPassword(anyString(), anyString());
    }

    @Test
    void resetPasswordRejectsWeakPassword() {
        when(loginAccountMapper.selectOne(any())).thenReturn(account());
        when(valueOperations.get("community:pwdreset:code:" + EMAIL)).thenReturn("123456");

        assertThatThrownBy(() -> service.resetPassword(EMAIL, "123456", "weak"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("新密码须为 12-72 位");

        verify(sessionService, never()).resetPassword(anyString(), anyString());
    }

    private static CommunityUserLoginAccount account() {
        CommunityUserLoginAccount account = new CommunityUserLoginAccount();
        account.setId(100L);
        account.setUserId(200L);
        account.setLoginType("EMAIL");
        account.setNormalizedIdentifier(EMAIL);
        account.setPasswordHash("$2a$10$hash");
        return account;
    }
}
