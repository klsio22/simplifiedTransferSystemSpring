package com.simplifiedTransferSystemSpring.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
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
                        "https://util.devi.tools/api/v1/notify",
                        notificationRequest,
                        String.class);

                if (notificationResponse != null && notificationResponse.getStatusCode().is2xxSuccessful()) {
                    logger.info("Notification sent successfully to {} on attempt {}", email, attempts);
                    return true;
                }

                if (notificationResponse != null) {
                    logger.warn("Notification attempt {} returned non-success status {} for {}",
                            attempts, notificationResponse.getStatusCode(), email);
                } else {
                    logger.warn("Notification attempt {} returned null response for {}",
                            attempts, email);
                }

            } catch (HttpServerErrorException e) {
                logger.warn("Server error on notification attempt {} for {}: {} - {}",
                        attempts, email, e.getStatusCode(), e.getResponseBodyAsString());

                if (attempts < maxAttempts) {
                    waitBeforeRetry(attempts);
                }

            } catch (RestClientException e) {
                logger.warn("Network error on notification attempt {} for {}: {}",
                        attempts, email, e.getMessage());

                if (attempts < maxAttempts) {
                    waitBeforeRetry(attempts);
                }
            }
        }

        logger.error("All {} notification attempts failed for {}", maxAttempts, email);
        return false;
    }

    private void waitBeforeRetry(int attemptNumber) {
        try {
            long backoffMillis = 100L * attemptNumber;
            Thread.sleep(backoffMillis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            logger.warn("Notification retry wait interrupted");
        }
    }
}