package app;

import data.Member;
import data.Project;
import data.Task;
import data.TaskStatus;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Home 화면(디자인 배경 이미지 기반 / 절대좌표 배치)
 * + 전체 70% 스케일 적용
 *
 * ✅ 요구사항(피그마 좌표):
 * - 배경(home_bg) 1321x837, (x=-3507,y=-49)
 * - 신규 프로젝트 버튼(home_bg_new_project_btn) 30x30, (x=-3242,y=92)
 * - 긴급알림 텍스트 (x=-3098, y=115/143/171)
 * - 전체/진행/완료 카운트 (x=-3135,y=274), (x=-2812,y=274), (x=-2489,y=274)
 * - 현재 날짜(simDate) (x=-3470,y=17) 폰트 23
 * - 프로젝트 카드(home_bg_project_subject) 278x84, (x=-3490, y=146부터 일정 간격)
 *
 * 👉 Swing에서는 배경의 좌상단을 (0,0)으로 두고,
 *    상대좌표 = (피그마좌표 - 배경피그마좌표) 로 변환해서 배치합니다.
 *    + SCALE(0.70)로 전체 축소
 */
public class HomeFrame extends JFrame {

    // ✅ 원본 배경 크기
    private static final int BASE_W = 1321;
    private static final int BASE_H = 837;

 // ✅ 전체 스케일(80%)
    private static final double SCALE = 0.85;
    private static int S(int v) { return (int) Math.round(v * SCALE); }


    // ===== 피그마 기준 좌표(배경의 원점) =====
    private static final int FIG_BG_X = -3507;
    private static final int FIG_BG_Y = -49;

    // ✅ 상대좌표 + 스케일 적용
    private static int relX(int figX) { return S(figX - FIG_BG_X); }
    private static int relY(int figY) { return S(figY - FIG_BG_Y); }

    // ===== 에셋 파일명 =====
    private static final String BG_NAME   = "home_bg.png";
    private static final String BTN_NAME  = "home_bg_new_project_btn.png";
    private static final String CARD_NAME = "home_bg_project_subject.png";

    private final DefaultListModel<Project> projectListModel = new DefaultListModel<Project>();
    private Project selectedProject = null;

    private final HomeCanvas canvas = new HomeCanvas();

    public HomeFrame() {
        super("팀프로젝트 홈");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // ✅ 배경 기준 크기(1321x837) -> 70% 축소
        setSize(S(BASE_W), S(BASE_H));
        setLocationRelativeTo(null);
        setResizable(false);

        setContentPane(canvas);

        refreshHomeUI();
    }

    // ===================== 데이터/로직 =====================

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

        // 시연 편하게: simDate도 프로젝트 시작일로 맞추기
        p.setSimDate(ps);

        projectListModel.addElement(p);
        selectedProject = p;

        refreshHomeUI();

