package com.controle.financas.financeiro_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.controle.financas.financeiro_api.model.CentroCusto;
import java.util.List;

public interface CentroCustoRepository extends JpaRepository<CentroCusto, Long> {
    List<CentroCusto> findByUsuarioId(Long usuarioId);
    List<CentroCusto> findByUsuarioIdAndTipoIn(Long usuarioId, List<String> tipos);
}
