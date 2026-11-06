package com.locadora.veiculos.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VeiculoRequestDTO {

    @NotBlank(message = "O modelo do veículo é obrigatório")
    @Size(min = 2, max = 100, message = "O modelo deve ter entre 2 e 100 caracteres")
    private String modelo;

    @NotBlank(message = "A placa é obrigatória")
    @Size(min = 7, max = 10, message = "A placa deve ter entre 7 e 10 caracteres")
    private String placa;

    @NotBlank(message = "A categoria é obrigatória")
    private String categoria;

    @NotNull(message = "O valor da diária é obrigatório")
    @DecimalMin(value = "0.01", message = "O valor da diária deve ser maior que zero")
    private BigDecimal valorDiaria;
}
