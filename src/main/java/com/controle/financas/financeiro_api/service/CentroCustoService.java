package com.controle.financas.financeiro_api.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.controle.financas.financeiro_api.model.CentroCusto;
import com.controle.financas.financeiro_api.model.Usuario;
import com.controle.financas.financeiro_api.repository.CentroCustoRepository;
import com.controle.financas.financeiro_api.repository.LancamentoRepository;

@Service
public class CentroCustoService {

    private final CentroCustoRepository centroCustoRepository;
    private final LancamentoRepository lancamentoRepository;

    public CentroCustoService(CentroCustoRepository centroCustoRepository, LancamentoRepository lancamentoRepository) {
        this.centroCustoRepository = centroCustoRepository;
        this.lancamentoRepository = lancamentoRepository;
    }

    public List<CentroCusto> listarTodosDoUsuario() {
        Usuario usuarioLogado = getUsuarioAutenticado();
        return centroCustoRepository.findByUsuarioId(usuarioLogado.getId());
    }

    @Transactional
    public CentroCusto salvar(CentroCusto centroCusto) {
        Usuario usuarioLogado = getUsuarioAutenticado();
        centroCusto.setUsuario(usuarioLogado);
        centroCusto.setTipo(centroCusto.getTipo().toUpperCase());
        if (centroCusto.getAtivo() == null) {
            centroCusto.setAtivo(true);
        }
        return centroCustoRepository.save(centroCusto);
    }

    @Transactional
    public CentroCusto atualizar(Long id, CentroCusto dadosAtualizados) {
        Usuario usuarioLogado = getUsuarioAutenticado();
        CentroCusto existente = centroCustoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Centro de custo não encontrado."));

        if (!existente.getUsuario().getId().equals(usuarioLogado.getId())) {
            throw new IllegalArgumentException("Acesso negado a este centro de custo.");
        }

        existente.setNome(dadosAtualizados.getNome());
        existente.setTipo(dadosAtualizados.getTipo().toUpperCase());
        return centroCustoRepository.save(existente);
    }

    @Transactional
    public CentroCusto alternarStatus(Long id) {
        Usuario usuarioLogado = getUsuarioAutenticado();
        CentroCusto existente = centroCustoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Centro de custo não encontrado."));

        if (!existente.getUsuario().getId().equals(usuarioLogado.getId())) {
            throw new IllegalArgumentException("Acesso negado a este centro de custo.");
        }

        boolean statusAtual = existente.getAtivo() == null || existente.getAtivo();
        existente.setAtivo(!statusAtual);
        return centroCustoRepository.save(existente);
    }

    @Transactional
    public void excluir(Long id) {
        Usuario usuarioLogado = getUsuarioAutenticado();
        CentroCusto existente = centroCustoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Centro de custo não encontrado."));

        if (!existente.getUsuario().getId().equals(usuarioLogado.getId())) {
            throw new IllegalArgumentException("Acesso negado a este centro de custo.");
        }

        if (lancamentoRepository.existsByCentroCustoId(id)) {
            throw new IllegalArgumentException("Não é possível excluir este centro de custo pois existem lançamentos vinculados a ele. Você pode apenas desativá-lo.");
        }

        centroCustoRepository.delete(existente);
    }

    /**
     * Retorna apenas os centros ATIVOS para os seletores de Contas a Pagar e Contas a Receber
     */
    public List<CentroCusto> listarPorFluxoELogado(String fluxoLancamento) {
        Usuario usuarioLogado = getUsuarioAutenticado();
        String fluxo = fluxoLancamento.toUpperCase();

        if (!"DESPESA".equals(fluxo) && !"RECEITA".equals(fluxo)) {
            throw new IllegalArgumentException("Fluxo de lançamento inválido. Use estritamente 'DESPESA' ou 'RECEITA'.");
        }

        return centroCustoRepository.findByUsuarioIdAndTipoIn(
                usuarioLogado.getId(),
                Collections.singletonList(fluxo)
        ).stream()
         .filter(c -> c.getAtivo() == null || c.getAtivo())
         .collect(Collectors.toList());
    }

    private Usuario getUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Usuario) auth.getPrincipal();
    }
}
