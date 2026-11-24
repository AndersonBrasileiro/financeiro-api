package com.controle.financas.financeiro_api.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.controle.financas.financeiro_api.model.CentroCusto;
import com.controle.financas.financeiro_api.service.CentroCustoService;

@RestController
@RequestMapping("/api/centros-custo")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class CentroCustoController {

    private final CentroCustoService centroCustoService;

    public CentroCustoController(CentroCustoService centroCustoService) {
        this.centroCustoService = centroCustoService;
    }

    @GetMapping
    public ResponseEntity<List<CentroCusto>> listarTodos() {
        return ResponseEntity.ok(centroCustoService.listarTodosDoUsuario());
    }

    @PostMapping
    public ResponseEntity<CentroCusto> criar(@RequestBody CentroCusto centroCusto) {
        CentroCusto novoCentro = centroCustoService.salvar(centroCusto);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoCentro);
    }

    @GetMapping("/filtrar")
    public ResponseEntity<List<CentroCusto>> buscarFiltrados(@RequestParam String fluxo) {
        List<CentroCusto> lista = centroCustoService.listarPorFluxoELogado(fluxo);
        return ResponseEntity.ok(lista);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody CentroCusto centroCusto) {
        try {
            return ResponseEntity.ok(centroCustoService.atualizar(id, centroCusto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("mensagem", e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> alternarStatus(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(centroCustoService.alternarStatus(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("mensagem", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluir(@PathVariable Long id) {
        try {
            centroCustoService.excluir(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("mensagem", e.getMessage()));
        }
    }
}
