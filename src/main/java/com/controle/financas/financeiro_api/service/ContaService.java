package com.controle.financas.financeiro_api.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.controle.financas.financeiro_api.model.Conta;
import com.controle.financas.financeiro_api.model.Usuario;
import com.controle.financas.financeiro_api.model.Movimentacao;
import com.controle.financas.financeiro_api.repository.ContaRepository;
import com.controle.financas.financeiro_api.repository.MovimentacaoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ContaService {

    private final ContaRepository contaRepository;
    private final MovimentacaoRepository movimentacaoRepository;

    public ContaService(ContaRepository contaRepository, MovimentacaoRepository movimentacaoRepository) {
        this.contaRepository = contaRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    public List<Conta> listarTodas() {
        Usuario usuarioLogado = getUsuarioAutenticado();
        return contaRepository.findByUsuarioId(usuarioLogado.getId());
    }

    public List<Conta> listarPorUsuario(Long usuarioId) {
        Usuario usuarioLogado = getUsuarioAutenticado();
        if (!usuarioLogado.getId().equals(usuarioId)) {
            throw new org.springframework.security.access.AccessDeniedException("Acesso negado aos dados deste usuário.");
        }
        return contaRepository.findByUsuarioId(usuarioId);
    }

    @Transactional
    public Conta salvar(Conta conta) {
        Usuario usuarioLogado = getUsuarioAutenticado();
        conta.setUsuario(usuarioLogado);

        boolean ehContaNova = (conta.getId() == null);

        if (ehContaNova) {
            conta.setSaldoAtual(conta.getSaldoInicial());
        }
        
        Conta contaSalva = contaRepository.save(conta);

        // 🚀 GATILHO DE ABERTURA CORRIGIDO: Totalmente alinhado com o seu Model de Movimentacao!
        if (ehContaNova && contaSalva.getSaldoInicial() != null && contaSalva.getSaldoInicial().compareTo(BigDecimal.ZERO) > 0) {
            Movimentacao movInicial = new Movimentacao();
            movInicial.setConta(contaSalva);
            movInicial.setValor(contaSalva.getSaldoInicial());
            
            // Atributos mapeados de acordo com o seu LancamentoService
            movInicial.setDataHora(LocalDateTime.now());
            movInicial.setTipoMovimentacao("ENTRADA"); 
            movInicial.setHistorico("Saldo Inicial - Abertura de Conta");

            movimentacaoRepository.save(movInicial);
        }

        return contaSalva;
    }

    private Usuario getUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Usuario) auth.getPrincipal();
    }
}
