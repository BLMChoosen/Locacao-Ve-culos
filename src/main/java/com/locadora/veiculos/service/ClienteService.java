package com.locadora.veiculos.service;

import com.locadora.veiculos.dto.request.ClienteRequestDTO;
import com.locadora.veiculos.dto.response.ClienteResponseDTO;
import com.locadora.veiculos.entity.Cliente;
import com.locadora.veiculos.entity.enums.StatusLocacao;
import com.locadora.veiculos.exception.ConflitoDadosException;
import com.locadora.veiculos.exception.OperacaoNaoPermitidaException;
import com.locadora.veiculos.exception.ResourceNotFoundException;
import com.locadora.veiculos.repository.ClienteRepository;
import com.locadora.veiculos.repository.LocacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final LocacaoRepository locacaoRepository;

    @Transactional
    public ClienteResponseDTO cadastrar(ClienteRequestDTO dto) {
        String cpfLimpo = dto.getCpf().replaceAll("[^0-9]", "");
        String emailFormatado = dto.getEmail().trim().toLowerCase();

        if (clienteRepository.existsByCpf(cpfLimpo)) {
            throw new ConflitoDadosException("Já existe um cliente cadastrado com o CPF: " + dto.getCpf());
        }

        if (clienteRepository.existsByEmail(emailFormatado)) {
            throw new ConflitoDadosException("Já existe um cliente cadastrado com o e-mail: " + emailFormatado);
        }

        Cliente cliente = Cliente.builder()
                .nome(dto.getNome().trim())
                .cpf(cpfLimpo)
                .email(emailFormatado)
                .dataCadastro(LocalDate.now())
                .bloqueadoAte(null)
                .build();

        Cliente salvo = clienteRepository.save(cliente);
        return ClienteResponseDTO.fromEntity(salvo);
    }

    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> listarTodos() {
        return clienteRepository.findAll().stream()
                .map(ClienteResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorId(Long id) {
        Cliente cliente = buscarEntidadePorId(id);
        return ClienteResponseDTO.fromEntity(cliente);
    }

    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> buscarPorNome(String nome) {
        return clienteRepository.findByNomeContainingIgnoreCase(nome).stream()
                .map(ClienteResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClienteResponseDTO atualizar(Long id, ClienteRequestDTO dto) {
        Cliente cliente = buscarEntidadePorId(id);
        String cpfLimpo = dto.getCpf().replaceAll("[^0-9]", "");
        String emailFormatado = dto.getEmail().trim().toLowerCase();

        if (clienteRepository.existsByCpfAndIdNot(cpfLimpo, id)) {
            throw new ConflitoDadosException("O CPF '" + dto.getCpf() + "' já está em uso por outro cliente");
        }

        if (clienteRepository.existsByEmailAndIdNot(emailFormatado, id)) {
            throw new ConflitoDadosException("O e-mail '" + emailFormatado + "' já está em uso por outro cliente");
        }

        cliente.setNome(dto.getNome().trim());
        cliente.setCpf(cpfLimpo);
        cliente.setEmail(emailFormatado);

        Cliente atualizado = clienteRepository.save(cliente);
        return ClienteResponseDTO.fromEntity(atualizado);
    }

    @Transactional
    public void excluir(Long id) {
        Cliente cliente = buscarEntidadePorId(id);

        if (locacaoRepository.existsByClienteIdAndStatus(id, StatusLocacao.ATIVA)) {
            throw new OperacaoNaoPermitidaException(
                    "Não é possível excluir o cliente ID " + id + " (" + cliente.getNome() +
                    "), pois ele possui locação ativa no momento."
            );
        }

        if (locacaoRepository.existsByClienteId(id)) {
            throw new OperacaoNaoPermitidaException(
                    "Não é possível excluir o cliente ID " + id + " (" + cliente.getNome() +
                    "), pois ele possui histórico de transações de locação no sistema."
            );
        }

        clienteRepository.delete(cliente);
    }

    @Transactional(readOnly = true)
    public Cliente buscarEntidadePorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com o ID: " + id));
    }
}
