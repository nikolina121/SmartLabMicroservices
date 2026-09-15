package org.example.inventoryservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "components")
public class Component {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "modelCode je obavezan.")
    @Size(max = 100, message = "modelCode ne sme biti duži od 100 karaktera.")
    @Column(name = "model_code", nullable = false, unique = true, length = 100)
    private String modelCode;

    @NotBlank(message = "manufacturer je obavezan.")
    @Size(max = 100, message = "manufacturer ne sme biti duži od 100 karaktera.")
    @Column(nullable = false, length = 100)
    private String manufacturer;

    @NotBlank(message = "componentType je obavezan.")
    @Size(max = 30, message = "componentType ne sme biti duži od 30 karaktera.")
    @Column(name = "component_type", nullable = false, length = 30)
    private String componentType;

    @JsonIgnore
    @OneToOne(mappedBy = "component", cascade = CascadeType.ALL)
    private BoardSpecification specification;
}

