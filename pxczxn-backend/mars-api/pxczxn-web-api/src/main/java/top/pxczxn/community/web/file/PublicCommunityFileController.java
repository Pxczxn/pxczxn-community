package top.pxczxn.community.web.file;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.file.application.CommunityFileService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/files")
public class PublicCommunityFileController {

    private final CommunityFileService fileService;

    @GetMapping("/{fileId}/content")
    public ResponseEntity<byte[]> content(@PathVariable Long fileId) {
        return CommunityFileController.response(fileService.readPublic(fileId));
    }
}
