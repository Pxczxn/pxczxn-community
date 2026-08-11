package top.pxczxn.community.user.application;

import java.util.Map;

public interface CommunityPreferenceService {

    CommunityPreferenceSettings mine();

    CommunityPreferenceSettings update(Map<String, Object> settings);
}
