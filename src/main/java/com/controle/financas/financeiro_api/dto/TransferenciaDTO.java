package com.controle.financas.financeiro_api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.controle.financas.financeiro_api.model.Transferencia;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferenciaDTO {
    private Long id;
    private String descricao;
    private BigDecimal valor;
    private LocalDate dataTransferencia;
    private String contaOrigemNome;
    private String contaDestinoNome;

    public TransferenciaDTO(Transferencia t) {
        this.id = t.getId();
        this.descricao = t.getDescricao();
        this.valor = t.getValor();
        this.dataTransferencia = t.getDataTransferencia();
        this.contaOrigemNome = t.getContaOrigem() != null ? t.getContaOrigem().getNome() : "N/A";
        this.contaDestinoNome = t.getContaDestino() != null ? t.getContaDestino().getNome() : "N/A";
    }
}
