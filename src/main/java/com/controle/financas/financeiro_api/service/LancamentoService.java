package com.controle.financas.financeiro_api.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.controle.financas.financeiro_api.model.Categoria;
import com.controle.financas.financeiro_api.model.Conta;
import com.controle.financas.financeiro_api.model.Lancamento;
import com.controle.financas.financeiro_api.model.Movimentacao;
import com.controle.financas.financeiro_api.model.Usuario;
import com.controle.financas.financeiro_api.repository.CategoriaRepository;
import com.controle.financas.financeiro_api.repository.ContaRepository;
import com.controle.financas.financeiro_api.repository.LancamentoRepository;
import com.controle.financas.financeiro_api.repository.MovimentacaoRepository;

@Service
public class LancamentoService {

    private final LancamentoRepository lancamentoRepository;
    private final ContaRepository contaRepository;
    private final MovimentacaoRepository movimentacaoRepository;
    private final CategoriaRepository categoriaRepository;

    public LancamentoService(LancamentoRepository lancamentoRepository,
            ContaRepository contaRepository,
            MovimentacaoRepository movimentacaoRepository,
            CategoriaRepository categoriaRepository) {
        this.lancamentoRepository = lancamentoRepository;
        this.contaRepository = contaRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    private Usuario getUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Usuario) auth.getPrincipal();
    }

    private void validarCategoriaAnalitica(Categoria categoriaRef) {
        if (categoriaRef == null || categoriaRef.getId() == null) {
            throw new IllegalArgumentException("A categoria do lançamento é obrigatória.");
        }
        Categoria categoriaCompleta = categoriaRepository.findById(categoriaRef.getId())
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada."));

        if (categoriaCompleta.getCategoriaPai() == null) {
            throw new IllegalStateException("Não é permitido lançar diretamente em uma categoria sintética (Grupo Pai). Selecione uma subcategoria analítica.");
        }
    }

    @Transactional
    public Lancamento salvar(Lancamento lancamento) {
        validarCategoriaAnalitica(lancamento.getCategoria());

        lancamento.setUsuario(getUsuarioLogado());
        Lancamento lancamentoSalvo = lancamentoRepository.save(lancamento);

        if (lancamentoSalvo.isPago()) {
            efetuarBaixa(lancamentoSalvo);
        }

        return lancamentoSalvo;
    }

    @Transactional
    public Lancamento atualizar(Long id, Lancamento dadosAtualizados) {
        Lancamento lancamentoExistente = buscarPorId(id);

        if (lancamentoExistente.isPago()) {
            throw new IllegalStateException("Não é permitido alterar um título que já foi liquidado/baixado.");
        }

        validarCategoriaAnalitica(dadosAtualizados.getCategoria());

        lancamentoExistente.setDescricao(dadosAtualizados.getDescricao());
        lancamentoExistente.setValor(dadosAtualizados.getValor());
        lancamentoExistente.setDataVencimento(dadosAtualizados.getDataVencimento());
        lancamentoExistente.setConta(dadosAtualizados.getConta());
        lancamentoExistente.setCategoria(dadosAtualizados.getCategoria());
        lancamentoExistente.setCentroCusto(dadosAtualizados.getCentroCusto());
        lancamentoExistente.setFavorecido(dadosAtualizados.getFavorecido());

        return lancamentoRepository.save(lancamentoExistente);
    }

    private void efetuarBaixa(Lancamento lancamento) {
        Conta conta = contaRepository.findById(lancamento.getConta().getId())
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada com o ID informado."));

        BigDecimal valor = lancamento.getValor();

        if ("RECEITA".equalsIgnoreCase(lancamento.getTipoLancamento())) {
            conta.setSaldoAtual(conta.getSaldoAtual().add(valor));
        } else if ("DESPESA".equalsIgnoreCase(lancamento.getTipoLancamento())) {
            conta.setSaldoAtual(conta.getSaldoAtual().subtract(valor));
        }

        contaRepository.save(conta);
        lancamento.setConta(conta);

        Movimentacao movimento = new Movimentacao();
        movimento.setDataHora(LocalDateTime.now());
        movimento.setValor(valor);
        movimento.setConta(conta);
        movimento.setLancamento(lancamento);
        movimento.setCategoria(lancamento.getCategoria());
        movimento.setCentroCusto(lancamento.getCentroCusto());

        if ("RECEITA".equalsIgnoreCase(lancamento.getTipoLancamento())) {
            movimento.setTipoMovimentacao("ENTRADA");
            movimento.setHistorico("Baixa automática: Recebimento de " + lancamento.getDescricao());
        } else {
            movimento.setTipoMovimentacao("SAIDA");
            movimento.setHistorico("Baixa automática: Pagamento de " + lancamento.getDescricao());
        }

        movimentacaoRepository.save(movimento);
    }
    @Transactional
    public Lancamento baixarLancamento(Long id) {
        Lancamento lancamento = buscarPorId(id);

        if (lancamento.isPago()) {
            throw new IllegalStateException("Este lançamento já encontra-se baixado/pago.");
        }

        lancamento.setPago(true);
        lancamento.setDataPagamento(LocalDate.now());

        Lancamento lancamentoAtualizado = lancamentoRepository.save(lancamento);
        efetuarBaixa(lancamentoAtualizado);

        return lancamentoAtualizado;
    }

    @Transactional
    public Lancamento estornarLancamento(Long id) {
        Lancamento lancamento = buscarPorId(id);

        if (!lancamento.isPago()) {
            throw new IllegalStateException("Apenas títulos já baixados/pagos podem ser estornados.");
        }

        List<Movimentacao> movimentacoes = movimentacaoRepository.findByLancamentoId(lancamento.getId());
        if (!movimentacoes.isEmpty()) {
            movimentacaoRepository.deleteAll(movimentacoes);
        }

        Conta conta = contaRepository.findById(lancamento.getConta().getId())
                .orElseThrow(() -> new IllegalArgumentException("Conta vinculada não encontrada."));

        BigDecimal valor = lancamento.getValor();
        if ("RECEITA".equalsIgnoreCase(lancamento.getTipoLancamento())) {
            conta.setSaldoAtual(conta.getSaldoAtual().subtract(valor));
        } else if ("DESPESA".equalsIgnoreCase(lancamento.getTipoLancamento())) {
            conta.setSaldoAtual(conta.getSaldoAtual().add(valor));
        }
        contaRepository.save(conta);

        lancamento.setPago(false);
        lancamento.setDataPagamento(null);

        return lancamentoRepository.save(lancamento);
    }

    public List<Lancamento> listarTodos() {
        Usuario usuarioLogado = getUsuarioLogado();
        return lancamentoRepository.findAll().stream()
                .filter(l -> l.getUsuario().getId().equals(usuarioLogado.getId()))
                .collect(Collectors.toList());
    }

    public List<Lancamento> filtrar(String tipoLancamento, LocalDate dataInicio, LocalDate dataFim,
            Long contaId, Long centroCustoId, Long categoriaId) {
        Usuario usuarioLogado = getUsuarioLogado();

        return lancamentoRepository.findAll().stream()
                .filter(l -> l.getUsuario().getId().equals(usuarioLogado.getId()))
                .filter(l -> tipoLancamento == null || tipoLancamento.isBlank() || l.getTipoLancamento().equalsIgnoreCase(tipoLancamento))
                .filter(l -> dataInicio == null || !l.getDataVencimento().isBefore(dataInicio))
                .filter(l -> dataFim == null || !l.getDataVencimento().isAfter(dataFim))
                .filter(l -> contaId == null || (l.getConta() != null && l.getConta().getId().equals(contaId)))
                .filter(l -> centroCustoId == null || (l.getCentroCusto() != null && l.getCentroCusto().getId().equals(centroCustoId)))
                .filter(l -> categoriaId == null || (l.getCategoria() != null && l.getCategoria().getId().equals(categoriaId)))
                .collect(Collectors.toList());
    }

    public Lancamento buscarPorId(Long id) {
        Lancamento lancamento = lancamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lançamento não encontrado com o ID: " + id));

        if (!lancamento.getUsuario().getId().equals(getUsuarioLogado().getId())) {
            throw new IllegalArgumentException("Acesso negado a este lançamento.");
        }
        return lancamento;
    }

    @Transactional
    public void deletar(Long id) {
        Lancamento lancamento = buscarPorId(id);
        if (lancamento.isPago()) {
            throw new IllegalStateException("Não é permitido excluir um lançamento que já foi baixado/pago.");
        }
        lancamentoRepository.delete(lancamento);
    }
}
