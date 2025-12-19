package app;

import calc.Analyzer;
import data.Member;
import data.Project;
import data.Task;
import data.TaskStatus;
import ui.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Project 화면(대시보드 + 팀 화면 커스텀)
 *
 * ✅ 화면 찌그러짐(메뉴바/인셋) 방지:
 *   - RootCanvas에 preferredSize 지정 후 pack()
 *
 * ✅ 이미지 안 깨지게(품질 유지):
 *   - 배경/버튼/카드/배지는 모두 BufferedImage로 로드 → 고퀄 스케일(멀티스텝) → 1:1로 그리기(추가 스케일링 금지)
 */
public class ProjectFrame extends JFrame implements DataChangeListener {

    private final Project project;
    private final String loginName;

    // ===== 디자인 기준 =====
    private static final int BASE_W = 1321;
    private static final int BASE_H = 837;

    // HomeFrame과 동일하게 85%로 맞춤
    private static final double SCALE = 0.85;

    // ===== 피그마 기준(대시보드) =====
    private static final int FIG_DASH_BG_X = -2037;
    private static final int FIG_DASH_BG_Y = -49;

    private static int dashRelX(int figX) { return figX - FIG_DASH_BG_X; }
    private static int dashRelY(int figY) { return figY - FIG_DASH_BG_Y; }

    // ===== 피그마 기준(팀) =====
    private static final int FIG_TEAM_BG_X = -663;
    private static final int FIG_TEAM_BG_Y = -49;

    private static int teamRelX(int figX) { return figX - FIG_TEAM_BG_X; }
    private static int teamRelY(int figY) { return figY - FIG_TEAM_BG_Y; }

    private static int S(int v) { return (int) Math.round(v * SCALE); }

    // ===== 에셋 파일명 (대시보드) =====
    private static final String BG_DASH = "dashboard_bg.png";
    private static final String HOME_BTN = "dashboard_bg_homebtn.png";

    private static final String BTN_DASH = "dashboard_btn_dashboard.png";
    private static final String BTN_DASH_CH = "dashboard_btn_dashboard_ch.png";

    private static final String BTN_TEAM = "dashboard_btn_team.png";
    private static final String BTN_TEAM_CH = "dashboard_btn_team_ch.png";

    private static final String BTN_PLAN = "dashboard_btn_plan.png";
    private static final String BTN_PLAN_CH = "dashboard_btn_plan_ch.png";

    private static final String BTN_JOB = "dashboard_btn_job.png";
    private static final String BTN_JOB_CH = "dashboard_btn_job_ch.png";

    private static final String BTN_GANTT = "dashboard_btn_gantt.png";
    private static final String BTN_GANTT_CH = "dashboard_btn_gantt_ch.png";

    // ===== 에셋 파일명 (팀) =====
    private static final String BG_TEAM = "team_bg.png";

    // ===== 에셋 파일명 (계획/진행) =====
    // 없으면 dashboard_bg로 자동 대체(이미지 안 넣어도 동작)
    private static final String BG_PLAN = "plan_bg.png";

    private static final String TEAM_BG_CATEGORY = "team_bg_category.png";               // 121x38
    private static final String TEAM_BTN_NEW_CATEGORY = "team_btn_new_category.png";   // 121x38
    private static final String TEAM_BTN_NEW_MEMBER = "team_btn_new_member.png";       // 156x50

    private static final String TEAM_BG_MEMBER = "team_bg_member.png";                 // 990x107
    private static final String TEAM_BG_MEMBER_CATEGORY = "team_bg_member_category.png"; // 105x30

    // ===== 사이즈(원본 가정) =====
    private static final int HOME_W = 107;
    private static final int HOME_H = 35;

    private static final int NAV_W = 240;
    private static final int NAV_H = 45;

    private static final int TEAM_CATEGORY_W = 121;
    private static final int TEAM_CATEGORY_H = 38;

    private static final int TEAM_NEW_MEMBER_W = 156;
    private static final int TEAM_NEW_MEMBER_H = 50;

    private static final int TEAM_MEMBER_W = 990;
    private static final int TEAM_MEMBER_H = 107;

    private static final int TEAM_MEMBER_BADGE_W = 105;
    private static final int TEAM_MEMBER_BADGE_H = 30;

    // ===== 기존 기능 패널(카드로 띄움) =====
    private final TeamPanel teamPanel;         // 기존 로직 갱신용
    private final BoardPanel boardPanel;
    private final MyTasksPanel myTasksPanel;
    private final GanttPanel ganttPanel;
    private final DashboardPanel dashboardPanel;

    private final RootCanvas canvas;

    private boolean suppressNextAlert = false;

    public ProjectFrame(Project project, String loginName) {
        super(((project != null) ? project.getProjectName() : "프로젝트") + " - (" + loginName + ")");

        if (project == null) throw new IllegalArgumentException("ProjectFrame: project is null");
        this.project = project;
        this.loginName = loginName;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);

        // 기존 패널들
        teamPanel = new TeamPanel(project, this);
        boardPanel = new BoardPanel(project, this);
        myTasksPanel = new MyTasksPanel(project, loginName, this);
        ganttPanel = new GanttPanel(project);
        dashboardPanel = new DashboardPanel(project);

        // 메인 캔버스
        canvas = new RootCanvas();
        setContentPane(canvas);

        // 메뉴바
        setJMenuBar(makeMenuBar());

        // ✅ pack()으로 콘텐츠 영역 크기를 정확히 맞춤
        pack();
        setLocationRelativeTo(null);

        // 초기 분석/갱신
        Analyzer.recalculate(project);
        onDataChanged();

