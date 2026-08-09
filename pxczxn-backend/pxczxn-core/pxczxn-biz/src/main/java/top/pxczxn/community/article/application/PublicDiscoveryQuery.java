package top.pxczxn.community.article.application;

public record PublicDiscoveryQuery(
    String keyword,
    String tagSlug,
    String sort,
    Integer pageNum,
    Integer pageSize
) {}