package top.pxczxn.community.social.application;

@FunctionalInterface
public interface TeamBlogMemberResolver {

    boolean isMember(Long userId, Long blogId);
}
