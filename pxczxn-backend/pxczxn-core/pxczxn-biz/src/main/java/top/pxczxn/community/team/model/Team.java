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

    /** 团队分类。 */
    private String category;

    /** 内容方向。 */
    private String contentDirection;

    /** 主页主题。 */
    private String theme;

    /** SEO 标题。 */
    private String seoTitle;

    /** SEO 描述。 */
    private String seoDescription;

    /** 是否公开成员列表 1=公开 0=隐藏。 */
    private Boolean publicMembers;

    /** 是否开放外部投稿 1=开放 0=仅成员。 */
    private Boolean allowSubmissions;

    /** 投稿说明。 */
    private String submissionGuideline;

    /** 联系方式。 */
    private String contactInfo;

    private String status;

    private Integer lockVersion;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
