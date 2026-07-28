package top.pxczxn.community.search.application;

public record UnifiedSearchQuery(String keyword, String type, Integer pageNum, Integer pageSize) {
}
