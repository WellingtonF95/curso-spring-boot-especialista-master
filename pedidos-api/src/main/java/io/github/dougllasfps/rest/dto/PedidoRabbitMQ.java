package io.github.dougllasfps.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PedidoRabbitMQ {

    private Integer idCliente;
    private BigDecimal total;
    private Integer idProduto;
    private Integer quantidade;
}