package br.com.vrsoft.pedidos.backend.service;

import br.com.vrsoft.pedidos.backend.config.RabbitConfig;
import br.com.vrsoft.pedidos.backend.model.Pedido;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Serviço responsável pela orquestração do processamento de pedidos.
 * Gerencia o ciclo de vida: Recepção -> Processamento Simulado -> Sucesso/Falha.
 */
@Service
public class PedidoService {

	private static final Logger logger = Logger.getLogger(PedidoService.class.getName());
	private final RabbitTemplate rabbitTemplate;

	// Manutenção de Status em Memória
	private final Map<UUID, String> statusMap = new ConcurrentHashMap<>();

	public PedidoService(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	/**
     * Registra o pedido inicial e o enfileira para processamento assíncrono.
     * Atualiza o status inicial para "RECEBIDO".
     *
     * @param pedido O DTO do pedido recebido via API.
     */
	public void registrarPedido(Pedido pedido) {
		statusMap.put(pedido.getId(), "RECEBIDO");
		// Publicação na fila de entrada
		rabbitTemplate.convertAndSend(RabbitConfig.QUEUE_ENTRADA, pedido);
	}

	public String consultarStatus(UUID id) {
		return statusMap.getOrDefault(id, "NAO_ENCONTRADO");
	}

	/**
     * Consumidor da fila de entrada. Simula um processamento complexo.
     * <p>
     * Regras de Negócio Simuladas:
     * <ul>
     * <li>Latency: Dorme de 1 a 3 segundos (Thread.sleep).</li>
     * <li>Resiliência: 20% de chance de falha aleatória.</li>
     * <li>DLQ: Em caso de erro, rejeita a mensagem sem requeue para envio à Dead Letter Queue.</li>
     * </ul>
     *
     * @param pedido O objeto desserializado automaticamente pelo Jackson/Spring AMQP.
     * @throws AmqpRejectAndDontRequeueException Exceção específica para acionar a DLQ no RabbitMQ.
     */
	@RabbitListener(queues = RabbitConfig.QUEUE_ENTRADA)
	public void processarPedido(Pedido pedido) {
		statusMap.put(pedido.getId(), "PROCESSANDO");
		logger.info("Iniciando processamento pedido: " + pedido.getId()); //

		try {
			// Simular processamento 1-3s
			Thread.sleep(1000 + (long) (Math.random() * 2000));

			// 20% de chance de erro
			if (Math.random() < 0.2) {
				throw new RuntimeException("ExcecaoDeProcessamento Simulada");
			}

			// Sucesso
			statusMap.put(pedido.getId(), "SUCESSO");
			Map<String, Object> statusMsg = Map.of("idPedido", pedido.getId(), "status", "SUCESSO", "dataProcessamento",
					LocalDateTime.now());
			// Publica na fila de sucesso
			rabbitTemplate.convertAndSend(RabbitConfig.QUEUE_SUCESSO, statusMsg);
			logger.info("Sucesso no pedido: " + pedido.getId());

		} catch (Exception e) {
			// Tratamento de Falha
			logger.severe("Falha no pedido " + pedido.getId() + ": " + e.getMessage());

			statusMap.put(pedido.getId(), "FALHA");

			Map<String, Object> errorMsg = Map.of("idPedido", pedido.getId(), "status", "FALHA", "mensagemErro",
					e.getMessage());
			// Publica na fila de falha
			rabbitTemplate.convertAndSend(RabbitConfig.QUEUE_FALHA, errorMsg);

			// Rejeitar para enviar para DLQ
			// AmqpRejectAndDontRequeueException diz ao Rabbit para não recolocar na fila original,
			// forçando o envio para a DLQ configurada no passo 2.
			throw new AmqpRejectAndDontRequeueException("Enviando para DLQ", e);
		}
	}
}