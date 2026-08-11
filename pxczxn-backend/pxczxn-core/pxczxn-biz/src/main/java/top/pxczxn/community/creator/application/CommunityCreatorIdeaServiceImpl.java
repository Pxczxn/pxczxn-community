package top.pxczxn.community.creator.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.creator.model.CommunityCreatorIdea;
import top.pxczxn.community.creator.persistence.CommunityCreatorIdeaMapper;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.platform.common.exception.BusinessException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunityCreatorIdeaServiceImpl implements CommunityCreatorIdeaService {
    private final CommunityCreatorIdeaMapper mapper;
    private final CommunityAuth auth;

    @Override
    @Transactional(readOnly = true)
    public List<CommunityCreatorIdeaView> mine() {
        Long actor = auth.getLoginUserId();
        return mapper.selectList(Wrappers.<CommunityCreatorIdea>query()
                        .eq("owner_user_id", actor)
                        .isNull("deleted_at")
                        .orderByDesc("created_at"))
                .stream().map(CommunityCreatorIdeaServiceImpl::view).toList();
    }

    @Override
    @Transactional
    public CommunityCreatorIdeaView create(String title, String content, List<String> tags, String sourceType) {
        String normalizedTitle = required(title, 160, "Idea title");
        String normalizedContent = required(content, 4000, "Idea content");
        List<String> normalizedTags = tags == null ? List.of() : tags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(String::strip).distinct().toList();
        if (normalizedTags.size() > 10 || normalizedTags.stream().anyMatch(tag -> tag.length() > 32 || tag.contains(","))) {
            throw new BusinessException(400, "Idea tags are invalid");
        }
        String normalizedSource = sourceType == null || sourceType.isBlank() ? "MANUAL" : sourceType.strip().toUpperCase();
        if (!List.of("MANUAL", "ARTICLE", "MOMENT").contains(normalizedSource)) {
            throw new BusinessException(400, "Idea source type is invalid");
        }
        CommunityCreatorIdea idea = new CommunityCreatorIdea();
        idea.setId(IdWorker.getId());
        idea.setOwnerUserId(auth.getLoginUserId());
        idea.setTitle(normalizedTitle);
        idea.setContent(normalizedContent);
        idea.setTagsText(String.join(",", normalizedTags));
        idea.setSourceType(normalizedSource);
        idea.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        if (mapper.insert(idea) != 1) throw new BusinessException(500, "Unable to create idea");
        return view(idea);
    }

    private static String required(String value, int maxLength, String label) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isEmpty() || normalized.length() > maxLength) throw new BusinessException(400, label + " is invalid");
        return normalized;
    }

    private static CommunityCreatorIdeaView view(CommunityCreatorIdea idea) {
        List<String> tags = idea.getTagsText() == null || idea.getTagsText().isBlank() ? List.of() : List.of(idea.getTagsText().split(","));
        return new CommunityCreatorIdeaView(idea.getId(), idea.getTitle(), idea.getContent(), tags, idea.getSourceType(), idea.getCreatedAt());
    }
}
