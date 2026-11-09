package com.locadora.veiculos.controller;

import com.locadora.veiculos.dto.request.VeiculoRequestDTO;
import com.locadora.veiculos.dto.response.LocacaoResponseDTO;
import com.locadora.veiculos.dto.response.VeiculoResponseDTO;
import com.locadora.veiculos.service.LocacaoService;
import com.locadora.veiculos.service.VeiculoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/veiculos")
@RequiredArgsConstructor
public class VeiculoController {

    private final VeiculoService veiculoService;
    private final LocacaoService locacaoService;

    @PostMapping
    public ResponseEntity<VeiculoResponseDTO> cadastrar(@Valid @RequestBody VeiculoRequestDTO dto) {
        VeiculoResponseDTO resposta = veiculoService.cadastrar(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(resposta.getId()).toUri();
        return ResponseEntity.created(uri).body(resposta);
    }

    @GetMapping
    public ResponseEntity<List<VeiculoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(veiculoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VeiculoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(veiculoService.buscarPorId(id));
    }

    @GetMapping("/busca")
    public ResponseEntity<List<VeiculoResponseDTO>> buscarPorModelo(@RequestParam String modelo) {
        return ResponseEntity.ok(veiculoService.buscarPorModelo(modelo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VeiculoResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody VeiculoRequestDTO dto) {
        return ResponseEntity.ok(veiculoService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        veiculoService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/locacoes")
    public ResponseEntity<List<LocacaoResponseDTO>> listarLocacoesDoVeiculo(@PathVariable Long id) {
        return ResponseEntity.ok(locacaoService.listarPorVeiculo(id));
    }
}
