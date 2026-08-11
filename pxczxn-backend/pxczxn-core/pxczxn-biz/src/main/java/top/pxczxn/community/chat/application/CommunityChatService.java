package top.pxczxn.community.chat.application;

import java.util.List;

public interface CommunityChatService {

    CommunityChatMessageView send(Long actor, Long recipient, String content);

    List<CommunityChatMessageView> history(Long actor, Long peer, Integer limit);

    List<CommunityChatConversationView> conversations(Long actor, Integer limit);

    void markRead(Long actor, Long peer);
}
