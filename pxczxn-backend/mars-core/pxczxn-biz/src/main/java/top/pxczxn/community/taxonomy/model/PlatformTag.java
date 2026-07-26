package top.pxczxn.community.taxonomy.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("platform_tag")
public class PlatformTag {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String name;

    private String slug;

    private String description;

    private String status;

    private Long mergedToTagId;

    private Long usageCount;

    private Long createdByAdminId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
