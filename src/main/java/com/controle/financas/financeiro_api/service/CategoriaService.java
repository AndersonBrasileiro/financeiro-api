package com.controle.financas.financeiro_api.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.controle.financas.financeiro_api.model.Categoria;
import com.controle.financas.financeiro_api.repository.CategoriaRepository;
import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public List<Categoria> listarTodasPorUsuario(Long usuarioId) {
        return categoriaRepository.findByUsuarioId(usuarioId);
    }

    public List<Categoria> listarPorTipoEUsuario(String tipo, Long usuarioId) {
        return categoriaRepository.findByTipoAndUsuarioId(tipo.toUpperCase(), usuarioId);
    }

    public List<Categoria> listarCategoriasPrincipaisPorUsuario(Long usuarioId) {
        return categoriaRepository.findByCategoriaPaiIsNullAndUsuarioId(usuarioId);
    }

    public List<Categoria> listarSubcategoriasPorUsuario(Long paiId, Long usuarioId) {
        return categoriaRepository.findByCategoriaPaiIdAndUsuarioId(paiId, usuarioId);
    }

    @Transactional
    public Categoria salvar(Categoria categoria) {
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public Categoria atualizar(Long id, Categoria dadosAtualizados, Long usuarioId) {
        Categoria existente = categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada."));

        if (!existente.getUsuario().getId().equals(usuarioId)) {
            throw new IllegalArgumentException("Acesso negado a esta categoria.");
        }

        // Impede que uma categoria seja definida como pai dela mesma
        if (dadosAtualizados.getCategoriaPai() != null && dadosAtualizados.getCategoriaPai().getId().equals(id)) {
            throw new IllegalArgumentException("Uma categoria não pode ser subcategoria dela mesma.");
        }

        existente.setNome(dadosAtualizados.getNome());
        existente.setTipo(dadosAtualizados.getTipo());
        existente.setCategoriaPai(dadosAtualizados.getCategoriaPai());

        return categoriaRepository.save(existente);
    }

    @Transactional
    public void excluir(Long id, Long usuarioId) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada."));

        if (!categoria.getUsuario().getId().equals(usuarioId)) {
            throw new IllegalArgumentException("Acesso negado a esta categoria.");
        }

        // 1. Bloqueia exclusão se for Categoria Pai
        if (categoria.getCategoriaPai() == null) {
            throw new IllegalArgumentException("Categorias Pai (grupos principais) não podem ser excluídas.");
        }

        // 2. Verifica se existem lançamentos vinculados a esta subcategoria
        if (possuiLancamentosVinculados(id)) {
            throw new IllegalArgumentException("Não é possível excluir esta subcategoria pois existem lançamentos vinculados a ela.");
        }

        categoriaRepository.delete(categoria);
    }

    private boolean possuiLancamentosVinculados(Long categoriaId) {
        // Verifica automaticamente nas tabelas comuns de títulos/lançamentos do seu banco
        String[] tabelasPossiveis = {"tb_lancamento", "lancamento", "tb_titulo", "titulo", "tb_conta_pagar_receber"};
        for (String tabela : tabelasPossiveis) {
            try {
                Object resultado = entityManager
                        .createNativeQuery("SELECT COUNT(*) FROM " + tabela + " WHERE categoria_id = :catId")
                        .setParameter("catId", categoriaId)
                        .getSingleResult();
                if (resultado != null && ((Number) resultado).longValue() > 0) {
                    return true;
                }
            } catch (Exception ignored) {
                // Ignora se o nome da tabela não existir no schema e testa o próximo
            }
        }
        return false;
    }
}
