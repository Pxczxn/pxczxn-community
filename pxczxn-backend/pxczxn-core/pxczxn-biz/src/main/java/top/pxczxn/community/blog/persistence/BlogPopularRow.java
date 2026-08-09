package top.pxczxn.community.blog.persistence;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlogPopularRow {
    private Long id;
    private String blogType;
    private String name;
    private String slug;
    private String summary;
    private Long avatarFileId;
    private Long backgroundFileId;
    private Long followerCount;
    private Long articleCount;
    private String ownerName;
    private Long ownerUserId;
}