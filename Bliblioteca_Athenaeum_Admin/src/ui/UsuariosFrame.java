/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package ui;

import api.ApiClient;
import api.Session;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableCellRenderer;

import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import java.util.Date;
import java.util.Calendar;
/**
 *
 * @author elsni
 */
public class UsuariosFrame extends javax.swing.JFrame {
    private final Gson gson = new Gson();
    private DefaultTableModel model;
    
    private boolean fechaNacVacia = true;
    
    private JFormattedTextField tfFecha; // referencia al textfield del spinner

    // Para volver al menú
    private final JFrame parent;

    private final DateTimeFormatter fmtTabla = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
    // Formatos de fecha
    private final DateTimeFormatter fmtDB = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public UsuariosFrame(JFrame parent) {
        this.parent = parent;
        initComponents();
        aplicarDiseno();
        initFechaSpinner(); 
        initComboBoxes();
        initTable();
        cargarUsuarios();
        txtId.setEnabled(false);
    }

    public UsuariosFrame() { // para pruebas si lo corres directo
        this.parent = null;
        initComponents();
        initFechaSpinner(); 
        aplicarDiseno();
        initComboBoxes();
        initTable();
        cargarUsuarios();
        txtId.setEnabled(false);
    }
    
    private String getFechaSpinnerYYYYMMDD() {
        try {
            if (fechaNacVacia) return "";

            Date d = (Date) spFechaNac.getValue();
            LocalDate ld = new java.sql.Date(d.getTime()).toLocalDate();
            return ld.format(fmtDB); // yyyy-MM-dd
        } catch (Exception e) {
            return "";
        }
    }
    
    private String normalizarDui(String duiRaw) {
        if (duiRaw == null) return "";
        // quita espacios y guiones (ej: 01234567-8 -> 012345678)
        return duiRaw.trim().replace("-", "").replace(" ", "");
    }

    private boolean validarDuiSiViene(String duiRaw) {
        String dui = normalizarDui(duiRaw);

        // opcional: si está vacío, no validamos
        if (dui.isEmpty()) return true;

        // solo números
        if (!dui.matches("\\d+")) {
            JOptionPane.showMessageDialog(this,
                    "El DUI solo debe contener números.\nEjemplo: 01234567-8",
                    "DUI inválido",
                    JOptionPane.ERROR_MESSAGE);
            txtDui.requestFocus();
            return false;
        }

        // 9 dígitos (formato típico SV)
        if (dui.length() != 9) {
            JOptionPane.showMessageDialog(this,
                    "El DUI debe tener 9 dígitos.\nEjemplo: 01234567-8",
                    "DUI inválido",
                    JOptionPane.ERROR_MESSAGE);
            txtDui.requestFocus();
            return false;
        }

        return true;
    }
    
    private void mostrarError(String titulo, Exception ex) {
        String msg = (ex.getMessage() == null) ? "Error desconocido" : ex.getMessage();

        if (msg.contains("HTTP 400")) msg = "Datos inválidos (400). Revisa campos.";
        else if (msg.contains("HTTP 401")) msg = "Sesión inválida. Vuelve a iniciar sesión.";
        else if (msg.contains("HTTP 403")) msg = "No tienes permisos / usuario suspendido (403).";
        else if (msg.contains("HTTP 404")) msg = "No encontrado (404).";
        else if (msg.contains("HTTP 500")) msg = "Error del servidor (500).";

        JOptionPane.showMessageDialog(this, msg, titulo, JOptionPane.ERROR_MESSAGE);
    }
    
    private void setSpinnerFecha(String s) {
        try {
            if (s == null || s.trim().isEmpty()) {
                limpiarFecha();
                return;
            }

            s = s.trim();

            LocalDate d;
            if (s.contains("/")) {
                // viene de tabla: dd/MM/yyyy
                DateTimeFormatter fmtUI = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                d = LocalDate.parse(s, fmtUI);
            } else {
                // viene DB: yyyy-MM-dd
                if (s.length() >= 10) s = s.substring(0, 10);
                d = LocalDate.parse(s, fmtDB);
            }

            java.sql.Date date = java.sql.Date.valueOf(d);
            ponerFecha(date);

        } catch (Exception ex) {
            limpiarFecha();
        }
    }

    private Date maxFecha18; // guardamos la fecha máxima permitida (hoy - 18 años)

