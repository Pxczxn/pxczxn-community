package top.pxczxn.community.web.social;

import com.mars.common.exception.BusinessException;
import com.mars.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.social.application.CreateFavoriteFolderCommand;
import top.pxczxn.community.social.application.FavoriteFolderService;
import top.pxczxn.community.social.application.FavoriteService;
import top.pxczxn.community.social.application.UpdateFavoriteFolderCommand;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class FavoriteController {

    private final FavoriteFolderService folderService;
    private final FavoriteService favoriteService;

    @GetMapping("/social/me/favorite-folders")
    public Result<List<FavoriteFolderResponse>> myFolders() {
        return Result.ok(folderService.mine().stream()
                .map(FavoriteFolderResponse::from)
                .toList());
    }

    @PostMapping("/social/me/favorite-folders")
    public Result<FavoriteFolderResponse> createFolder(
            @RequestBody CreateFavoriteFolderRequest request
    ) {
        return Result.ok(FavoriteFolderResponse.from(
                folderService.create(request == null
                        ? null
                        : new CreateFavoriteFolderCommand(
                                request.name(),
                                request.description(),
                                request.visibility(),
                                request.sortOrder()
                        ))
        ));
    }

    @PatchMapping("/social/me/favorite-folders/{folderId}")
    public Result<FavoriteFolderResponse> updateFolder(
            @PathVariable Long folderId,
            @RequestBody UpdateFavoriteFolderRequest request
    ) {
        return Result.ok(FavoriteFolderResponse.from(
                folderService.update(
                        folderId,
                        request == null
                                ? null
                                : new UpdateFavoriteFolderCommand(
                                        request.name(),
                                        request.description(),
                                        request.clearDescription(),
                                        request.visibility(),
                                        request.sortOrder()
                                )
                )
        ));
    }

    @DeleteMapping("/social/me/favorite-folders/{folderId}")
    public Result<Void> deleteFolder(@PathVariable Long folderId) {
        folderService.delete(folderId);
        return Result.ok();
    }

    @GetMapping("/users/{userId}/favorite-folders")
    public Result<List<FavoriteFolderResponse>> userFolders(
            @PathVariable Long userId
    ) {
        return Result.ok(folderService.visibleFolders(userId).stream()
                .map(FavoriteFolderResponse::from)
                .toList());
    }

    @GetMapping("/favorite-folders/{folderId}/items")
    public Result<FavoriteContentPageResponse> folderItems(
            @PathVariable Long folderId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return Result.ok(FavoriteContentPageResponse.from(
                favoriteService.folderItems(folderId, pageNum, pageSize)
        ));
    }

    @PostMapping("/interactions/{targetType}/{targetId}/favorite")
    public Result<FavoriteRelationshipResponse> favorite(
            @PathVariable String targetType,
            @PathVariable Long targetId,
            @RequestBody(required = false) UpdateFavoriteRequest request
    ) {
        return Result.ok(FavoriteRelationshipResponse.from(
                favoriteService.favorite(
                        targetType, targetId, folderIds(request)
                )
        ));
    }

    @PutMapping("/interactions/{targetType}/{targetId}/favorite")
    public Result<FavoriteRelationshipResponse> updateFavoriteFolders(
            @PathVariable String targetType,
            @PathVariable Long targetId,
            @RequestBody UpdateFavoriteRequest request
    ) {
        return Result.ok(FavoriteRelationshipResponse.from(
                favoriteService.updateFolders(
                        targetType, targetId, folderIds(request)
                )
        ));
    }

    @DeleteMapping("/interactions/{targetType}/{targetId}/favorite")
    public Result<FavoriteRelationshipResponse> unfavorite(
            @PathVariable String targetType,
            @PathVariable Long targetId
    ) {
        return Result.ok(FavoriteRelationshipResponse.from(
                favoriteService.unfavorite(targetType, targetId)
        ));
    }

    @GetMapping("/interactions/{targetType}/{targetId}/favorite")
    public Result<FavoriteRelationshipResponse> relationship(
            @PathVariable String targetType,
            @PathVariable Long targetId
    ) {
        return Result.ok(FavoriteRelationshipResponse.from(
                favoriteService.relationship(targetType, targetId)
        ));
    }

    @GetMapping("/interactions/{targetType}/{targetId}/favoritors")
    public Result<ContentFavoritorPageResponse> favoritors(
            @PathVariable String targetType,
            @PathVariable Long targetId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return Result.ok(ContentFavoritorPageResponse.from(
                favoriteService.favoritors(
                        targetType, targetId, pageNum, pageSize
                )
        ));
    }

    private static List<Long> folderIds(UpdateFavoriteRequest request) {
        if (request == null || request.folderIds() == null) {
            return List.of();
        }
        return request.folderIds().stream()
                .map(FavoriteController::folderId)
                .toList();
    }

    private static Long folderId(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(400, "收藏夹 ID 无效");
        }
        try {
            long parsed = Long.parseUnsignedLong(value.strip());
            if (parsed <= 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new BusinessException(400, "收藏夹 ID 无效");
        }
    }
}
