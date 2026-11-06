package com.locadora.veiculos.dto.response;

import com.locadora.veiculos.entity.Veiculo;
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
public class VeiculoResponseDTO {

    private Long id;
    private String modelo;
    private String placa;
    private String categoria;
    private BigDecimal valorDiaria;

    public static VeiculoResponseDTO fromEntity(Veiculo veiculo) {
        if (veiculo == null) return null;
        return VeiculoResponseDTO.builder()
                .id(veiculo.getId())
                .modelo(veiculo.getModelo())
                .placa(veiculo.getPlaca())
                .categoria(veiculo.getCategoria())
                .valorDiaria(veiculo.getValorDiaria())
                .build();
    }
}
