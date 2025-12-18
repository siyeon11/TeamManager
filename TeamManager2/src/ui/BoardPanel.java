package ui;

import data.Project;
import data.Task;
import data.TaskStatus;

import javax.swing.*;
import java.awt.*;
import java.time.temporal.ChronoUnit;

public class BoardPanel extends JPanel {
    private final Project project;
    private final DataChangeListener listener;

    private final DefaultListModel<Task> todoM = new DefaultListModel<Task>();
    private final DefaultListModel<Task> doingM = new DefaultListModel<Task>();
    private final DefaultListModel<Task> doneM = new DefaultListModel<Task>();
    private final DefaultListModel<Task> blockedM = new DefaultListModel<Task>();

    private final JList<Task> todoL = new JList<Task>(todoM);
    private final JList<Task> doingL = new JList<Task>(doingM);
    private final JList<Task> doneL = new JList<Task>(doneM);
    private final JList<Task> blockedL = new JList<Task>(blockedM);

    private Task selected = null;

    public BoardPanel(Project project, DataChangeListener listener) {
        this.project = project;
        this.listener = listener;

        setLayout(new BorderLayout(10, 10));
        add(buildBoard(), BorderLayout.CENTER);
        add(buildBottom(), BorderLayout.SOUTH);

        ListSelectionListenerHook(todoL);
        ListSelectionListenerHook(doingL);
        ListSelectionListenerHook(doneL);
        ListSelectionListenerHook(blockedL);

        TaskRenderer r = new TaskRenderer();
        todoL.setCellRenderer(r);
        doingL.setCellRenderer(r);
        doneL.setCellRenderer(r);
        blockedL.setCellRenderer(r);

        refresh();
    }

    private void ListSelectionListenerHook(JList<Task> l) {
        l.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        l.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selected = l.getSelectedValue();
                // 다른 리스트 선택 해제(학생식)
                if (l != todoL) todoL.clearSelection();
                if (l != doingL) doingL.clearSelection();
                if (l != doneL) doneL.clearSelection();
                if (l != blockedL) blockedL.clearSelection();
            }
        });
    }

    private JComponent buildBoard() {
        JPanel grid = new JPanel(new GridLayout(1, 4, 10, 10));
        grid.add(wrap("To Do", todoL));
        grid.add(wrap("Doing", doingL));
        grid.add(wrap("Done", doneL));
        grid.add(wrap("Blocked", blockedL));
        return grid;
    }

    private JComponent wrap(String title, JList<Task> list) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(new JScrollPane(list), BorderLayout.CENTER);
        return p;
    }

    private JComponent buildBottom() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton toTodo = new JButton("ToDo");
        JButton toDoing = new JButton("Doing");
        JButton toDone = new JButton("Done");
        JButton toBlocked = new JButton("Blocked");
        JButton refresh = new JButton("새로고침");

        toTodo.addActionListener(e -> setStatus(TaskStatus.NOT_STARTED));
        toDoing.addActionListener(e -> setStatus(TaskStatus.IN_PROGRESS));
        toDone.addActionListener(e -> setStatus(TaskStatus.DONE));
        toBlocked.addActionListener(e -> setStatus(TaskStatus.BLOCKED));
        refresh.addActionListener(e -> this.refresh());

        p.add(new JLabel("선택된 카드 상태 변경:"));
        p.add(toTodo);
        p.add(toDoing);
        p.add(toDone);
        p.add(toBlocked);
        p.add(refresh);

        return p;
    }

    private void setStatus(TaskStatus st) {
        if (selected == null) return;

        // ✅ Task 메소드 시그니처 맞추기
        selected.updateStatus(st);
        if (st == TaskStatus.DONE) selected.updateProgress(100);

        listener.onDataChanged();
    }

    public void refresh() {
        todoM.clear();
        doingM.clear();
        doneM.clear();
        blockedM.clear();

        for (Task t : project.getTasks()) {
            if (t.getStatus() == TaskStatus.DONE) doneM.addElement(t);
            else if (t.getStatus() == TaskStatus.BLOCKED) blockedM.addElement(t);
            else if (t.getStatus() == TaskStatus.IN_PROGRESS) doingM.addElement(t);
            else todoM.addElement(t);
        }
    }

    private class TaskRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                     boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Task) {
                Task t = (Task) value;

                // ✅ getDeadline() -> getPlanEnd()
                long d = ChronoUnit.DAYS.between(project.getSimDate(), t.getPlanEnd());

                int exp = t.getExpectedProgress(project.getSimDate());
                int delta = t.getScheduleDelta(project.getSimDate());
                String s = (delta < 0) ? (" / 늦음 " + (-delta) + "%") : (delta > 0 ? (" / 빠름 " + delta + "%") : "");

                setText(t.getTitle()
                        + " / " + t.getAssignee().getName()
                        + " / D-" + d
                        + " / 현재 " + t.getProgress() + "% (예상 " + exp + "%)"
                        + s);
            }
            return this;
        }
    }
}
