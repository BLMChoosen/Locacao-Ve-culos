package com.locadora.veiculos.dto.response;

import com.locadora.veiculos.entity.Locacao;
import com.locadora.veiculos.entity.enums.StatusLocacao;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocacaoResponseDTO {

    private Long id;
    private VeiculoResponseDTO veiculo;
    private ClienteResponseDTO cliente;
    private LocalDate dataInicio;
    private LocalDate dataPrevistaDevolucao;
    private LocalDate dataRealDevolucao;
    private BigDecimal valorTotal;
    private StatusLocacao status;
    private Long diasAtraso;
    private BigDecimal valorMulta;

    public static LocacaoResponseDTO fromEntity(Locacao locacao) {
        if (locacao == null) return null;

        Long diasAtraso = null;
        BigDecimal valorMulta = null;

        if (locacao.getDataRealDevolucao() != null &&
                locacao.getDataRealDevolucao().isAfter(locacao.getDataPrevistaDevolucao())) {
            diasAtraso = ChronoUnit.DAYS.between(locacao.getDataPrevistaDevolucao(), locacao.getDataRealDevolucao());
            if (diasAtraso > 0 && locacao.getVeiculo() != null) {
                // Multa de 20% sobre a diaria por dia de atraso
                valorMulta = locacao.getVeiculo().getValorDiaria()
                        .multiply(BigDecimal.valueOf(0.20))
                        .multiply(BigDecimal.valueOf(diasAtraso));
            }
        }

        return LocacaoResponseDTO.builder()
                .id(locacao.getId())
                .veiculo(VeiculoResponseDTO.fromEntity(locacao.getVeiculo()))
                .cliente(ClienteResponseDTO.fromEntity(locacao.getCliente()))
                .dataInicio(locacao.getDataInicio())
                .dataPrevistaDevolucao(locacao.getDataPrevistaDevolucao())
                .dataRealDevolucao(locacao.getDataRealDevolucao())
                .valorTotal(locacao.getValorTotal())
                .status(locacao.getStatus())
                .diasAtraso(diasAtraso)
                .valorMulta(valorMulta)
                .build();
    }
}
