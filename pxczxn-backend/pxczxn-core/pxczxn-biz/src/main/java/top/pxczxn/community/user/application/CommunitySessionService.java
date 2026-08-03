package top.pxczxn.community.user.application;

public interface CommunitySessionService {

    CommunityLoginSession login(CommunityLoginCommand command);

    void logout();

    CurrentCommunityUser getCurrentUser();

    String forcePasswordReset(Long userId);

    void changePassword(String currentPassword, String newPassword);

    boolean requiresPasswordChange(Long userId);
}
