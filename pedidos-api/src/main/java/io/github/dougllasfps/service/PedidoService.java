package io.github.dougllasfps.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.dougllasfps.domain.entity.Pedido;
import io.github.dougllasfps.domain.enums.StatusPedido;
import io.github.dougllasfps.rest.dto.PedidoDTO;

import java.util.Optional;

public interface PedidoService {
    Pedido salvar( PedidoDTO dto ) throws JsonProcessingException;
    Optional<Pedido> obterPedidoCompleto(Integer id);
    void atualizaStatus(Integer id, StatusPedido statusPedido);
}
