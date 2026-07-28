package top.pxczxn.community.editorial.application;
import java.time.LocalDateTime; import java.util.List;
public record EditorialCollectionCommand(String kind,String title,String slug,String summary,Long coverFileId,String status,LocalDateTime startsAt,LocalDateTime endsAt,Integer displayOrder,List<EditorialItemCommand> items) {}
