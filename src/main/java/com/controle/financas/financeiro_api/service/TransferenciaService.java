package com.controle.financas.financeiro_api.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.controle.financas.financeiro_api.model.Conta;
import com.controle.financas.financeiro_api.model.Movimentacao;
import com.controle.financas.financeiro_api.model.Transferencia;
import com.controle.financas.financeiro_api.model.Usuario;
import com.controle.financas.financeiro_api.repository.ContaRepository;
import com.controle.financas.financeiro_api.repository.MovimentacaoRepository;
import com.controle.financas.financeiro_api.repository.TransferenciaRepository;
import com.controle.financas.financeiro_api.dto.TransferenciaDTO;

@Service
public class TransferenciaService {

    private final TransferenciaRepository transferenciaRepository;
    private final ContaRepository contaRepository;
    private final MovimentacaoRepository movimentacaoRepository;

    public TransferenciaService(TransferenciaRepository transferenciaRepository,
            ContaRepository contaRepository,
            MovimentacaoRepository movimentacaoRepository) {
        this.transferenciaRepository = transferenciaRepository;
        this.contaRepository = contaRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    private Usuario getUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Usuario) auth.getPrincipal();
    }

    @Transactional
    public Transferencia transferir(Transferencia transferencia) {
        BigDecimal valor = transferencia.getValor();

        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor da transferência deve ser maior que zero.");
        }

        Usuario usuarioLogado = getUsuarioLogado();

        Long origemId = Objects.requireNonNull(transferencia.getContaOrigem().getId(), "Conta origem é obrigatória.");
        Long destinoId = Objects.requireNonNull(transferencia.getContaDestino().getId(), "Conta destino é obrigatória.");

        Conta origem = contaRepository.findById(origemId)
                .orElseThrow(() -> new IllegalArgumentException("Conta origem não encontrada."));

        Conta destino = contaRepository.findById(destinoId)
                .orElseThrow(() -> new IllegalArgumentException("Conta destino não encontrada."));

        if (!origem.getUsuario().getId().equals(usuarioLogado.getId())
                || !destino.getUsuario().getId().equals(usuarioLogado.getId())) {
            throw new AccessDeniedException("Acesso negado: As contas devem pertencer ao seu usuário.");
        }

        if (origem.getId().equals(destino.getId())) {
            throw new IllegalArgumentException("A conta de origem não pode ser igual à conta de destino.");
        }

        if (transferencia.getDataTransferencia() == null) {
            transferencia.setDataTransferencia(LocalDate.now());
        }

        transferencia.setUsuario(usuarioLogado);

        origem.setSaldoAtual(origem.getSaldoAtual().subtract(valor));
        destino.setSaldoAtual(destino.getSaldoAtual().add(valor));

        contaRepository.save(origem);
        contaRepository.save(destino);

        transferencia.setContaOrigem(origem);
        transferencia.setContaDestino(destino);
        Transferencia transferenciaSalva = transferenciaRepository.save(transferencia);

        gerarMovimentacaoExtrato(origem, valor, "SAIDA", "Transferência enviada para " + destino.getNome(), transferenciaSalva);
        gerarMovimentacaoExtrato(destino, valor, "ENTRADA", "Transferência recebida de " + origem.getNome(), transferenciaSalva);

        return transferenciaSalva;
    }

    @Transactional(readOnly = true)
    public List<TransferenciaDTO> listarTodas() {
        Usuario usuarioLogado = getUsuarioLogado();
        return transferenciaRepository.findAll().stream()
                .filter(t -> t.getUsuario() != null && t.getUsuario().getId().equals(usuarioLogado.getId()))
                .sorted((a, b) -> {
                    LocalDate dataA = a.getDataTransferencia() != null ? a.getDataTransferencia() : LocalDate.MIN;
                    LocalDate dataB = b.getDataTransferencia() != null ? b.getDataTransferencia() : LocalDate.MIN;
                    return dataB.compareTo(dataA);
                })
                .map(TransferenciaDTO::new)
                .collect(Collectors.toList());
    }


    @Transactional
    public void excluirTransferencia(Long id) {
        Long transfId = Objects.requireNonNull(id, "ID da transferência não pode ser nulo.");
        Transferencia transferencia = transferenciaRepository.findById(transfId)
                .orElseThrow(() -> new IllegalArgumentException("Transferência não encontrada com o ID: " + transfId));

        if (!transferencia.getUsuario().getId().equals(getUsuarioLogado().getId())) {
            throw new AccessDeniedException("Acesso negado para excluir esta transferência.");
        }

        // 1. Remove as movimentações vinculadas no extrato (saída na origem e entrada no destino)
        List<Movimentacao> movimentacoes = movimentacaoRepository.findByTransferenciaId(transferencia.getId());
        if (!movimentacoes.isEmpty()) {
            movimentacaoRepository.deleteAll(movimentacoes);
        }

        // 2. Recompõe os saldos das duas contas envolvidas
        Long origemId = Objects.requireNonNull(transferencia.getContaOrigem().getId());
        Long destinoId = Objects.requireNonNull(transferencia.getContaDestino().getId());

        Conta origem = contaRepository.findById(origemId)
                .orElseThrow(() -> new IllegalArgumentException("Conta de origem não encontrada."));
        Conta destino = contaRepository.findById(destinoId)
                .orElseThrow(() -> new IllegalArgumentException("Conta de destino não encontrada."));

        BigDecimal valor = transferencia.getValor();
        origem.setSaldoAtual(origem.getSaldoAtual().add(valor));
        destino.setSaldoAtual(destino.getSaldoAtual().subtract(valor));

        contaRepository.save(origem);
        contaRepository.save(destino);

        // 3. Exclui o registro da transferência
        transferenciaRepository.delete(transferencia);
    }

    private void gerarMovimentacaoExtrato(Conta conta, BigDecimal valor, String tipo, String historico, Transferencia transf) {
        LocalDate dataRef = transf.getDataTransferencia() != null ? transf.getDataTransferencia() : LocalDate.now();

        Movimentacao movimento = new Movimentacao();
        movimento.setDataHora(dataRef.atTime(LocalTime.now()));
        movimento.setValor(valor);
        movimento.setConta(conta);
        movimento.setTipoMovimentacao(tipo);
        movimento.setHistorico(historico);
        movimento.setTransferencia(transf);
        movimento.setCategoria(null);
        movimento.setCentroCusto(null);

        movimentacaoRepository.save(movimento);
    }
}
