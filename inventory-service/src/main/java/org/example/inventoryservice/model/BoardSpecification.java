package org.example.inventoryservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "board_specifications")
public class BoardSpecification {

    @Id
    @Column(name = "component_id")
    private Long componentId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "component_id")
    private Component component;

    @NotBlank(message = "cpuArchitecture je obavezan")
    @Column(name = "cpu_architecture", nullable = false, length = 50)
    private String cpuArchitecture;

    @NotNull(message = "clockSpeedMhz je obavezan")
    @Column(name = "clock_speed_mhz", nullable = false)
    private Integer clockSpeedMhz;

    @NotNull(message = "flashMemoryKb je obavezan")
    @Column(name = "flash_memory_kb", nullable = false)
    private Integer flashMemoryKb;

    @NotNull(message = "sramKb je obavezan")
    @Column(name = "sram_kb", nullable = false)
    private Integer sramKb;

    @NotNull(message = "operatingVoltage je obavezan")
    @Column(name = "operating_voltage", nullable = false, precision = 3, scale = 1)
    private BigDecimal operatingVoltage;

    @Column(name = "digital_pins_count", nullable = false)
    private Integer digitalPinsCount = 0;

    @Column(name = "analog_pins_count", nullable = false)
    private Integer analogPinsCount = 0;

    @Column(name = "timer_count", nullable = false)
    private Integer timerCount = 0;

    @Column(name = "pwm_channels_count", nullable = false)
    private Integer pwmChannelsCount = 0;

    @Column(name = "spi_count", nullable = false)
    private Integer spiCount = 0;

    @Column(name = "i2c_count", nullable = false)
    private Integer i2cCount = 0;

    @Column(name = "uart_count", nullable = false)
    private Integer uartCount = 0;

    @Column(name = "has_wifi", nullable = false)
    private Boolean hasWifi = false;

    @Column(name = "has_bluetooth", nullable = false)
    private Boolean hasBluetooth = false;
}
