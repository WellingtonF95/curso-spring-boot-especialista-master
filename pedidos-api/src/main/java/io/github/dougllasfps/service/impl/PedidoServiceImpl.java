package io.github.dougllasfps.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.dougllasfps.domain.entity.Cliente;
import io.github.dougllasfps.domain.entity.ItemPedido;
import io.github.dougllasfps.domain.entity.Pedido;
import io.github.dougllasfps.domain.entity.Produto;
import io.github.dougllasfps.domain.enums.StatusPedido;
import io.github.dougllasfps.domain.repository.Clientes;
import io.github.dougllasfps.domain.repository.ItemsPedido;
import io.github.dougllasfps.domain.repository.Pedidos;
import io.github.dougllasfps.domain.repository.Produtos;
import io.github.dougllasfps.exception.PedidoNaoEncontradoException;
import io.github.dougllasfps.exception.RegraNegocioException;
import io.github.dougllasfps.rest.dto.ItemPedidoDTO;
import io.github.dougllasfps.rest.dto.PedidoDTO;
import io.github.dougllasfps.service.PedidoService;
import lombok.RequiredArgsConstructor;
import org.consumerPedido.PedidoAvro;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PedidoServiceImpl implements PedidoService {

    private final Pedidos repository;
    private final Clientes clientesRepository;
    private final Produtos produtosRepository;
    private final ItemsPedido itemsPedidoRepository;
    private final KafkaTemplate<String, PedidoAvro> kafkaTemplate;

    @Value("${spring.kafka.producer.topic}")
    private String topicPedidos;

    @Override
    @Transactional
    public Pedido salvar( PedidoDTO dto ) throws JsonProcessingException {
        Integer idCliente = dto.getCliente();
        Cliente cliente = clientesRepository
                .findById(idCliente)
                .orElseThrow(() -> new RegraNegocioException("Código de cliente inválido."));

        Pedido pedido = new Pedido();
        pedido.setTotal(dto.getTotal());
        pedido.setDataPedido(LocalDate.now());
        pedido.setCliente(cliente);
        pedido.setStatus(StatusPedido.REALIZADO);

        List<ItemPedido> itemsPedido = converterItems(pedido, dto.getItems());
        repository.save(pedido);
        itemsPedidoRepository.saveAll(itemsPedido);
        pedido.setItens(itemsPedido);
        enviaMensagemAoConsumerPedidoViaKafka(dto, dto.getItems());
        System.out.println("Sucesso ao enviar a mensagem ao ConsumerPedido");
        return pedido;
    }

    private void enviaMensagemAoConsumerPedidoViaKafka(PedidoDTO dto, List<ItemPedidoDTO> items) {
        List<PedidoAvro> avros = convertPedido(dto, items);

        avros.forEach(avro -> {
            kafkaTemplate.send(topicPedidos, avro);
            System.out.println("Sucesso ao enviar Avro ao Schema Registry para o consumerPedido");
        });
    }

    private static List<PedidoAvro> convertPedido(PedidoDTO pedidoDTO, List<ItemPedidoDTO> items) {
        List<PedidoAvro> avros = new ArrayList<>();

        items.forEach(item -> {
            PedidoAvro avro = new PedidoAvro();
            avro.setCliente(pedidoDTO.getCliente());
            avro.setTotal(pedidoDTO.getTotal().doubleValue());
            avro.setProduto(item.getProduto());
            avro.setQuantidade(item.getQuantidade());
            avro.setDocumento(pedidoDTO.getDocumento());
            avro.setFuncionario(pedidoDTO.getFuncionario());
            avro.setNumeroPedido(pedidoDTO.getNumeroPedido());

            avros.add(avro);
        });

        return avros;
    }

    @Override
    public Optional<Pedido> obterPedidoCompleto(Integer id) {
        return repository.findByIdFetchItens(id);
    }

    @Override
    @Transactional
    public void atualizaStatus( Integer id, StatusPedido statusPedido ) {
        repository
                .findById(id)
                .map( pedido -> {
                    pedido.setStatus(statusPedido);
                    return repository.save(pedido);
                }).orElseThrow(() -> new PedidoNaoEncontradoException() );
    }

    private List<ItemPedido> converterItems(Pedido pedido, List<ItemPedidoDTO> items){
        if(items.isEmpty()){
            throw new RegraNegocioException("Não é possível realizar um pedido sem items.");
        }

        return items
                .stream()
                .map( dto -> {
                    Integer idProduto = dto.getProduto();
                    Produto produto = produtosRepository
                            .findById(idProduto)
                            .orElseThrow(
                                    () -> new RegraNegocioException(
                                            "Código de produto inválido: "+ idProduto
                                    ));

                    ItemPedido itemPedido = new ItemPedido();
                    itemPedido.setQuantidade(dto.getQuantidade());
                    itemPedido.setPedido(pedido);
                    itemPedido.setProduto(produto);
                    return itemPedido;
                }).collect(Collectors.toList());

    }
}
