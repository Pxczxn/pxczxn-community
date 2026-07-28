package top.pxczxn.community.editorial.model;
import com.baomidou.mybatisplus.annotation.*; import lombok.Getter; import lombok.Setter; import java.time.LocalDateTime;
@Getter @Setter @TableName("editorial_collection_item") public class EditorialCollectionItem { @TableId(type=IdType.INPUT) private Long id; private Long collectionId; private String targetType; private Long targetId; private Integer displayOrder; private LocalDateTime createdAt; }
