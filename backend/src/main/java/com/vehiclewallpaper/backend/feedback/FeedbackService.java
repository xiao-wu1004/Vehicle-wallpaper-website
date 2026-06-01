package com.vehiclewallpaper.backend.feedback;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    @PostConstruct
    @Transactional
    public void seedHighlights() {
        if (feedbackRepository.existsByFeaturedTrue()) {
            return;
        }

        feedbackRepository.save(buildFeatured("用户A", "user.a@example.com",
            "在这里找到的壁纸质量非常高，让我每次打开电脑都很愉快！"));
        feedbackRepository.save(buildFeatured("用户B", "user.b@example.com",
            "我很喜欢这个网站的设计风格，壁纸种类丰富，下载体验也很流畅。"));
    }

    @Transactional
    public FeedbackSubmissionResponse submit(FeedbackSubmissionRequest request, String userAgent) {
        FeedbackMessage message = new FeedbackMessage();
        message.setName(trimToLength(request.getName(), 80));
        message.setEmail(trimToLength(request.getEmail(), 160).toLowerCase(Locale.ROOT));
        message.setMessage(request.getMessage().trim());
        message.setSourcePage(normalizePage(request.getPage()));
        message.setUserAgent(trimToLength(userAgent == null ? "unknown" : userAgent, 512));
        message.setStatus(FeedbackStatus.PENDING);
        message.setFeatured(false);

        FeedbackMessage saved = feedbackRepository.save(message);
        return new FeedbackSubmissionResponse(
            saved.getId(),
            saved.getStatus().name(),
            "反馈已收到，我们会尽快查看。",
            saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<FeedbackHighlightResponse> getHighlights() {
        List<FeedbackMessage> messages = feedbackRepository.findTop6ByStatusAndFeaturedTrueOrderByCreatedAtDesc(FeedbackStatus.APPROVED);
        List<FeedbackHighlightResponse> responses = new ArrayList<FeedbackHighlightResponse>();
        for (FeedbackMessage message : messages) {
            responses.add(new FeedbackHighlightResponse(
                message.getId(),
                message.getName(),
                message.getMessage(),
                message.getCreatedAt()
            ));
        }
        return responses;
    }

    private FeedbackMessage buildFeatured(String name, String email, String messageText) {
        FeedbackMessage message = new FeedbackMessage();
        message.setName(name);
        message.setEmail(email);
        message.setMessage(messageText);
        message.setSourcePage("/seed");
        message.setUserAgent("system-seed");
        message.setStatus(FeedbackStatus.APPROVED);
        message.setFeatured(true);
        return message;
    }

    private String normalizePage(String page) {
        if (page == null || page.trim().isEmpty()) {
            return "/unknown";
        }
        return trimToLength(page.trim(), 255);
    }

    private String trimToLength(String value, int maxLength) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
