package com.locadora.veiculos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.locadora.veiculos.dto.request.ClienteRequestDTO;
import com.locadora.veiculos.dto.request.DevolucaoRequestDTO;
import com.locadora.veiculos.dto.request.LocacaoRequestDTO;
import com.locadora.veiculos.dto.request.VeiculoRequestDTO;
import com.locadora.veiculos.entity.Cliente;
import com.locadora.veiculos.entity.Locacao;
import com.locadora.veiculos.entity.Veiculo;
import com.locadora.veiculos.entity.enums.StatusLocacao;
import com.locadora.veiculos.repository.ClienteRepository;
import com.locadora.veiculos.repository.LocacaoRepository;
import com.locadora.veiculos.repository.VeiculoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class LocadoraIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private LocacaoRepository locacaoRepository;

    @Test
    @DisplayName("Deve cadastrar um novo veículo com sucesso (201 Created)")
    void deveCadastrarVeiculoComSucesso() throws Exception {
        VeiculoRequestDTO dto = VeiculoRequestDTO.builder()
                .modelo("Honda Civic Touring")
                .placa("CIV2026")
                .categoria("SEDAN")
                .valorDiaria(new BigDecimal("210.00"))
                .build();

        mockMvc.perform(post("/veiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.placa").value("CIV2026"))
                .andExpect(jsonPath("$.modelo").value("Honda Civic Touring"));
    }

    @Test
    @DisplayName("Deve rejeitar cadastro de veículo com placa duplicada (409 Conflict)")
    void deveRejeitarVeiculoComPlacaDuplicada() throws Exception {
        VeiculoRequestDTO dto = VeiculoRequestDTO.builder()
                .modelo("Toyota Corolla Duplicado")
                .placa("BRA2E19") // Placa já existente do DataLoader
                .categoria("SEDAN")
                .valorDiaria(new BigDecimal("180.00"))
                .build();

        mockMvc.perform(post("/veiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflito de dados"))
                .andExpect(jsonPath("$.message", containsString("Já existe um veículo cadastrado com a placa")));
    }

    @Test
    @DisplayName("Deve buscar veículos por modelo via query method (200 OK)")
    void deveBuscarVeiculosPorModelo() throws Exception {
        mockMvc.perform(get("/veiculos/busca").param("modelo", "corolla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].modelo", containsString("Corolla")));
    }

    @Test
    @DisplayName("Deve impedir exclusão de veículo com locação vinculada (400 Bad Request)")
    void deveImpedirExclusaoDeVeiculoComLocacao() throws Exception {
        // Veículo ID 1 ou 2 possui locação criada pelo DataLoader
        Veiculo veiculoComLocacao = veiculoRepository.findAll().stream()
                .filter(v -> locacaoRepository.existsByVeiculoId(v.getId()))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(delete("/veiculos/" + veiculoComLocacao.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Operação não permitida"))
                .andExpect(jsonPath("$.message", containsString("possui registros de locação vinculados")));
    }

    @Test
    @DisplayName("RN1: Não deve permitir locação de veículo que já possui locação ATIVA")
    void deveImpedirLocacaoDeVeiculoOcupado() throws Exception {
        // Obter locação ativa criada pelo DataLoader
        Locacao locacaoAtiva = locacaoRepository.findAll().stream()
                .filter(l -> l.getStatus() == StatusLocacao.ATIVA)
                .findFirst()
                .orElseThrow();

        Cliente outroCliente = clienteRepository.save(Cliente.builder()
                .nome("Lucas Fernandes")
                .cpf("77788899900")
                .email("lucas.fernandes@test.com")
                .dataCadastro(LocalDate.now())
                .build());

        LocacaoRequestDTO dto = LocacaoRequestDTO.builder()
                .veiculoId(locacaoAtiva.getVeiculo().getId()) // Mesmo veículo já alugado
                .clienteId(outroCliente.getId())
                .dataInicio(LocalDate.now())
                .dataPrevistaDevolucao(LocalDate.now().plusDays(2))
                .build();

        mockMvc.perform(post("/locacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Violação de regra de negócio"))
                .andExpect(jsonPath("$.message", containsString("já possui uma locação ativa em andamento")));
    }

    @Test
    @DisplayName("RN2: Não deve permitir nova locação para cliente bloqueado")
    void deveImpedirLocacaoParaClienteBloqueado() throws Exception {
        // Cliente c3 do DataLoader possui bloqueadoAte no futuro
        Cliente clienteBloqueado = clienteRepository.findByCpf("33344455566").orElseThrow();

        Veiculo veiculoLivre = veiculoRepository.save(Veiculo.builder()
                .modelo("Hyundai HB20")
                .placa("HB2A026")
                .categoria("HATCH")
                .valorDiaria(new BigDecimal("100.00"))
                .build());

        LocacaoRequestDTO dto = LocacaoRequestDTO.builder()
                .veiculoId(veiculoLivre.getId())
                .clienteId(clienteBloqueado.getId())
                .dataInicio(LocalDate.now())
                .dataPrevistaDevolucao(LocalDate.now().plusDays(3))
                .build();

        mockMvc.perform(post("/locacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Violação de regra de negócio"))
                .andExpect(jsonPath("$.message", containsString("está temporariamente bloqueado para novas locações")));
    }

    @Test
    @DisplayName("RN2: Não deve permitir mais de 2 locações ativas para o mesmo cliente")
    void deveImpedirMaisDeDuasLocacoesAtivas() throws Exception {
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nome("Fernanda Lima")
                .cpf("99911122233")
                .email("fernanda.lima@test.com")
                .dataCadastro(LocalDate.now())
                .build());

        Veiculo v1 = veiculoRepository.save(Veiculo.builder()
                .modelo("Carro A")
                .placa("AAA1111")
                .categoria("HATCH")
                .valorDiaria(new BigDecimal("100.00"))
                .build());

        Veiculo v2 = veiculoRepository.save(Veiculo.builder()
                .modelo("Carro B")
                .placa("BBB2222")
                .categoria("HATCH")
                .valorDiaria(new BigDecimal("100.00"))
                .build());

        Veiculo v3 = veiculoRepository.save(Veiculo.builder()
                .modelo("Carro C")
                .placa("CCC3333")
                .categoria("HATCH")
                .valorDiaria(new BigDecimal("100.00"))
                .build());

        // Cria 1ª locação
        locacaoRepository.save(Locacao.builder()
                .veiculo(v1).cliente(cliente).dataInicio(LocalDate.now())
                .dataPrevistaDevolucao(LocalDate.now().plusDays(3))
                .valorTotal(new BigDecimal("300.00")).status(StatusLocacao.ATIVA).build());

        // Cria 2ª locação
        locacaoRepository.save(Locacao.builder()
                .veiculo(v2).cliente(cliente).dataInicio(LocalDate.now())
                .dataPrevistaDevolucao(LocalDate.now().plusDays(3))
                .valorTotal(new BigDecimal("300.00")).status(StatusLocacao.ATIVA).build());

        // Tenta 3ª locação
        LocacaoRequestDTO dto = LocacaoRequestDTO.builder()
                .veiculoId(v3.getId())
                .clienteId(cliente.getId())
                .dataInicio(LocalDate.now())
                .dataPrevistaDevolucao(LocalDate.now().plusDays(2))
                .build();

        mockMvc.perform(post("/locacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Violação de regra de negócio"))
                .andExpect(jsonPath("$.message", containsString("atingiu o limite máximo permitido de 2 locações ativas")));
    }

    @Test
    @DisplayName("RN3: Deve calcular o valorTotal automaticamente pelo backend (diárias * valorDiária)")
    void deveCalcularValorTotalAutomaticamente() throws Exception {
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nome("Beatriz Silva")
                .cpf("44455566677")
                .email("beatriz.silva@test.com")
                .dataCadastro(LocalDate.now())
                .build());

        Veiculo veiculo = veiculoRepository.save(Veiculo.builder()
                .modelo("Nissan Kicks")
                .placa("NIS5K55")
                .categoria("SUV")
                .valorDiaria(new BigDecimal("150.00"))
                .build());

        // 4 diárias = 4 * 150.00 = 600.00
        LocacaoRequestDTO dto = LocacaoRequestDTO.builder()
                .veiculoId(veiculo.getId())
                .clienteId(cliente.getId())
                .dataInicio(LocalDate.now())
                .dataPrevistaDevolucao(LocalDate.now().plusDays(4))
                .build();

        mockMvc.perform(post("/locacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorTotal").value(600.00))
                .andExpect(jsonPath("$.status").value("ATIVA"));
    }

    @Test
    @DisplayName("RN4: Devolução com atraso deve aplicar multa de 20% da diária por dia e bloquear cliente por 15 dias")
    void deveAplicarMultaEBloquearClienteEmCasoDeAtraso() throws Exception {
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nome("Marcos Vinicius")
                .cpf("88899900011")
                .email("marcos.vinicius@test.com")
                .dataCadastro(LocalDate.now())
                .build());

        Veiculo veiculo = veiculoRepository.save(Veiculo.builder()
                .modelo("Volkswagen Polo")
                .placa("POL1V11")
                .categoria("HATCH")
                .valorDiaria(new BigDecimal("100.00"))
                .build());

        // Locação de 2 dias (100.00 * 2 = 200.00)
        LocalDate inicio = LocalDate.now().minusDays(5);
        LocalDate prevista = LocalDate.now().minusDays(3);
        Locacao locacao = locacaoRepository.save(Locacao.builder()
                .veiculo(veiculo)
                .cliente(cliente)
                .dataInicio(inicio)
                .dataPrevistaDevolucao(prevista)
                .valorTotal(new BigDecimal("200.00"))
                .status(StatusLocacao.ATIVA)
                .build());

        // Devolução com 3 dias de atraso (devolvendo na data de hoje)
        // Atraso = 3 dias
        // Diárias extras = 3 * 100.00 = 300.00
        // Multa (20% por dia) = 3 * (100.00 * 0.20) = 60.00
        // Novo total = 200.00 + 300.00 + 60.00 = 560.00
        LocalDate dataReal = LocalDate.now();
        DevolucaoRequestDTO devDto = DevolucaoRequestDTO.builder()
                .dataRealDevolucao(dataReal)
                .build();

        mockMvc.perform(patch("/locacoes/" + locacao.getId() + "/devolucao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(devDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"))
                .andExpect(jsonPath("$.valorTotal").value(560.00))
                .andExpect(jsonPath("$.diasAtraso").value(3))
                .andExpect(jsonPath("$.valorMulta").value(60.00));

        // Verificar que o cliente foi bloqueado por 15 dias
        Cliente clienteAtualizado = clienteRepository.findById(cliente.getId()).orElseThrow();
        assertNotNull(clienteAtualizado.getBloqueadoAte());
        assertEquals(LocalDate.now().plusDays(15), clienteAtualizado.getBloqueadoAte());
    }

    @Test
    @DisplayName("Devolução no prazo não aplica multa nem bloqueia cliente")
    void deveFinalizarSemMultaNoPrazo() throws Exception {
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nome("Julia Costa")
                .cpf("55566677788")
                .email("julia.costa@test.com")
                .dataCadastro(LocalDate.now())
                .build());

        Veiculo veiculo = veiculoRepository.save(Veiculo.builder()
                .modelo("Renault Kwid")
                .placa("KWD2R22")
                .categoria("HATCH")
                .valorDiaria(new BigDecimal("90.00"))
                .build());

        LocalDate inicio = LocalDate.now().minusDays(3);
        LocalDate prevista = LocalDate.now();
        Locacao locacao = locacaoRepository.save(Locacao.builder()
                .veiculo(veiculo)
                .cliente(cliente)
                .dataInicio(inicio)
                .dataPrevistaDevolucao(prevista)
                .valorTotal(new BigDecimal("270.00"))
                .status(StatusLocacao.ATIVA)
                .build());

        DevolucaoRequestDTO devDto = DevolucaoRequestDTO.builder()
                .dataRealDevolucao(prevista)
                .build();

        mockMvc.perform(patch("/locacoes/" + locacao.getId() + "/devolucao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(devDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"))
                .andExpect(jsonPath("$.valorTotal").value(270.00))
                .andExpect(jsonPath("$.diasAtraso").doesNotExist())
                .andExpect(jsonPath("$.valorMulta").doesNotExist());

        Cliente clienteAposDevolucao = clienteRepository.findById(cliente.getId()).orElseThrow();
        assertNull(clienteAposDevolucao.getBloqueadoAte());
    }

    @Test
    @DisplayName("Deve cadastrar cliente com sucesso e gerar dataCadastro automaticamente (201 Created)")
    void deveCadastrarClienteComSucesso() throws Exception {
        ClienteRequestDTO dto = ClienteRequestDTO.builder()
                .nome("Larissa Manoela")
                .cpf("12398745600")
                .email("larissa.manoela@email.com")
                .build();

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nome").value("Larissa Manoela"))
                .andExpect(jsonPath("$.dataCadastro").isNotEmpty());
    }

    @Test
    @DisplayName("Deve rejeitar cliente com CPF duplicado (409 Conflict)")
    void deveRejeitarClienteComCpfDuplicado() throws Exception {
        ClienteRequestDTO dto = ClienteRequestDTO.builder()
                .nome("Carlos Clone")
                .cpf("11122233344") // CPF existente do DataLoader
                .email("outro.email@email.com")
                .build();

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflito de dados"))
                .andExpect(jsonPath("$.message", containsString("Já existe um cliente cadastrado com o CPF")));
    }

    @Test
    @DisplayName("Deve validar campos obrigatórios e retornar lista de erros nos DTOs (400 Bad Request)")
    void deveValidarCamposObrigatorios() throws Exception {
        ClienteRequestDTO dtoInvalido = ClienteRequestDTO.builder()
                .nome("")
                .cpf("")
                .email("email-invalido")
                .build();

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Dados de entrada inválidos"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("Deve retornar 404 ao buscar recurso inexistente por ID")
    void deveRetornar404AoBuscarInexistente() throws Exception {
        mockMvc.perform(get("/veiculos/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.message", containsString("Veículo não encontrado com o ID: 99999")));
    }

    @Test
    @DisplayName("Deve permitir cancelar uma locação ativa com sucesso")
    void deveCancelarLocacaoAtiva() throws Exception {
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nome("Renato Augusto")
                .cpf("99988877766")
                .email("renato.augusto@email.com")
                .dataCadastro(LocalDate.now())
                .build());

        Veiculo veiculo = veiculoRepository.save(Veiculo.builder()
                .modelo("Chevrolet Spin")
                .placa("SPN7C77")
                .categoria("MINIVAN")
                .valorDiaria(new BigDecimal("160.00"))
                .build());

        Locacao locacao = locacaoRepository.save(Locacao.builder()
                .veiculo(veiculo)
                .cliente(cliente)
                .dataInicio(LocalDate.now())
                .dataPrevistaDevolucao(LocalDate.now().plusDays(2))
                .valorTotal(new BigDecimal("320.00"))
                .status(StatusLocacao.ATIVA)
                .build());

        mockMvc.perform(patch("/locacoes/" + locacao.getId() + "/cancelar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));
    }
}
