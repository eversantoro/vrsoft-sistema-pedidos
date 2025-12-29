package br.com.vrsoft.pedidos.frontend;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Cliente Desktop Swing para o Sistema de Pedidos.
 * Responsável pelo envio de requisições e monitoramento de status via Polling Assíncrono.
 */
public class PedidoApp extends JFrame {

    private JTextField txtProduto;
    private JTextField txtQuantidade;
    private DefaultTableModel tableModel;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Map para controlar quais IDs precisamos monitorar via Polling
    private final Map<String, Integer> pedidosMonitorados = new ConcurrentHashMap<>(); // ID -> RowIndex

    public PedidoApp() {
        setTitle("Sistema de Pedidos Assíncrono [VR Software]");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initUI();
        iniciarPolling(); // Inicia o loop de verificação
    }

    private void initUI() {
        // Layout básico
        JPanel panelForm = new JPanel(new GridLayout(3, 2));
        panelForm.add(new JLabel("Produto:"));
        txtProduto = new JTextField();
        panelForm.add(txtProduto);

        panelForm.add(new JLabel("Quantidade:"));
        txtQuantidade = new JTextField();
        panelForm.add(txtQuantidade);

        JButton btnEnviar = new JButton("Enviar Pedido");
        panelForm.add(btnEnviar);

        // Tabela para exibir status (JTable é melhor que JTextArea para dados estruturados)
        String[] columns = {"ID", "Produto", "Status"};
        tableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        setLayout(new BorderLayout());
        add(panelForm, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Ação do Botão
        btnEnviar.addActionListener(e -> enviarPedido());
    }

    private void enviarPedido() {
        try {
            String produto = txtProduto.getText();
            int qtd = Integer.parseInt(txtQuantidade.getText());
            UUID id = UUID.randomUUID(); // Gera UUID no cliente

            // JSON Payload
            String json = String.format("{\"id\":\"%s\", \"produto\":\"%s\", \"quantidade\":%d}", id, produto, qtd);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/pedidos"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            // Chamada assíncrona para não travar a UI no clique
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 202) {
                            // Atualização da GUI na EDT
                            SwingUtilities.invokeLater(() -> {
                                adicionarNaTabela(id.toString(), produto, "ENVIADO, AGUARDANDO PROCESSO"); // 
                                limparCampos();
                            });
                        } else {
                            SwingUtilities.invokeLater(() -> 
                                JOptionPane.showMessageDialog(this, "Erro ao enviar: " + response.statusCode()));
                        }
                    })
                    .exceptionally(ex -> {
                         SwingUtilities.invokeLater(() -> 
                             JOptionPane.showMessageDialog(this, "Erro de Conexão: " + ex.getMessage())); // 
                         return null;
                    });

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantidade inválida!");
        }
    }

    private void adicionarNaTabela(String id, String produto, String status) {
        tableModel.addRow(new Object[]{id, produto, status});
        // Adiciona ao map de monitoramento (ID -> Índice da linha)
        pedidosMonitorados.put(id, tableModel.getRowCount() - 1);
    }
    
    private void limparCampos() {
        txtProduto.setText("");
        txtQuantidade.setText("");
    }

    /**
     * Inicia o mecanismo de Polling para atualização da interface.
     * <p>
     * Utiliza um {@link ScheduledExecutorService} para realizar chamadas HTTP em background
     * a cada 3 segundos, garantindo que a GUI não congele.
     * As atualizações visuais são devolvidas para a Event Dispatch Thread (EDT) 
     * via {@link SwingUtilities#invokeLater(Runnable)}.
     */
    private void iniciarPolling() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        
        // Executa a cada 3 segundos
        scheduler.scheduleAtFixedRate(() -> {
            // Itera sobre pedidos que ainda não finalizaram
            pedidosMonitorados.forEach((id, rowIndex) -> {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8080/api/pedidos/status/" + id))
                            .GET()
                            .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    String status = response.body(); // O corpo é a string do status (ex: "SUCESSO")

                    // Se mudou para final, atualiza e remove do polling
                    if ("SUCESSO".equals(status) || "FALHA".equals(status)) {
                        SwingUtilities.invokeLater(() -> {
                            tableModel.setValueAt(status, rowIndex, 2);
                        });
                        pedidosMonitorados.remove(id); // Para de monitorar este ID
                    } else {
                        // Opcional: Atualizar status intermediário "PROCESSANDO"
                        SwingUtilities.invokeLater(() -> {
                            tableModel.setValueAt(status, rowIndex, 2);
                        });
                    }

                } catch (Exception e) {
                    System.err.println("Erro no polling: " + e.getMessage());
                }
            });
        }, 1, 3, TimeUnit.SECONDS);
    }

}