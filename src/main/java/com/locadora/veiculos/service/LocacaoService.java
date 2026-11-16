package com.locadora.veiculos.service;

import com.locadora.veiculos.dto.request.DevolucaoRequestDTO;
import com.locadora.veiculos.dto.request.LocacaoRequestDTO;
import com.locadora.veiculos.dto.response.LocacaoResponseDTO;
import com.locadora.veiculos.entity.Cliente;
import com.locadora.veiculos.entity.Locacao;
import com.locadora.veiculos.entity.Veiculo;
import com.locadora.veiculos.entity.enums.StatusLocacao;
import com.locadora.veiculos.exception.RegraNegocioException;
import com.locadora.veiculos.exception.ResourceNotFoundException;
import com.locadora.veiculos.repository.ClienteRepository;
import com.locadora.veiculos.repository.LocacaoRepository;
import com.locadora.veiculos.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocacaoService {

    private final LocacaoRepository locacaoRepository;
    private final VeiculoRepository veiculoRepository;
    private final ClienteRepository clienteRepository;

    /**
     * Registra uma nova locação aplicando as regras de negócio:
     * - RN1: Veículo deve estar disponível (sem locação ATIVA)
     * - RN2: Cliente não pode exceder 2 locações ativas e não pode estar bloqueado
     * - RN3: Cálculo dinâmico do valor total baseado na diária e no período
     */
    @Transactional
    public LocacaoResponseDTO criarLocacao(LocacaoRequestDTO dto) {
        // Validação de datas
        if (dto.getDataPrevistaDevolucao().isBefore(dto.getDataInicio())) {
            throw new RegraNegocioException("A data prevista de devolução não pode ser anterior à data de início.");
        }

        Veiculo veiculo = veiculoRepository.findById(dto.getVeiculoId())
                .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado com ID: " + dto.getVeiculoId()));

        Cliente cliente = clienteRepository.findById(dto.getClienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + dto.getClienteId()));

        // Validação de Bloqueio do Cliente
        if (cliente.getBloqueadoAte() != null && cliente.getBloqueadoAte().isAfter(LocalDate.now())) {
            throw new RegraNegocioException(
                    "O cliente '" + cliente.getNome() + "' está temporariamente bloqueado para novas locações até " +
                    cliente.getBloqueadoAte() + " devido a pendências/atrasos anteriores."
            );
        }

        // RN1 (Disponibilidade): Um Veículo só pode ser alugado se não tiver nenhuma Locação com status ATIVA
        boolean veiculoOcupado = locacaoRepository.existsByVeiculoIdAndStatus(veiculo.getId(), StatusLocacao.ATIVA);
        if (veiculoOcupado) {
            throw new RegraNegocioException(
                    "O veículo '" + veiculo.getModelo() + "' (Placa: " + veiculo.getPlaca() +
                    ") já possui uma locação ativa em andamento e não está disponível."
            );
        }

        // RN2 (Limites): Um Cliente pode ter no máximo 2 locações ativas simultaneamente
        long locacoesAtivas = locacaoRepository.countByClienteIdAndStatus(cliente.getId(), StatusLocacao.ATIVA);
        if (locacoesAtivas >= 2) {
            throw new RegraNegocioException(
                    "O cliente '" + cliente.getNome() + "' atingiu o limite máximo permitido de 2 locações ativas simultâneas."
            );
        }

        // RN3 (Cálculo Automático): O valorTotal inicial é calculado no backend multiplicando
        // a quantidade de diárias pelo valorDiaria do veículo. Se mesmo dia, cobra no mínimo 1 diária.
        long dias = ChronoUnit.DAYS.between(dto.getDataInicio(), dto.getDataPrevistaDevolucao());
        long diariasCalculadas = dias == 0 ? 1 : dias;
        BigDecimal valorTotalInicial = veiculo.getValorDiaria().multiply(BigDecimal.valueOf(diariasCalculadas));

        Locacao locacao = Locacao.builder()
                .veiculo(veiculo)
                .cliente(cliente)
                .dataInicio(dto.getDataInicio())
                .dataPrevistaDevolucao(dto.getDataPrevistaDevolucao())
                .dataRealDevolucao(null)
                .valorTotal(valorTotalInicial.setScale(2, RoundingMode.HALF_UP))
                .status(StatusLocacao.ATIVA)
                .build();

        Locacao salva = locacaoRepository.save(locacao);
        return LocacaoResponseDTO.fromEntity(salva);
    }

    /**
     * RN4 (Penalidades/Condicionais Complexas):
     * Ao registrar a devolução, se a dataRealDevolucao for posterior à dataPrevistaDevolucao,
     * aplica multa de 20% sobre a diária por dia de atraso, cobra as diárias excedentes e
     * bloqueia o cliente por 15 dias (bloqueadoAte = hoje + 15 dias).
     */
    @Transactional
    public LocacaoResponseDTO registrarDevolucao(Long id, DevolucaoRequestDTO dto) {
        Locacao locacao = buscarEntidadePorId(id);

        if (locacao.getStatus() != StatusLocacao.ATIVA) {
            throw new RegraNegocioException(
                    "A locação ID " + id + " não pode ser devolvida pois seu status atual é: " + locacao.getStatus()
            );
        }

        LocalDate dataDevolucao = (dto != null && dto.getDataRealDevolucao() != null)
                ? dto.getDataRealDevolucao()
                : LocalDate.now();

        if (dataDevolucao.isBefore(locacao.getDataInicio())) {
            throw new RegraNegocioException("A data de devolução não pode ser anterior à data de início da locação.");
        }

        locacao.setDataRealDevolucao(dataDevolucao);
        locacao.setStatus(StatusLocacao.FINALIZADA);

        // Verifica atraso
        if (dataDevolucao.isAfter(locacao.getDataPrevistaDevolucao())) {
            long diasAtraso = ChronoUnit.DAYS.between(locacao.getDataPrevistaDevolucao(), dataDevolucao);
            BigDecimal valorDiaria = locacao.getVeiculo().getValorDiaria();

            // Cobrança das diárias extras de atraso
            BigDecimal valorDiariasExtras = valorDiaria.multiply(BigDecimal.valueOf(diasAtraso));

            // Multa de 20% da diária por cada dia de atraso
            BigDecimal valorMultaAtraso = valorDiaria
                    .multiply(new BigDecimal("0.20"))
                    .multiply(BigDecimal.valueOf(diasAtraso));

            BigDecimal novoValorTotal = locacao.getValorTotal()
                    .add(valorDiariasExtras)
                    .add(valorMultaAtraso)
                    .setScale(2, RoundingMode.HALF_UP);

            locacao.setValorTotal(novoValorTotal);

            // Penalidade ao cliente: bloqueio por 15 dias a partir de hoje
            Cliente cliente = locacao.getCliente();
            cliente.setBloqueadoAte(LocalDate.now().plusDays(15));
            clienteRepository.save(cliente);
        }

        Locacao atualizada = locacaoRepository.save(locacao);
        return LocacaoResponseDTO.fromEntity(atualizada);
    }

    /**
     * Cancela uma locação ativa.
     */
    @Transactional
    public LocacaoResponseDTO cancelarLocacao(Long id) {
        Locacao locacao = buscarEntidadePorId(id);

        if (locacao.getStatus() != StatusLocacao.ATIVA) {
            throw new RegraNegocioException(
                    "Apenas locações ativas podem ser canceladas. Status atual: " + locacao.getStatus()
            );
        }

        locacao.setStatus(StatusLocacao.CANCELADA);
        Locacao cancelada = locacaoRepository.save(locacao);
        return LocacaoResponseDTO.fromEntity(cancelada);
    }

    @Transactional(readOnly = true)
    public List<LocacaoResponseDTO> listarTodas() {
        return locacaoRepository.findAll().stream()
                .map(LocacaoResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LocacaoResponseDTO buscarPorId(Long id) {
        Locacao locacao = buscarEntidadePorId(id);
        return LocacaoResponseDTO.fromEntity(locacao);
    }

    @Transactional(readOnly = true)
    public List<LocacaoResponseDTO> listarPorVeiculo(Long veiculoId) {
        // Valida se veículo existe
        if (!veiculoRepository.existsById(veiculoId)) {
            throw new ResourceNotFoundException("Veículo não encontrado com ID: " + veiculoId);
        }
        return locacaoRepository.findByVeiculoId(veiculoId).stream()
                .map(LocacaoResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LocacaoResponseDTO> listarPorCliente(Long clienteId) {
        // Valida se cliente existe
        if (!clienteRepository.existsById(clienteId)) {
            throw new ResourceNotFoundException("Cliente não encontrado com ID: " + clienteId);
        }
        return locacaoRepository.findByClienteId(clienteId).stream()
                .map(LocacaoResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Locacao buscarEntidadePorId(Long id) {
        return locacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Locação não encontrada com o ID: " + id));
    }
}
