package top.pxczxn.community.moderation.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("content_keyword_rule")
public class ContentKeywordRule {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String keyword;

    private String normalizedKeyword;

    private String severity;

    private String status;

    private String description;

    private Integer sortOrder;

    private Long createdByAdminId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
