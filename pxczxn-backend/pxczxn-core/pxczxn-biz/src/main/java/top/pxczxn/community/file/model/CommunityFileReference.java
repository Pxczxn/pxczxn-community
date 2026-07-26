package top.pxczxn.community.file.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("community_file_reference")
public class CommunityFileReference {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long fileId;

    private Long ownerUserId;

    private String targetType;

    private Long targetId;

    private String usageType;

    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;
}
