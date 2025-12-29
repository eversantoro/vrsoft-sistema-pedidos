package br.com.vrsoft.pedidos.backend.controller;

import br.com.vrsoft.pedidos.backend.model.Pedido;
import br.com.vrsoft.pedidos.backend.service.PedidoService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Controlador REST para interface com o cliente Desktop.
 * Implementa padrão Fire-and-Forget (assíncrono) para criação de pedidos.
 */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

	private final PedidoService pedidoService;

	public PedidoController(PedidoService pedidoService) {
		this.pedidoService = pedidoService;
	}

	/**
     * Recebe um novo pedido para processamento.
     * <p>
     * Validações básicas são aplicadas (quantidade > 0).
     * Retorna imediatamente HTTP 202 (Accepted) indicando que o pedido foi enfileirado,
     * mas ainda não processado.
     *
     * @param pedido Payload JSON contendo produto e quantidade.
     * @return ResponseEntity com o Pedido enriquecido (ID e Data) ou Bad Request.
     */
	@PostMapping
	public ResponseEntity<?> criarPedido(@RequestBody Pedido pedido) {
		// Validação Simples
		if (pedido.getQuantidade() <= 0 || pedido.getProduto() == null || pedido.getProduto().isEmpty()) {
			return ResponseEntity.badRequest().build(); // HTTP 400
		}

		if (pedido.getId() == null) {
	        pedido.setId(UUID.randomUUID());
	    }
		
		pedido.setDataCriacao(LocalDateTime.now());

		pedidoService.registrarPedido(pedido);

		// Retorna HTTP 202 Accepted
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(pedido);
	}

	/**
     * Endpoint para Polling de status.
     * Utilizado pelo cliente Swing para atualizar a interface gráfica.
     * * @param id UUID do pedido.
     * @return String contendo o status atual (RECEBIDO, PROCESSANDO, SUCESSO, FALHA).
     */
	@GetMapping("/status/{id}")
	public ResponseEntity<String> getStatus(@PathVariable UUID id) {
		return ResponseEntity.ok(pedidoService.consultarStatus(id));
	}
}