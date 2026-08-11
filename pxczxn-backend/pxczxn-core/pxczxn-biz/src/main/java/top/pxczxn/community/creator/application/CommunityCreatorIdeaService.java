package top.pxczxn.community.creator.application;

import java.util.List;

public interface CommunityCreatorIdeaService {
    List<CommunityCreatorIdeaView> mine();
    CommunityCreatorIdeaView create(String title, String content, List<String> tags, String sourceType);
}
