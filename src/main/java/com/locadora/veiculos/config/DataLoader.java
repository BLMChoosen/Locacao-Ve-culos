package com.locadora.veiculos.config;

import com.locadora.veiculos.entity.Cliente;
import com.locadora.veiculos.entity.Locacao;
import com.locadora.veiculos.entity.Veiculo;
import com.locadora.veiculos.entity.enums.StatusLocacao;
import com.locadora.veiculos.repository.ClienteRepository;
import com.locadora.veiculos.repository.LocacaoRepository;
import com.locadora.veiculos.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final VeiculoRepository veiculoRepository;
    private final ClienteRepository clienteRepository;
    private final LocacaoRepository locacaoRepository;

    @Override
    public void run(String... args) {
        if (veiculoRepository.count() > 0) {
            return;
        }

        // Cadastro inicial de Veículos
        Veiculo v1 = Veiculo.builder()
                .modelo("Toyota Corolla XEi 2.0")
                .placa("BRA2E19")
                .categoria("SEDAN")
                .valorDiaria(new BigDecimal("180.00"))
                .build();

        Veiculo v2 = Veiculo.builder()
                .modelo("Jeep Renegade Longitude")
                .placa("JEP4X88")
                .categoria("SUV")
                .valorDiaria(new BigDecimal("220.00"))
                .build();

        Veiculo v3 = Veiculo.builder()
                .modelo("Fiat Argo Drive 1.0")
                .placa("ARG9F12")
                .categoria("HATCH")
                .valorDiaria(new BigDecimal("110.00"))
                .build();

        Veiculo v4 = Veiculo.builder()
                .modelo("Chevrolet Tracker Premier")
                .placa("TRK7A34")
                .categoria("SUV")
                .valorDiaria(new BigDecimal("240.00"))
                .build();

        veiculoRepository.save(v1);
        veiculoRepository.save(v2);
        veiculoRepository.save(v3);
        veiculoRepository.save(v4);

        // Cadastro inicial de Clientes
        Cliente c1 = Cliente.builder()
                .nome("Carlos Eduardo da Silva")
                .cpf("11122233344")
                .email("carlos.silva@email.com")
                .dataCadastro(LocalDate.now().minusDays(40))
                .bloqueadoAte(null)
                .build();

        Cliente c2 = Cliente.builder()
                .nome("Mariana Souza Santos")
                .cpf("22233344455")
                .email("mariana.souza@email.com")
                .dataCadastro(LocalDate.now().minusDays(20))
                .bloqueadoAte(null)
                .build();

        Cliente c3 = Cliente.builder()
                .nome("Roberto Mendes de Almeida")
                .cpf("33344455566")
                .email("roberto.almeida@email.com")
                .dataCadastro(LocalDate.now().minusDays(10))
                .bloqueadoAte(LocalDate.now().plusDays(10)) // Cliente pré-bloqueado para testes da RN2
                .build();

        clienteRepository.save(c1);
        clienteRepository.save(c2);
        clienteRepository.save(c3);

        // Locação 1: Ativa (Jeep Renegade para Mariana)
        Locacao l1 = Locacao.builder()
                .veiculo(v2)
                .cliente(c2)
                .dataInicio(LocalDate.now().minusDays(2))
                .dataPrevistaDevolucao(LocalDate.now().plusDays(3))
                .dataRealDevolucao(null)
                .valorTotal(new BigDecimal("1100.00")) // 5 diárias * 220.00
                .status(StatusLocacao.ATIVA)
                .build();

        // Locação 2: Finalizada no prazo (Toyota Corolla para Carlos)
        Locacao l2 = Locacao.builder()
                .veiculo(v1)
                .cliente(c1)
                .dataInicio(LocalDate.now().minusDays(15))
                .dataPrevistaDevolucao(LocalDate.now().minusDays(10))
                .dataRealDevolucao(LocalDate.now().minusDays(10))
                .valorTotal(new BigDecimal("900.00")) // 5 diárias * 180.00
                .status(StatusLocacao.FINALIZADA)
                .build();

        locacaoRepository.save(l1);
        locacaoRepository.save(l2);
    }
}
