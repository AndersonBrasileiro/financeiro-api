package com.controle.financas.financeiro_api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.controle.financas.financeiro_api.model.Conta;
import com.controle.financas.financeiro_api.model.Moeda;
import com.controle.financas.financeiro_api.model.Usuario;
import com.controle.financas.financeiro_api.repository.MoedaRepository;
import com.controle.financas.financeiro_api.service.ContaService;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contas")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ContaController {

    private final ContaService contaService;
    private final MoedaRepository moedaRepository;

    public ContaController(ContaService contaService, MoedaRepository moedaRepository) {
        this.contaService = contaService;
        this.moedaRepository = moedaRepository;
    }

    @GetMapping
    public ResponseEntity<List<Conta>> buscarTodas(Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return ResponseEntity.ok(contaService.listarPorUsuario(usuarioLogado.getId()));
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<Conta>> buscarPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(contaService.listarPorUsuario(usuarioId));
    }

    @PostMapping
    public ResponseEntity<Conta> criar(@RequestBody Conta conta, Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        conta.setUsuario(usuarioLogado);

        Moeda moedaDoUsuario = moedaRepository.findByCodigoIsoAndUsuarioId("BRL", usuarioLogado.getId())
            .orElseThrow(() -> new IllegalArgumentException("Moeda padrão BRL não encontrada para este usuário."));

        conta.setJavaMoeda(moedaDoUsuario);

        Conta novaConta = contaService.salvar(conta);
        return ResponseEntity.status(HttpStatus.CREATED).body(novaConta);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody Conta conta, Authentication authentication) {
        try {
            Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
            Conta atualizada = contaService.atualizar(id, conta, usuarioLogado.getId());
            return ResponseEntity.ok(atualizada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("mensagem", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluir(@PathVariable Long id, Authentication authentication) {
        try {
            Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
            contaService.excluir(id, usuarioLogado.getId());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("mensagem", e.getMessage()));
        }
    }
}
