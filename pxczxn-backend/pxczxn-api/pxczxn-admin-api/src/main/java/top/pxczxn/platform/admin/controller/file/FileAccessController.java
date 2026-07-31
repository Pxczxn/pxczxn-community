package top.pxczxn.platform.admin.controller.file;

import top.pxczxn.platform.system.storage.FileStorageFactory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 文件访问控制器。
 *
 * <p>存储桶保持私有，文件内容统一由应用读取并返回；这样本地存储和
 * MinIO 存储使用相同的 URL 契约。</p>
 */
@Slf4j
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileAccessController {

    private final FileStorageFactory storageFactory;

    /**
     * 访问当前存储提供方中的文件
     */
    @GetMapping("/**")
    public ResponseEntity<byte[]> getFile(HttpServletRequest request) {
        // 获取文件路径
        // 注意：ApiPrefixConfig 给 @RestController 自动加了 /api 前缀
        // 所以实际请求URI是 /api/files/xxx，需要从 /api/files 之后截取
        String requestUri = request.getRequestURI();
        int filesIndex = requestUri.indexOf("/files/");
        String filePath = (filesIndex >= 0) ? requestUri.substring(filesIndex + "/files".length()) : requestUri;

        try {
            if (!storageFactory.getStorage().exists(filePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            byte[] bytes = storageFactory.getStorage().getFile(filePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(bytes);
        } catch (RuntimeException e) {
            log.error("读取文件失败: {}", filePath, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
