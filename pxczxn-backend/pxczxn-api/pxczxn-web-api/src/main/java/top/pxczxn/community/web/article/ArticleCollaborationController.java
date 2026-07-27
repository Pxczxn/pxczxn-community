package top.pxczxn.community.web.article;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.pxczxn.community.collaboration.application.*;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.platform.common.result.Result;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/articles")
public class ArticleCollaborationController {
    private final ArticleCollaborationService service; private final CommunityAuth auth;
    @PostMapping("/{articleId}/collaborators/invitations") public Result<ArticleCollaborationResponse> invite(@PathVariable Long articleId, @Valid @RequestBody InviteRequest r) { return Result.ok(ArticleCollaborationResponse.invitation(service.invite(auth.getLoginUserId(), articleId, new InviteArticleCollaboratorCommand(r.inviteeUserId(), r.contributionType(), r.canEdit(), r.attributionOrder(), r.message(), r.idempotencyKey())))); }
    @GetMapping("/{articleId}/collaborators") public Result<List<ArticleCollaborationResponse>> collaborators(@PathVariable Long articleId) { return Result.ok(service.collaborators(auth.getLoginUserId(), articleId).stream().map(ArticleCollaborationResponse::collaborator).toList()); }
    @GetMapping("/collaboration-invitations/me") public Result<List<ArticleCollaborationResponse>> mine() { return Result.ok(service.myPendingInvitations(auth.getLoginUserId()).stream().map(ArticleCollaborationResponse::invitation).toList()); }
    @PostMapping("/collaboration-invitations/{invitationId}/accept") public Result<ArticleCollaborationResponse> accept(@PathVariable Long invitationId, @Valid @RequestBody RespondRequest r) { return Result.ok(ArticleCollaborationResponse.collaborator(service.accept(auth.getLoginUserId(), invitationId, new RespondArticleCollaborationCommand(r.expectedLockVersion())))); }
    @PostMapping("/collaboration-invitations/{invitationId}/reject") public Result<Void> reject(@PathVariable Long invitationId, @Valid @RequestBody RespondRequest r) { service.reject(auth.getLoginUserId(), invitationId, new RespondArticleCollaborationCommand(r.expectedLockVersion())); return Result.ok(); }
    @PostMapping("/collaboration-invitations/{invitationId}/cancel") public Result<Void> cancel(@PathVariable Long invitationId, @Valid @RequestBody RespondRequest r) { service.cancel(auth.getLoginUserId(), invitationId, new RespondArticleCollaborationCommand(r.expectedLockVersion())); return Result.ok(); }
    @DeleteMapping("/{articleId}/collaborators/{collaboratorId}") public Result<Void> revoke(@PathVariable Long articleId, @PathVariable Long collaboratorId, @RequestParam Integer expectedLockVersion) { service.revoke(auth.getLoginUserId(), articleId, collaboratorId, expectedLockVersion); return Result.ok(); }
    public record InviteRequest(@NotNull Long inviteeUserId, String contributionType, Boolean canEdit, @NotNull Integer attributionOrder, String message, @NotBlank String idempotencyKey) {}
    public record RespondRequest(@NotNull Integer expectedLockVersion) {}
}
