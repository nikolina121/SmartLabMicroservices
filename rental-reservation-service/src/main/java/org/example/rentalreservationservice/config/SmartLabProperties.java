package org.example.rentalreservationservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@Getter
@Setter
@ConfigurationProperties(prefix = "smartlab")
public class SmartLabProperties {

    private PenaltySettings penalty = new PenaltySettings();
    private LimitSettings limits = new LimitSettings();

    @Getter
    @Setter
    public static class PenaltySettings {
        private BigDecimal dailyRate = new BigDecimal("200.00");
        private int blockAfterDaysLate = 3;
    }

    @Getter
    @Setter
    public static class LimitSettings {
        private long studentActiveBorrows = 2;
        private long engineerActiveBorrows = 5;
        private int studentPickupHours = 2;
        private int engineerPickupHours = 24;
        private int engineerExtraDueDays = 3;
        private int reminderHours = 24;
    }
}
