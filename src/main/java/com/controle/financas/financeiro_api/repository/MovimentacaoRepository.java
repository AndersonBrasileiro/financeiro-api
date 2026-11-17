package com.controle.financas.financeiro_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
import com.controle.financas.financeiro_api.model.Movimentacao;
import java.util.List;

//@Repository
public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long> {
    
    // Puxa o extrato de uma conta específica ordenado pelas datas mais recentes
    List<Movimentacao> findByContaIdOrderByDataHoraDesc(Long contaId);
    // Localiza a movimentação gerada pela baixa de um lançamento específico (para estorno)
    List<Movimentacao> findByLancamentoId(Long lancamentoId);
}

