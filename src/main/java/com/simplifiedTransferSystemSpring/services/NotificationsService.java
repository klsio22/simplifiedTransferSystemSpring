package com.simplifiedTransferSystemSpring.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.simplifiedTransferSystemSpring.domain.user.User;
import com.simplifiedTransferSystemSpring.dtos.NotificationDTO;

@Service
public class NotificationsService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationsService.class);

    @Autowired
    private RestTemplate restTemplate;

    public boolean sendNotification(User user, String message) {
        String email = user.getEmail();

        NotificationDTO notificationRequest = new NotificationDTO(email, message);

        int attempts = 0;
        int maxAttempts = 3;
        while (attempts < maxAttempts) {
            attempts++;
            try {
                ResponseEntity<String> notificationResponse = restTemplate.postForEntity(
                        "https://util.devi.tools/api/v1/notify", notificationRequest, String.class);

                if (notificationResponse != null) {
                    var status = notificationResponse.getStatusCode();
                    if (status.is2xxSuccessful()) {
                        logger.info("Notification sent successfully to {} (attempt {})", email, attempts);
                        return true;
                    } else {
                        logger.warn("Notification attempt {} returned non-success status {} for {}", attempts, status, email);
                    }
                } else {
                    logger.warn("Notification attempt {} returned null response for {}", attempts, email);
                }
            } catch (Exception e) {
                logger.warn("Failed to send notification to {} (attempt {}): {}", email, attempts, e.getMessage());
            }

            try {
                Thread.sleep(200L * attempts);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                logger.warn("Notification retry interrupted");
                break;
            }
        }

        logger.error("All notification attempts failed for {}", email);
        return false;
    }

} 
