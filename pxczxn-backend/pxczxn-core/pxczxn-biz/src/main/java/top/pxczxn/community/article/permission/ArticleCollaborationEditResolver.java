package top.pxczxn.community.article.permission;

import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.user.model.CommunityUser;

@FunctionalInterface
public interface ArticleCollaborationEditResolver {
    boolean canEdit(CommunityUser actor, Article article);
}
