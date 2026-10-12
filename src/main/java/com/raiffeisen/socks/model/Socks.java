package com.raiffeisen.socks.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Entity
@Table(
        name = "socks",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_socks_color_cotton",
                columnNames = {"color", "cotton_part"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Socks {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "color", nullable = false, length = 50)
    String color;

    @Column(name = "cotton_part", nullable = false)
    Integer cottonPart;

    @Column(name = "quantity", nullable = false)
    Long quantity;
}
