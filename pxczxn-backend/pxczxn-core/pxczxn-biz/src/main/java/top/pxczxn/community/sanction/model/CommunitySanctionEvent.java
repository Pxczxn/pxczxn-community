package top.pxczxn.community.sanction.model;
import com.baomidou.mybatisplus.annotation.IdType; import com.baomidou.mybatisplus.annotation.TableId; import com.baomidou.mybatisplus.annotation.TableName; import lombok.Getter; import lombok.Setter; import java.time.LocalDateTime;
@Getter @Setter @TableName("community_sanction_event") public class CommunitySanctionEvent { @TableId(type = IdType.AUTO) private Long id; private Long sanctionId; private String actorType; private Long actorId; private String eventType; private String snapshot; private LocalDateTime occurredAt; }
