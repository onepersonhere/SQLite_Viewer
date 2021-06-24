package viewer;

import org.sqlite.SQLiteDataSource;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.sql.*;
import java.util.Vector;

public class SQLiteViewer extends JFrame {
    JButton OpenFileButton = new JButton("Open");
    JTextField FileNameTextField = new JTextField();
    JComboBox TablesComboBox = new JComboBox();
    JTextArea QueryTextArea = new JTextArea();
    JButton ExecuteQueryButton = new JButton("Execute");
    JTable Table = new JTable();
    SQLiteDataSource dataSource = new SQLiteDataSource();

    public SQLiteViewer() {
        setTitle("SQLite Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 900);
        setLayout(new FlowLayout());
        setResizable(false);
        setLocationRelativeTo(null);
        iniComponents();
        setVisible(true);
    }
    private void iniComponents(){

        QueryTextArea.setEnabled(false);
        ExecuteQueryButton.setEnabled(false);

        JPanel upperPanel = new JPanel(new BorderLayout(5,5));
        OpenFileButton.setName("OpenFileButton");
        FileNameTextField.setName("FileNameTextField");
        FileNameTextField.setPreferredSize(new Dimension(600,20));
        upperPanel.add(FileNameTextField, BorderLayout.WEST);
        upperPanel.add(OpenFileButton, BorderLayout.EAST);
        add(upperPanel);

        TablesComboBox.setName("TablesComboBox");
        TablesComboBox.setPreferredSize(new Dimension(675,20));
        QueryTextArea.setName("QueryTextArea");
        QueryTextArea.setPreferredSize(new Dimension(565,200));
        ExecuteQueryButton.setName("ExecuteQueryButton");
        ExecuteQueryButton.setPreferredSize(new Dimension(100,30));
        add(TablesComboBox);

        JPanel lowerPanel = new JPanel(new BorderLayout(5,5));
        JPanel splitPanel = new JPanel(new BorderLayout(5,5));
        splitPanel.add(ExecuteQueryButton,BorderLayout.NORTH);
        lowerPanel.add(QueryTextArea, BorderLayout.WEST);
        lowerPanel.add(splitPanel, BorderLayout.EAST);
        add(lowerPanel);

        Table.setName("Table");
        Table.setPreferredSize(new Dimension(675, 550));
        add(Table);
        functions();

    }
    private void functions(){
        OpenFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                TablesComboBox.removeAllItems();
                String filename = FileNameTextField.getText();
                String url = "jdbc:sqlite:" + filename;
                dataSource.setUrl(url);
                File file = new File(filename);
                //send a query to db with filename
                //display all public tables onto JComboBox (aka titles)
                if(file.exists()) {
                    try (Connection con = dataSource.getConnection()) {
                        if (con.isValid(5)) {
                            TablesComboBox.setEnabled(true);
                            QueryTextArea.setEnabled(true);
                            ExecuteQueryButton.setEnabled(true);
                            viewTable(con);
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }else {
                    JOptionPane.showMessageDialog(new Frame(), "File doesn't exist!");
                    TablesComboBox.setEnabled(false);
                    QueryTextArea.setEnabled(false);
                    ExecuteQueryButton.setEnabled(false);
                }
            }
        });

        ExecuteQueryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                //select the table mentioned in JComboBox
                //generates the query for it
                //pass the query and display the result in the JTextArea
                String query = QueryTextArea.getText();
                try (Connection con = dataSource.getConnection()) {
                    if (con.isValid(5)) {
                        try (Statement stmt = con.createStatement()) {
                            ResultSet rs = stmt.executeQuery(query);
                            Table.setModel(buildTableModel(rs));
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(new Frame(), e);
                }

            }
        });
        TablesComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                QueryTextArea.setText("");
                String n = (String) TablesComboBox.getSelectedItem();
                String q = "SELECT * FROM " + n +";";
                QueryTextArea.append(q);

            }
        });
    }
    public void viewTable(Connection con) throws SQLException {
        String query = "SELECT name FROM sqlite_master WHERE type ='table' AND name NOT LIKE 'sqlite_%';";
        try (Statement stmt = con.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                String name = rs.getString("name");
                TablesComboBox.addItem(name);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static DefaultTableModel buildTableModel(ResultSet rs)
            throws SQLException {

        ResultSetMetaData metaData = rs.getMetaData();

        // names of columns
        Vector<String> columnNames = new Vector<String>();
        int columnCount = metaData.getColumnCount();
        for (int column = 1; column <= columnCount; column++) {
            columnNames.add(metaData.getColumnName(column));
        }

        // data of the table
        Vector<Vector<Object>> data = new Vector<Vector<Object>>();
        while (rs.next()) {
            Vector<Object> vector = new Vector<Object>();
            for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                vector.add(rs.getObject(columnIndex));
            }
            data.add(vector);
        }

        return new DefaultTableModel(data, columnNames);

    }
}
