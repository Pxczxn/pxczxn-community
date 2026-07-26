package top.pxczxn.community.user.application;

public interface CommunitySessionService {

    CommunityLoginSession login(CommunityLoginCommand command);

    void logout();

    CurrentCommunityUser getCurrentUser();
}
