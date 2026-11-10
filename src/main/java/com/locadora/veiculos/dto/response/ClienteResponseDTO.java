package com.locadora.veiculos.dto.response;

import com.locadora.veiculos.entity.Cliente;
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
public class ClienteResponseDTO {

    private Long id;
    private String nome;
    private String cpf;
    private String email;
    private LocalDate dataCadastro;
    private LocalDate bloqueadoAte;
    private boolean estaBloqueado;

    public static ClienteResponseDTO fromEntity(Cliente cliente) {
        if (cliente == null) return null;
        boolean bloqueado = cliente.getBloqueadoAte() != null && cliente.getBloqueadoAte().isAfter(LocalDate.now());
        return ClienteResponseDTO.builder()
                .id(cliente.getId())
                .nome(cliente.getNome())
                .cpf(cliente.getCpf())
                .email(cliente.getEmail())
                .dataCadastro(cliente.getDataCadastro())
                .bloqueadoAte(cliente.getBloqueadoAte())
                .estaBloqueado(bloqueado)
                .build();
    }
}
