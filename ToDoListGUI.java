import java.awt.*;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class ToDoListGUI extends JFrame {
    private static final String URL = "jdbc:mysql://localhost:3306/todo_list";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "rkarthih279@";

    private JTextField taskNameField, dueDateField, priorityField, descriptionField;
    private JTable taskTable;
    private JComboBox<String> statusComboBox, filterComboBox;
    private JButton addButton, deleteButton, updateButton, filterButton;

    public ToDoListGUI() {
        setTitle("Enhanced To-Do List Manager");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Input Panel
        JPanel inputPanel = new JPanel(new GridLayout(6, 2, 5, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Task Details"));

        taskNameField = new JTextField(20);
        dueDateField = new JTextField(10);
        priorityField = new JTextField(10);
        descriptionField = new JTextField(30);
        statusComboBox = new JComboBox<>(new String[]{"Pending", "Completed"});

        inputPanel.add(new JLabel("Task Name:"));
        inputPanel.add(taskNameField);
        inputPanel.add(new JLabel("Due Date (YYYY-MM-DD):"));
        inputPanel.add(dueDateField);
        inputPanel.add(new JLabel("Priority (1-5):"));
        inputPanel.add(priorityField);
        inputPanel.add(new JLabel("Description:"));
        inputPanel.add(descriptionField);
        inputPanel.add(new JLabel("Status:"));
        inputPanel.add(statusComboBox);

        // Buttons
        addButton = new JButton("Add Task");
        deleteButton = new JButton("Delete Task");
        updateButton = new JButton("Update Status");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(updateButton);

        // Task Table
        String[] columnNames = {"ID", "Task Name", "Due Date", "Priority", "Description", "Status"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
        taskTable = new JTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(taskTable);

        // Filter Panel
        JPanel filterPanel = new JPanel();
        filterPanel.setBorder(BorderFactory.createTitledBorder("Filter Tasks"));
        filterComboBox = new JComboBox<>(new String[]{"All", "Pending", "Completed"});
        filterButton = new JButton("Apply Filter");
        filterPanel.add(new JLabel("Filter by Status:"));
        filterPanel.add(filterComboBox);
        filterPanel.add(filterButton);

        // Add components to frame
        add(inputPanel, BorderLayout.NORTH);
        add(tableScrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        add(filterPanel, BorderLayout.EAST);

        // Action Listeners
        addButton.addActionListener(e -> addTask());
        deleteButton.addActionListener(e -> deleteTask());
        updateButton.addActionListener(e -> updateTaskStatus());
        filterButton.addActionListener(e -> filterTasks());

        loadTasks("All");
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    private void addTask() {
        String taskName = taskNameField.getText();
        String dueDateStr = dueDateField.getText();
        String priority = priorityField.getText();
        String description = descriptionField.getText();
        String status = (String) statusComboBox.getSelectedItem();

        if (!isValidDate(dueDateStr) || !isValidPriority(priority)) {
            JOptionPane.showMessageDialog(this, "Invalid input. Ensure date is YYYY-MM-DD and priority is 1-5.");
            return;
        }

        String query = "INSERT INTO tasks (task_name, due_date, priority, description, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, taskName);
            pst.setDate(2, Date.valueOf(dueDateStr));
            pst.setString(3, priority);
            pst.setString(4, description);
            pst.setString(5, status);
            pst.executeUpdate();
            loadTasks("All");
            clearInputFields();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error adding task: " + e.getMessage());
        }
    }

    private void deleteTask() {
        int selectedRow = taskTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a task to delete.");
            return;
        }

        int taskId = (int) taskTable.getValueAt(selectedRow, 0);
        String query = "DELETE FROM tasks WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setInt(1, taskId);
            pst.executeUpdate();
            loadTasks("All");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error deleting task: " + e.getMessage());
        }
    }

    private void updateTaskStatus() {
        int selectedRow = taskTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a task to update.");
            return;
        }

        int taskId = (int) taskTable.getValueAt(selectedRow, 0);
        String status = (String) statusComboBox.getSelectedItem();
        String query = "UPDATE tasks SET status = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, status);
            pst.setInt(2, taskId);
            pst.executeUpdate();
            loadTasks("All");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error updating task status: " + e.getMessage());
        }
    }

    private void loadTasks(String filter) {
        String query = "SELECT * FROM tasks";
        if (!"All".equals(filter)) {
            query += " WHERE status = '" + filter + "'";
        }
        query += " ORDER BY due_date";

        DefaultTableModel tableModel = (DefaultTableModel) taskTable.getModel();
        tableModel.setRowCount(0);

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("task_name"),
                        rs.getDate("due_date"),
                        rs.getString("priority"),
                        rs.getString("description"),
                        rs.getString("status")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading tasks: " + e.getMessage());
        }
    }

    private void filterTasks() {
        String filter = (String) filterComboBox.getSelectedItem();
        loadTasks(filter);
    }

    private void clearInputFields() {
        taskNameField.setText("");
        dueDateField.setText("");
        priorityField.setText("");
        descriptionField.setText("");
        statusComboBox.setSelectedIndex(0);
    }

    private boolean isValidDate(String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);
        try {
            sdf.parse(dateStr);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    private boolean isValidPriority(String priorityStr) {
        try {
            int priority = Integer.parseInt(priorityStr);
            return priority >= 1 && priority <= 5;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ToDoListGUI frame = new ToDoListGUI();
            frame.setVisible(true);
        });
    }
}
