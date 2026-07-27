package top.pxczxn.community.series.application;

public record UpdateTeamSeriesCommand(String title, String slug, String summary, Long coverFileId,
                                      String serializationStatus, Integer expectedLockVersion) { }
