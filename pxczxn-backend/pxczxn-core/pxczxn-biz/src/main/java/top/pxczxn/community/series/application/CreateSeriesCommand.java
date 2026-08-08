package top.pxczxn.community.series.application;

public record CreateSeriesCommand(Long blogId, String title, String slug, String summary, Long coverFileId,
                                  String serializationStatus) { }
