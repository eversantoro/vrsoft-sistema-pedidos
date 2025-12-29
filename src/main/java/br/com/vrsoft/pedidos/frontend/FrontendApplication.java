package br.com.vrsoft.pedidos.frontend;

import javax.swing.SwingUtilities;

public class FrontendApplication {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PedidoApp frame = new PedidoApp();
            frame.setVisible(true);
        });
    }
}