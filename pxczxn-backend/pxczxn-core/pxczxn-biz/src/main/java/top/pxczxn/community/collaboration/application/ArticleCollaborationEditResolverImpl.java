package top.pxczxn.community.collaboration.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.permission.ArticleCollaborationEditResolver;
import top.pxczxn.community.collaboration.model.ArticleCollaborator;
import top.pxczxn.community.collaboration.persistence.ArticleCollaboratorMapper;
import top.pxczxn.community.user.model.CommunityUser;

@Component
@RequiredArgsConstructor
public class ArticleCollaborationEditResolverImpl implements ArticleCollaborationEditResolver {
    private final ArticleCollaboratorMapper collaboratorMapper;

    @Override
    public boolean canEdit(CommunityUser actor, Article article) {
        if (actor == null || actor.getId() == null || article == null || article.getId() == null) return false;
        ArticleCollaborator collaborator = collaboratorMapper.findActive(article.getId(), actor.getId());
        return collaborator != null && Boolean.TRUE.equals(collaborator.getCanEdit());
    }
}
