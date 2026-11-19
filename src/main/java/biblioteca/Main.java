package biblioteca;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {
    private static SshTunnel tunnel;
    private static Connection conn;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }

        Config cfg;
        try {
            cfg = new Config("config.properties");
            tunnel = new SshTunnel();
            tunnel.open(cfg);                       // 1) túnel SSH
            conn = connect(cfg);                    // 2) JDBC a través del túnel
        } catch (Exception e) {
            cerrarTodo();
            JOptionPane.showMessageDialog(null,
                    "No se pudo iniciar la aplicación:\n" + e.getMessage(),
                    "Error de conexión", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Biblioteca · Préstamo de libros (Escritorio)");
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

            PrestamosPanel prestamos = new PrestamosPanel(conn);
            LibrosPanel libros = new LibrosPanel(conn);

            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Préstamos", prestamos);
            tabs.addTab("Libros", libros);
            tabs.addChangeListener(e -> {
                if (tabs.getSelectedComponent() == prestamos) prestamos.refrescar();
                else libros.refrescar();
            });

            frame.add(tabs, BorderLayout.CENTER);
            frame.setSize(950, 600);
            frame.setLocationRelativeTo(null);

            // Al salir: cerrar JDBC y luego la sesión SSH
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    cerrarTodo();
                    frame.dispose();
                    System.exit(0);
                }
            });
            frame.setVisible(true);
        });
    }

    private static Connection connect(Config c) throws SQLException {
        String url = "jdbc:mysql://127.0.0.1:" + c.getInt("local.port") + "/" + c.get("db.name")
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=8000";
        return DriverManager.getConnection(url, c.get("db.user"), c.get("db.password"));
    }

    private static void cerrarTodo() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("[JDBC] Conexión cerrada.");
            }
        } catch (SQLException ignored) { }
        if (tunnel != null) tunnel.close();
    }

    static void error(Component parent, Exception e) {
        JOptionPane.showMessageDialog(parent, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
