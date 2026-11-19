package com.controle.financas.financeiro_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.controle.financas.financeiro_api.model.Categoria;
import com.controle.financas.financeiro_api.model.Usuario;
import com.controle.financas.financeiro_api.service.CategoriaService;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categorias")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public ResponseEntity<List<Categoria>> listar(
            @RequestParam(value = "tipo", required = false) String tipo,
            @AuthenticationPrincipal Usuario usuarioLogado) {

        if (tipo != null) {
            return ResponseEntity.ok(categoriaService.listarPorTipoEUsuario(tipo, usuarioLogado.getId()));
        }
        return ResponseEntity.ok(categoriaService.listarTodasPorUsuario(usuarioLogado.getId()));
    }

    @PostMapping
    public ResponseEntity<Categoria> criar(
            @RequestBody Categoria categoria,
            @AuthenticationPrincipal Usuario usuarioLogado) {

        categoria.setUsuario(usuarioLogado);
        return ResponseEntity.ok(categoriaService.salvar(categoria));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(
            @PathVariable Long id,
            @RequestBody Categoria categoria,
            @AuthenticationPrincipal Usuario usuarioLogado) {
        try {
            Categoria atualizada = categoriaService.atualizar(id, categoria, usuarioLogado.getId());
            return ResponseEntity.ok(atualizada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("mensagem", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluir(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioLogado) {
        try {
            categoriaService.excluir(id, usuarioLogado.getId());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("mensagem", e.getMessage()));
        }
    }
}
