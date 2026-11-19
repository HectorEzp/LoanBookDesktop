package biblioteca;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class PrestamosPanel extends JPanel {
    private final Connection conn;

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Libro", "Usuario", "Préstamo", "Devolución", "Estado", "id_libro"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);

    private final JComboBox<Item> cbLibro = new JComboBox<>();
    private final JTextField txtUsuario = new JTextField();
    private final JTextField txtFecha = new JTextField();
    private final JTextField txtDev = new JTextField();
    private final JComboBox<String> cbEstado =
            new JComboBox<>(new String[]{"ACTIVO", "DEVUELTO", "VENCIDO"});

    public PrestamosPanel(Connection conn) {
        this.conn = conn;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.removeColumn(table.getColumnModel().getColumn(6)); // oculta id_libro
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) cargarFormulario();
        });
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridLayout(0, 4, 8, 6));
        form.add(new JLabel("Libro:"));                      form.add(cbLibro);
        form.add(new JLabel("Usuario:"));                    form.add(txtUsuario);
        form.add(new JLabel("Fecha préstamo (AAAA-MM-DD):")); form.add(txtFecha);
        form.add(new JLabel("Fecha devolución (opcional):")); form.add(txtDev);
        form.add(new JLabel("Estado:"));                     form.add(cbEstado);

        JButton bNuevo = new JButton("Registrar nuevo");
        JButton bActualizar = new JButton("Modificar seleccionado");
        JButton bEliminar = new JButton("Eliminar seleccionado");
        JButton bLimpiar = new JButton("Limpiar");
        JButton bRefrescar = new JButton("Actualizar / Refrescar tabla");

        bNuevo.addActionListener(e -> insertar());
        bActualizar.addActionListener(e -> modificar());
        bEliminar.addActionListener(e -> eliminar());
        bLimpiar.addActionListener(e -> limpiar());
        bRefrescar.addActionListener(e -> refrescar());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        botones.add(bNuevo); botones.add(bActualizar); botones.add(bEliminar);
        botones.add(bLimpiar); botones.add(bRefrescar);

        JPanel sur = new JPanel(new BorderLayout(0, 6));
        sur.add(form, BorderLayout.CENTER);
        sur.add(botones, BorderLayout.SOUTH);
        add(sur, BorderLayout.SOUTH);

        refrescar();
    }

    /** Recarga combo de libros y tabla de préstamos desde la base de datos. */
    public void refrescar() {
        try {
            cbLibro.removeAllItems();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id_libro, titulo FROM libros ORDER BY titulo")) {
                while (rs.next()) cbLibro.addItem(new Item(rs.getInt(1), rs.getString(2)));
            }
            model.setRowCount(0);
            String sql = "SELECT p.id_prestamo, l.titulo, p.nombre_usuario, p.fecha_prestamo, "
                    + "p.fecha_devolucion, p.estado, p.id_libro "
                    + "FROM prestamos p JOIN libros l ON l.id_libro = p.id_libro "
                    + "ORDER BY p.id_prestamo DESC";
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    model.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3),
                            rs.getDate(4), rs.getDate(5), rs.getString(6), rs.getInt(7)});
                }
            }
        } catch (SQLException e) {
            Main.error(this, e);
        }
    }

    private void cargarFormulario() {
        int r = table.getSelectedRow();
        if (r < 0) return;
        int idLibro = (Integer) model.getValueAt(r, 6);
        for (int i = 0; i < cbLibro.getItemCount(); i++) {
            if (cbLibro.getItemAt(i).id == idLibro) { cbLibro.setSelectedIndex(i); break; }
        }
        txtUsuario.setText(String.valueOf(model.getValueAt(r, 2)));
        txtFecha.setText(String.valueOf(model.getValueAt(r, 3)));
        Object dev = model.getValueAt(r, 4);
        txtDev.setText(dev == null ? "" : dev.toString());
        cbEstado.setSelectedItem(model.getValueAt(r, 5));
    }

    private void limpiar() {
        table.clearSelection();
        txtUsuario.setText(""); txtFecha.setText(""); txtDev.setText("");
        cbEstado.setSelectedIndex(0);
    }

    /** Valida el formulario. Devuelve false si hay datos inválidos. */
    private boolean validar() {
        if (cbLibro.getSelectedItem() == null) {
            aviso("No hay libros registrados. Registra un libro primero."); return false;
        }
        if (txtUsuario.getText().isBlank()) { aviso("Ingresa el nombre del usuario."); return false; }
        try {
            Date.valueOf(txtFecha.getText().trim());
            if (!txtDev.getText().isBlank()) Date.valueOf(txtDev.getText().trim());
        } catch (IllegalArgumentException e) {
            aviso("Las fechas deben tener el formato AAAA-MM-DD."); return false;
        }
        return true;
    }

    private void bind(PreparedStatement ps) throws SQLException {
        ps.setInt(1, ((Item) cbLibro.getSelectedItem()).id);
        ps.setString(2, txtUsuario.getText().trim());
        ps.setDate(3, Date.valueOf(txtFecha.getText().trim()));
        if (txtDev.getText().isBlank()) ps.setNull(4, Types.DATE);
        else ps.setDate(4, Date.valueOf(txtDev.getText().trim()));
        ps.setString(5, (String) cbEstado.getSelectedItem());
    }

    private void insertar() {
        if (!validar()) return;
        String sql = "INSERT INTO prestamos (id_libro, nombre_usuario, fecha_prestamo, "
                + "fecha_devolucion, estado) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps);
            ps.executeUpdate();
            limpiar(); refrescar();
        } catch (SQLException e) { Main.error(this, e); }
    }

    private void modificar() {
        int r = table.getSelectedRow();
        if (r < 0) { aviso("Selecciona un préstamo de la tabla."); return; }
        if (!validar()) return;
        int id = (Integer) model.getValueAt(r, 0);
        String sql = "UPDATE prestamos SET id_libro=?, nombre_usuario=?, fecha_prestamo=?, "
                + "fecha_devolucion=?, estado=? WHERE id_prestamo=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps);
            ps.setInt(6, id);
            ps.executeUpdate();
            refrescar();
        } catch (SQLException e) { Main.error(this, e); }
    }

    private void eliminar() {
        int r = table.getSelectedRow();
        if (r < 0) { aviso("Selecciona un préstamo de la tabla."); return; }
        int id = (Integer) model.getValueAt(r, 0);
        if (JOptionPane.showConfirmDialog(this, "¿Eliminar el préstamo #" + id + "?",
                "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM prestamos WHERE id_prestamo=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
            limpiar(); refrescar();
        } catch (SQLException e) { Main.error(this, e); }
    }

    private void aviso(String m) {
        JOptionPane.showMessageDialog(this, m, "Aviso", JOptionPane.WARNING_MESSAGE);
    }
}
