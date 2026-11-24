package com.controle.financas.financeiro_api.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.controle.financas.financeiro_api.model.Conta;
import com.controle.financas.financeiro_api.model.Usuario;
import com.controle.financas.financeiro_api.model.Movimentacao;
import com.controle.financas.financeiro_api.repository.ContaRepository;
import com.controle.financas.financeiro_api.repository.LancamentoRepository;
import com.controle.financas.financeiro_api.repository.MovimentacaoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ContaService {

    private final ContaRepository contaRepository;
    private final MovimentacaoRepository movimentacaoRepository;
    private final LancamentoRepository lancamentoRepository;

    public ContaService(ContaRepository contaRepository,
                        MovimentacaoRepository movimentacaoRepository,
                        LancamentoRepository lancamentoRepository) {
        this.contaRepository = contaRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.lancamentoRepository = lancamentoRepository;
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

        if (ehContaNova && contaSalva.getSaldoInicial() != null && contaSalva.getSaldoInicial().compareTo(BigDecimal.ZERO) > 0) {
            Movimentacao movInicial = new Movimentacao();
            movInicial.setConta(contaSalva);
            movInicial.setValor(contaSalva.getSaldoInicial());
            movInicial.setDataHora(LocalDateTime.now());
            movInicial.setTipoMovimentacao("ENTRADA");
            movInicial.setHistorico("Saldo Inicial - Abertura de Conta");

            movimentacaoRepository.save(movInicial);
        }

        return contaSalva;
    }

    @Transactional
    public Conta atualizar(Long id, Conta dadosAtualizados, Long usuarioId) {
        Conta existente = contaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conta bancária não encontrada."));

        if (!existente.getUsuario().getId().equals(usuarioId)) {
            throw new IllegalArgumentException("Acesso negado a esta conta.");
        }

        existente.setNome(dadosAtualizados.getNome());
        existente.setTipoConta(dadosAtualizados.getTipoConta());

        return contaRepository.save(existente);
    }

    @Transactional
    public void excluir(Long id, Long usuarioId) {
        Conta conta = contaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conta bancária não encontrada."));

        if (!conta.getUsuario().getId().equals(usuarioId)) {
            throw new IllegalArgumentException("Acesso negado a esta conta.");
        }

        // Verifica se há lançamentos (Contas a Pagar/Receber) vinculados
        if (lancamentoRepository.existsByContaId(id)) {
            throw new IllegalArgumentException("Não é possível excluir esta conta pois existem lançamentos vinculados a ela.");
        }

        // Verifica movimentações no extrato (ignorando apenas o registro inicial de abertura)
        List<Movimentacao> movimentacoes = movimentacaoRepository.findByContaIdOrderByDataHoraDesc(id);
        boolean possuiMovimentacaoReal = movimentacoes.stream()
                .anyMatch(m -> !"Saldo Inicial - Abertura de Conta".equals(m.getHistorico()));

        if (possuiMovimentacaoReal) {
            throw new IllegalArgumentException("Não é possível excluir esta conta pois ela já possui movimentações no extrato.");
        }

        // Se só tinha a movimentação de abertura, limpa-a antes de apagar a conta
        if (!movimentacoes.isEmpty()) {
            movimentacaoRepository.deleteAll(movimentacoes);
        }

        contaRepository.delete(conta);
    }

    private Usuario getUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Usuario) auth.getPrincipal();
    }
}
