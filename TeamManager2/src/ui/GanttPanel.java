package ui;

import data.Project;
import data.Task;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

public class GanttPanel extends JPanel {

    private enum Mode { PLAN, ACTUAL }

    private final Project project;
    private Mode mode = Mode.PLAN;

    // 간단 설정(학생식)
    private final int pxPerDay = 18;       // 하루를 몇 픽셀로 할지
    private final int rowH = 34;           // 한 줄 높이
    private final int barH = 24;           // 바 높이
    private final int leftLabelW = 210;    // 왼쪽 "업무명" 영역 폭
    private final int topPad = 50;         // 위 여백
    private final int leftPad = 10;        // 좌측 여백

    private final JLabel infoLabel = new JLabel();

    private final JRadioButton planBtn = new JRadioButton("계획", true);
    private final JRadioButton actualBtn = new JRadioButton("실제");

    // 바들을 올려놓는 캔버스(절대배치)
    private final JPanel canvas = new JPanel(null);
    private final JScrollPane scroll = new JScrollPane(canvas);

    // 카테고리 -> 색 매핑(고정되게)
    private final Map<String, Color> colorMap = new HashMap<String, Color>();
    private final Color[] palette = new Color[]{
            new Color(200, 230, 255),
            new Color(210, 250, 210),
            new Color(255, 230, 200),
            new Color(245, 210, 245),
            new Color(255, 245, 200),
            new Color(210, 210, 255),
            new Color(230, 230, 230)
    };

    public GanttPanel(Project project) {
        this.project = project;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createTitledBorder("간트차트 (네모 패널 버전)"));

        add(buildTop(), BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        canvas.setBackground(Color.WHITE);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        refresh();
    }

    private JComponent buildTop() {
        JPanel top = new JPanel(new BorderLayout());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ButtonGroup bg = new ButtonGroup();
        bg.add(planBtn);
        bg.add(actualBtn);

        planBtn.addActionListener(e -> { mode = Mode.PLAN; refresh(); });
        actualBtn.addActionListener(e -> { mode = Mode.ACTUAL; refresh(); });

        left.add(new JLabel("보기: "));
        left.add(planBtn);
        left.add(actualBtn);

        top.add(left, BorderLayout.WEST);

        infoLabel.setText(" ");
        top.add(infoLabel, BorderLayout.CENTER);

        return top;
    }

