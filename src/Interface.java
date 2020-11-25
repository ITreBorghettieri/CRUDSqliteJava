import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class Interface extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Interface::new);
    }

    private Interface() {
        this.setTitle("Gestione tabelle");
        this.setSize(500,500);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.initUI();
        this.setVisible(true);
    }

    private void initUI() {
        //NORTH
        JMenuBar menuBar = new JMenuBar();

        //Menu fileMenu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');
        //New
        JMenuItem fileNew = new JMenuItem("New table");
        fileNew.setMnemonic('N');
        fileNew.setIcon(UIManager.getIcon("FileView.fileIcon"));
        //Open
        JMenuItem fileOpen = new JMenuItem("Open");
        fileOpen.setMnemonic('O');
        fileOpen.setIcon(UIManager.getIcon("FileView.directoryIcon"));
        //Save
        JMenu fileSaveMenu = new JMenu("Save as");
        fileSaveMenu.setMnemonic('S');
        fileSaveMenu.setIcon(UIManager.getIcon("FileView.floppyDriveIcon"));
        //csv
        JMenuItem csv = new JMenuItem("CSV");

        //Menu tablesMenu
        JMenu tablesMenu = new JMenu("Tabelle");
        tablesMenu.setMnemonic('T');
        tablesMenu.setEnabled(false);

        //Menu editMenu
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic('E');
        editMenu.setEnabled(false);
        //Create
        JMenuItem editCreate = new JMenuItem("Create");
        editCreate.setMnemonic('C');
        //Update
        JMenu editUpdateMenu = new JMenu("Update");
        editUpdateMenu.setMnemonic('U');
        //Delete
        JMenuItem editDelete = new JMenuItem("Delete");
        editDelete.setMnemonic('D');

        fileMenu.add(fileNew);
        fileMenu.add(fileOpen);

        fileSaveMenu.add(csv);

        fileMenu.add(fileSaveMenu);

        editMenu.add(editCreate);
        editMenu.add(editUpdateMenu);
        editMenu.add(editDelete);

        menuBar.add(fileMenu);
        menuBar.add(tablesMenu);
        menuBar.add(editMenu);

        this.add(menuBar, BorderLayout.NORTH);

        //CENTER
        Table table = new Table();
        JScrollPane mainPanel = new JScrollPane(table);
        this.add(mainPanel, BorderLayout.CENTER);


        //LISTENER
        //New
        fileNew.addActionListener(actionEvent -> {
            table.clear();
            tablesMenu.setEnabled(false);
            editMenu.setEnabled(false);
            table.newTable();
        });

        //Open
        fileOpen.addActionListener(actionEvent -> {
            JFileChooser fileChooser = new JFileChooser(System.getProperty("user.home") + "/Desktop");
            int rc = fileChooser.showOpenDialog(null);
            if(rc == JFileChooser.APPROVE_OPTION) {
                table.clear();
                tablesMenu.removeAll();
                editUpdateMenu.removeAll();
                table.setPath(fileChooser.getSelectedFile());
                table.open();

                for(String tableName : table.getTables()) {
                    JMenuItem tableItem = new JMenuItem(tableName);
                    tablesMenu.add(tableItem);

                    tableItem.addActionListener(actionEvent1 -> {
                        JMenuItem selectedTableItem = (JMenuItem) actionEvent1.getSource();
                        table.setCurrentTableName(selectedTableItem.getText());
                        table.writeTable();

                        editUpdateMenu.removeAll();

                        for (String fieldName : table.getFieldsName()) {
                            JMenuItem fieldItem = new JMenuItem(fieldName);
                            editUpdateMenu.add(fieldItem).addActionListener(actionEvent2 -> {
                                if(table.getSelectedRow() == -1) {
                                    JOptionPane.showMessageDialog(new JFrame(), "Selezionare prima una riga da modificare.", "Errore", JOptionPane.ERROR_MESSAGE);
                                } else {
                                    String selectedFieldName = fieldItem.getText();
                                    String newValue = JOptionPane.showInputDialog("Inserire il nuovo valore di " + fieldItem.getText() + ":");
                                    String primaryKey = (String) table.getValueAt(table.getSelectedRow(), table.findPrimaryKeyIndex());
                                    table.updateRecord(selectedFieldName, newValue, primaryKey);
                                    table.writeTable();
                                }
                            });
                        }
                        editMenu.setEnabled(true);
                    });
                }
                tablesMenu.setEnabled(true);
            }
        });

        //Save
        //CSV
        csv.addActionListener(actionEvent -> {
            JFileChooser fileChooser = new JFileChooser(System.getProperty("user.home") + "/Desktop");
            int rc = fileChooser.showSaveDialog(null);
            if(rc == JFileChooser.APPROVE_OPTION) {
                table.setPath(fileChooser.getSelectedFile());
                table.saveCSV();
            }
        });

        editCreate.addActionListener(actionEvent -> {
            ArrayList<String> inputData = new ArrayList<>();
            for (String col : table.getFieldsName()) {
                inputData.add(JOptionPane.showInputDialog("Inserire " + col + ":"));
            }
            table.createRecord(inputData);
            table.writeTable();
        });

        editDelete.addActionListener(actionEvent -> {
            if(table.getSelectedRow() == -1) {
                JOptionPane.showMessageDialog(new JFrame(), "Selezionare prima una riga da eliminare", "Errore", JOptionPane.ERROR_MESSAGE);
            } else {
                String primaryKey = (String) table.getValueAt(table.getSelectedRow(), table.findPrimaryKeyIndex());
                table.deleteRecord(primaryKey);
                table.writeTable();
            }
        });
    }
}
