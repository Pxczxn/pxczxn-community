package top.pxczxn.community.governance.application;

import cn.dev33.satoken.stp.StpLogic;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.chat.persistence.CommunityChatMessageMapper;
import top.pxczxn.community.file.persistence.CommunityFileReferenceMapper;
import top.pxczxn.community.governance.model.CommunityAccountEnforcementAppeal;
import top.pxczxn.community.governance.model.CommunityAccountEnforcementCase;
import top.pxczxn.community.governance.model.CommunityAccountEnforcementEvent;
import top.pxczxn.community.governance.persistence.CommunityAccountEnforcementAppealMapper;
import top.pxczxn.community.governance.persistence.CommunityAccountEnforcementCaseMapper;
import top.pxczxn.community.governance.persistence.CommunityAccountEnforcementEventMapper;
import top.pxczxn.community.governance.persistence.CommunityAccountEnforcementReviewMapper;
import top.pxczxn.community.sanction.application.CommunitySanctionService;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.social.persistence.CommunityCommentMapper;
import top.pxczxn.community.social.persistence.CommunityMomentMapper;
import top.pxczxn.community.team.persistence.TeamMapper;
import top.pxczxn.community.team.submission.persistence.TeamSubmissionMapper;
import top.pxczxn.community.user.application.CommunitySessionService;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserLoginAccountMapper;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
import top.pxczxn.platform.common.exception.BusinessException;
import top.pxczxn.platform.file.service.SysFileService;
import top.pxczxn.platform.system.helper.SystemConfigHelper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountEnforcementServiceTest {

    private CommunityAccountEnforcementCaseMapper cases;
    private CommunityAccountEnforcementAppealMapper appeals;
    private CommunityAccountEnforcementReviewMapper reviews;
    private CommunityUserMapper users;
    private CommunityAuth communityAuth;
    private CommunityUserLoginAccountMapper loginAccounts;
    private CommunityChatMessageMapper chatMessages;
    private CommunityAccountEnforcementEventMapper events;
    private SystemConfigHelper configHelper;
    private AccountEnforcementService service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "account-enforcement-test"),
                top.pxczxn.community.user.model.CommunityUserLoginAccount.class
        );
        cases = mock(CommunityAccountEnforcementCaseMapper.class);
        appeals = mock(CommunityAccountEnforcementAppealMapper.class);
        reviews = mock(CommunityAccountEnforcementReviewMapper.class);
        users = mock(CommunityUserMapper.class);
        communityAuth = mock(CommunityAuth.class);
        loginAccounts = mock(CommunityUserLoginAccountMapper.class);
        chatMessages = mock(CommunityChatMessageMapper.class);
        events = mock(CommunityAccountEnforcementEventMapper.class);
        configHelper = mock(SystemConfigHelper.class);
        when(configHelper.getInt(SystemConfigHelper.GROUP_SYSTEM, "minimumApprovalCount", 2)).thenReturn(2);
        when(configHelper.getBoolean(SystemConfigHelper.GROUP_SYSTEM, "allowSuperAdminSelfReview", true)).thenReturn(true);
        service = new AccountEnforcementService(
                cases,
                appeals,
                reviews,
                events,
                users,
                communityAuth,
                mock(CommunitySessionService.class),
                loginAccounts,
                mock(CommunitySanctionService.class),
                mock(TeamMapper.class),
                mock(TeamSubmissionMapper.class),
                mock(ArticleMapper.class),
                mock(CommunityMomentMapper.class),
                mock(CommunityCommentMapper.class),
                chatMessages,
                mock(CommunityFileReferenceMapper.class),
                mock(SysFileService.class),
                configHelper
        );
    }

    @Test
    void terminalCaseCannotBeReviewedAgain() {
        CommunityAccountEnforcementCase item = enforcementCase("REJECTED", "ACCOUNT_DELETE");
        when(cases.selectById(1L)).thenReturn(item);

        assertThatThrownBy(() -> service.review(20L, 1L,
                new AccountEnforcementService.ReviewCommand("APPROVE", "再次审核"), false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("状态不允许继续审核");

        verify(reviews, never()).insert(any());
    }

    @Test
    void normalAdministratorInitialApprovalMovesRequestToSuperAdministratorReview() {
        CommunityAccountEnforcementCase item = enforcementCase("SUBMITTED", "ACCOUNT_DELETE");
        when(cases.selectById(1L)).thenReturn(item);

        service.review(20L, 1L, new AccountEnforcementService.ReviewCommand("APPROVE", "初审通过"), false);

        assertThat(item.getStatus()).isEqualTo("UNDER_REVIEW");
        verify(reviews).insert(any());
        verify(cases).updateById(item);
    }

    @Test
    void superAdministratorCanReviewOwnApplicationWhenSettingAllowsIt() {
        CommunityAccountEnforcementCase item = enforcementCase("SUBMITTED", "ACCOUNT_DELETE");
        item.setRequestedByAdminId(20L);
        when(cases.selectById(1L)).thenReturn(item);

        service.review(20L, 1L, new AccountEnforcementService.ReviewCommand("APPROVE", "超级管理员自审"), true);

        assertThat(item.getStatus()).isEqualTo("UNDER_REVIEW");
        verify(reviews).insert(any());
    }

    @Test
    void normalAdministratorCannotPerformSecondaryReview() {
        CommunityAccountEnforcementCase item = enforcementCase("UNDER_REVIEW", "ACCOUNT_DELETE");
        when(cases.selectById(1L)).thenReturn(item);

        assertThatThrownBy(() -> service.review(20L, 1L,
                new AccountEnforcementService.ReviewCommand("APPROVE", "尝试复审"), false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("超级管理员");

        verify(reviews, never()).insert(any());
    }

    @Test
    void superAdministratorSecondaryApprovalMakesRequestReadyForExecution() {
        CommunityAccountEnforcementCase item = enforcementCase("UNDER_REVIEW", "ACCOUNT_DELETE");
        when(cases.selectById(1L)).thenReturn(item);

        service.review(30L, 1L, new AccountEnforcementService.ReviewCommand("APPROVE", "复审通过"), true);

        assertThat(item.getStatus()).isEqualTo("APPROVED");
        verify(reviews).insert(any());
        verify(cases).updateById(item);
    }

    @Test
    void longFreezeIsSubmittedForApprovalInsteadOfExecutedDirectly() {
        when(users.selectById(100L)).thenReturn(user(100L));

        CommunityAccountEnforcementCase item = service.submit(20L,
                new AccountEnforcementService.SubmitCommand(
                        100L, "LONG_FREEZE", "RISK_INVESTIGATION",
                        "调查期间暂停账号使用", "长期冻结申请，等待复审",
                        "{\"description\":\"关联风险调查记录\"}", null, null, null
                ));

        assertThat(item.getStatus()).isEqualTo("SUBMITTED");
        assertThat(item.getMeasureType()).isEqualTo("LONG_FREEZE");
        verify(cases).insert(item);
        verify(users, never()).updateById(any());
    }

    @Test
    void approvedLongFreezeBecomesActiveAndRemainsAppealable() {
        CommunityAccountEnforcementCase item = enforcementCase("APPROVED", "LONG_FREEZE");
        CommunityUser target = user(100L);
        when(cases.selectById(1L)).thenReturn(item);
        when(users.selectById(100L)).thenReturn(target);
        when(communityAuth.stpLogic()).thenReturn(mock(StpLogic.class));

        String confirmation = service.confirmationText(1L);
        service.execute(30L, 1L, confirmation);

        assertThat(item.getStatus()).isEqualTo("ACTIVE");
        assertThat(item.getAppealDeadlineAt()).isNotNull();
        assertThat(item.getExecuteAfter()).isNull();
        assertThat(target.getStatus()).isEqualTo("FROZEN");
    }

    @Test
    void appealIsRejectedAfterThePublishedDeadline() {
        CommunityAccountEnforcementCase item = enforcementCase("ACTIVE", "LONG_FREEZE");
        item.setAppealAllowed(true);
        item.setAppealDeadlineAt(now().minusMinutes(1));
        when(cases.selectById(1L)).thenReturn(item);

        assertThatThrownBy(() -> service.appeal(100L, 1L, "请求复核", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("超过申诉期限");

        verify(appeals, never()).insert(any());
    }

    @Test
    void emptyEvidenceObjectIsRejected() {
        when(users.selectById(100L)).thenReturn(user(100L));

        assertThatThrownBy(() -> service.freeze(20L,
                new AccountEnforcementService.FreezeCommand(
                        100L, "SECURITY_RISK", "临时控制账号", "内部调查中", "{}", now().plusDays(1)
                ), false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("证据说明");

        verify(cases, never()).insert(any());
    }

    @Test
    void modifyAppealChangesTemporaryFreezeDeadline() {
        CommunityAccountEnforcementCase item = enforcementCase("APPEALED", "TEMP_FREEZE");
        item.setExpiresAt(now().plusDays(5));
        CommunityAccountEnforcementAppeal appeal = new CommunityAccountEnforcementAppeal();
        appeal.setId(2L);
        appeal.setCaseId(1L);
        appeal.setStatus("SUBMITTED");
        when(appeals.selectById(2L)).thenReturn(appeal);
        when(cases.selectById(1L)).thenReturn(item);
        LocalDateTime modifiedDeadline = now().plusDays(2);

        CommunityAccountEnforcementAppeal result = service.reviewAppeal(
                20L, 2L, "MODIFY", "缩短冻结期限", modifiedDeadline
        );

        assertThat(result.getStatus()).isEqualTo("MODIFIED");
        assertThat(item.getStatus()).isEqualTo("ACTIVE");
        assertThat(item.getExpiresAt()).isEqualTo(modifiedDeadline);
        verify(appeals).updateById(appeal);
        verify(cases).updateById(item);
    }

    @Test
    void appealDecisionsUseDatabaseStatusValuesAndAdminAuditActor() {
        CommunityAccountEnforcementCase item = enforcementCase("APPEALED", "TEMP_FREEZE");
        CommunityAccountEnforcementAppeal appeal = new CommunityAccountEnforcementAppeal();
        appeal.setId(2L);
        appeal.setCaseId(1L);
        appeal.setStatus("SUBMITTED");
        when(appeals.selectById(2L)).thenReturn(appeal);
        when(cases.selectById(1L)).thenReturn(item);

        CommunityAccountEnforcementAppeal result = service.reviewAppeal(
                20L, 2L, "UPHOLD", "维持原处理", null
        );

        assertThat(result.getStatus()).isEqualTo("UPHELD");
        org.mockito.ArgumentCaptor<CommunityAccountEnforcementEvent> eventCaptor =
                org.mockito.ArgumentCaptor.forClass(CommunityAccountEnforcementEvent.class);
        verify(events, atLeastOnce()).insert(eventCaptor.capture());
        CommunityAccountEnforcementEvent event = eventCaptor.getAllValues().get(eventCaptor.getAllValues().size() - 1);
        assertThat(event.getActorType()).isEqualTo("ADMIN");
        assertThat(event.getEventType()).isEqualTo("APPEAL_UPHOLD");
    }

    @Test
    void returnedCaseCanReceiveEvidenceFromItsRequester() {
        CommunityAccountEnforcementCase item = enforcementCase("SUBMITTED", "ACCOUNT_DELETE");
        item.setEvidenceSnapshot("{\"description\":\"old\"}");
        when(cases.selectById(1L)).thenReturn(item);
        when(reviews.selectCount(any())).thenReturn(1L);

        service.supplementEvidence(10L, 1L, "{\"description\":\"new\"}");

        assertThat(item.getEvidenceSnapshot()).contains("old", "new");
        verify(cases).updateById(item);
    }

    @Test
    void chatCleanupOnlyHidesMessagesFromTheTargetUser() {
        CommunityAccountEnforcementCase item = enforcementCase("APPEAL_WINDOW", "DATA_CLEANUP");
        item.setCleanupScope("[\"CHAT_MESSAGES\"]");
        item.setExecuteAfter(now().minusMinutes(1));
        when(cases.selectList(any())).thenReturn(List.of(), List.of(item));
        CommunityUser target = user(100L);
        when(users.selectById(100L)).thenReturn(target);
        when(communityAuth.stpLogic()).thenReturn(mock(StpLogic.class));

        service.finalizeDue();

        verify(chatMessages).hideForUser(anyLong(), any(LocalDateTime.class));
        assertThat(item.getStatus()).isEqualTo("FINALIZED");
        assertThat(target.getStatus()).isEqualTo("NORMAL");
    }

    @Test
    void accountDeletionAnonymizesIdentityAndInvalidatesCredentials() {
        CommunityAccountEnforcementCase item = enforcementCase("APPEAL_WINDOW", "ACCOUNT_DELETE");
        item.setExecuteAfter(now().minusMinutes(1));
        CommunityUser target = user(100L);
        target.setUsername("original-user");
        target.setDisplayName("原用户");
        when(cases.selectList(any())).thenReturn(List.of(), List.of(item));
        when(users.selectById(100L)).thenReturn(target);
        when(communityAuth.stpLogic()).thenReturn(mock(StpLogic.class));

        service.finalizeDue();

        assertThat(target.getUsername()).isEqualTo("deleted_100");
        assertThat(target.getDisplayName()).isEqualTo("已注销用户");
        assertThat(target.getStatus()).isEqualTo("DELETED");
        verify(loginAccounts).update(any(), any());
        verify(users).updateById(target);
    }

    @Test
    void confirmationTextMatchesTheTextRequiredByExecution() {
        CommunityAccountEnforcementCase item = enforcementCase("APPROVED", "ACCOUNT_DELETE");
        when(cases.selectById(1L)).thenReturn(item);
        when(users.selectById(100L)).thenReturn(user(100L));
        when(communityAuth.stpLogic()).thenReturn(mock(StpLogic.class));

        String confirmation = service.confirmationText(1L);

        assertThat(confirmation).isEqualTo("我已审核，确认对账号 user-100（ID：100）执行强制删除操作");
        service.execute(20L, 1L, confirmation);

        verify(cases).updateById(item);
        assertThat(item.getStatus()).isEqualTo("APPEAL_WINDOW");
    }

    private static CommunityAccountEnforcementCase enforcementCase(String status, String measureType) {
        CommunityAccountEnforcementCase item = new CommunityAccountEnforcementCase();
        item.setId(1L);
        item.setTargetUserId(100L);
        item.setRequestedByAdminId(10L);
        item.setStatus(status);
        item.setMeasureType(measureType);
        item.setLockVersion(0);
        return item;
    }

    private static CommunityUser user(Long id) {
        CommunityUser user = new CommunityUser();
        user.setId(id);
        user.setUsername("user-" + id);
        user.setDisplayName("用户 " + id);
        user.setStatus("NORMAL");
        return user;
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
