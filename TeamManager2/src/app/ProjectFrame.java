package app;

import calc.Analyzer;
import data.Project;
import ui.*;
import ui.GanttPanel;

import javax.swing.*;
import java.awt.*;

public class ProjectFrame extends JFrame implements DataChangeListener {
    private final Project project;
    private final String loginName;

    private final GanttPanel ganttPanel;
    private final JTabbedPane tabs = new JTabbedPane();

    private final MyTasksPanel myTasksPanel;
    private final BoardPanel boardPanel;
    private final TeamPanel teamPanel;
    private final DashboardPanel dashboardPanel;

    public ProjectFrame(Project project, String loginName) {
        super(project.getProjectName() + " - (" + loginName + ")");
        this.project = project;
        this.loginName = loginName;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1150, 680);
        setLocationRelativeTo(null);

        myTasksPanel = new MyTasksPanel(project, loginName, this);
        boardPanel = new BoardPanel(project, this);
        teamPanel = new TeamPanel(project, this);
        dashboardPanel = new DashboardPanel(project);
        ganttPanel = new GanttPanel(project);

        tabs.addTab("내 상태", myTasksPanel);
        tabs.addTab("보드", boardPanel);
        tabs.addTab("Tea", teamPanel);
        tabs.addTab("진행화면", dashboardPanel);
        tabs.addTab("Gantt", ganttPanel); //간트차트

        add(tabs, BorderLayout.CENTER);
        setJMenuBar(makeMenuBar());

        Analyzer.recalculate(project);
        dashboardPanel.refresh();
        teamPanel.refreshMembersUI();
        teamPanel.refreshTasksUI();
        myTasksPanel.refresh();
        boardPanel.refresh();

        // ✅ 접속하자마자 요약(버튼 포함)
        showStartupAlertWithButtons();
        // ✅ 첫 화면: 내 업무
        tabs.setSelectedIndex(0);
    }

    private JMenuBar makeMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu simMenu = new JMenu("임의 날짜");
        JMenuItem nextDay = new JMenuItem("1일 후");
        JMenuItem nextWeek = new JMenuItem("7일 후");
        JMenuItem setDate = new JMenuItem("날짜 지정 설정");
        JMenuItem showAlert = new JMenuItem("Show Alert Now");

        nextDay.addActionListener(e -> {
            project.advanceDays(1);
            onDataChanged();
            showStartupAlertWithButtons();
        });

        nextWeek.addActionListener(e -> {
            project.advanceDays(7);
            onDataChanged();
            showStartupAlertWithButtons();
        });

        setDate.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(this, "날짜를 입력하세요. (예)2025-12-18)", project.getSimDate().toString());
            if (input != null) {
                try {
                    java.time.LocalDate d = java.time.LocalDate.parse(input.trim());
                    project.setSimDate(d);
                    onDataChanged();
                    showStartupAlertWithButtons();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "형식 오류! 예: 2025-12-31");
                }
            }
        });

        showAlert.addActionListener(e -> showStartupAlertWithButtons());

        simMenu.add(nextDay);
        simMenu.add(nextWeek);
        simMenu.add(setDate);
        simMenu.addSeparator();
        simMenu.add(showAlert);

        bar.add(simMenu);
        return bar;
    }

    private void showStartupAlertWithButtons() {
        String msg = Analyzer.buildStartupAlert(project);

        Object[] options = new Object[]{"내 업무로 이동", "마감 임박 보기", "닫기"};
        int r = JOptionPane.showOptionDialog(
                this,
                msg,
                "프로젝트 요약(접속 알림)",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (r == 0) {
            tabs.setSelectedIndex(0); // 내 업무
        } else if (r == 1) {
            UrgentTasksDialog dlg = new UrgentTasksDialog(this, project);
            dlg.setVisible(true);
        }
    }

    @Override
    public void onDataChanged() {
        // 데이터 갱신 -> 분석 -> UI 새로고침
        Analyzer.recalculate(project);

        teamPanel.refreshMembersUI();
        teamPanel.refreshTasksUI();
        myTasksPanel.refresh();
        boardPanel.refresh();
        dashboardPanel.refresh();
        ganttPanel.refresh();

        // 기존 경고(짧은 WARNING)
        String alert = Analyzer.checkAlerts(project);
        if (alert != null) {
            JOptionPane.showMessageDialog(this, alert, "주의", JOptionPane.WARNING_MESSAGE);
        }
    }
}