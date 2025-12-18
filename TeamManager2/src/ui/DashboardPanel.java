package ui;

import calc.Analyzer;
import data.Member;
import data.Project;
import data.Task;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class DashboardPanel extends JPanel {
    private final Project project;

    private final JLabel dateLabel = new JLabel();
    private final JLabel scheduleLabel = new JLabel(); // ✅ 팀 전체 계획 대비
    private final JProgressBar teamBar = new JProgressBar(0, 100);

    private final DefaultTableModel roleModel = new DefaultTableModel(
            new String[]{"카테고리", "평균 진행률"}, 0
    );
    private final JTable roleTable = new JTable(roleModel);

    // ✅ 오른쪽: 팀원별 계획 대비 늦음/빠름
    private final DefaultTableModel memberPlanModel = new DefaultTableModel(
            new String[]{"팀원", "업무수", "계획 대비"}, 0
    );
    private final JTable memberPlanTable = new JTable(memberPlanModel);

    public DashboardPanel(Project project) {
        this.project = project;
        setLayout(new BorderLayout(10, 10));

        add(buildTop(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildBottom(), BorderLayout.SOUTH);

        refresh();
    }

    private JComponent buildTop() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("요약"));

        JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT));
        line.add(new JLabel("simDate: "));
        line.add(dateLabel);
        line.add(Box.createHorizontalStrut(20));
        line.add(new JLabel("팀 전체 계획 대비: "));
        line.add(scheduleLabel);

        teamBar.setStringPainted(true);

        p.add(line, BorderLayout.NORTH);
        p.add(teamBar, BorderLayout.SOUTH);
        return p;
    }

    private JComponent buildCenter() {
        JPanel p = new JPanel(new GridLayout(1, 2, 10, 10));

        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(BorderFactory.createTitledBorder("카테고리별 평균 진행률"));
        left.add(new JScrollPane(roleTable), BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout());
        right.setBorder(BorderFactory.createTitledBorder("팀원별 계획 대비(늦음/빠름)"));
        right.add(new JScrollPane(memberPlanTable), BorderLayout.CENTER);

        p.add(left);
        p.add(right);
        return p;
    }

    private JComponent buildBottom() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton r = new JButton("새로고침");
        r.addActionListener(e -> refresh());
        p.add(r);
        return p;
    }

    public void refresh() {
        dateLabel.setText(project.getSimDate().toString());

        teamBar.setValue(project.getTeamProgress());
        teamBar.setString(project.getTeamProgress() + "%");

        int projDelta = Analyzer.calcProjectScheduleDelta(project);
        if (projDelta < 0) scheduleLabel.setText((-projDelta) + "% 늦음");
        else if (projDelta > 0) scheduleLabel.setText(projDelta + "% 빠름");
        else scheduleLabel.setText("비슷");

        // 카테고리별 평균 진행률
        Map<String, int[]> map = new LinkedHashMap<String, int[]>();
        for (Task t : project.getTasks()) {
            String cat = t.getCategory();
            if (!map.containsKey(cat)) map.put(cat, new int[]{0, 0});
            map.get(cat)[0] += t.getProgress();
            map.get(cat)[1] += 1;
        }

        roleModel.setRowCount(0);
        for (String cat : map.keySet()) {
            int sum = map.get(cat)[0];
            int cnt = map.get(cat)[1];
            int avg = (cnt == 0) ? 0 : Math.round((float) sum / cnt);
            roleModel.addRow(new Object[]{cat, avg + "%"});
        }

        // 팀원별 계획 대비
        memberPlanModel.setRowCount(0);
        for (Member m : project.getMembers()) {
            int taskCount = 0;
            for (Task t : project.getTasks()) if (t.getAssignee() == m) taskCount++;

            int delta = Analyzer.calcMemberScheduleDelta(project, m);
            String s;
            if (delta < 0) s = (-delta) + "% 늦음";
            else if (delta > 0) s = delta + "% 빠름";
            else s = "비슷";

            memberPlanModel.addRow(new Object[]{m.getName(), taskCount, s});
        }
    }
}
