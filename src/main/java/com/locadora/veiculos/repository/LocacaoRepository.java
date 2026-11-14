package com.locadora.veiculos.repository;

import com.locadora.veiculos.entity.Locacao;
import com.locadora.veiculos.entity.enums.StatusLocacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocacaoRepository extends JpaRepository<Locacao, Long> {

    List<Locacao> findByVeiculoId(Long veiculoId);

    List<Locacao> findByClienteId(Long clienteId);

    List<Locacao> findByStatus(StatusLocacao status);

    boolean existsByVeiculoIdAndStatus(Long veiculoId, StatusLocacao status);

    long countByClienteIdAndStatus(Long clienteId, StatusLocacao status);

    boolean existsByVeiculoId(Long veiculoId);

    boolean existsByClienteId(Long clienteId);

    boolean existsByClienteIdAndStatus(Long clienteId, StatusLocacao status);
}
