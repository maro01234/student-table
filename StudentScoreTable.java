import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.text.DecimalFormat;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

/** 生徒名と平均点を表に表示する、Java 17対応のサンプル。 */
public class StudentScoreTable {
    public static void main(String[] args) {
        // Swingの画面はイベントディスパッチスレッドで作成する。
        SwingUtilities.invokeLater(StudentScoreTable::createWindow);
    }

    private static void createWindow() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // OSの外観が使えない場合も、標準の外観で表示する。
        }

        JTable table = new JTable(new StudentTableModel());
        table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        table.setRowHeight(36);
        table.setShowGrid(true);
        table.setGridColor(new Color(205, 210, 215));
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        table.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(320);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);

        // 数値のまま管理し、表示するときだけ小数第1位まで整える。
        // 文字列として保存しないので、平均点を数値順に並べ替えられる。
        DefaultTableCellRenderer scoreRenderer = new DefaultTableCellRenderer() {
            private final DecimalFormat format = new DecimalFormat("0.0");

            @Override
            protected void setValue(Object value) {
                setText(value == null ? "" : format.format(value));
            }
        };
        scoreRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        table.setDefaultRenderer(Double.class, scoreRenderer);

        JLabel title = new JLabel("生徒別の平均点");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

        JLabel help = new JLabel(
                "セルをダブルクリックして編集／列名をクリックして並べ替え（保存なし）");
        help.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.add(title, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(help, BorderLayout.SOUTH);

        JFrame frame = new JFrame("生徒の成績一覧");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(panel);
        frame.setSize(660, 390);
        frame.setMinimumSize(new Dimension(600, 320));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /** 表のデータと編集ルールを管理するクラス。 */
    static class StudentTableModel extends AbstractTableModel {
        private final String[] columnNames = {"生徒名", "平均点（100点満点）"};

        // サンプルの平均点。実際の生徒名・計算済み平均点に変更できる。
        private final Object[][] students = {
                {"佐藤 花子", 85.5},
                {"鈴木 太郎", 72.0},
                {"高橋 美咲", 91.3},
                {"田中 健", 68.7},
                {"伊藤 葵", 88.0}
        };

        @Override
        public int getRowCount() {
            return students.length;
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column == 0 ? String.class : Double.class;
        }

        @Override
        public Object getValueAt(int row, int column) {
            return students[row][column];
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return true;
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            String input = value == null ? "" : value.toString().strip();
            if (column == 0) {
                if (input.isEmpty()) {
                    showInputError("生徒名を入力してください。");
                    return;
                }
                students[row][column] = input;
            } else {
                try {
                    double score = Double.parseDouble(input);
                    if (!Double.isFinite(score) || score < 0 || score > 100) {
                        throw new NumberFormatException();
                    }
                    students[row][column] = score;
                } catch (NumberFormatException exception) {
                    showInputError("平均点には0〜100の数値を入力してください。");
                    return;
                }
            }
            fireTableCellUpdated(row, column);
        }

        private void showInputError(String message) {
            JOptionPane.showMessageDialog(null, message, "入力エラー",
                    JOptionPane.WARNING_MESSAGE);
        }
    }
}
