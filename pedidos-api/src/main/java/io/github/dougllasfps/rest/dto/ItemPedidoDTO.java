package io.github.dougllasfps.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemPedidoDTO implements Serializable {
    private Integer produto;
    private Integer quantidade;
}
