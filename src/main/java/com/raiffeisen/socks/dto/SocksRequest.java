package com.raiffeisen.socks.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SocksRequest {

    @NotBlank(message = "цвет не может быть пустым")
    @Size(max = 50, message = "цвет не может быть длиннее 50 символов")
    String color;

    @NotNull(message = "процент хлопка обязателен")
    @Min(value = 0, message = "процент хлопка не может быть меньше 0")
    @Max(value = 100, message = "процент хлопка не может быть больше 100")
    Integer cottonPart;

    @NotNull(message = "количество обязательно")
    @Positive(message = "количество должно быть больше 0")
    Long quantity;
}
