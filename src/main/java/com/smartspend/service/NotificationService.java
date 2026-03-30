package com.smartspend.service;

import com.smartspend.dto.DTOs.*;
import com.smartspend.entity.User;
import com.smartspend.exception.AppException;
import com.smartspend.repository.NotificationRepository;
import com.smartspend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ApiResponse<PageResponse<NotificationResponse>> getNotifications(String email, int page, int size) {
        User user = getUser(email);
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<com.smartspend.entity.Notification> notifPage =
            notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pr);

        return ApiResponse.success(PageResponse.<NotificationResponse>builder()
            .content(notifPage.getContent().stream().map(n -> NotificationResponse.builder()
                .id(n.getId()).title(n.getTitle()).message(n.getMessage())
                .type(n.getType()).read(n.isRead()).createdAt(n.getCreatedAt()).build())
                .collect(Collectors.toList()))
            .page(notifPage.getNumber()).size(notifPage.getSize())
            .totalElements(notifPage.getTotalElements())
            .totalPages(notifPage.getTotalPages()).last(notifPage.isLast())
            .build());
    }

    @Transactional(readOnly = true)
    public ApiResponse<Long> getUnreadCount(String email) {
        User user = getUser(email);
        return ApiResponse.success(notificationRepository.countByUserIdAndIsReadFalse(user.getId()));
    }

    public ApiResponse<String> markAllRead(String email) {
        User user = getUser(email);
        notificationRepository.markAllReadByUserId(user.getId());
        return ApiResponse.success("All notifications marked as read", null);
    }

    public ApiResponse<String> markRead(String email, Long id) {
        User user = getUser(email);
        notificationRepository.findById(id).ifPresent(n -> {
            if (n.getUser().getId().equals(user.getId())) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
        return ApiResponse.success("Notification marked as read", null);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new AppException("User not found", 404));
    }
}
