package ui;

import data.Project;
import data.Task;
import data.TaskStatus;

import javax.swing.*;
import java.awt.*;
import java.time.temporal.ChronoUnit;

public class MyTasksPanel extends JPanel {
    private final Project project;
    private final String loginName;
    private final DataChangeListener listener;

    private final DefaultListModel<Task> model = new DefaultListModel<Task>();
    private final JList<Task> list = new JList<Task>(model);

    private final JLabel titleLabel = new JLabel("-");
    private final JLabel ddLabel = new JLabel("-");
    private final JLabel expectedLabel = new JLabel("-");   // ✅ 예상 진행률
    private final JLabel deltaLabel = new JLabel("-");      // ✅ 늦음/빠름

    private final JProgressBar progBar = new JProgressBar(0, 100);

    private final JSlider slider = new JSlider(0, 100, 0);
    private final JButton plus10 = new JButton("+10%");
    private final JButton minus10 = new JButton("-10%");

    private final JComboBox<TaskStatus> statusCombo = new JComboBox<TaskStatus>(TaskStatus.values());
    private final JCheckBox blockedCheck = new JCheckBox("막힘(BLOCKED)");

    private Task cur = null;

    public MyTasksPanel(Project project, String loginName, DataChangeListener listener) {
        this.project = project;
        this.loginName = loginName;
        this.listener = listener;

        setLayout(new BorderLayout(10, 10));
        add(buildLeft(), BorderLayout.WEST);
        add(buildRight(), BorderLayout.CENTER);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object value, int index,
                                                         boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(l, value, index, isSelected, cellHasFocus);
                if (value instanceof Task) {
                    Task t = (Task) value;
                    long d = ChronoUnit.DAYS.between(project.getSimDate(), t.getPlanEnd());
                    int exp = t.getExpectedProgress(project.getSimDate());
                    int delta = t.getScheduleDelta(project.getSimDate());
                    String s = (delta < 0) ? (" / 늦음 " + (-delta) + "%") : (delta > 0 ? (" / 빠름 " + delta + "%") : "");
                    setText(t.getTitle() + "  / D-" + d + "  / 현재 " + t.getProgress() + "% / 예상 " + exp + "%" + s);
                }
                return this;
            }
        });

        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cur = list.getSelectedValue();
                bind(cur);
            }
        });

        slider.setMajorTickSpacing(10);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);

        plus10.addActionListener(e -> changeProgress(+10));
        minus10.addActionListener(e -> changeProgress(-10));

        slider.addChangeListener(e -> {
            if (cur == null) return;
            if (slider.getValueIsAdjusting()) return;
            cur.updateProgress(slider.getValue());
            listener.onDataChanged();
            bind(cur);
        });

        statusCombo.addActionListener(e -> {
            if (cur == null) return;
            TaskStatus st = (TaskStatus) statusCombo.getSelectedItem();
            if (st == null) return;

            cur.updateStatus(st);
            if (st == TaskStatus.DONE) cur.updateProgress(100);

            listener.onDataChanged();
            bind(cur);
        });

        blockedCheck.addActionListener(e -> {
            if (cur == null) return;
            if (blockedCheck.isSelected()) {
                cur.updateStatus(TaskStatus.BLOCKED);
            } else {
                if (cur.getProgress() >= 100) cur.updateStatus(TaskStatus.DONE);
                else if (cur.getProgress() > 0) cur.updateStatus(TaskStatus.IN_PROGRESS);
                else cur.updateStatus(TaskStatus.NOT_STARTED);
            }
            listener.onDataChanged();
            bind(cur);
        });

        refresh();
    }

    private JComponent buildLeft() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("내 업무"));
        p.setPreferredSize(new Dimension(450, 0));
        p.add(new JScrollPane(list), BorderLayout.CENTER);
        return p;
    }

    private JComponent buildRight() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createTitledBorder("업데이트(원클릭)"));

        JPanel info = new JPanel(new GridLayout(0, 1, 4, 4));
        info.add(new JLabel("업무명:"));
        info.add(titleLabel);

        info.add(new JLabel("D-day(sim 기준):"));
        info.add(ddLabel);

        info.add(new JLabel("예상 진행률(계획 기준):"));
        info.add(expectedLabel);

        info.add(new JLabel("계획 대비 상태:"));
        info.add(deltaLabel);

        progBar.setStringPainted(true);

        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.add(info, BorderLayout.NORTH);
        top.add(progBar, BorderLayout.SOUTH);

        JPanel controls = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4,4,4,4);
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx=0; g.gridy=0; g.weightx=1; g.gridwidth=4;
        controls.add(slider, g);

        g.gridwidth=1;
        g.gridx=0; g.gridy=1;
        controls.add(minus10, g);
        g.gridx=1; g.gridy=1;
        controls.add(plus10, g);

        g.gridx=2; g.gridy=1;
        controls.add(new JLabel("상태:"), g);
        g.gridx=3; g.gridy=1;
        controls.add(statusCombo, g);

        g.gridx=0; g.gridy=2; g.gridwidth=4;
        controls.add(blockedCheck, g);

        root.add(top, BorderLayout.NORTH);
        root.add(controls, BorderLayout.CENTER);
        return root;
    }

    private void changeProgress(int delta) {
        if (cur == null) return;
        int p = cur.getProgress() + delta;
        p = Math.max(0, Math.min(100, p));
        cur.updateProgress(p);
        listener.onDataChanged();
        bind(cur);
    }

    private void bind(Task t) {
        if (t == null) {
            titleLabel.setText("-");
            ddLabel.setText("-");
            expectedLabel.setText("-");
            deltaLabel.setText("-");
            progBar.setValue(0);
            progBar.setString("0%");
            slider.setValue(0);
            blockedCheck.setSelected(false);
            return;
        }

        titleLabel.setText(t.getTitle());
        long d = ChronoUnit.DAYS.between(project.getSimDate(), t.getPlanEnd());
        ddLabel.setText("D-" + d + " (마감: " + t.getPlanEnd() + ")");

        int exp = t.getExpectedProgress(project.getSimDate());
        expectedLabel.setText(exp + "%");

        int delta = t.getScheduleDelta(project.getSimDate());
        if (delta < 0) deltaLabel.setText("계획 대비 " + (-delta) + "% 늦음");
        else if (delta > 0) deltaLabel.setText("계획 대비 " + delta + "% 빠름");
        else deltaLabel.setText("계획과 비슷");

        progBar.setValue(t.getProgress());
        progBar.setString(t.getProgress() + "%");
        slider.setValue(t.getProgress());

        statusCombo.setSelectedItem(t.getStatus());
        blockedCheck.setSelected(t.getStatus() == TaskStatus.BLOCKED);
    }

    public void refresh() {
        model.clear();
        for (Task t : project.getTasks()) {
            if (t.getAssignee() != null && loginName.equals(t.getAssignee().getName())) {
                model.addElement(t);
            }
        }
        list.repaint();
    }
}
