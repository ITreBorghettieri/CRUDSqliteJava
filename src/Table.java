import javax.swing.*;
import java.sql.*;
import java.io.*;
import javax.swing.table.DefaultTableModel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Table extends JTable {
    private final DefaultTableModel currentTableModel = (DefaultTableModel) this.getModel();
    private File path = null;
    private final ArrayList<String> tables = new ArrayList<>();
    private final ArrayList<String> fieldsName = new ArrayList<>();
    private final ArrayList<String> fieldsType = new ArrayList<>();
    private String currentTableName = "";

    public Table() {
        super();
    }

    public DefaultTableModel getCurrentTableModel() {
        return currentTableModel;
    }

    public File getPath() {
        return path;
    }

    public void setPath(File path) {
        this.path = path;
    }

    public ArrayList<String> getTables() {
        return tables;
    }

    public void appendTables(String s) {
        this.tables.add(s);
    }

    public ArrayList<String> getFieldsName() {
        return fieldsName;
    }

    public void appendFieldsName(String s) {
        this.fieldsName.add(s);
    }

    public ArrayList<String> getFieldsType() {
        return fieldsType;
    }

    public void appendFieldsType(String s) {
        this.fieldsType.add(s);
    }

    public String getCurrentTableName() {
        return currentTableName;
    }

    public void setCurrentTableName(String currentTableName) {
        this.currentTableName = currentTableName;
    }

    public void newTable() {
        int cols = Integer.parseInt(JOptionPane.showInputDialog("Numero colonne:"));
        while(cols < 1)
            cols = Integer.parseInt(JOptionPane.showInputDialog(new JFrame(), "Errore, inserire minimo 1 colonna", "Errore", JOptionPane.ERROR_MESSAGE));
        int rows = Integer.parseInt(JOptionPane.showInputDialog("Numero righe:"));
        while(rows < 1)
            rows = Integer.parseInt(JOptionPane.showInputDialog(new JFrame(), "Errore, inserire minimo 1 riga", "Errore", JOptionPane.ERROR_MESSAGE));

        for(int i = 0; i < cols; i++) {
            this.getCurrentTableModel().addColumn(JOptionPane.showInputDialog("Inserire nome colonna " + (i + 1) + ":"));
        }

        this.getCurrentTableModel().setRowCount(rows);
    }

    public void open() {
        String[] split = this.getPath().getName().split("[.]");
        String ext = split[split.length - 1];

        if(ext.equals("csv")) {
            this.loadCSV();
        }
        if(ext.equals("db")) {
            this.loadDB();
        }
    }

    public void loadCSV() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(this.getPath()));
            this.getCurrentTableModel().setColumnCount(0);
            this.getCurrentTableModel().setColumnCount(0);

            String[] cols = br.readLine().split("[,;]");
            for(String col : cols) {
                this.getCurrentTableModel().addColumn(col);
            }

            String row = br.readLine();
            while(row != null) {
                this.getCurrentTableModel().addRow(row.split("[,;]"));
                row = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadDB() {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + this.getPath());
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM sqlite_master WHERE type='table' AND tbl_name NOT LIKE 'sqlite_%'");
            this.getTables().clear();
            while(resultSet.next()) {
                this.appendTables(resultSet.getString("tbl_name"));
            }
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void writeTable() {
        try {
            this.getCurrentTableModel().setColumnCount(0);
            this.getCurrentTableModel().setRowCount(0);
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + this.getPath());
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + this.getCurrentTableName() + ");");
            int colsCount = 0;
            this.getFieldsName().clear();
            this.getFieldsType().clear();
            while(resultSet.next()) {
                this.appendFieldsName(resultSet.getString("name"));
                this.appendFieldsType(resultSet.getString("type"));
                this.getCurrentTableModel().addColumn(this.getFieldsName().get(colsCount));
                colsCount++;
            }

            String[] row = new String[colsCount];
            resultSet = statement.executeQuery("SELECT * FROM " + this.getCurrentTableName());
            while(resultSet.next()) {
                for(int r = 0; r < colsCount; r++) {
                    row[r] = resultSet.getString(this.getFieldsName().get(r));
                }
                this.getCurrentTableModel().addRow(row);
            }
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveCSV() {
        try {
            PrintWriter pw = new PrintWriter(this.getPath());
            for (int col = 0; col < this.getColumnCount(); col++) {
                if(!this.getColumnName(col).equals("")) {
                    pw.print(this.getColumnName(col));
                    if(col != (this.getColumnCount() - 1)){
                        pw.print(",");
                    }else{
                        pw.println();
                    }
                }
            }

            for (int row = 0; row < this.getRowCount(); row++) {
                for (int col = 0; col < this.getColumnCount(); col++) {
                    pw.print(this.getValueAt(row, col));
                    if(col != (this.getColumnCount() - 1))
                        pw.print(",");
                }
                pw.println();
            }

            pw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createRecord(ArrayList<String> valueDB){
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + this.getPath());
            StringBuilder query = new StringBuilder("INSERT INTO " + this.getCurrentTableName() + " (");

            int fieldsNameLen = this.getFieldsName().size();

            int index = 0;
            for (String col : this.getFieldsName()){
                query.append(col);
                if(index < fieldsNameLen - 1){
                    query.append(",");
                }
                index++;
            }
            query.append(") VALUES(");

            index = 0;
            for (String ignored : this.getFieldsName()){
                query.append("?");
                if(index < fieldsNameLen - 1){
                    query.append(",");
                }
                index++;
            }
            query.append(");");
            PreparedStatement preparedStatement = connection.prepareStatement(query.toString());

            index = 0;
            for (String currentType : this.getFieldsType()) {
                switch (currentType) {
                    case "INTEGER":
                        preparedStatement.setInt(index + 1, Integer.parseInt(valueDB.get(index)));
                        break;

                    case "TEXT":
                        preparedStatement.setString(index + 1, valueDB.get(index));
                        break;

                    case "BLOB":
                        byte[] byteData = valueDB.get(index).getBytes(StandardCharsets.UTF_8); //Better to specify encoding
                        Blob blobData = connection.createBlob();
                        blobData.setBytes(1, byteData);
                        preparedStatement.setBlob(index + 1, blobData);
                        break;

                    case "REAL":
                        preparedStatement.setDouble(index + 1, Double.parseDouble(valueDB.get(index)));
                        break;

                    default:
                        return;
                }
                index++;
            }
            //query = "INSERT INTO table_name(field1, field2, field3) VALUES(?, ?, ?);"
            preparedStatement.executeUpdate();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateRecord(String fieldName, String newValue, String primaryKey){
        String fieldType = this.getFieldsType().get(this.getFieldsName().indexOf(fieldName));
        int primaryKeyIndex = this.findPrimaryKeyIndex();
        String primaryKeyName = this.getFieldsName().get(primaryKeyIndex);
        String primaryKeyType = this.getFieldsType().get(primaryKeyIndex);

        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + this.getPath());
            String query = "UPDATE " + this.getCurrentTableName() + " SET " + fieldName + " = ? WHERE " + primaryKeyName + " = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(query);

            switch (fieldType) {
                case "INTEGER":
                    preparedStatement.setInt(1, Integer.parseInt(newValue));
                    break;

                case "TEXT":
                    preparedStatement.setString(1, newValue);
                    break;

                case "BLOB":
                    byte[] byteData = newValue.getBytes(StandardCharsets.UTF_8); //Better to specify encoding
                    Blob blobData = connection.createBlob();
                    blobData.setBytes(1, byteData);
                    preparedStatement.setBlob(1, blobData);
                    break;

                case "REAL":
                    preparedStatement.setDouble(1, Double.parseDouble(newValue));
                    break;

                default:
                    return;
            }
            switch (primaryKeyType) {
                case "INTEGER":
                    preparedStatement.setInt(2, Integer.parseInt(primaryKey));
                    break;

                case "TEXT":
                    preparedStatement.setString(2, primaryKey);
                    break;

                case "BLOB":
                    byte[] byteData = primaryKey.getBytes(StandardCharsets.UTF_8); //Better to specify encoding
                    Blob blobData = connection.createBlob();
                    blobData.setBytes(2, byteData);
                    preparedStatement.setBlob(2, blobData);
                    break;

                case "REAL":
                    preparedStatement.setDouble(2, Double.parseDouble(primaryKey));
                    break;

                default:
            }

            preparedStatement.executeUpdate();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteRecord(String primaryKey) {
        String primaryKeyName = this.getFieldsName().get(this.findPrimaryKeyIndex());
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + this.getPath());
            Statement statement = connection.createStatement();

            String query = "DELETE FROM " + this.currentTableName + " WHERE " + primaryKeyName + " = " + primaryKey;

            statement.execute(query);
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int findPrimaryKeyIndex() {
        int index = 0;
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + this.getPath());
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + this.getCurrentTableName() + ");");

            while(resultSet.next()) {
                if(resultSet.getInt("pk") != 0) {
                    connection.close();
                    return index;
                }
                index++;
            }
            connection.close();
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void clear() {
        this.getCurrentTableModel().setColumnCount(0);
        this.getCurrentTableModel().setRowCount(0);
    }
}
