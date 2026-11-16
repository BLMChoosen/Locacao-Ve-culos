package com.locadora.veiculos.controller;

import com.locadora.veiculos.dto.request.DevolucaoRequestDTO;
import com.locadora.veiculos.dto.request.LocacaoRequestDTO;
import com.locadora.veiculos.dto.response.LocacaoResponseDTO;
import com.locadora.veiculos.service.LocacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/locacoes")
@RequiredArgsConstructor
public class LocacaoController {

    private final LocacaoService locacaoService;

    @PostMapping
    public ResponseEntity<LocacaoResponseDTO> criarLocacao(@Valid @RequestBody LocacaoRequestDTO dto) {
        LocacaoResponseDTO resposta = locacaoService.criarLocacao(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(resposta.getId()).toUri();
        return ResponseEntity.created(uri).body(resposta);
    }

    @GetMapping
    public ResponseEntity<List<LocacaoResponseDTO>> listarTodas(
            @RequestParam(required = false) Long veiculoId,
            @RequestParam(required = false) Long clienteId) {
        if (veiculoId != null) {
            return ResponseEntity.ok(locacaoService.listarPorVeiculo(veiculoId));
        }
        if (clienteId != null) {
            return ResponseEntity.ok(locacaoService.listarPorCliente(clienteId));
        }
        return ResponseEntity.ok(locacaoService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocacaoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(locacaoService.buscarPorId(id));
    }

    @PatchMapping("/{id}/devolucao")
    public ResponseEntity<LocacaoResponseDTO> registrarDevolucao(
            @PathVariable Long id,
            @RequestBody(required = false) DevolucaoRequestDTO dto) {
        return ResponseEntity.ok(locacaoService.registrarDevolucao(id, dto));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<LocacaoResponseDTO> cancelarLocacao(@PathVariable Long id) {
        return ResponseEntity.ok(locacaoService.cancelarLocacao(id));
    }
}
