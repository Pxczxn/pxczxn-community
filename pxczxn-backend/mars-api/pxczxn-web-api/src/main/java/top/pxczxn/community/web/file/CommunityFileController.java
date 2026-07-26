package top.pxczxn.community.web.file;

import com.mars.common.exception.BusinessException;
import com.mars.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import top.pxczxn.community.file.application.CommunityFileContent;
import top.pxczxn.community.file.application.CommunityFileService;
import top.pxczxn.community.file.application.UploadCommunityFileCommand;

import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
public class CommunityFileController {

    private final CommunityFileService fileService;

    @PostMapping
    public Result<CommunityFileResponse> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }
        if (file.getSize() > CommunityFileService.MAX_UPLOAD_SIZE_BYTES) {
            throw new BusinessException(400, "单个文件不能超过 20MB");
        }
        try {
            return Result.ok(CommunityFileResponse.from(fileService.upload(
                    new UploadCommunityFileCommand(
                            file.getOriginalFilename(),
                            file.getContentType(),
                            file.getBytes()
                    )
            )));
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(500, "读取上传文件失败");
        }
    }

    @GetMapping("/{fileId}")
    public Result<CommunityFileResponse> detail(@PathVariable Long fileId) {
        return Result.ok(CommunityFileResponse.from(fileService.getMine(fileId)));
    }

    @GetMapping("/{fileId}/content")
    public ResponseEntity<byte[]> content(@PathVariable Long fileId) {
        return response(fileService.readMine(fileId));
    }

    @DeleteMapping("/{fileId}")
    public Result<Void> delete(@PathVariable Long fileId) {
        fileService.deleteMine(fileId);
        return Result.ok();
    }

    static ResponseEntity<byte[]> response(CommunityFileContent file) {
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(file.originalName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(file.mimeType()))
                .contentLength(file.content().length)
                .body(file.content());
    }
}
