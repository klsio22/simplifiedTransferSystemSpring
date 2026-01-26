package com.simplifiedStransferSystemSpring.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.simplifiedStransferSystemSpring.domain.user.User;
import com.simplifiedStransferSystemSpring.dtos.NotificationDTO;

@Service
public class NotificationsService {

    @Autowired
    private RestTemplate restTemplate;

    public void sendNotification(User user, String message) throws RuntimeException {
        String email = user.getEmail();

        NotificationDTO notificationRequest = new NotificationDTO(email, message);

        ResponseEntity<String> notificationResponse = restTemplate.postForEntity(
                "https://util.devi.tools/api/v1/notify", notificationRequest, String.class);

        if (!(notificationResponse.getStatusCode() == HttpStatus.OK)) {
            System.out.println("Service failed to send notification");
            throw new RuntimeException("Failed to send notification");
        }

    }

}
