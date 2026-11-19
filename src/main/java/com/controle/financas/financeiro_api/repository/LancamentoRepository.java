package com.controle.financas.financeiro_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
import com.controle.financas.financeiro_api.model.Lancamento;
import java.math.BigDecimal;

//@Repository
public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {
    
    // Soma o valor de todos os lançamentos EM ABERTO de um determinado tipo (RECEITA ou DESPESA)
    @Query("SELECT COALESCE(SUM(l.valor), 0) FROM Lancamento l WHERE l.usuario.id = :usuarioId AND l.pago = false AND l.tipoLancamento = :tipo")
    BigDecimal somarValoresEmAberto(@Param("usuarioId") Long usuarioId, @Param("tipo") String tipo);
    
    boolean existsByCategoriaId(Long categoriaId);
}