        // ✅ 처음엔 대시보드가 선택된 상태
        canvas.setView(View.DASHBOARD);
    }

    // ===== 메뉴(임의 날짜) =====
    private JMenuBar makeMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu simMenu = new JMenu("임의 날짜");
        JMenuItem nextDay = new JMenuItem("1일 후");
        JMenuItem nextWeek = new JMenuItem("7일 후");
        JMenuItem setDate = new JMenuItem("날짜 지정 설정");

        nextDay.addActionListener(e -> {
            project.advanceDays(1);
            onDataChanged();
        });

        nextWeek.addActionListener(e -> {
            project.advanceDays(7);
            onDataChanged();
        });

        setDate.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(this,
                    "날짜를 입력하세요. (예)2025-12-18)",
                    project.getSimDate().toString());
            if (input != null) {
                try {
                    LocalDate d = LocalDate.parse(input.trim());
                    project.setSimDate(d);
                    onDataChanged();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "형식 오류! 예: 2025-12-31");
                }
            }
        });

        simMenu.add(nextDay);
        simMenu.add(nextWeek);
        simMenu.add(setDate);

        bar.add(simMenu);
        return bar;
    }

    // ===== 데이터 변경 콜백 =====
    @Override
    public void onDataChanged() {
        Analyzer.recalculate(project);

        // 기존 패널 갱신
        teamPanel.refreshMembersUI();
        teamPanel.refreshTasksUI();
        boardPanel.refresh();
        myTasksPanel.refresh();
        ganttPanel.refresh();
        dashboardPanel.refresh();

        // 오버레이 갱신
        canvas.refreshDashboardOverlay();
        canvas.refreshTeamOverlay();
        canvas.refreshPlanUI();

        if (!suppressNextAlert) {
            String alert = Analyzer.checkAlerts(project);
            if (alert != null) {
                JOptionPane.showMessageDialog(this, alert, "주의", JOptionPane.WARNING_MESSAGE);
            }
        }
        suppressNextAlert = false;
    }

    private void suppressAlertOnce() {
        suppressNextAlert = true;
    }

    // ===== 화면 전환 =====
    private enum View { DASHBOARD, TEAM, PLAN, JOB, GANTT }

    private class RootCanvas extends JPanel {

        // 배경(스케일된 BufferedImage로 보관)
        private final BufferedImage bgDashScaled;
        private final BufferedImage bgTeamScaled;
        private final BufferedImage bgPlanScaled;

        // 네비 버튼들
        private final JButton homeBtn;

        private final JButton dashBtn;
        private final JButton teamBtn;
        private final JButton planBtn;
        private final JButton jobBtn;
        private final JButton ganttBtn;

        // 아이콘(기본/선택)
        private final ImageIcon dashIcon, dashIconCh;
        private final ImageIcon teamIcon, teamIconCh;
        private final ImageIcon planIcon, planIconCh;
        private final ImageIcon jobIcon, jobIconCh;
        private final ImageIcon ganttIcon, ganttIconCh;

        // 프로젝트 텍스트(대시보드에서만)
        private final JLabel projectNameLabel = new JLabel();
        private final JLabel projectDateLabel = new JLabel();

        // 대시보드 숫자(전체/진행/완료/취소=BLOCKED)
        private final JLabel totalTasksLabel = new JLabel("0");
        private final JLabel doingTasksLabel = new JLabel("0");
        private final JLabel doneTasksLabel  = new JLabel("0");
        private final JLabel blockedTasksLabel  = new JLabel("0");

        // 긴급 알림(최대 3줄)
        private final JLabel[] urgentLabels = new JLabel[] { new JLabel(""), new JLabel(""), new JLabel("") };

        // 콘텐츠(PLAN/JOB/GANTT만 사용)
        private final CardLayout cardLayout = new CardLayout();
        private final JPanel content = new JPanel(cardLayout);

        // ✅ 계획/진행(업무 등록) 화면
        private final PlanRegisterPanel planPanel = new PlanRegisterPanel();

        // ===== TEAM 오버레이 =====
        private final BufferedImage teamRoleBgScaled;          // team_bg_category (121x38)
        private final ImageIcon teamAddRoleIcon;              // team_btn_new_category (121x38)
        private final ImageIcon teamAddMemberIcon;            // team_btn_new_member (156x50)
        private final BufferedImage teamMemberBgScaled;        // team_bg_member (990x107)
        private final BufferedImage teamMemberBadgeBgScaled;   // team_bg_member_category (105x30)

        private final List<JButton> teamRoleButtons = new ArrayList<>();
        private JButton teamAddRoleBtn;
        private JButton teamAddMemberBtn;

        private final List<MemberCard> memberCards = new ArrayList<>();
        private Member selectedMember = null;

        private View view = View.DASHBOARD;

        RootCanvas() {
            setLayout(null);

            // ✅ 콘텐츠 영역(그림 그리는 영역)의 "정확한" 크기
            setPreferredSize(new Dimension(S(BASE_W), S(BASE_H)));

            // 배경을 한 번만 고퀄 스케일링
            bgDashScaled = scaleImageHQ(loadImage(BG_DASH), S(BASE_W), S(BASE_H));
            bgTeamScaled = scaleImageHQ(loadImage(BG_TEAM), S(BASE_W), S(BASE_H));
            BufferedImage planRaw = loadImage(BG_PLAN);
            bgPlanScaled = (planRaw != null) ? scaleImageHQ(planRaw, S(BASE_W), S(BASE_H)) : null;

            // TEAM 에셋
            teamRoleBgScaled = scaleImageHQ(loadImage(TEAM_BG_CATEGORY), S(TEAM_CATEGORY_W), S(TEAM_CATEGORY_H));
            teamAddRoleIcon = loadScaledIcon(TEAM_BTN_NEW_CATEGORY, S(TEAM_CATEGORY_W), S(TEAM_CATEGORY_H));
            teamAddMemberIcon = loadScaledIcon(TEAM_BTN_NEW_MEMBER, S(TEAM_NEW_MEMBER_W), S(TEAM_NEW_MEMBER_H));
            teamMemberBgScaled = scaleImageHQ(loadImage(TEAM_BG_MEMBER), S(TEAM_MEMBER_W), S(TEAM_MEMBER_H));
            teamMemberBadgeBgScaled = scaleImageHQ(loadImage(TEAM_BG_MEMBER_CATEGORY), S(TEAM_MEMBER_BADGE_W), S(TEAM_MEMBER_BADGE_H));

            // ---------- 홈 버튼 ----------
            ImageIcon homeScaled = loadScaledIcon(HOME_BTN, S(HOME_W), S(HOME_H));
            homeBtn = makeImageButton(homeScaled);
            homeBtn.setBounds(
                    S(dashRelX(-2018)), S(dashRelY(-21)),
                    S(HOME_W), S(HOME_H)
            );
            homeBtn.addActionListener(e -> {
                for (Window w : Window.getWindows()) {
                    if (w instanceof HomeFrame) {
                        w.toFront();
                        w.requestFocus();
                        break;
                    }
                }
                dispose();
            });
            add(homeBtn);

            // ---------- 네비 버튼들 ----------
            dashIcon   = loadScaledIcon(BTN_DASH,     S(NAV_W), S(NAV_H));
            dashIconCh = loadScaledIcon(BTN_DASH_CH,  S(NAV_W), S(NAV_H));

            teamIcon   = loadScaledIcon(BTN_TEAM,    S(NAV_W), S(NAV_H));
            teamIconCh = loadScaledIcon(BTN_TEAM_CH, S(NAV_W), S(NAV_H));

            planIcon   = loadScaledIcon(BTN_PLAN,    S(NAV_W), S(NAV_H));
            planIconCh = loadScaledIcon(BTN_PLAN_CH, S(NAV_W), S(NAV_H));

            jobIcon   = loadScaledIcon(BTN_JOB,    S(NAV_W), S(NAV_H));
            jobIconCh = loadScaledIcon(BTN_JOB_CH, S(NAV_W), S(NAV_H));

            ganttIcon   = loadScaledIcon(BTN_GANTT,    S(NAV_W), S(NAV_H));
            ganttIconCh = loadScaledIcon(BTN_GANTT_CH, S(NAV_W), S(NAV_H));

            dashBtn = makeImageButton(dashIcon);
            teamBtn = makeImageButton(teamIcon);
            planBtn = makeImageButton(planIcon);
            jobBtn  = makeImageButton(jobIcon);
            ganttBtn= makeImageButton(ganttIcon);

            dashBtn.setBounds(S(dashRelX(-2018)), S(dashRelY(50)),  S(NAV_W), S(NAV_H));
            teamBtn.setBounds(S(dashRelX(-2018)), S(dashRelY(105)), S(NAV_W), S(NAV_H));
            planBtn.setBounds(S(dashRelX(-2018)), S(dashRelY(160)), S(NAV_W), S(NAV_H));
            jobBtn .setBounds(S(dashRelX(-2018)), S(dashRelY(215)), S(NAV_W), S(NAV_H));
            ganttBtn.setBounds(S(dashRelX(-2018)), S(dashRelY(270)), S(NAV_W), S(NAV_H));

            dashBtn.addActionListener(e -> setView(View.DASHBOARD));
            teamBtn.addActionListener(e -> setView(View.TEAM));
            planBtn.addActionListener(e -> setView(View.PLAN));
            jobBtn.addActionListener(e -> setView(View.JOB));
            ganttBtn.addActionListener(e -> setView(View.GANTT));

            add(dashBtn);
            add(teamBtn);
            add(planBtn);
            add(jobBtn);
            add(ganttBtn);

            // ---------- 프로젝트 이름/기간 ----------
            projectNameLabel.setText(project.getProjectName());
            projectNameLabel.setFont(new Font("Dialog", Font.BOLD, S(28)));
            projectNameLabel.setHorizontalAlignment(SwingConstants.LEFT);
            projectNameLabel.setBounds(S(dashRelX(-1730)), S(dashRelY(-7)), S(650), S(40));
            add(projectNameLabel);

            projectDateLabel.setText(project.getProjectStart() + " ~ " + project.getProjectEnd());
            projectDateLabel.setFont(new Font("Dialog", Font.PLAIN, S(18)));
            projectDateLabel.setForeground(new Color(0x69, 0x70, 0x7B)); // #69707B
            projectDateLabel.setHorizontalAlignment(SwingConstants.LEFT);
            projectDateLabel.setBounds(S(dashRelX(-1730)), S(dashRelY(30)), S(650), S(30));
            add(projectDateLabel);

            // ---------- 대시보드 숫자 ----------
            Font numFont = new Font("Dialog", Font.BOLD, S(38));
            totalTasksLabel.setFont(numFont);
            doingTasksLabel.setFont(numFont);
            doneTasksLabel.setFont(numFont);
            blockedTasksLabel.setFont(numFont);

            totalTasksLabel.setBounds(S(dashRelX(-1705)), S(dashRelY(359)), S(120), S(60));
            doingTasksLabel.setBounds(S(dashRelX(-1453)), S(dashRelY(359)), S(120), S(60));
            doneTasksLabel .setBounds(S(dashRelX(-1201)), S(dashRelY(359)), S(120), S(60));
            blockedTasksLabel.setBounds(S(dashRelX(-948)), S(dashRelY(359)), S(120), S(60));

            add(totalTasksLabel);
            add(doingTasksLabel);
            add(doneTasksLabel);
            add(blockedTasksLabel);

            // ---------- 긴급 알림 라인 ----------
            int ux = S(dashRelX(-1730));
            int uy0 = S(dashRelY(440));
            int lineH = S(26);

            for (int i = 0; i < urgentLabels.length; i++) {
                JLabel l = urgentLabels[i];
                l.setFont(new Font("Dialog", Font.PLAIN, S(16)));
                l.setForeground(new Color(30, 30, 30));
                l.setBounds(ux, uy0 + i * lineH, S(900), S(22));
                l.setVisible(false);
                add(l);
            }

            // ---------- 콘텐츠 영역(카드 전환) ----------
            content.setOpaque(false);

            JPanel empty = new JPanel();
            empty.setOpaque(false);

            // dash/team는 오버레이만 사용
            content.add(empty, "dash");
            content.add(empty, "team");

            // 기존 패널은 PLAN/JOB/GANTT에만 올림
            // ✅ PLAN: 업무 등록/계획 입력 화면
            content.add(planPanel, "plan");
            // ✅ JOB: 업무 보드(칸반)
            content.add(wrapPanel(boardPanel), "job");
            content.add(wrapPanel(ganttPanel), "gantt");

            int cx = S(dashRelX(-1730));
            int cy = S(dashRelY(90));
            int cw = S(BASE_W) - cx - S(40);
            int ch = S(BASE_H) - cy - S(40);
            content.setBounds(cx, cy, cw, ch);
            add(content);

            // TEAM UI 생성/배치(초기에는 숨김)
            buildTeamOverlay();

            refreshDashboardOverlay();
            applyNavIcons();
            setView(View.DASHBOARD);
        }

        private JPanel wrapPanel(JComponent inner) {
            JPanel p = new JPanel(new BorderLayout());
            p.setOpaque(false);
            p.add(inner, BorderLayout.CENTER);
            return p;
        }

        // ===== TEAM 오버레이 =====
        private void buildTeamOverlay() {
            // 1) 역할 버튼들(프로젝트 roles 기반)
            rebuildTeamRoleButtons();

            // 2) 멤버 추가 버튼
            teamAddMemberBtn = makeImageButton(teamAddMemberIcon);
            teamAddMemberBtn.setBounds(
                    S(teamRelX(478)), S(teamRelY(-14)),
                    S(TEAM_NEW_MEMBER_W), S(TEAM_NEW_MEMBER_H)
            );
            teamAddMemberBtn.addActionListener(e -> {
                String name = JOptionPane.showInputDialog(ProjectFrame.this, "이름을 입력하세요.");
                if (name == null) return;
                name = name.trim();
                if (name.isEmpty()) return;

                suppressAlertOnce();

                // 멤버 추가
                // Member(String name, String rolesStr)
                project.addMember(new Member(name, ""));

                // 선택은 새 멤버로
                selectedMember = findMemberByName(name);

                // 멤버 카드 재생성
                rebuildMemberCards();
                refreshTeamOverlay();
                onDataChanged();
            });
            add(teamAddMemberBtn);
            teamAddMemberBtn.setVisible(false);

            // 3) 멤버 카드 생성/배치
            rebuildMemberCards();
            setTeamOverlayVisible(false);
        }

        private Member findMemberByName(String name) {
            for (Member m : project.getMembers()) {
                if (m.getName() != null && m.getName().equals(name)) return m;
            }
            return null;
        }

        private void rebuildTeamRoleButtons() {
            // 기존 버튼 제거
            for (JButton b : teamRoleButtons) remove(b);
            teamRoleButtons.clear();
            if (teamAddRoleBtn != null) {
                remove(teamAddRoleBtn);
                teamAddRoleBtn = null;
            }

            // 기본 배치: 첫 역할 -331,144부터 (3개 기준이면 add버튼이 71,144)
            final int startX = -331;
            final int y = 144;
            final int step = 134; // (-331,-197,-63,71) 맞추는 간격

            List<String> roles = project.getRoles();
            for (int i = 0; i < roles.size(); i++) {
                final String roleName = roles.get(i); // ✅ effectively final

                JButton roleBtn = makeImageTextButton(
                        (teamRoleBgScaled != null) ? new ImageIcon(teamRoleBgScaled) : null,
                        roleName,
                        18,
                        new Color(30, 30, 30)
                );
                int x = startX + i * step;
                roleBtn.setBounds(
                        S(teamRelX(x)), S(teamRelY(y)),
                        S(TEAM_CATEGORY_W), S(TEAM_CATEGORY_H)
                );
                roleBtn.addActionListener(e -> assignRoleToSelected(roleName));
                add(roleBtn);
                teamRoleButtons.add(roleBtn);
            }

            // addRole 버튼(항상 마지막)
            int addX = startX + roles.size() * step; // roles=3이면 71
            teamAddRoleBtn = makeImageButton(teamAddRoleIcon);
            teamAddRoleBtn.setBounds(
                    S(teamRelX(addX)), S(teamRelY(y)),
                    S(TEAM_CATEGORY_W), S(TEAM_CATEGORY_H)
            );
            teamAddRoleBtn.addActionListener(e -> {
                String role = JOptionPane.showInputDialog(ProjectFrame.this, "역할(카테고리) 이름을 입력하세요.");
                if (role == null) return;
                role = role.trim();
                if (role.isEmpty()) return;

                suppressAlertOnce();

                project.addRole(role);

                // 새 역할 버튼은 '추가 버튼'이 있던 자리에 생성
                Rectangle r = teamAddRoleBtn.getBounds();
                final String roleName = role; // ✅ effectively final

                JButton roleBtn = makeImageTextButton(
                        (teamRoleBgScaled != null) ? new ImageIcon(teamRoleBgScaled) : null,
                        roleName,
                        18,
                        new Color(30, 30, 30)
                );
                roleBtn.setBounds(r);
                roleBtn.addActionListener(ev -> assignRoleToSelected(roleName));

                add(roleBtn);
                teamRoleButtons.add(roleBtn);

                // ✅ 요구사항: addRole 버튼은 원래 위치에서 x + 118 이동
                teamAddRoleBtn.setBounds(
                        r.x + S(118),
                        r.y,
                        r.width,
                        r.height
                );

                refreshTeamOverlay();
                onDataChanged();
            });
            add(teamAddRoleBtn);

            // 기본은 숨김
            for (JButton b : teamRoleButtons) b.setVisible(false);
            teamAddRoleBtn.setVisible(false);
        }

        private void assignRoleToSelected(String roleName) {
            if (selectedMember == null) {
                JOptionPane.showMessageDialog(ProjectFrame.this, "멤버를 먼저 선택하세요.");
                return;
            }

            suppressAlertOnce();

            List<String> roles = selectedMember.getRoles();
            if (roles == null) roles = new ArrayList<>();

            if (!roles.contains(roleName)) {
                roles.remove("미정");
                roles.add(roleName);
                selectedMember.setRoles(roles);
            }

            refreshTeamOverlay();
            onDataChanged();
        }

        private void rebuildMemberCards() {
            for (MemberCard c : memberCards) remove(c);
            memberCards.clear();

            List<Member> members = project.getMembers();
            for (int i = 0; i < members.size(); i++) {
                Member m = members.get(i);

                MemberCard card = new MemberCard(m, i);
                memberCards.add(card);
                add(card);
            }

            // 기본 숨김
            for (MemberCard c : memberCards) c.setVisible(false);
        }

        private void setTeamOverlayVisible(boolean on) {
            for (JButton b : teamRoleButtons) b.setVisible(on);
            if (teamAddRoleBtn != null) teamAddRoleBtn.setVisible(on);
            if (teamAddMemberBtn != null) teamAddMemberBtn.setVisible(on);
            for (MemberCard c : memberCards) c.setVisible(on);

            if (!on) {
                selectedMember = null;
                for (MemberCard c : memberCards) c.setSelected(false);
            }
        }

        void refreshTeamOverlay() {
            // 멤버 카드만 갱신
            for (MemberCard c : memberCards) {
                c.refreshBadges();
                c.repaint();
            }
        }

        // ✅ PLAN(업무 등록) 화면 갱신
        void refreshPlanUI() {
            planPanel.refreshFromProject();
        }

        // ===== 멤버 카드 컴포넌트 =====
        private class MemberCard extends JComponent {
            private final Member member;
            private final int index;

            private final JLabel nameLabel = new JLabel();
            private final List<RoleBadge> badges = new ArrayList<>();

            private boolean selected = false;

            MemberCard(Member member, int index) {
                this.member = member;
                this.index = index;

                setLayout(null);
                setOpaque(false);

                // 위치/크기
                applyBounds();

                // 이름
                nameLabel.setText(member.getName());
                nameLabel.setFont(new Font("Dialog", Font.BOLD, S(20)));
                nameLabel.setHorizontalAlignment(SwingConstants.LEFT);

                // 이름 좌표(피그마 기준): x:-255, y:269(첫 번째) / y:+122씩
                int cardX = getX();
                int cardY = getY();

                int nameX = S(teamRelX(-255)) - cardX;
                int nameY = S(teamRelY(269 + index * 122)) - cardY;

                nameLabel.setBounds(nameX, nameY, S(700), S(28));
                add(nameLabel);

                // 선택 처리
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        selectedMember = MemberCard.this.member;
                        for (MemberCard c : memberCards) c.setSelected(c == MemberCard.this);
                        repaint();
                    }
                });

                refreshBadges();
            }

            private void applyBounds() {
                // 멤버 카드 위치(피그마 기준): x:-356, y:247(첫번째), 두번째는 y:369 => +122
                int x = S(teamRelX(-356));
                int y = S(teamRelY(247 + index * 122));
                setBounds(x, y, S(TEAM_MEMBER_W), S(TEAM_MEMBER_H));
            }

            void setSelected(boolean on) {
                this.selected = on;
                repaint();
            }

            void refreshBadges() {
                // 기존 배지 제거
                for (RoleBadge b : badges) remove(b);
                badges.clear();

                List<String> roles = member.getRoles();
                if (roles == null) roles = new ArrayList<>();

                // 표시할 역할만(미정 제외)
                List<String> show = new ArrayList<>();
                for (String r : roles) {
                    if (r == null) continue;
                    if ("미정".equals(r)) continue;
                    if (!show.contains(r)) show.add(r);
                }

                // 배치(피그마 기준): 첫 배지 x:-255, y:303 / 다음은 x:+118
                int cardX = getX();
                int cardY = getY();

                int baseYFig = 303 + index * 122;
                int baseXFig = -255;

                for (int i = 0; i < show.size(); i++) {
                    String role = show.get(i);

                    int badgeX = S(teamRelX(baseXFig + i * 118)) - cardX;
                    int badgeY = S(teamRelY(baseYFig)) - cardY;

                    RoleBadge badge = new RoleBadge(role);
                    badge.setBounds(badgeX, badgeY, S(TEAM_MEMBER_BADGE_W), S(TEAM_MEMBER_BADGE_H));
                    add(badge);
                    badges.add(badge);
                }

                revalidate();
                repaint();
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                if (teamMemberBgScaled != null) {
                    g.drawImage(teamMemberBgScaled, 0, 0, null); // 1:1
                } else {
                    g.setColor(new Color(240, 240, 240));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }

                if (selected) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(new Color(0, 120, 215));
                    g2.setStroke(new BasicStroke(Math.max(1f, S(2))));
                    g2.drawRoundRect(S(2), S(2), getWidth() - S(4), getHeight() - S(4), S(12), S(12));
                    g2.dispose();
                }
            }
        }

        private class RoleBadge extends JComponent {
            private final JLabel label = new JLabel();

            RoleBadge(String text) {
                setLayout(null);
                setOpaque(false);

                label.setText(text);
                label.setFont(new Font("Dialog", Font.BOLD, S(16)));
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setVerticalAlignment(SwingConstants.CENTER);
                label.setBounds(0, 0, S(TEAM_MEMBER_BADGE_W), S(TEAM_MEMBER_BADGE_H));
                add(label);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (teamMemberBadgeBgScaled != null) {
                    g.drawImage(teamMemberBadgeBgScaled, 0, 0, null);
                } else {
                    g.setColor(new Color(230, 230, 230));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        }

        // ===== 대시보드 오버레이 =====
        void refreshDashboardOverlay() {
            projectNameLabel.setText(project.getProjectName());
            projectDateLabel.setText(project.getProjectStart() + " ~ " + project.getProjectEnd());

            int total = project.getTasks().size();
            int done = 0, doing = 0, blocked = 0;

            for (Task t : project.getTasks()) {
                if (t.getStatus() == TaskStatus.DONE) done++;
                else if (t.getStatus() == TaskStatus.IN_PROGRESS) doing++;
                else if (t.getStatus() == TaskStatus.BLOCKED) blocked++;
            }

            totalTasksLabel.setText(String.valueOf(total));
            doingTasksLabel.setText(String.valueOf(doing));
            doneTasksLabel.setText(String.valueOf(done));
            blockedTasksLabel.setText(String.valueOf(blocked));

            List<String> lines = buildUrgentLines();
            for (int i = 0; i < urgentLabels.length; i++) {
                if (i < lines.size()) urgentLabels[i].setText(lines.get(i));
                else urgentLabels[i].setText("");
            }

            setDashboardOverlayVisible(view == View.DASHBOARD);
        }

        private List<String> buildUrgentLines() {
            List<String> lines = new ArrayList<>();

            LocalDate sim = project.getSimDate();
            int dueSoon = 0;
            int zeroProgressDueSoon = 0;
            int blocked = 0;
            int delayed = 0;

            for (Task t : project.getTasks()) {
                if (t.getStatus() == TaskStatus.BLOCKED) blocked++;

                if (t.getPlanEnd() != null && t.getStatus() != TaskStatus.DONE) {
                    long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(sim, t.getPlanEnd());
                    if (daysLeft <= 3) {
                        dueSoon++;
                        if (t.getProgress() == 0) zeroProgressDueSoon++;
                    }
                }

                if (t.getStatus() != TaskStatus.DONE) {
                    int delta = t.getScheduleDelta(sim);
                    if (delta < 0) delayed++;
                }
            }

            if (zeroProgressDueSoon > 0) lines.add("• 마감 임박인데 0% 업무 " + zeroProgressDueSoon + "개");
            else if (dueSoon > 0) lines.add("• 마감 3일 이내 미완료 업무 " + dueSoon + "개");

            if (blocked > 0) lines.add("• 취소/막힘(BLOCKED) 업무 " + blocked + "개");
            if (delayed > 0) lines.add("• 계획 대비 지연 업무 " + delayed + "개");

            if (lines.size() > 3) return lines.subList(0, 3);
            return lines;
        }

        private void setDashboardOverlayVisible(boolean on) {
            totalTasksLabel.setVisible(on);
            doingTasksLabel.setVisible(on);
            doneTasksLabel.setVisible(on);
            blockedTasksLabel.setVisible(on);
            for (JLabel l : urgentLabels) {
                l.setVisible(on && l.getText() != null && !l.getText().isEmpty());
            }
        }

        // ==================================================
        // PLAN 화면: 업무 등록(계획 일정 입력) + 표 출력
        // - 기존 TeamPanel의 업무 등록 기능을 그대로 옮긴 버전
        // - 디자인(배치)은 자유롭게, 기능(등록/표 갱신/실제 시작·마감 수정)은 유지
        // ==================================================
        private class PlanRegisterPanel extends JPanel {

            private final JComboBox<Member> assigneeCombo = new JComboBox<>();
            private final JComboBox<String> categoryCombo = new JComboBox<>();
            private final JTextField titleField = new JTextField();
            private final JSlider diffSlider = new JSlider(1, 5, 3);
            private final JTextField planStartField = new JTextField();
            private final JTextField planEndField = new JTextField();
            private final JButton addBtn = new JButton("업무 등록");

            private final TaskTableModel tableModel = new TaskTableModel();
            private final JTable table = new JTable(tableModel);

            PlanRegisterPanel() {
                setLayout(new BorderLayout(12, 12));
                setOpaque(false);

                // ===== 입력 폼 =====
                JPanel form = new JPanel(new GridBagLayout());
                form.setOpaque(false);

                GridBagConstraints gc = new GridBagConstraints();
                gc.insets = new Insets(6, 6, 6, 6);
                gc.fill = GridBagConstraints.HORIZONTAL;
                gc.weightx = 1;

                Font labelFont = new Font("Dialog", Font.PLAIN, Math.max(12, S(14)));

                // 담당자
                gc.gridx = 0; gc.gridy = 0; gc.weightx = 0;
                JLabel lAssignee = new JLabel("담당자:");
                lAssignee.setFont(labelFont);
                form.add(lAssignee, gc);

                gc.gridx = 1; gc.gridy = 0; gc.weightx = 1;
                form.add(assigneeCombo, gc);

                // 카테고리
                gc.gridx = 2; gc.gridy = 0; gc.weightx = 0;
                JLabel lCat = new JLabel("카테고리:");
                lCat.setFont(labelFont);
                form.add(lCat, gc);

                gc.gridx = 3; gc.gridy = 0; gc.weightx = 1;
                form.add(categoryCombo, gc);

                // 업무명
                gc.gridx = 0; gc.gridy = 1; gc.weightx = 0;
                JLabel lTitle = new JLabel("업무명:");
                lTitle.setFont(labelFont);
                form.add(lTitle, gc);

                gc.gridx = 1; gc.gridy = 1; gc.gridwidth = 3; gc.weightx = 1;
                form.add(titleField, gc);
                gc.gridwidth = 1;

                // 난이도
                gc.gridx = 0; gc.gridy = 2; gc.weightx = 0;
                JLabel lDiff = new JLabel("난이도:");
                lDiff.setFont(labelFont);
                form.add(lDiff, gc);

                gc.gridx = 1; gc.gridy = 2; gc.weightx = 1;
                diffSlider.setMajorTickSpacing(1);
                diffSlider.setPaintTicks(true);
                diffSlider.setPaintLabels(true);
                form.add(diffSlider, gc);

                // 계획 시작
                gc.gridx = 0; gc.gridy = 3; gc.weightx = 0;
                JLabel lPs = new JLabel("계획 시작:");
                lPs.setFont(labelFont);
                form.add(lPs, gc);

                gc.gridx = 1; gc.gridy = 3; gc.weightx = 1;
                form.add(planStartField, gc);

                // 계획 마감
                gc.gridx = 2; gc.gridy = 3; gc.weightx = 0;
                JLabel lPe = new JLabel("계획 마감:");
                lPe.setFont(labelFont);
                form.add(lPe, gc);

                gc.gridx = 3; gc.gridy = 3; gc.weightx = 1;
                form.add(planEndField, gc);

                // 등록 버튼
                gc.gridx = 3; gc.gridy = 4; gc.weightx = 0;
                gc.anchor = GridBagConstraints.EAST;
                addBtn.setPreferredSize(new Dimension(Math.max(90, S(120)), Math.max(28, S(34))));
                form.add(addBtn, gc);
                gc.anchor = GridBagConstraints.CENTER;

                add(form, BorderLayout.NORTH);

                // ===== 표 =====
                table.setRowHeight(Math.max(22, S(26)));
                table.setFillsViewportHeight(true);
                JScrollPane sp = new JScrollPane(table);
                add(sp, BorderLayout.CENTER);

                // 콤보 렌더러(멤버 이름만 보이게)
                assigneeCombo.setRenderer(new DefaultListCellRenderer() {
                    @Override
                    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                        if (value instanceof Member m) {
                            setText(m.getName());
                        }
                        return this;
                    }
                });

                // 기본 날짜
                planStartField.setText(project.getSimDate().toString());
                planEndField.setText(project.getSimDate().plusDays(7).toString());

                // 등록 액션
                addBtn.addActionListener(e -> addTask());

                // 초기 데이터 로딩
                refreshFromProject();
            }

            private void addTask() {
                String title = titleField.getText().trim();
                if (title.isEmpty()) {
                    JOptionPane.showMessageDialog(ProjectFrame.this, "업무명을 입력하세요.");
                    return;
                }

                String category = (String) categoryCombo.getSelectedItem();
                if (category == null) category = "";

                Member assignee = (Member) assigneeCombo.getSelectedItem();
                if (assignee == null) {
                    assignee = ensureDefaultAssignee();
                }

                LocalDate ps;
                LocalDate pe;
                try {
                    ps = LocalDate.parse(planStartField.getText().trim());
                    pe = LocalDate.parse(planEndField.getText().trim());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ProjectFrame.this, "날짜 형식 오류! 예: 2025-12-31");
                    return;
                }

                if (pe.isBefore(ps)) {
                    JOptionPane.showMessageDialog(ProjectFrame.this, "계획 마감이 계획 시작보다 빠를 수 없습니다.");
                    return;
                }

                int diff = diffSlider.getValue();

                // ✅ Task 클래스는 (title, category, assignee, difficulty, planStart, planEnd) 생성자를 사용
                Task t = new Task(title, category, assignee, diff, ps, pe);
                project.addTask(t);

                // 입력 초기화(업무명만)
                titleField.setText("");

                // 전체 갱신
                ProjectFrame.this.onDataChanged();
            }

            private Member ensureDefaultAssignee() {
                // 멤버가 0명이어도 업무 등록이 가능하도록 기본 담당자 보장
                Member m = project.findMemberByName(loginName);
                if (m == null) {
                    m = new Member(loginName, "");
                    project.addMember(m);
                }
                return m;
            }

            void refreshFromProject() {
                // 멤버 콤보 갱신
                Member selected = (Member) assigneeCombo.getSelectedItem();
                assigneeCombo.removeAllItems();

                if (project.getMembers().isEmpty()) {
                    // 비어있으면 기본 담당자 하나라도 넣기
                    Member m = ensureDefaultAssignee();
                    assigneeCombo.addItem(m);
                } else {
                    for (Member m : project.getMembers()) assigneeCombo.addItem(m);
                }

                if (selected != null) {
                    // 동일 이름의 멤버가 있으면 재선택
                    Member re = project.findMemberByName(selected.getName());
                    if (re != null) assigneeCombo.setSelectedItem(re);
                }

                // 카테고리 콤보 갱신(roles)
                Object selCat = categoryCombo.getSelectedItem();
                categoryCombo.removeAllItems();
                if (project.getRoles().isEmpty()) {
                    categoryCombo.addItem("기본");
                } else {
                    for (String r : project.getRoles()) categoryCombo.addItem(r);
                }
                if (selCat != null) categoryCombo.setSelectedItem(selCat);

                // 표 갱신
                tableModel.reload();
            }

            // ===== 표 모델(TeamPanel 로직 이식) =====
            private class TaskTableModel extends javax.swing.table.AbstractTableModel {

                private final String[] cols = {
                        "담당자", "카테고리", "업무", "난이도",
                        "계획 시작", "계획 마감", "실제 시작", "실제 마감", "진행률"
                };

                @Override
                public int getRowCount() {
                    return project.getTasks().size();
                }

                @Override
                public int getColumnCount() {
                    return cols.length;
                }

                @Override
                public String getColumnName(int column) {
                    return cols[column];
                }

                @Override
                public Object getValueAt(int rowIndex, int columnIndex) {
                    Task t = project.getTasks().get(rowIndex);
                    return switch (columnIndex) {
                        case 0 -> (t.getAssignee() != null ? t.getAssignee().getName() : "");
                        case 1 -> t.getCategory();
                        case 2 -> t.getTitle();
                        case 3 -> t.getDifficulty();
                        case 4 -> t.getPlanStart();
                        case 5 -> t.getPlanEnd();
                        case 6 -> t.getActualStart();
                        case 7 -> t.getActualEnd();
                        case 8 -> t.getProgress();
                        default -> "";
                    };
                }

                @Override
                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    // 실제 시작/마감/진행률만 수정 가능
                    return columnIndex == 6 || columnIndex == 7 || columnIndex == 8;
                }

                @Override
                public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                    Task t = project.getTasks().get(rowIndex);
                    try {
                        if (columnIndex == 6) {
                            t.setActualStart(parseOrNull(aValue));
                        } else if (columnIndex == 7) {
                            t.setActualEnd(parseOrNull(aValue));
                        } else if (columnIndex == 8) {
                            int p = Integer.parseInt(String.valueOf(aValue));
                            t.updateProgress(p);
                        }
                        ProjectFrame.this.onDataChanged();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ProjectFrame.this, "입력 형식 오류!\n날짜: YYYY-MM-DD\n진행률: 0~100");
                        fireTableRowsUpdated(rowIndex, rowIndex);
                    }
                }

                private LocalDate parseOrNull(Object v) {
                    if (v == null) return null;
                    String s = String.valueOf(v).trim();
                    if (s.isEmpty()) return null;
                    return LocalDate.parse(s);
                }

                void reload() {
                    fireTableDataChanged();
                }
            }
        }

        // ===== 화면 전환 =====
        void setView(View v) {
            this.view = v;
            applyNavIcons();

            boolean isDash = (v == View.DASHBOARD);
            boolean isTeam = (v == View.TEAM);

            // ✅ 팀 화면에서 프로젝트명/기간 숨김
            projectNameLabel.setVisible(isDash);
            projectDateLabel.setVisible(isDash);
            setDashboardOverlayVisible(isDash);

            // TEAM 오버레이
            setTeamOverlayVisible(isTeam);

            // 카드
            switch (v) {
                case DASHBOARD:
                    cardLayout.show(content, "dash");
                    break;
                case TEAM:
                    cardLayout.show(content, "team");
                    break;
                case PLAN:
                    cardLayout.show(content, "plan");
                    break;
                case JOB:
                    cardLayout.show(content, "job");
                    break;
                case GANTT:
                    cardLayout.show(content, "gantt");
                    break;
            }

            repaint();
        }

        private void applyNavIcons() {
            setButtonIcon(dashBtn, (view == View.DASHBOARD) ? dashIconCh : dashIcon);
            setButtonIcon(teamBtn, (view == View.TEAM) ? teamIconCh : teamIcon);
            setButtonIcon(planBtn, (view == View.PLAN) ? planIconCh : planIcon);
            setButtonIcon(jobBtn,  (view == View.JOB) ? jobIconCh : jobIcon);
            setButtonIcon(ganttBtn,(view == View.GANTT) ? ganttIconCh : ganttIcon);
        }

        private void setButtonIcon(JButton btn, ImageIcon icon) {
            if (icon != null) {
                btn.setIcon(icon);
                btn.setText(null);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            BufferedImage bg;
            if (view == View.TEAM && bgTeamScaled != null) bg = bgTeamScaled;
            else if (view == View.PLAN && bgPlanScaled != null) bg = bgPlanScaled;
            else bg = bgDashScaled;

            if (bg != null) {
                g.drawImage(bg, 0, 0, null); // ✅ 1:1
            } else {
                g.setColor(Color.LIGHT_GRAY);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.DARK_GRAY);
                g.drawString("배경 이미지를 찾지 못했습니다.", 20, 20);
            }
        }
    }

    // ===== 버튼 생성(투명 이미지 버튼) =====
    private JButton makeImageButton(ImageIcon icon) {
        JButton b = new JButton();
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMargin(new Insets(0,0,0,0));
        if (icon != null) b.setIcon(icon);
        return b;
    }

    private JButton makeImageTextButton(ImageIcon bg, String text, int fontSize, Color fg) {
        JButton b = makeImageButton(bg);
        b.setText(text);
        b.setFont(new Font("Dialog", Font.BOLD, S(fontSize)));
        b.setForeground(fg);
        b.setHorizontalTextPosition(SwingConstants.CENTER);
        b.setVerticalTextPosition(SwingConstants.CENTER);
        return b;
    }

    // ===== 이미지 로더(BufferedImage) =====
    private BufferedImage loadImage(String fileName) {
        if (fileName == null) return null;

        // 1) classpath에서 찾기
        String[] candidates = new String[] {
                "/" + fileName,
                "/img/" + fileName,
                "/images/" + fileName,
                "/assets/" + fileName,
                "/ui/img/" + fileName
        };

        for (String c : candidates) {
            try {
                URL url = ProjectFrame.class.getResource(c);
                if (url != null) {
                    BufferedImage img = ImageIO.read(url);
                    return toCompatible(img);
                }
            } catch (Exception ignore) {}
        }

        // 2) 실행 경로 기준 파일로 찾기
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
                    BufferedImage img = ImageIO.read(f);
                    return toCompatible(img);
                }
            } catch (Exception ignore) {}
        }

        return null;
    }

    private BufferedImage toCompatible(BufferedImage src) {
        if (src == null) return null;
        int type = src.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        if (src.getType() == type) return src;
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), type);
        Graphics2D g2 = dst.createGraphics();
        g2.drawImage(src, 0, 0, null);
        g2.dispose();
        return dst;
    }

    // ===== 고퀄 스케일링(멀티스텝) =====
    private BufferedImage scaleImageHQ(BufferedImage src, int targetW, int targetH) {
        if (src == null) return null;
        if (targetW <= 0 || targetH <= 0) return src;
        if (src.getWidth() == targetW && src.getHeight() == targetH) return src;

        BufferedImage img = src;

        // 다운스케일일 때는 멀티스텝(반씩 줄이기)로 선명도 유지
        if (targetW < img.getWidth() && targetH < img.getHeight()) {
            int w = img.getWidth();
            int h = img.getHeight();
            while (w / 2 >= targetW && h / 2 >= targetH) {
                w /= 2;
                h /= 2;
                img = scaleOnce(img, w, h);
            }
        }

        return scaleOnce(img, targetW, targetH);
    }

    private BufferedImage scaleOnce(BufferedImage src, int w, int h) {
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = dst.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(src, 0, 0, w, h, null);
        g2.dispose();
        return dst;
    }

    private ImageIcon loadScaledIcon(String fileName, int w, int h) {
        BufferedImage raw = loadImage(fileName);
        if (raw == null) return null;
        BufferedImage scaled = scaleImageHQ(raw, w, h);
        return new ImageIcon(scaled);
    }
}
