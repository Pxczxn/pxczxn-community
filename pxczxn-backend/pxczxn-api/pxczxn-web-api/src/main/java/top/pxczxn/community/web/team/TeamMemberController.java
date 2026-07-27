package top.pxczxn.community.web.team;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.team.application.InviteTeamMemberCommand;
import top.pxczxn.community.team.application.TeamInvitationView;
import top.pxczxn.community.team.application.TeamMemberService;
import top.pxczxn.community.team.application.TeamMemberView;
import top.pxczxn.platform.common.result.Result;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/teams")
public class TeamMemberController {

    private final TeamMemberService memberService;
    private final CommunityAuth communityAuth;

    @GetMapping("/{teamId}/members")
    public Result<List<TeamMemberView>> members(@PathVariable Long teamId) {
        return Result.ok(memberService.members(communityAuth.getLoginUserId(), teamId));
    }

    @PostMapping("/{teamId}/invitations")
    public Result<TeamInvitationView> invite(@PathVariable Long teamId, @RequestBody TeamMemberRequest request) {
        return Result.ok(memberService.invite(communityAuth.getLoginUserId(),
                new InviteTeamMemberCommand(teamId, request.userId(), request.roleCode(), request.idempotencyKey())));
    }

    @GetMapping("/invitations/me")
    public Result<List<TeamInvitationView>> invitations() {
        return Result.ok(memberService.myPendingInvitations(communityAuth.getLoginUserId()));
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public Result<Void> accept(@PathVariable Long invitationId) {
        memberService.accept(communityAuth.getLoginUserId(), invitationId);
        return Result.ok();
    }

    @PostMapping("/invitations/{invitationId}/reject")
    public Result<Void> reject(@PathVariable Long invitationId) {
        memberService.reject(communityAuth.getLoginUserId(), invitationId);
        return Result.ok();
    }

    @PostMapping("/{teamId}/members/{userId}/role")
    public Result<Void> changeRole(@PathVariable Long teamId, @PathVariable Long userId,
                                   @RequestBody TeamMemberRequest request) {
        memberService.changeRole(communityAuth.getLoginUserId(), teamId, userId, request.roleCode());
        return Result.ok();
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    public Result<Void> remove(@PathVariable Long teamId, @PathVariable Long userId) {
        memberService.remove(communityAuth.getLoginUserId(), teamId, userId);
        return Result.ok();
    }

    @PostMapping("/{teamId}/leave")
    public Result<Void> leave(@PathVariable Long teamId) {
        memberService.leave(communityAuth.getLoginUserId(), teamId);
        return Result.ok();
    }

    @PostMapping("/{teamId}/owner")
    public Result<Void> transfer(@PathVariable Long teamId, @RequestBody TeamMemberRequest request) {
        memberService.transferOwnership(communityAuth.getLoginUserId(), teamId, request.userId());
        return Result.ok();
    }

    @PostMapping("/{teamId}/disband")
    public Result<Void> disband(@PathVariable Long teamId) {
        memberService.disband(communityAuth.getLoginUserId(), teamId);
        return Result.ok();
    }

    public record TeamMemberRequest(Long userId, String roleCode, String idempotencyKey) { }
}
