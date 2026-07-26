package top.pxczxn.community.user.application;

public interface CommunityRegistrationService {

    boolean isUsernameAvailable(String username);

    boolean isEmailAvailable(String email);

    RegisteredCommunityUser register(RegisterCommunityUserCommand command);
}
