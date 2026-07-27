package top.pxczxn.community.series.application;

public record CreateTeamSeriesCommand(Long teamId, String title, String slug, String summary, Long coverFileId,
                                      String serializationStatus) { }
