package app;

import data.Member;
import data.Project;

import javax.swing.*;
import java.awt.*;

public class MemberSelectDialog extends JDialog {
    private String selectedMemberName = null;

    public MemberSelectDialog(JFrame owner, Project project) {
        super(owner, "내 이름 선택", true);
        setSize(360, 260);
        setLocationRelativeTo(owner);

        DefaultListModel<String> m = new DefaultListModel<String>();
        for (Member mem : project.getMembers()) m.addElement(mem.getName());

        final JList<String> list = new JList<String>(m);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JButton ok = new JButton("선택");
        JButton cancel = new JButton("취소");

        ok.addActionListener(e -> {
            String sel = list.getSelectedValue();
            if (sel == null) {
                JOptionPane.showMessageDialog(this, "이름을 선택하세요.");
                return;
            }
            selectedMemberName = sel;
            dispose();
        });
        cancel.addActionListener(e -> dispose());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(cancel);
        bottom.add(ok);

        add(new JScrollPane(list), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    public String getSelectedMemberName() {
        return selectedMemberName;
    }
}
