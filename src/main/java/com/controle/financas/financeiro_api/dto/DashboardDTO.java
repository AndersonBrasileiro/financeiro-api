package com.controle.financas.financeiro_api.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {
    private BigDecimal saldoDisponivel;    
    private BigDecimal totalContasReceberEmAberto;
    private BigDecimal totalContasPagarEmAberto;
    private BigDecimal saldoProjetadoFuturo; // (Receber - Pagar)
}