        // ✅ 팀원 없어도 바로 열기
        openProject(p);
    }

    private void renameProject(Project p) {
        if (p == null) return;

        String name = JOptionPane.showInputDialog(this, "새 이름", p.getProjectName());
        if (name == null) return;
        name = name.trim();
        if (name.isEmpty()) return;

        p.setProjectName(name);
        refreshHomeUI();
    }

    private void deleteProject(Project p) {
        if (p == null) return;

        int ok = JOptionPane.showConfirmDialog(this, "삭제할까요?", "확인", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        projectListModel.removeElement(p);
        if (selectedProject == p) selectedProject = null;

        if (selectedProject == null && projectListModel.getSize() > 0) {
            selectedProject = projectListModel.getElementAt(0);
        }

        refreshHomeUI();
    }

    private void openProject(Project sel) {
        if (sel == null) return;

        if (sel.getMembers().isEmpty()) {
            ProjectFrame f = new ProjectFrame(sel, "관리자");
            f.setVisible(true);
            return;
        }

        MemberSelectDialog dlg = new MemberSelectDialog(this, sel);
        dlg.setVisible(true);
        if (dlg.getSelectedMemberName() == null) return;

        ProjectFrame f = new ProjectFrame(sel, dlg.getSelectedMemberName());
        f.setVisible(true);
    }

    private boolean isProjectDone(Project p) {
        if (p == null) return false;
        if (p.getTasks().isEmpty()) return false;
        for (Task t : p.getTasks()) {
            if (t.getStatus() != TaskStatus.DONE) return false;
        }
        return true;
    }

    private List<String> buildUrgentLines(Project p) {
        List<String> lines = new ArrayList<String>();
        if (p == null) return lines;

        LocalDate sim = p.getSimDate();

        int dueSoon = 0;
        int blocked = 0;
        int delayed = 0;

        for (Task t : p.getTasks()) {
            if (t.getStatus() == TaskStatus.BLOCKED) blocked++;

            if (t.getPlanEnd() != null && t.getStatus() != TaskStatus.DONE) {
                long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(sim, t.getPlanEnd());
                if (daysLeft <= 3) dueSoon++;
            }

            if (t.getStatus() != TaskStatus.DONE) {
                int delta = t.getScheduleDelta(sim);
                if (delta < 0) delayed++;
            }
        }

        if (dueSoon > 0) lines.add("• 마감 3일 이내 업무 " + dueSoon + "개 발견");
        if (blocked > 0) lines.add("• Blocked 상태 업무 " + blocked + "개");
        if (delayed > 0) lines.add("• 지연 진행중 업무 " + delayed + "개");

        return lines;
    }

    private void refreshHomeUI() {
        if (selectedProject == null && projectListModel.getSize() > 0) {
            selectedProject = projectListModel.getElementAt(0);
        }
        canvas.refreshFromModel(projectListModel, selectedProject);
    }

    // ===================== UI (배경 이미지 + 절대배치) =====================

    private class HomeCanvas extends JPanel {

        private final ImageIcon bg = loadIcon(BG_NAME);
        private final ImageIcon cardBg = loadIcon(CARD_NAME);

        // 버튼 아이콘은 버튼 크기에 맞춰 미리 스케일
        private final ImageIcon addIcon = scaleIcon(loadIcon(BTN_NAME), S(30), S(30));

        private final JButton addProjectBtn = new JButton();

        // ✅ 현재 날짜(simDate) 라벨
        private final JLabel simDateLabel = new JLabel("");

        private final JLabel[] urgentLabels = new JLabel[]{ new JLabel(""), new JLabel(""), new JLabel("") };

        private final JLabel totalCountLabel = new JLabel("0");
        private final JLabel doingCountLabel = new JLabel("0");
        private final JLabel doneCountLabel  = new JLabel("0");

        private final List<ProjectCard> cards = new ArrayList<ProjectCard>();

        HomeCanvas() {
            setLayout(null);

            // 신규 프로젝트 버튼(이미지)
            addProjectBtn.setBorderPainted(false);
            addProjectBtn.setContentAreaFilled(false);
            addProjectBtn.setFocusPainted(false);
            addProjectBtn.setOpaque(false);
            if (addIcon != null) addProjectBtn.setIcon(addIcon);

            addProjectBtn.addActionListener(e -> addProjectAndOpen());
            add(addProjectBtn);

            // ✅ simDate 라벨(피그마 좌표)
            simDateLabel.setFont(new Font("Dialog", Font.BOLD, S(23)));
            simDateLabel.setForeground(new Color(30, 30, 30));
            add(simDateLabel);

            // 긴급 알림 라인(최대 3개)
            for (JLabel l : urgentLabels) {
                l.setFont(new Font("Dialog", Font.PLAIN, S(16)));
                l.setForeground(new Color(30, 30, 30));
                l.setVisible(false);
                add(l);
            }

            // 카운트(폰트 38)
            Font countFont = new Font("Dialog", Font.BOLD, S(38));
            totalCountLabel.setFont(countFont);
            doingCountLabel.setFont(countFont);
            doneCountLabel.setFont(countFont);

            totalCountLabel.setForeground(new Color(55, 120, 255));
            doingCountLabel.setForeground(new Color(0, 150, 0));
            doneCountLabel.setForeground(new Color(155, 70, 200));

            add(totalCountLabel);
            add(doingCountLabel);
            add(doneCountLabel);
        }

        private ImageIcon scaleIcon(ImageIcon icon, int w, int h) {
            if (icon == null || icon.getImage() == null) return null;
            Image img = icon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        }

        void refreshFromModel(DefaultListModel<Project> model, Project selected) {

            // ✅ simDate 표시(선택 프로젝트 있으면 그 simDate, 없으면 오늘)
            LocalDate sim = (selected != null && selected.getSimDate() != null) ? selected.getSimDate() : LocalDate.now();
            simDateLabel.setText(sim.toString());
            simDateLabel.setBounds(relX(-3470), relY(11), S(260), S(32));

            // 1) 버튼 위치
            addProjectBtn.setBounds(relX(-3242), relY(92), S(30), S(30));

            // 2) 긴급 알림 라인(선택 프로젝트 기준)
            List<String> lines = buildUrgentLines(selected);
            int baseX = relX(-3098);
            int[] ys = new int[]{ relY(115), relY(143), relY(171) };

            for (int i = 0; i < urgentLabels.length; i++) {
                if (i < lines.size()) {
                    urgentLabels[i].setText(lines.get(i));
                    urgentLabels[i].setBounds(baseX, ys[i], S(800), S(22));
                    urgentLabels[i].setVisible(true);
                } else {
                    urgentLabels[i].setText("");
                    urgentLabels[i].setVisible(false);
                }
            }

            // 3) 프로젝트 카운트(전체/진행/완료)
            int total = model.getSize();
            int done = 0;
            int doing = 0;

            for (int i = 0; i < model.getSize(); i++) {
                Project p = model.getElementAt(i);
                if (isProjectDone(p)) done++;
                else doing++;
            }

            totalCountLabel.setText(String.valueOf(total));
            doingCountLabel.setText(String.valueOf(doing));
            doneCountLabel.setText(String.valueOf(done));

            totalCountLabel.setBounds(relX(-3140), relY(250), S(120), S(60));
            doingCountLabel.setBounds(relX(-2820), relY(250), S(120), S(60));
            doneCountLabel.setBounds(relX(-2500), relY(250), S(120), S(60));

            // 4) 프로젝트 카드 목록 리빌드
            for (ProjectCard c : cards) remove(c);
            cards.clear();

            int cardX = relX(-3490);
            int firstY = relY(146);
            int gapY = S(97);

            int max = Math.min(model.getSize(), 7);

            for (int i = 0; i < max; i++) {
                Project p = model.getElementAt(i);

                ProjectCard card = new ProjectCard(p, cardBg);
                card.setBounds(cardX, firstY + i * gapY, S(278), S(84));

                cards.add(card);
                add(card);
            }

            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (bg != null && bg.getImage() != null) {
                g.drawImage(bg.getImage(), 0, 0, getWidth(), getHeight(), null);
            } else {
                g.setColor(Color.LIGHT_GRAY);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.DARK_GRAY);
                g.drawString("배경 이미지(" + BG_NAME + ")를 찾지 못했습니다.", 20, 20);
            }
        }
    }

    private class ProjectCard extends JPanel {
        private final Project project;
        private final ImageIcon bg;

        private final JLabel nameLabel = new JLabel();
        private final JLabel dateLabel = new JLabel();

        ProjectCard(Project project, ImageIcon bg) {
            this.project = project;
            this.bg = bg;

            setLayout(null);
            setOpaque(false);

            nameLabel.setText(project.getProjectName());
            nameLabel.setFont(new Font("Dialog", Font.BOLD, S(18)));
            nameLabel.setForeground(new Color(20, 20, 20));

            String range = "";
            if (project.getProjectStart() != null && project.getProjectEnd() != null) {
                range = project.getProjectStart() + " ~ " + project.getProjectEnd();
            }
            dateLabel.setText(range);
            dateLabel.setFont(new Font("Dialog", Font.PLAIN, S(14)));
            dateLabel.setForeground(new Color(90, 90, 90));

            nameLabel.setBounds(S(16), S(20), S(240), S(20));
            dateLabel.setBounds(S(16), S(48), S(240), S(18));

            add(nameLabel);
            add(dateLabel);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        selectedProject = project;
                        refreshHomeUI();

                        if (e.getClickCount() >= 2) {
                            openProject(project);
                        }
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        showPopup(e.getX(), e.getY());
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setCursor(Cursor.getDefaultCursor());
                }
            });
        }

        private void showPopup(int x, int y) {
            JPopupMenu menu = new JPopupMenu();
            JMenuItem open = new JMenuItem("열기");
            JMenuItem rename = new JMenuItem("이름 변경");
            JMenuItem del = new JMenuItem("삭제");

            open.addActionListener(e -> openProject(project));
            rename.addActionListener(e -> renameProject(project));
            del.addActionListener(e -> deleteProject(project));

            menu.add(open);
            menu.add(rename);
            menu.addSeparator();
            menu.add(del);

            menu.show(this, x, y);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (bg != null && bg.getImage() != null) {
                g.drawImage(bg.getImage(), 0, 0, getWidth(), getHeight(), null);
            } else {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, getWidth(), getHeight());
            }

            if (selectedProject == project) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(40, 120, 255));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(2, 2, getWidth() - 4, getHeight() - 4, S(5), S(5));
                g2.dispose();
            }
        }
    }

    // ===================== 이미지 로더 =====================

    private ImageIcon loadIcon(String fileName) {
        if (fileName == null) return null;

        String[] candidates = new String[] {
                "/" + fileName,
                "/img/" + fileName,
                "/images/" + fileName,
                "/assets/" + fileName,
                "/ui/img/" + fileName
        };

        for (String c : candidates) {
            try {
                URL url = HomeFrame.class.getResource(c);
                if (url != null) return new ImageIcon(url);
            } catch (Exception ignore) {}
        }

        String[] fileCandidates = new String[] {
                fileName,
                "assets/" + fileName,
                "img/" + fileName,
                "images/" + fileName
        };

        for (String p : fileCandidates) {
            try {
                File f = new File(p);
                if (f.exists()) {
                    return new ImageIcon(ImageIO.read(f));
                }
            } catch (Exception ignore) {}
        }

        return null;
    }
}
