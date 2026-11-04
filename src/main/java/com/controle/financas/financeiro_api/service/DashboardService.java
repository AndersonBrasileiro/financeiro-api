package com.controle.financas.financeiro_api.service;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import com.controle.financas.financeiro_api.dto.DashboardDTO;
import com.controle.financas.financeiro_api.model.Conta;
import com.controle.financas.financeiro_api.repository.ContaRepository; // 🌟 Importação incluída
import com.controle.financas.financeiro_api.repository.LancamentoRepository;

@Service
public class DashboardService {

    private final LancamentoRepository lancamentoRepository;
    private final ContaRepository contaRepository; // 🌟 Dependência incluída

    // Atualização do construtor para injeção de dependências do Spring
    public DashboardService(LancamentoRepository lancamentoRepository, ContaRepository contaRepository) {
        this.lancamentoRepository = lancamentoRepository;
        this.contaRepository = contaRepository;
    }

    /**
     * REGRA DE NEGÓCIO MULTI-TENANT: Consolida o saldo disponível atual e os saldos futuros em aberto do usuário
     */
    public DashboardDTO obterResumoFinanceiro(Long usuarioId) {
        // 1. 📈 MOTOR DO SALDO ATUAL: Busca todas as contas do usuário e soma os saldos reais atuais
        BigDecimal saldoDisponivel = contaRepository.findByUsuarioId(usuarioId).stream()
                .map(Conta::getSaldoAtual)
                .filter(saldo -> saldo != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Busca a soma das receitas em aberto (pago = false)
        BigDecimal receber = lancamentoRepository.somarValoresEmAberto(usuarioId, "RECEITA");
        if (receber == null) receber = BigDecimal.ZERO;
        
        // 3. Busca a soma das despesas em aberto (pago = false)
        BigDecimal pagar = lancamentoRepository.somarValoresEmAberto(usuarioId, "DESPESA");
        if (pagar == null) pagar = BigDecimal.ZERO;

        // 4. Calcula o saldo projetado futuro (Saldo Disponível + Receber - Pagar)
        BigDecimal projetado = saldoDisponivel.add(receber).subtract(pagar);

        // Retorna o DTO com as 4 variáveis perfeitamente alinhadas com o seu construtor
        return new DashboardDTO(saldoDisponivel, pagar, receber, projetado);
    }
}
