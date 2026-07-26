package top.pxczxn.community.social.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("favorite_folder")
public class FavoriteFolder {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long ownerUserId;

    private String name;

    private String description;

    private String visibility;

    private Integer isDefault;

    private Long itemCount;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @TableField(exist = false)
    private String activeName;

    @TableField(exist = false)
    private Long defaultOwnerId;
}
