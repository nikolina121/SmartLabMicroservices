package org.example.rentalreservationservice;

import org.example.rentalreservationservice.config.SmartLabProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
@EnableConfigurationProperties(SmartLabProperties.class)
public class RentalReservationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RentalReservationServiceApplication.class, args);
    }
}
