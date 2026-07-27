package top.pxczxn.community.team.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("team")
public class Team {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long blogId;

    private Long ownerUserId;

    private String status;

    private Integer lockVersion;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
