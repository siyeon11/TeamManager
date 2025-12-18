package app;

import data.Member;
import data.Project;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

public class HomeFrame extends JFrame {
    private DefaultListModel<Project> projectListModel = new DefaultListModel<Project>();
    private JList<Project> projectJList = new JList<Project>(projectListModel);

    public HomeFrame() {
        super("팀프로젝트 홈");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(720, 420);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout(10, 10));
        add(buildCenter(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        // ✅ 기본 프로젝트 없음 (리스트 비어있음)
    }

    private JComponent buildCenter() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("프로젝트 목록"));

        projectJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        projectJList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                         boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Project) {
                    Project pr = (Project) value;
                    setText(pr.getProjectName()
                            + "  (기간: " + pr.getProjectStart() + " ~ " + pr.getProjectEnd()
                            + ", simDate: " + pr.getSimDate() + ")");
                }
                return this;
            }
        });

        p.add(new JScrollPane(projectJList), BorderLayout.CENTER);
        return p;
    }

    private JComponent buildButtons() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton addBtn = new JButton("새 프로젝트");
        JButton renameBtn = new JButton("이름 변경");
        JButton deleteBtn = new JButton("삭제");
        JButton openBtn = new JButton("열기");

        addBtn.addActionListener(e -> addProjectAndOpen());
        renameBtn.addActionListener(e -> renameProject());
        deleteBtn.addActionListener(e -> deleteProject());
        openBtn.addActionListener(e -> openProject());

        p.add(addBtn);
        p.add(renameBtn);
        p.add(deleteBtn);
        p.add(openBtn);
        return p;
    }

    // ✅ 새 프로젝트 만들기: 이름 + 시작일 + 종료일 입력받고 바로 ProjectFrame 열기
    private void addProjectAndOpen() {
        String name = JOptionPane.showInputDialog(this, "프로젝트 이름");
        if (name == null) return;
        name = name.trim();
        if (name.isEmpty()) return;

        String s1 = JOptionPane.showInputDialog(this, "프로젝트 시작일(YYYY-MM-DD)", LocalDate.now().toString());
        if (s1 == null) return;

        String s2 = JOptionPane.showInputDialog(this, "프로젝트 종료일(YYYY-MM-DD)", LocalDate.now().plusDays(14).toString());
        if (s2 == null) return;

        LocalDate ps, pe;
        try {
            ps = LocalDate.parse(s1.trim());
            pe = LocalDate.parse(s2.trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "날짜 형식 오류! 예: 2025-12-31");
            return;
        }

        if (pe.isBefore(ps)) {
            JOptionPane.showMessageDialog(this, "종료일이 시작일보다 빠를 수 없습니다.");
            return;
        }

        Project p = new Project();
        p.setProjectName(name);
        p.setProjectStart(ps);
        p.setProjectEnd(pe);

        // ✅ 시연 편하게: simDate도 프로젝트 시작일로 맞추기
        p.setSimDate(ps);

        projectListModel.addElement(p);
        projectJList.setSelectedValue(p, true);

        // ✅ 팀원 없어도 바로 열기
        ProjectFrame f = new ProjectFrame(p, "관리자");
        f.setVisible(true);

        // 홈 리스트 갱신
        projectJList.repaint();
    }

    private void renameProject() {
        Project sel = projectJList.getSelectedValue();
        if (sel == null) {
            JOptionPane.showMessageDialog(this, "프로젝트를 선택하세요.");
            return;
        }
        String name = JOptionPane.showInputDialog(this, "새 이름", sel.getProjectName());
        if (name == null) return;
        name = name.trim();
        if (name.isEmpty()) return;

        sel.setProjectName(name);
        projectJList.repaint();
    }

    private void deleteProject() {
        Project sel = projectJList.getSelectedValue();
        if (sel == null) return;
        int ok = JOptionPane.showConfirmDialog(this, "삭제할까요?", "확인", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            projectListModel.removeElement(sel);
        }
    }

    private void openProject() {
        Project sel = projectJList.getSelectedValue();
        if (sel == null) {
            JOptionPane.showMessageDialog(this, "프로젝트를 선택하세요.");
            return;
        }

        // 팀원이 없으면 관리자 모드로 바로 열기
        if (sel.getMembers().isEmpty()) {
            ProjectFrame f = new ProjectFrame(sel, "관리자");
            f.setVisible(true);
            return;
        }

        // 팀원이 있으면 내 이름 선택하고 열기
        MemberSelectDialog dlg = new MemberSelectDialog(this, sel);
        dlg.setVisible(true);
        if (dlg.getSelectedMemberName() == null) return;

        ProjectFrame f = new ProjectFrame(sel, dlg.getSelectedMemberName());
        f.setVisible(true);
    }
}
