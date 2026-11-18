package com.controle.financas.financeiro_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.controle.financas.financeiro_api.model.Transferencia;
import com.controle.financas.financeiro_api.service.TransferenciaService;
import com.controle.financas.financeiro_api.dto.TransferenciaDTO;

@RestController
@RequestMapping("/api/transferencias")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class TransferenciaController {

    private final TransferenciaService transferenciaService;

    public TransferenciaController(TransferenciaService transferenciaService) {
        this.transferenciaService = transferenciaService;
    }

    @PostMapping
    public ResponseEntity<Transferencia> realizarTransferencia(@RequestBody Transferencia transferencia) {
        Transferencia novaTransferencia = transferenciaService.transferir(transferencia);
        return ResponseEntity.status(HttpStatus.CREATED).body(novaTransferencia);
    }

    @GetMapping
    public ResponseEntity<List<TransferenciaDTO>> listarTransferencias() {
        return ResponseEntity.ok(transferenciaService.listarTodas());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirTransferencia(@PathVariable Long id) {
        transferenciaService.excluirTransferencia(id);
        return ResponseEntity.noContent().build();
    }
}
