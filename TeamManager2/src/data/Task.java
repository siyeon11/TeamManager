package data;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Task {
    private String title;
    private String category;         // 카테고리(=역할 목록과 연동)
    private Member assignee;
    private int difficulty;

    // ✅ 계획 일정
    private LocalDate planStart;
    private LocalDate planEnd;

    // ✅ 실제 일정(표에서 직접 입력)
    private LocalDate actualStart;
    private LocalDate actualEnd;

    private int progress = 0;
    private TaskStatus status = TaskStatus.NOT_STARTED;

    public Task(String title, String category, Member assignee, int difficulty,
                LocalDate planStart, LocalDate planEnd) {
        this.title = title;
        this.category = category;
        this.assignee = assignee;
        this.difficulty = difficulty;
        this.planStart = planStart;
        this.planEnd = planEnd;
    }

    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public Member getAssignee() { return assignee; }
    public int getDifficulty() { return difficulty; }

    public LocalDate getPlanStart() { return planStart; }
    public LocalDate getPlanEnd() { return planEnd; }

    public LocalDate getActualStart() { return actualStart; }
    public LocalDate getActualEnd() { return actualEnd; }

    public void setActualStart(LocalDate d) { actualStart = d; }
    public void setActualEnd(LocalDate d) { actualEnd = d; }

    public int getProgress() { return progress; }
    public TaskStatus getStatus() { return status; }

    public void updateProgress(int p) {
        p = Math.max(0, Math.min(100, p));
        progress = p;

        if (status != TaskStatus.BLOCKED) {
            if (progress == 0) status = TaskStatus.NOT_STARTED;
            else if (progress >= 100) status = TaskStatus.DONE;
            else status = TaskStatus.IN_PROGRESS;
        }
    }

    public void updateStatus(TaskStatus st) {
        if (st == null) return;
        status = st;

        if (status == TaskStatus.DONE) progress = 100;
        if (status == TaskStatus.NOT_STARTED) progress = 0;
    }

    // ✅ simDate 기준 “계획상 예상 진행률”
    public int getExpectedProgress(LocalDate simDate) {
        if (simDate == null) return 0;
        if (planStart == null || planEnd == null) return 0;

        if (simDate.isBefore(planStart)) return 0;
        if (simDate.isAfter(planEnd) || simDate.equals(planEnd)) return 100;

        long total = ChronoUnit.DAYS.between(planStart, planEnd);
        if (total <= 0) return 100;

        long elapsed = ChronoUnit.DAYS.between(planStart, simDate);
        int exp = (int) Math.round((elapsed * 100.0) / total);
        return Math.max(0, Math.min(100, exp));
    }

    // ✅ 실제-예상(음수=늦음, 양수=빠름)
    public int getScheduleDelta(LocalDate simDate) {
        int exp = getExpectedProgress(simDate);
        return progress - exp;
    }

    @Override
    public String toString() {
        return title + " (" + status + ", " + progress + "%)";
    }
}
