package com.locadora.veiculos.service;

import com.locadora.veiculos.dto.request.VeiculoRequestDTO;
import com.locadora.veiculos.dto.response.VeiculoResponseDTO;
import com.locadora.veiculos.entity.Veiculo;
import com.locadora.veiculos.exception.ConflitoDadosException;
import com.locadora.veiculos.exception.OperacaoNaoPermitidaException;
import com.locadora.veiculos.exception.ResourceNotFoundException;
import com.locadora.veiculos.repository.LocacaoRepository;
import com.locadora.veiculos.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final LocacaoRepository locacaoRepository;

    @Transactional
    public VeiculoResponseDTO cadastrar(VeiculoRequestDTO dto) {
        String placaFormatada = dto.getPlaca().trim().toUpperCase();
        if (veiculoRepository.existsByPlaca(placaFormatada)) {
            throw new ConflitoDadosException("Já existe um veículo cadastrado com a placa: " + placaFormatada);
        }

        Veiculo veiculo = Veiculo.builder()
                .modelo(dto.getModelo().trim())
                .placa(placaFormatada)
                .categoria(dto.getCategoria().trim())
                .valorDiaria(dto.getValorDiaria())
                .build();

        Veiculo salvo = veiculoRepository.save(veiculo);
        return VeiculoResponseDTO.fromEntity(salvo);
    }

    @Transactional(readOnly = true)
    public List<VeiculoResponseDTO> listarTodos() {
        return veiculoRepository.findAll().stream()
                .map(VeiculoResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VeiculoResponseDTO buscarPorId(Long id) {
        Veiculo veiculo = buscarEntidadePorId(id);
        return VeiculoResponseDTO.fromEntity(veiculo);
    }

    @Transactional(readOnly = true)
    public List<VeiculoResponseDTO> buscarPorModelo(String modelo) {
        return veiculoRepository.findByModeloContainingIgnoreCase(modelo).stream()
                .map(VeiculoResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public VeiculoResponseDTO atualizar(Long id, VeiculoRequestDTO dto) {
        Veiculo veiculo = buscarEntidadePorId(id);
        String placaFormatada = dto.getPlaca().trim().toUpperCase();

        if (veiculoRepository.existsByPlacaAndIdNot(placaFormatada, id)) {
            throw new ConflitoDadosException("A placa '" + placaFormatada + "' já está em uso por outro veículo");
        }

        veiculo.setModelo(dto.getModelo().trim());
        veiculo.setPlaca(placaFormatada);
        veiculo.setCategoria(dto.getCategoria().trim());
        veiculo.setValorDiaria(dto.getValorDiaria());

        Veiculo atualizado = veiculoRepository.save(veiculo);
        return VeiculoResponseDTO.fromEntity(atualizado);
    }

    @Transactional
    public void excluir(Long id) {
        Veiculo veiculo = buscarEntidadePorId(id);

        if (locacaoRepository.existsByVeiculoId(id)) {
            throw new OperacaoNaoPermitidaException(
                    "Não é possível excluir o veículo ID " + id + " (" + veiculo.getModelo() + " - " + veiculo.getPlaca() +
                    "), pois ele possui registros de locação vinculados no sistema."
            );
        }

        veiculoRepository.delete(veiculo);
    }

    @Transactional(readOnly = true)
    public Veiculo buscarEntidadePorId(Long id) {
        return veiculoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado com o ID: " + id));
    }
}
