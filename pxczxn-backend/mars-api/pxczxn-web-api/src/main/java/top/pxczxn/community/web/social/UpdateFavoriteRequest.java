package top.pxczxn.community.web.social;

import java.util.List;

public record UpdateFavoriteRequest(
        List<String> folderIds
) {
}
