package biblioteca;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class LibrosPanel extends JPanel {
    private final Connection conn;

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Título", "Autor", "ISBN", "Ejemplares"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);

    private final JTextField txtTitulo = new JTextField();
    private final JTextField txtAutor = new JTextField();
    private final JTextField txtIsbn = new JTextField();
    private final JTextField txtEjemplares = new JTextField("1");

    public LibrosPanel(Connection conn) {
        this.conn = conn;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) cargarFormulario();
        });
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridLayout(0, 4, 8, 6));
        form.add(new JLabel("Título:"));     form.add(txtTitulo);
        form.add(new JLabel("Autor:"));      form.add(txtAutor);
        form.add(new JLabel("ISBN:"));       form.add(txtIsbn);
        form.add(new JLabel("Ejemplares:")); form.add(txtEjemplares);

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

    public void refrescar() {
        model.setRowCount(0);
        String sql = "SELECT id_libro, titulo, autor, isbn, ejemplares_disponibles "
                + "FROM libros ORDER BY id_libro DESC";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                model.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getInt(5)});
            }
        } catch (SQLException e) {
            Main.error(this, e);
        }
    }

    private void cargarFormulario() {
        int r = table.getSelectedRow();
        if (r < 0) return;
        txtTitulo.setText(String.valueOf(model.getValueAt(r, 1)));
        txtAutor.setText(String.valueOf(model.getValueAt(r, 2)));
        txtIsbn.setText(String.valueOf(model.getValueAt(r, 3)));
        txtEjemplares.setText(String.valueOf(model.getValueAt(r, 4)));
    }

    private void limpiar() {
        table.clearSelection();
        txtTitulo.setText(""); txtAutor.setText(""); txtIsbn.setText(""); txtEjemplares.setText("1");
    }

    private boolean validar() {
        if (txtTitulo.getText().isBlank() || txtAutor.getText().isBlank() || txtIsbn.getText().isBlank()) {
            aviso("Título, autor e ISBN son obligatorios."); return false;
        }
        try {
            if (Integer.parseInt(txtEjemplares.getText().trim()) < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            aviso("Ejemplares debe ser un número entero mayor o igual a 0."); return false;
        }
        return true;
    }

    private void bind(PreparedStatement ps) throws SQLException {
        ps.setString(1, txtTitulo.getText().trim());
        ps.setString(2, txtAutor.getText().trim());
        ps.setString(3, txtIsbn.getText().trim());
        ps.setInt(4, Integer.parseInt(txtEjemplares.getText().trim()));
    }

    private void insertar() {
        if (!validar()) return;
        String sql = "INSERT INTO libros (titulo, autor, isbn, ejemplares_disponibles) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps);
            ps.executeUpdate();
            limpiar(); refrescar();
        } catch (SQLIntegrityConstraintViolationException e) {
            aviso("Ya existe un libro con ese ISBN.");
        } catch (SQLException e) { Main.error(this, e); }
    }

    private void modificar() {
        int r = table.getSelectedRow();
        if (r < 0) { aviso("Selecciona un libro de la tabla."); return; }
        if (!validar()) return;
        int id = (Integer) model.getValueAt(r, 0);
        String sql = "UPDATE libros SET titulo=?, autor=?, isbn=?, ejemplares_disponibles=? WHERE id_libro=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps);
            ps.setInt(5, id);
            ps.executeUpdate();
            refrescar();
        } catch (SQLIntegrityConstraintViolationException e) {
            aviso("Ya existe otro libro con ese ISBN.");
        } catch (SQLException e) { Main.error(this, e); }
    }

    private void eliminar() {
        int r = table.getSelectedRow();
        if (r < 0) { aviso("Selecciona un libro de la tabla."); return; }
        int id = (Integer) model.getValueAt(r, 0);
        if (JOptionPane.showConfirmDialog(this, "¿Eliminar el libro #" + id + "?",
                "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM libros WHERE id_libro=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
            limpiar(); refrescar();
        } catch (SQLIntegrityConstraintViolationException e) {
            aviso("No se puede eliminar: el libro tiene préstamos asociados.");
        } catch (SQLException e) { Main.error(this, e); }
    }

    private void aviso(String m) {
        JOptionPane.showMessageDialog(this, m, "Aviso", JOptionPane.WARNING_MESSAGE);
    }
}
