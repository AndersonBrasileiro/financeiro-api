package com.controle.financas.financeiro_api.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.controle.financas.financeiro_api.model.Lancamento;
import com.controle.financas.financeiro_api.service.LancamentoService;

@RestController
@RequestMapping("/api/lancamentos")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class LancamentoController {

    private final LancamentoService lancamentoService;

    public LancamentoController(LancamentoService lancamentoService) {
        this.lancamentoService = lancamentoService;
    }

    @PostMapping
    public ResponseEntity<Lancamento> criar(@RequestBody Lancamento lancamento) {
        Lancamento novoLancamento = lancamentoService.salvar(lancamento);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoLancamento);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Lancamento> atualizar(@PathVariable Long id, @RequestBody Lancamento lancamento) {
        Lancamento lancamentoAtualizado = lancamentoService.atualizar(id, lancamento);
        return ResponseEntity.ok(lancamentoAtualizado);
    }

    @PutMapping("/{id}/baixar")
    public ResponseEntity<Lancamento> baixar(@PathVariable Long id) {
        Lancamento lancamentoBaixado = lancamentoService.baixarLancamento(id);
        return ResponseEntity.ok(lancamentoBaixado);
    }

    @PutMapping("/{id}/estornar")
    public ResponseEntity<Lancamento> estornar(@PathVariable Long id) {
        Lancamento lancamentoEstornado = lancamentoService.estornarLancamento(id);
        return ResponseEntity.ok(lancamentoEstornado);
    }

    @GetMapping
    public ResponseEntity<List<Lancamento>> buscarTodos() {
        return ResponseEntity.ok(lancamentoService.listarTodos());
    }

    @GetMapping("/filtrar")
    public ResponseEntity<List<Lancamento>> filtrar(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) Long contaId,
            @RequestParam(required = false) Long centroCustoId,
            @RequestParam(required = false) Long categoriaId) {
        
        List<Lancamento> resultado = lancamentoService.filtrar(
                tipo, dataInicio, dataFim, contaId, centroCustoId, categoriaId);
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Lancamento> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(lancamentoService.buscarPorId(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        lancamentoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}