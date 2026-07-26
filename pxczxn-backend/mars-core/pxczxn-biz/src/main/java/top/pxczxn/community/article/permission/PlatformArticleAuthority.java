package top.pxczxn.community.article.permission;

public interface PlatformArticleAuthority {

    boolean isAuthenticated();

    boolean hasPermission(String permission);
}
