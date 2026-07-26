package top.pxczxn.community.file.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("file_object")
public class FileObject {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String storageProvider;

    private String bucketName;

    private String objectKey;

    private String originalName;

    private String mimeType;

    private Long sizeBytes;

    private String sha256;

    private String status;

    private Long createdByUserId;

    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;
}
