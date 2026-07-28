package top.pxczxn.community.abuse.model;
import com.baomidou.mybatisplus.annotation.TableName; import lombok.Getter; import lombok.Setter; import java.time.LocalDateTime;
@Getter @Setter @TableName("community_abuse_window") public class CommunityAbuseWindow { private String actorKey; private String actionType; private LocalDateTime windowStartedAt; private Integer attemptCount; private Integer rejectedCount; private LocalDateTime updatedAt; }
