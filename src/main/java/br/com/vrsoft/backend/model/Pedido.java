package br.com.vrsoft.backend.model;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Pedido {
	private UUID id;
	private String produto;
	private int quantidade;
	private LocalDateTime dataCriacao;
}