    private void initFechaSpinner() {
        Calendar min = Calendar.getInstance();
        min.set(1900, Calendar.JANUARY, 1);

        // Máximo permitido: hoy - 18 años
        Calendar max = Calendar.getInstance();
        max.add(Calendar.YEAR, -18);
        maxFecha18 = max.getTime();

        SpinnerDateModel modelFecha = new SpinnerDateModel(
                maxFecha18,        // valor inicial
                min.getTime(),     // mínimo
                maxFecha18,        // máximo
                Calendar.DAY_OF_MONTH
        );

        spFechaNac.setModel(modelFecha);

        JSpinner.DateEditor editor = new JSpinner.DateEditor(spFechaNac, "dd/MM/yyyy");
        spFechaNac.setEditor(editor);

        tfFecha = editor.getTextField();
        tfFecha.setEditable(false);
        tfFecha.setFocusable(false);

        // ✅ Si está vacío y tocan el spinner, se activa con la fecha máxima (hoy - 18)
        spFechaNac.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) {
                if (fechaNacVacia) ponerFecha(maxFecha18);
            }
        });

        // ✅ Si usan flechas, se activa y refresca
        spFechaNac.addChangeListener(e -> {
            if (fechaNacVacia) fechaNacVacia = false;
            refrescarTextoFecha();
        });

        limpiarFecha(); // inicia vacío
    }
    
    private void refrescarTextoFecha() {
        if (tfFecha == null) return;

        if (fechaNacVacia) {
            tfFecha.setText("");
            return;
        }

        Date d = (Date) spFechaNac.getValue();
        LocalDate ld = new java.sql.Date(d.getTime()).toLocalDate();
        tfFecha.setText(ld.format(fmtTabla)); // dd/MM/yyyy
    }

    private void limpiarFecha() {
        fechaNacVacia = true;

        // ✅ dummy seguro (siempre dentro del rango del modelo)
        if (maxFecha18 != null) spFechaNac.setValue(maxFecha18);

        refrescarTextoFecha(); // deja vacío visual
    }

    private void ponerFecha(Date d) {
        if (d == null) {
            limpiarFecha();
            return;
        }
        fechaNacVacia = false;
        spFechaNac.setValue(d);
        refrescarTextoFecha();
    }
    
    private boolean esMayorDeEdad() {
        if (fechaNacVacia) return true; // si no hay fecha, no validamos

        Date d = (Date) spFechaNac.getValue();
        LocalDate nacimiento = new java.sql.Date(d.getTime()).toLocalDate();
        LocalDate hoy = LocalDate.now();

        int edad = java.time.Period.between(nacimiento, hoy).getYears();

        return edad >= 18;
    }
    
    private void aplicarDiseno() {    
        // Paleta Athenaeum
        Color maroon = new Color(125, 15, 25);
        Color maroonDark = new Color(95, 10, 18);
        Color cream  = new Color(249, 243, 238);
        Color cream2 = new Color(245, 236, 228);
        Color borde  = new Color(220, 210, 200);

        // Fondo general + padding (se siente “app”)
        getContentPane().setBackground(cream);
        getRootPane().setBorder(new EmptyBorder(16, 16, 16, 16));

        // Header (título arriba)
        jLabel11.setForeground(maroon);
        jLabel11.setFont(new Font("Serif", Font.BOLD, 28));
        jLabel11.setHorizontalAlignment(SwingConstants.CENTER);

        // Labels del form
        Font labelFont = new Font("SansSerif", Font.BOLD, 13);
        for (JLabel lb : new JLabel[]{jLabel1,jLabel2,jLabel3,jLabel4,jLabel5,jLabel6,jLabel7,jLabel8,jLabel9,jLabel10}) {
            lb.setForeground(maroonDark);
            lb.setFont(labelFont);
        }

        // Bordes de inputs
        CompoundBorder inputBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borde, 1),
                new EmptyBorder(8, 10, 8, 10)
        );

        Font inputFont = new Font("SansSerif", Font.PLAIN, 13);

        // Inputs
        txtId.setFont(inputFont);
        txtNombres.setFont(inputFont);
        txtApellidos.setFont(inputFont);
        txtCorreo.setFont(inputFont);
        txtTelefono.setFont(inputFont);
        txtDui.setFont(inputFont);
        txtPass.setFont(inputFont);

        txtId.setBorder(inputBorder);
        txtNombres.setBorder(inputBorder);
        txtApellidos.setBorder(inputBorder);
        txtCorreo.setBorder(inputBorder);
        txtTelefono.setBorder(inputBorder);
        txtDui.setBorder(inputBorder);
        txtPass.setBorder(inputBorder);
        
        // Spinner Fecha Nac con el mismo estilo que los inputs
        spFechaNac.setBorder(inputBorder);
        spFechaNac.setFont(inputFont);

        // quitar doble borde dentro del editor
        JComponent ed = spFechaNac.getEditor();
        if (ed instanceof JSpinner.DateEditor dateEditor) {
            JFormattedTextField tf = dateEditor.getTextField();
            tf.setFont(inputFont);
            tf.setBorder(null);
        }

        // Combos
        cmbRol.setFont(inputFont);
        cmbEstado.setFont(inputFont);
        cmbRol.setBorder(BorderFactory.createLineBorder(borde, 1));
        cmbEstado.setBorder(BorderFactory.createLineBorder(borde, 1));

        // BOTONES: 2 estilos (primario y secundario)
        estiloBtnPrimario(btnGuardar, "Guardar", maroon, Color.WHITE, maroonDark);
        estiloBtnPrimario(btnRefrescar, "Refrescar", maroon, Color.WHITE, maroonDark);

        estiloBtnSecundario(btnLimpiar, "Limpiar", cream2, maroonDark, borde);
        estiloBtnSecundario(btnEliminar, "Eliminar", new Color(255, 235, 235), new Color(140, 0, 0), new Color(200, 120, 120));
        estiloBtnSecundario(btnRegresar, "Regresar al menú", cream2, maroonDark, borde);

        // TABLA: cabecera maroon + filas más altas + líneas suaves
        jTable1.setFont(new Font("SansSerif", Font.PLAIN, 12));
        jTable1.setRowHeight(26);
        jTable1.setGridColor(new Color(235, 228, 220));
        jTable1.setShowVerticalLines(false);

        JTableHeader header = jTable1.getTableHeader();
        header.setFont(new Font("SansSerif", Font.BOLD, 12));

        // ✅ FORZAR colores en Nimbus: poner renderer al header
        DefaultTableCellRenderer hdr = new DefaultTableCellRenderer();
        hdr.setOpaque(true);
        hdr.setBackground(maroon);      // fondo
        hdr.setForeground(Color.WHITE); // texto
        hdr.setHorizontalAlignment(SwingConstants.CENTER);
        hdr.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        header.setDefaultRenderer(hdr);
        header.repaint();

        // Renderer para alternar filas (zebra)
        DefaultTableCellRenderer zebra = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (isSelected) {
                    c.setBackground(new Color(210, 190, 190));
                    c.setForeground(Color.BLACK);
                } else {
                    c.setForeground(Color.BLACK);
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(250, 246, 242));
                }
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return c;
            }
        };
        for (int i = 0; i < jTable1.getColumnCount(); i++) {
            jTable1.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }

        // ScrollPane como “card”
        jScrollPane1.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borde, 1),
                new EmptyBorder(6, 6, 6, 6)
        ));
        jScrollPane1.getViewport().setBackground(Color.WHITE);

        // “Fumado” leve: cambia el texto de hint del header (opcional)
        setTitle("Athenaeum • Administración de Usuarios");

        // Centrar la ventana (solo posición, NO tamaño)
        setLocationRelativeTo(null);
    }

    private void estiloBtnPrimario(JButton b, String text, Color bg, Color fg, Color border) {
        b.setText(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1),
                new EmptyBorder(10, 18, 10, 18)
        ));
    }

    private void estiloBtnSecundario(JButton b, String text, Color bg, Color fg, Color border) {
        b.setText(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1),
                new EmptyBorder(10, 14, 10, 14)
        ));
    }
    
    private void cargarUsuarios() {
        try {
            String res = ApiClient.get("/api/usuarios");
            JsonArray arr = gson.fromJson(res, JsonArray.class);

            model.setRowCount(0);

            for (int i = 0; i < arr.size(); i++) {
                JsonObject u = arr.get(i).getAsJsonObject();

                model.addRow(new Object[]{
                        getInt(u,"id_usuario"),
                        getStr(u,"nombres"),
                        getStr(u,"apellidos"),
                        getStr(u,"correo"),
                        getStr(u,"rol"),
                        getStr(u,"estado"),
                        getStr(u,"telefono"),
                        getStr(u,"dui"),
                        getDateStrTabla(u,"fecha_nacimiento") // dd/MM/yyyy
                });
            }

        } catch (Exception ex) {
            mostrarError("Error cargando usuarios", ex);
        }
    }
    
    private int getInt(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsInt() : 0;
    }
    private String getStr(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }
    private String getDateStrTabla(JsonObject o, String k) {
        if (!o.has(k) || o.get(k).isJsonNull()) return "";

        String s = o.get(k).getAsString();
        if (s.length() >= 10) s = s.substring(0, 10); // yyyy-MM-dd

        try {
            LocalDate d = LocalDate.parse(s, fmtDB);
            return d.format(fmtTabla); // dd/MM/yyyy
        } catch (Exception ex) {
            return s; // si viene raro, lo muestra igual
        }
    }
    private void initTable() {
        model = new DefaultTableModel(
                new Object[]{"ID","Nombres","Apellidos","Correo","Rol","Estado","Teléfono","DUI","Fecha Nac"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        jTable1.setModel(model);

        jTable1.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;

            int row = jTable1.getSelectedRow();
            if (row < 0) return;

            txtId.setText(val(row,0));
            txtNombres.setText(val(row,1));
            txtApellidos.setText(val(row,2));
            txtCorreo.setText(val(row,3));
            cmbRol.setSelectedItem(val(row,4));
            cmbEstado.setSelectedItem(val(row,5));
            txtTelefono.setText(val(row,6));
            txtDui.setText(val(row,7));
            setSpinnerFecha(val(row,8));

            // NUNCA mostrar hash
            txtPass.setText("");

            // Bloquear si selecciona al admin logueado
            int idSel = Integer.parseInt(txtId.getText().trim());
            boolean esMiCuenta = (idSel == Session.idUsuario);

            setEdicionHabilitada(!esMiCuenta);

            if (esMiCuenta) {
                JOptionPane.showMessageDialog(this,
                        "No puedes editar/eliminar tu propia cuenta (sesión actual).",
                        "Bloqueado", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    private String val(int row, int col) {
        Object o = model.getValueAt(row, col);
        return o == null ? "" : o.toString();
    }
    
    private void setEdicionHabilitada(boolean enabled) {
        // permitimos ver pero no editar/eliminar
        txtNombres.setEnabled(enabled);
        txtApellidos.setEnabled(enabled);
        txtCorreo.setEnabled(enabled);
        txtTelefono.setEnabled(enabled);
        txtDui.setEnabled(enabled);
        cmbRol.setEnabled(enabled);
        cmbEstado.setEnabled(enabled);
        txtPass.setEnabled(enabled);
        
        spFechaNac.setEnabled(enabled);

        btnGuardar.setEnabled(true); // guardar permitido para crear nuevo (cuando limpies)
        btnEliminar.setEnabled(enabled);
    }
    
    private void initComboBoxes() {
        cmbRol.removeAllItems();
        cmbRol.addItem("ADMIN");
        cmbRol.addItem("EMPLEADO");
        cmbRol.addItem("LECTOR");

        cmbEstado.removeAllItems();
        cmbEstado.addItem("ACTIVO");
        cmbEstado.addItem("SUSPENDIDO");
    }
    
    private void limpiar() {
        txtId.setText("");
        txtNombres.setText("");
        txtApellidos.setText("");
        txtCorreo.setText("");
        txtTelefono.setText("");
        txtDui.setText("");
        txtPass.setText("");

        cmbRol.setSelectedItem("LECTOR");
        cmbEstado.setSelectedItem("ACTIVO");

        jTable1.clearSelection();

        // al limpiar, sí dejamos todo editable para crear
        setEdicionHabilitada(true);
        
        limpiarFecha();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        btnLimpiar = new javax.swing.JButton();
        txtId = new javax.swing.JTextField();
        txtNombres = new javax.swing.JTextField();
        txtApellidos = new javax.swing.JTextField();
        txtCorreo = new javax.swing.JTextField();
        txtTelefono = new javax.swing.JTextField();
        txtDui = new javax.swing.JTextField();
        cmbRol = new javax.swing.JComboBox<>();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        cmbEstado = new javax.swing.JComboBox<>();
        txtPass = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        btnGuardar = new javax.swing.JButton();
        btnEliminar = new javax.swing.JButton();
        jLabel11 = new javax.swing.JLabel();
        btnRefrescar = new javax.swing.JButton();
        btnRegresar = new javax.swing.JButton();
        spFechaNac = new javax.swing.JSpinner();
        btnPonerFecha = new javax.swing.JButton();
        btnQuitarFecha = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        btnLimpiar.setText("Limpiar");
        btnLimpiar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLimpiarActionPerformed(evt);
            }
        });

        txtNombres.setToolTipText("");

        txtCorreo.setToolTipText("");

        cmbRol.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel1.setText("ID");

        jLabel2.setText("Nombres");

        jLabel3.setText("Apellidos");

        jLabel4.setText("Correo");

        jLabel5.setText("Teléfono");

        jLabel6.setText("Dui");

        jLabel7.setText("Fecha Nacimiento");

        jLabel8.setText("Rol");

        jLabel9.setText("Estado");

        cmbEstado.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel10.setText("Contraseña");

        btnGuardar.setText("Guardar");
        btnGuardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarActionPerformed(evt);
            }
        });

        btnEliminar.setText("Eliminar");
        btnEliminar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEliminarActionPerformed(evt);
            }
        });

        jLabel11.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel11.setText("Panel De Administador");

        btnRefrescar.setText("Refrescar");
        btnRefrescar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefrescarActionPerformed(evt);
            }
        });

        btnRegresar.setText("Regresar al menu");
        btnRegresar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRegresarActionPerformed(evt);
            }
        });

        btnPonerFecha.setText("Poner Fecha");
        btnPonerFecha.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPonerFechaActionPerformed(evt);
            }
        });

        btnQuitarFecha.setText("Quitar");
        btnQuitarFecha.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnQuitarFechaActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1)
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addGap(41, 41, 41)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel3)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel6)
                                .addGap(29, 29, 29))
                            .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.LEADING))
                        .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.TRAILING))
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(txtApellidos)
                    .addComponent(txtDui)
                    .addComponent(txtTelefono)
                    .addComponent(txtId)
                    .addComponent(txtNombres)
                    .addComponent(txtCorreo, javax.swing.GroupLayout.PREFERRED_SIZE, 175, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel8)
                                    .addComponent(jLabel9)
                                    .addComponent(jLabel10))
                                .addGap(42, 42, 42))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jLabel7)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(cmbEstado, 0, 175, Short.MAX_VALUE)
                            .addComponent(cmbRol, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(txtPass)
                            .addComponent(spFechaNac))
                        .addGap(64, 64, 64))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(61, 61, 61)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(btnLimpiar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnRefrescar, javax.swing.GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, 104, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(btnGuardar, javax.swing.GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE)
                            .addComponent(btnEliminar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(67, 67, 67))))
            .addGroup(layout.createSequentialGroup()
                .addGap(119, 119, 119)
                .addComponent(jLabel11)
                .addGap(30, 30, 30)
                .addComponent(btnPonerFecha, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnQuitarFecha, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(64, 64, 64))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnRegresar, javax.swing.GroupLayout.PREFERRED_SIZE, 577, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(67, 67, 67))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(56, 56, 56)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtNombres, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel2)))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnPonerFecha)
                            .addComponent(btnQuitarFecha)
                            .addComponent(jLabel11))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(spFechaNac, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel7))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(cmbRol, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel8))))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtApellidos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3)
                            .addComponent(jLabel9))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtCorreo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel4)
                            .addComponent(jLabel10)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(cmbEstado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(txtPass, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtTelefono, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtDui, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel6)
                            .addComponent(btnRefrescar)))
                    .addComponent(btnLimpiar)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnGuardar)
                        .addGap(18, 18, 18)
                        .addComponent(btnEliminar)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnRegresar)
                .addGap(13, 13, 13)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnLimpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLimpiarActionPerformed
        // TODO add your handling code here:
        limpiar();
    }//GEN-LAST:event_btnLimpiarActionPerformed

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        try {
            String idTxt = txtId.getText().trim();

            // básicos obligatorios SIEMPRE
            String nombres = txtNombres.getText().trim();
            String apellidos = txtApellidos.getText().trim();
            String correo = txtCorreo.getText().trim();

            if (nombres.isEmpty()) { JOptionPane.showMessageDialog(this,"Nombres obligatorios"); return; }
            if (apellidos.isEmpty()) { JOptionPane.showMessageDialog(this,"Apellidos obligatorios"); return; }
            if (correo.isEmpty()) { JOptionPane.showMessageDialog(this,"Correo obligatorio"); return; }

            JsonObject body = new JsonObject();
            body.addProperty("nombres", nombres);
            body.addProperty("apellidos", apellidos);
            body.addProperty("correo", correo);

            // opcionales: solo enviar si hay algo
            String tel = txtTelefono.getText().trim();
            if (!tel.isEmpty()) body.addProperty("telefono", tel);

            String duiRaw = txtDui.getText().trim();

            // ✅ validar si viene
            if (!validarDuiSiViene(duiRaw)) return;

            String dui = normalizarDui(duiRaw);
            if (!dui.isEmpty()) body.addProperty("dui", dui);
            
            if (!esMayorDeEdad()) {
                JOptionPane.showMessageDialog(this,
                    "El usuario debe ser mayor de 18 años.",
                    "Edad inválida",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            String f = getFechaSpinnerYYYYMMDD();
            if (!f.isEmpty()) body.addProperty("fecha_nacimiento", f);

            // IMPORTANTÍSIMO: SIEMPRE mandar rol/estado (para que no te diga required)
            body.addProperty("rol", cmbRol.getSelectedItem().toString());
            body.addProperty("estado", cmbEstado.getSelectedItem().toString());

            // contraseña: solo si escriben algo
            String pass = txtPass.getText().trim();
            if (!pass.isEmpty()) body.addProperty("contraseña", pass);

            if (idTxt.isEmpty()) {
                // CREAR: contraseña obligatoria
                if (pass.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Para crear usuario, contraseña obligatoria");
                    return;
                }
                ApiClient.postJson("/api/usuarios", gson.toJson(body));
                JOptionPane.showMessageDialog(this, "Usuario creado ✅");
            } else {
                int id = Integer.parseInt(idTxt);

                // Bloquear editar propia cuenta (extra seguro)
                if (id == Session.idUsuario) {
                    JOptionPane.showMessageDialog(this, "No puedes editar tu propia cuenta.");
                    return;
                }

                body.addProperty("id_usuario", id);
                ApiClient.putJson("/api/usuarios/" + id, gson.toJson(body));
                JOptionPane.showMessageDialog(this, "Usuario actualizado ✅");
            }

            cargarUsuarios();
            limpiar();

        } catch (Exception ex) {
            mostrarError("Error cargando usuarios", ex);
        }
    }//GEN-LAST:event_btnGuardarActionPerformed

    private void btnEliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarActionPerformed
        try {
            String idTxt = txtId.getText().trim();
            if (idTxt.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Selecciona un usuario en la tabla");
                return;
            }

            int id = Integer.parseInt(idTxt);

            if (id == Session.idUsuario) {
                JOptionPane.showMessageDialog(this, "No puedes eliminar tu propia cuenta.");
                return;
            }

            int op = JOptionPane.showConfirmDialog(this, "¿Eliminar usuario " + idTxt + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
            if (op != JOptionPane.YES_OPTION) return;

            ApiClient.delete("/api/usuarios/" + id);

            JOptionPane.showMessageDialog(this, "Usuario eliminado ✅");
            cargarUsuarios();
            limpiar();

        } catch (Exception ex) {
            mostrarError("Error cargando usuarios", ex);
        }
    }//GEN-LAST:event_btnEliminarActionPerformed

    private void btnRefrescarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefrescarActionPerformed
        cargarUsuarios();
        limpiar();
    }//GEN-LAST:event_btnRefrescarActionPerformed

    private void btnRegresarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegresarActionPerformed
        // TODO add your handling code here:
        if (parent != null) parent.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnRegresarActionPerformed

    private void btnPonerFechaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPonerFechaActionPerformed
        // TODO add your handling code here:
        ponerFecha(maxFecha18);
    }//GEN-LAST:event_btnPonerFechaActionPerformed

    private void btnQuitarFechaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnQuitarFechaActionPerformed
        // TODO add your handling code here:
        limpiarFecha();
    }//GEN-LAST:event_btnQuitarFechaActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(UsuariosFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            new UsuariosFrame().setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnEliminar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnPonerFecha;
    private javax.swing.JButton btnQuitarFecha;
    private javax.swing.JButton btnRefrescar;
    private javax.swing.JButton btnRegresar;
    private javax.swing.JComboBox<String> cmbEstado;
    private javax.swing.JComboBox<String> cmbRol;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JSpinner spFechaNac;
    private javax.swing.JTextField txtApellidos;
    private javax.swing.JTextField txtCorreo;
    private javax.swing.JTextField txtDui;
    private javax.swing.JTextField txtId;
    private javax.swing.JTextField txtNombres;
    private javax.swing.JTextField txtPass;
    private javax.swing.JTextField txtTelefono;
    // End of variables declaration//GEN-END:variables
}
