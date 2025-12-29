# Sistema de Pedidos VR - Teste Prático

Solução desenvolvida para o teste prático de Desenvolvedor Java Pleno/Sênior. O sistema consiste em um Backend (Spring Boot) para processamento assíncrono de pedidos e um Frontend (Java Swing) para envio e monitoramento em tempo real.

## 🚀 Tecnologias Utilizadas

* **Java 17** (LTS)
* **Spring Boot 3.2.x** (Web, AMQP)
* **RabbitMQ** (Mensageria e DLQ)
* **Java Swing** (Interface Gráfica)
* **Lombok** (Produtividade)

## ⚙️ Configuração

O projeto foi configurado para conectar automaticamente à instância do RabbitMQ fornecida no enunciado do teste (CloudAMQP).

* **Host:** `jaragua-01.lmq.cloudamqp.com`
* **User/Vhost:** `bjnuffmq`
* **Exchange de DLQ:** `dlq.exchange` (Configurada automaticamente na inicialização)

## 🛠️ Como Executar

Devido ao timebox de 3 horas, o projeto foi estruturado como um **Monolito Modular** (Backend e Frontend compartilham o mesmo repositório e DTOs, mas em pacotes distintos).

### 1. Executar o Backend
Execute a classe principal do Spring Boot:
* Classe: `br.com.vrsoft.pedidos.backend.BackendApplication`
* O servidor iniciará na porta **8080**.
* Swagger UI (Opcional): [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

### 2. Executar o Frontend (Cliente Desktop)
Com o backend rodando, execute a aplicação Swing em um novo terminal/console:
* Classe: `br.com.vrsoft.pedidos.frontend.FrontendApplication`
* A janela "Sistema de Pedidos [VR Software]" será aberta.

## 🧠 Decisões de Arquitetura

1.  **Dead Letter Queue (DLQ):** A fila `pedidos.entrada.candidato` foi configurada com argumentos `x-dead-letter-exchange` para garantir que mensagens rejeitadas (falhas de processamento) não sejam perdidas, sendo desviadas para a DLQ.
2.  **Assincronicidade:** A API segue o padrão *Fire-and-Forget*. O cliente recebe um `HTTP 202 Accepted` imediatamente, e o processamento pesado ocorre em background.
3.  **Polling Seguro:** O cliente Swing utiliza `ScheduledExecutorService` em thread separada para consultar o status, garantindo que a interface gráfica (EDT) nunca congele durante as requisições de rede.
4.  **Estrutura de Projeto:** Optou-se por um projeto único (Single Module) para eliminar a complexidade de gestão de múltiplos artefatos Maven dentro do limite de tempo, mantendo, contudo, separação de pacotes entre camadas.

## 🧪 Testes

Para executar os testes unitários (JUnit 5 + Mockito):
```bash
mvn test