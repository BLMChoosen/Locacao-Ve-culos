package com.locadora.veiculos.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DevolucaoRequestDTO {

    /**
     * Data real em que o veículo foi devolvido.
     * Se não informada, a data atual do sistema será utilizada.
     */
    private LocalDate dataRealDevolucao;
}
