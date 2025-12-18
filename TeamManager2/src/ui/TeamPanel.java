package ui;

import data.Member;
import data.Project;
import data.Task;
import data.TaskStatus;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TeamPanel extends JPanel {
    private final Project project;
    private final DataChangeListener dataChangeListener;

    private final DefaultListModel<Member> memberListModel = new DefaultListModel<Member>();
    private final JList<Member> memberJList = new JList<Member>(memberListModel);

    private final JTextField nameField = new JTextField(10);

    private final DefaultListModel<String> roleListModel = new DefaultListModel<String>();
    private final JList<String> rolePickList = new JList<String>(roleListModel);
    private final JButton addRoleBtn = new JButton("역할 추가");

    private final JComboBox<Member> assigneeCombo = new JComboBox<Member>();

    // ✅ 카테고리 = 역할 리스트
    private final DefaultComboBoxModel<String> categoryModel = new DefaultComboBoxModel<String>();
    private final JComboBox<String> categoryCombo = new JComboBox<String>(categoryModel);

    private final JTextField taskTitleField = new JTextField(14);
    private final JSlider difficultySlider = new JSlider(1, 5, 3);

    // ✅ 계획 시작/마감(등록폼은 그대로)
    private final JTextField startField = new JTextField(10);
    private final JTextField endField = new JTextField(10);

    private final JButton addTaskBtn = new JButton("업무 등록");

    // ✅ 표: 실제 시작/마감은 “표에서 직접 입력”
    private final DefaultTableModel taskTableModel = new DefaultTableModel(
            new String[]{"담당자","카테고리","업무","난이도","계획 시작","계획 마감","실제 시작","실제 마감","진행률"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            // 실제 시작(6), 실제 마감(7)만 편집 가능
            return (column == 6 || column == 7);
        }
    };
    private final JTable taskTable = new JTable(taskTableModel);

    // ✅ row -> Task 매핑용(학생식)
    private final List<Task> tableTaskRef = new ArrayList<Task>();

    public TeamPanel(Project project, DataChangeListener dataChangeListener) {
        this.project = project;
        this.dataChangeListener = dataChangeListener;

        setLayout(new BorderLayout(10, 10));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildMemberPanel(), buildTaskPanel());
        split.setResizeWeight(0.40);
        add(split, BorderLayout.CENTER);

        // ✅ 역할 다중선택: Ctrl 없이 “클릭=토글”
        rolePickList.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                if (isSelectedIndex(index0)) super.removeSelectionInterval(index0, index1);
                else super.addSelectionInterval(index0, index1);
            }
        });
        rolePickList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        reloadRoleList();
        reloadCategoryCombo();

        addRoleBtn.addActionListener(e -> addRolePopup());
        addTaskBtn.addActionListener(e -> addTask());

        // ✅ 표 편집 감지(실제 일정 입력)
        taskTableModel.addTableModelListener(e -> onTableEdited(e));

        refreshMembersUI();
        refreshTasksUI();
    }

    private JPanel buildMemberPanel() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createTitledBorder("팀원 관리"));

        JPanel input = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4,4,4,4);
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx=0; g.gridy=0; g.weightx=0;
        input.add(new JLabel("이름:"), g);
        g.gridx=1; g.gridy=0; g.weightx=1;
        input.add(nameField, g);

        g.gridx=0; g.gridy=1; g.weightx=0;
        input.add(new JLabel("역할(여러개 클릭):"), g);

        rolePickList.setVisibleRowCount(7);
        JScrollPane roleSp = new JScrollPane(rolePickList);
        g.gridx=1; g.gridy=1; g.weightx=1; g.weighty=1; g.fill = GridBagConstraints.BOTH;
        input.add(roleSp, g);

        g.gridx=1; g.gridy=2; g.weightx=0; g.weighty=0; g.fill = GridBagConstraints.NONE;
        input.add(addRoleBtn, g);

        JButton addMemberBtn = new JButton("팀원 추가");
        addMemberBtn.addActionListener(e -> addMember());
        g.gridx=1; g.gridy=3;
        input.add(addMemberBtn, g);

        root.add(input, BorderLayout.CENTER);

        memberJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        root.add(new JScrollPane(memberJList), BorderLayout.EAST);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton del = new JButton("선택 삭제");
        del.addActionListener(e -> removeSelectedMember());
        bottom.add(del);
        root.add(bottom, BorderLayout.SOUTH);

        return root;
    }

    private JPanel buildTaskPanel() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createTitledBorder("업무 등록(계획 일정 입력)"));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4,4,4,4);
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx=0; g.gridy=0; g.weightx=0;
        form.add(new JLabel("담당자:"), g);
        g.gridx=1; g.gridy=0; g.weightx=1;
        form.add(assigneeCombo, g);

        g.gridx=2; g.gridy=0; g.weightx=0;
        form.add(new JLabel("카테고리:"), g);
        g.gridx=3; g.gridy=0; g.weightx=1;
        form.add(categoryCombo, g);

        g.gridx=0; g.gridy=1; g.weightx=0;
        form.add(new JLabel("업무명:"), g);
        g.gridx=1; g.gridy=1; g.weightx=1;
        form.add(taskTitleField, g);

        g.gridx=2; g.gridy=1; g.weightx=0;
        form.add(new JLabel("난이도:"), g);
        g.gridx=3; g.gridy=1; g.weightx=1;
        difficultySlider.setMajorTickSpacing(1);
        difficultySlider.setPaintTicks(true);
        difficultySlider.setPaintLabels(true);
        form.add(difficultySlider, g);

        g.gridx=0; g.gridy=2; g.weightx=0;
        form.add(new JLabel("계획 시작:"), g);
        g.gridx=1; g.gridy=2; g.weightx=1;
        startField.setText(project.getSimDate().toString());
        form.add(startField, g);

        g.gridx=2; g.gridy=2; g.weightx=0;
        form.add(new JLabel("계획 마감:"), g);
        g.gridx=3; g.gridy=2; g.weightx=1;
        endField.setText(project.getSimDate().plusDays(7).toString());
        form.add(endField, g);

        g.gridx=3; g.gridy=3; g.weightx=0; g.fill = GridBagConstraints.NONE;
        form.add(addTaskBtn, g);

        root.add(form, BorderLayout.NORTH);
        root.add(new JScrollPane(taskTable), BorderLayout.CENTER);

        JLabel hint = new JLabel("※ 실제 시작/실제 마감은 아래 표에서 더블클릭해서 입력(YYYY-MM-DD)");
        root.add(hint, BorderLayout.SOUTH);

        return root;
    }

    private void reloadRoleList() {
        roleListModel.clear();
        for (String r : project.getRoles()) roleListModel.addElement(r);
    }

    private void reloadCategoryCombo() {
        categoryModel.removeAllElements();
        for (String r : project.getRoles()) categoryModel.addElement(r);
        if (categoryModel.getSize() == 0) categoryModel.addElement("기타");
    }

    private void addRolePopup() {
        String role = JOptionPane.showInputDialog(this, "추가할 역할/카테고리명 입력");
        if (role == null) return;
        role = role.trim();
        if (role.isEmpty()) return;

        project.addRole(role);
        reloadRoleList();
        reloadCategoryCombo(); // ✅ 역할 추가 -> 카테고리도 자동 반영
    }

    private void addMember() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "이름을 입력하세요.");
            return;
        }

        java.util.List<String> selRoles = rolePickList.getSelectedValuesList();
        String rolesStr = "미정";
        if (selRoles != null && !selRoles.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i=0;i<selRoles.size();i++){
                if (i>0) sb.append(",");
                sb.append(selRoles.get(i));
            }
            rolesStr = sb.toString();
        }

        project.addMember(new Member(name, rolesStr));
        nameField.setText("");
        rolePickList.clearSelection();

        dataChangeListener.onDataChanged();
    }

    private void removeSelectedMember() {
        Member sel = memberJList.getSelectedValue();
        if (sel == null) return;

        int ok = JOptionPane.showConfirmDialog(this, "삭제할까요?", "확인", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            project.removeMember(sel);
            dataChangeListener.onDataChanged();
        }
    }

    private void addTask() {
        Member assignee = (Member) assigneeCombo.getSelectedItem();
        if (assignee == null) {
            JOptionPane.showMessageDialog(this, "먼저 팀원을 추가하세요.");
            return;
        }

        String category = (String) categoryCombo.getSelectedItem();
        String title = taskTitleField.getText().trim();
        int diff = difficultySlider.getValue();

        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "업무명을 입력하세요.");
            return;
        }

        LocalDate planStart;
        LocalDate planEnd;
        try {
            planStart = LocalDate.parse(startField.getText().trim());
            planEnd = LocalDate.parse(endField.getText().trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "날짜 형식 오류! 예: 2025-12-31");
            return;
        }

        if (planEnd.isBefore(planStart)) {
            JOptionPane.showMessageDialog(this, "마감이 시작보다 빠를 수 없습니다.");
            return;
        }

        Task t = new Task(title, category, assignee, diff, planStart, planEnd);
        t.updateStatus(TaskStatus.NOT_STARTED);
        t.updateProgress(0);

        // 실제 일정은 비워둠(표에서 입력)
        t.setActualStart(null);
        t.setActualEnd(null);

        project.addTask(t);
        taskTitleField.setText("");

        dataChangeListener.onDataChanged();
    }

    private void onTableEdited(TableModelEvent e) {
        if (e.getType() != TableModelEvent.UPDATE) return;
        int row = e.getFirstRow();
        int col = e.getColumn();
        if (row < 0 || row >= tableTaskRef.size()) return;

        // 실제 시작(6), 실제 마감(7)만 처리
        if (col != 6 && col != 7) return;

        Task t = tableTaskRef.get(row);
        Object v = taskTableModel.getValueAt(row, col);
        String s = (v == null) ? "" : v.toString().trim();

        try {
            LocalDate d = s.isEmpty() ? null : LocalDate.parse(s);

            if (col == 6) t.setActualStart(d);
            else t.setActualEnd(d);

            // 입력 후 갱신
            dataChangeListener.onDataChanged();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "날짜 형식 오류! 예: 2025-12-31 (또는 빈칸)");
            // 원상복구
            refreshTasksUI();
        }
    }

    public void refreshMembersUI() {
        memberListModel.clear();
        assigneeCombo.removeAllItems();

        for (Member m : project.getMembers()) {
            memberListModel.addElement(m);
            assigneeCombo.addItem(m);
        }
    }

    public void refreshTasksUI() {
        taskTableModel.setRowCount(0);
        tableTaskRef.clear();

        for (Task t : project.getTasks()) {
            tableTaskRef.add(t);

            taskTableModel.addRow(new Object[]{
                    t.getAssignee().getName(),
                    t.getCategory(),
                    t.getTitle(),
                    t.getDifficulty(),
                    t.getPlanStart(),
                    t.getPlanEnd(),
                    (t.getActualStart() == null ? "" : t.getActualStart().toString()),
                    (t.getActualEnd() == null ? "" : t.getActualEnd().toString()),
                    t.getProgress() + "%"
            });
        }
    }
}