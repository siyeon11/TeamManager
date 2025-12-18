package ui;

import calc.Analyzer;
import data.Member;
import data.Project;

import javax.swing.*;
import java.awt.*;

public class DiagnosisDialog extends JDialog {
    public DiagnosisDialog(JFrame owner, Project project, Member member) {
        super(owner, "진단 보고서 - " + member.getName(), true);
        setSize(520, 520);
        setLocationRelativeTo(owner);

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setText(Analyzer.buildMemberReport(project, member));

        add(new JScrollPane(area), BorderLayout.CENTER);

        JButton close = new JButton("닫기");
        close.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(close);
        add(bottom, BorderLayout.SOUTH);
    }
}
