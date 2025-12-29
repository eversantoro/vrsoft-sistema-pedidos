package br.com.vrsoft.backend;

import br.com.vrsoft.backend.config.RabbitConfig;
import br.com.vrsoft.backend.model.Pedido;
import br.com.vrsoft.backend.service.PedidoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

	@Mock
	private RabbitTemplate rabbitTemplate;

	@InjectMocks
	private PedidoService pedidoService;

	@Test
	void devePublicarMensagemNaFilaCorretamente() {
		// Cenário
		Pedido pedido = new Pedido(UUID.randomUUID(), "Notebook", 1, null);

		// Ação
		pedidoService.registrarPedido(pedido);

		// Verificação - Verifica se o convertAndSend foi chamado com a fila correta e o objeto correto
		verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.QUEUE_ENTRADA), eq(pedido));
	}
}