package br.com.vrsoft.pedidos.backend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração da infraestrutura de mensageria RabbitMQ.
 * Define a topologia de Filas, Exchanges e Bindings, incluindo a estratégia de Dead Letter Queue (DLQ).
 */
@Configuration
public class RabbitConfig {

	public static final String QUEUE_ENTRADA = "pedidos.entrada.candidato";
	public static final String QUEUE_DLQ = "pedidos.entrada.candidato.dlq";
	public static final String QUEUE_SUCESSO = "pedidos.status.sucesso.candidato";
	public static final String QUEUE_FALHA = "pedidos.status.falha.candidato";
	public static final String EXCHANGE_DLQ = "dlq.exchange";

	// Configuração da conexão com as credenciais fornecidas
	@Bean
	public CachingConnectionFactory connectionFactory() {
		CachingConnectionFactory connectionFactory = new CachingConnectionFactory("jaragua-01.lmq.cloudamqp.com");
		connectionFactory.setUsername("bjnuffmq");
		connectionFactory.setPassword("gj-YQIiEXyfxQxjsZtiYDKeXIT8ppUq7");
		connectionFactory.setVirtualHost("bjnuffmq");
		return connectionFactory;
	}

	/**
     * Define a fila de entrada principal com argumentos de DLQ.
     * <p>
     * Se uma mensagem for rejeitada (nack/reject) sem requeue, ela será enviada
     * automaticamente para o exchange 'dlq.exchange' com a routing key definida.
     */
	@Bean
	public Queue filaEntrada() {
		return QueueBuilder.durable(QUEUE_ENTRADA).withArgument("x-dead-letter-exchange", EXCHANGE_DLQ) // Envia para este exchange em caso de reject
				.withArgument("x-dead-letter-routing-key", QUEUE_DLQ) // Com esta routing key
				.build();
	}

	@Bean
	public Queue filaDlq() {
		return QueueBuilder.durable(QUEUE_DLQ).build();
	}

	@Bean
	public Queue filaSucesso() {
		return QueueBuilder.durable(QUEUE_SUCESSO).build();
	}

	@Bean
	public Queue filaFalha() {
		return QueueBuilder.durable(QUEUE_FALHA).build();
	}

	@Bean
	public DirectExchange dlqExchange() {
		return new DirectExchange(EXCHANGE_DLQ);
	}

	@Bean
	public Binding dlqBinding() {
		return BindingBuilder.bind(filaDlq()).to(dlqExchange()).with(QUEUE_DLQ);
	}

	// Converter para JSON automaticamente
	@Bean
	public org.springframework.amqp.support.converter.MessageConverter jsonMessageConverter() {
		return new org.springframework.amqp.support.converter.Jackson2JsonMessageConverter();
	}
}