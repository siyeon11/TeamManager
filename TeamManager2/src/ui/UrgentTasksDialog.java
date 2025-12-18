package ui;

import data.Project;
import data.Task;
import data.TaskStatus;

import javax.swing.*;
import java.awt.*;
import java.time.temporal.ChronoUnit;

public class UrgentTasksDialog extends JDialog {
    public UrgentTasksDialog(JFrame owner, Project project) {
        super(owner, "마감 임박 업무", true);
        setSize(520, 380);
        setLocationRelativeTo(owner);

        DefaultListModel<String> model = new DefaultListModel<String>();

        for (Task t : project.getTasks()) {
            long daysLeft = ChronoUnit.DAYS.between(project.getSimDate(), t.getPlanEnd());
            if (daysLeft <= 3 && t.getStatus() != TaskStatus.DONE) {
                String line = t.getTitle()
                        + " / " + t.getAssignee().getName()
                        + " / D-" + daysLeft
                        + " / " + t.getProgress() + "%"
                        + " / " + t.getStatus();
                model.addElement(line);
            }
        }

        if (model.isEmpty()) {
            model.addElement("마감 임박(3일 이내) 미완료 업무가 없습니다.");
        }

        JList<String> list = new JList<String>(model);
        add(new JScrollPane(list), BorderLayout.CENTER);

        JButton close = new JButton("닫기");
        close.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(close);
        add(bottom, BorderLayout.SOUTH);
    }
}
