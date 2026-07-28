package top.pxczxn.community.article.application;

public record PublicDiscoveryPageView(PublicArticlePageView page, PublicDiscoverySort sort) {
    public String sortExplanation() { return sort.explanation(); }
}