    // 외부(ProjectFrame)에서 데이터 바뀌면 호출해주면 됨
    public void refresh() {
        canvas.removeAll();

        LocalDate base = project.getProjectStart();
        LocalDate end = project.getProjectEnd();
        if (base == null || end == null) {
            infoLabel.setText("프로젝트 시작/종료 날짜가 없습니다.");
            canvas.setPreferredSize(new Dimension(600, 300));
            canvas.revalidate();
            canvas.repaint();
            return;
        }
        if (end.isBefore(base)) {
            // 학생식 안전장치: swap
            LocalDate tmp = base; base = end; end = tmp;
        }

        long totalDays = ChronoUnit.DAYS.between(base, end) + 1; // inclusive
        if (totalDays <= 0) totalDays = 1;

        infoLabel.setText("프로젝트 기간: " + base + " ~ " + end + " (총 " + totalDays + "일) / 현재 모드: " + (mode==Mode.PLAN ? "계획" : "실제"));

        // 업무 정렬(시작일 기준으로 보기 좋게)
        List<Task> list = new ArrayList<Task>(project.getTasks());
        Collections.sort(list, new Comparator<Task>() {
            @Override
            public int compare(Task a, Task b) {
                LocalDate as = (mode == Mode.PLAN) ? a.getPlanStart() : a.getActualStart();
                LocalDate bs = (mode == Mode.PLAN) ? b.getPlanStart() : b.getActualStart();
                if (as == null && bs == null) return 0;
                if (as == null) return 1;
                if (bs == null) return -1;
                return as.compareTo(bs);
            }
        });

        // 캔버스 크기(가로: 왼쪽 라벨 + 날짜폭)
        int timelineW = (int) (totalDays * pxPerDay);
        int totalW = leftLabelW + leftPad + timelineW + 40;
        int totalH = topPad + Math.max(1, list.size()) * rowH + 40;

        canvas.setPreferredSize(new Dimension(totalW, totalH));

        // 간단한 날짜 헤더(텍스트만)
        JLabel startLbl = new JLabel(base.toString());
        startLbl.setBounds(leftLabelW + leftPad, 10, 120, 20);
        canvas.add(startLbl);

        JLabel endLbl = new JLabel(end.toString());
        endLbl.setBounds(leftLabelW + leftPad + timelineW - 120, 10, 120, 20);
        canvas.add(endLbl);

        // 아주 단순한 "눈금" (7일마다 얇은 선)
        for (int d = 0; d <= totalDays; d += 7) {
            int x = leftLabelW + leftPad + d * pxPerDay;
            JPanel line = new JPanel();
            line.setBackground(new Color(235, 235, 235));
            line.setBounds(x, 35, 1, totalH - 60);
            canvas.add(line);
        }

        if (list.isEmpty()) {
            JLabel none = new JLabel("업무가 없습니다. (Team/Task에서 업무를 등록하세요)");
            none.setBounds(20, topPad, 500, 25);
            canvas.add(none);
            canvas.revalidate();
            canvas.repaint();
            return;
        }

        // 바 생성
        int row = 0;
        for (Task t : list) {
            int y = topPad + row * rowH;

            // 왼쪽 라벨(담당자/업무명)
            JLabel leftLabel = new JLabel(t.getAssignee().getName() + " / " + t.getTitle());
            leftLabel.setBounds(10, y, leftLabelW - 20, barH);
            canvas.add(leftLabel);

            LocalDate s = (mode == Mode.PLAN) ? t.getPlanStart() : t.getActualStart();
            LocalDate e = (mode == Mode.PLAN) ? t.getPlanEnd() : t.getActualEnd();

            // 날짜 미입력이면 회색 박스만
            if (s == null || e == null) {
                JPanel miss = makeBarPanel("미입력(" + t.getCategory() + ")", new Color(220,220,220));
                miss.setBounds(leftLabelW + leftPad, y, 110, barH);
                miss.setToolTipText(t.getTitle() + " / " + t.getCategory() + " / 날짜 미입력");
                canvas.add(miss);
                row++;
                continue;
            }

            if (e.isBefore(s)) {
                // 학생식: 잘못 넣으면 swap
                LocalDate tmp = s; s = e; e = tmp;
            }

            long startOff = ChronoUnit.DAYS.between(base, s);
            long endOff = ChronoUnit.DAYS.between(base, e);

            // 범위 밖이면 잘라서 보여주기(초보자식 clamp)
            startOff = Math.max(0, Math.min(totalDays - 1, startOff));
            endOff = Math.max(0, Math.min(totalDays - 1, endOff));
            if (endOff < startOff) endOff = startOff;

            int x = leftLabelW + leftPad + (int) startOff * pxPerDay;
            int w = (int) ((endOff - startOff + 1) * pxPerDay);
            if (w < 10) w = 10;

            Color c = colorForCategory(t.getCategory());
            JPanel bar = makeBarPanel(t.getCategory(), c);
            bar.setBounds(x, y, w, barH);

            String tip = t.getTitle()
                    + " / " + t.getAssignee().getName()
                    + " / " + t.getCategory()
                    + " / " + s + " ~ " + e
                    + " (" + (endOff - startOff + 1) + "일)";
            bar.setToolTipText(tip);

            canvas.add(bar);

            row++;
        }

        canvas.revalidate();
        canvas.repaint();
    }

    private JPanel makeBarPanel(String text, Color bg) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(bg);
        p.setBorder(BorderFactory.createLineBorder(new Color(120,120,120)));

        JLabel lbl = new JLabel(" " + text);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 12f));
        p.add(lbl, BorderLayout.CENTER);

        return p;
    }

    private Color colorForCategory(String cat) {
        if (cat == null) cat = "기타";
        cat = cat.trim();
        if (cat.isEmpty()) cat = "기타";

        if (colorMap.containsKey(cat)) return colorMap.get(cat);

        // 초보자식: 해시로 팔레트 선택(항상 같은 색)
        int idx = Math.abs(cat.hashCode()) % palette.length;
        Color c = palette[idx];
        colorMap.put(cat, c);
        return c;
    }
}