package io.github.dougllasfps.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dougllasfps.constants.RabbitConstants;
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
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageBuilderSupport;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, PedidoDTO> kafkaTemplate;

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
        System.out.println("Envia mensagem ao ConsumerPedido");
        enviaMensagemAoConsumerPedido(RabbitConstants.QUEUE_PEDIDO, dto);
        enviaMensagemAoConsumerPedidoViaKafka(dto);
        System.out.println("Sucesso ao enviar a mensagem ao ConsumerPedido");
        return pedido;
    }

    private void enviaMensagemAoConsumerPedido(String nameQueue, PedidoDTO dto) throws JsonProcessingException {
        byte[] bytes = objectMapper.writeValueAsString(dto).getBytes();
        MessageBuilderSupport<Message> messageBuilderSupport = MessageBuilder.withBody(bytes)
                .setContentType(MessageProperties.CONTENT_TYPE_JSON);
        Message message = messageBuilderSupport.build();
        rabbitTemplate.send(nameQueue, message);
    }

    private void enviaMensagemAoConsumerPedidoViaKafka(PedidoDTO dto) {
        kafkaTemplate.send(topicPedidos,dto);
        System.out.println("Sucesso ao enviar ao Kafka do consumerPedido");
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
