package calc;

import data.Member;
import data.Project;
import data.Task;
import data.TaskStatus;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Analyzer {

    public static void recalculate(Project project) {
        int total = project.getTasks().size();
        if (total == 0) project.setTeamProgress(0);
        else {
            int sum = 0;
            for (Task t : project.getTasks()) sum += t.getProgress();
            project.setTeamProgress(Math.round((float) sum / total));
        }
    }

    // ✅ 팀 전체 “계획 대비” 평균 델타(%) (음수면 늦음, 양수면 빠름)
    public static int calcProjectScheduleDelta(Project project) {
        int cnt = 0;
        int sum = 0;
        LocalDate sim = project.getSimDate();

        for (Task t : project.getTasks()) {
            // 아직 시작 전(예상 0) 같은 건 의미 없어서 제외해도 되고 포함해도 됨
            if (t.getPlanStart() != null && sim.isBefore(t.getPlanStart())) continue;

            sum += t.getScheduleDelta(sim);
            cnt++;
        }
        if (cnt == 0) return 0;
        return Math.round((float) sum / cnt);
    }

    // ✅ 특정 팀원 schedule delta 평균
    public static int calcMemberScheduleDelta(Project project, Member m) {
        int cnt = 0;
        int sum = 0;
        LocalDate sim = project.getSimDate();

        for (Task t : project.getTasks()) {
            if (t.getAssignee() == m) {
                if (t.getPlanStart() != null && sim.isBefore(t.getPlanStart())) continue;
                sum += t.getScheduleDelta(sim);
                cnt++;
            }
        }
        if (cnt == 0) return 0;
        return Math.round((float) sum / cnt);
    }

    // ✅ 경고: 마감 임박 + 일정 크게 늦음
    public static String checkAlerts(Project project) {
        LocalDate sim = project.getSimDate();

        for (Task t : project.getTasks()) {
            long daysLeft = ChronoUnit.DAYS.between(sim, t.getPlanEnd());
            if (daysLeft <= 1 && t.getProgress() == 0 && t.getStatus() != TaskStatus.DONE) {
                return "마감이 임박인데 진행률이 0%인 업무가 있습니다: [" + t.getTitle() + "]";
            }
        }

        for (Task t : project.getTasks()) {
            int delta = t.getScheduleDelta(sim);
            long daysLeft = ChronoUnit.DAYS.between(sim, t.getPlanEnd());
            if (daysLeft <= 3 && delta <= -25 && t.getStatus() != TaskStatus.DONE) {
                return "계획 대비 많이 늦은 업무가 있습니다: [" + t.getTitle() + "] (계획 대비 " + (-delta) + "% 늦음)";
            }
        }
        return null;
    }

    // ✅ 접속 요약(sim 기준)
    public static String buildStartupAlert(Project project) {
        StringBuilder sb = new StringBuilder();
        sb.append("📅 기준 날짜(sim): ").append(project.getSimDate()).append("\n\n");

        int projDelta = calcProjectScheduleDelta(project);
        if (projDelta < 0) sb.append("📉 팀 전체: 계획 대비 ").append(-projDelta).append("% 늦음\n\n");
        else if (projDelta > 0) sb.append("📈 팀 전체: 계획 대비 ").append(projDelta).append("% 빠름\n\n");
        else sb.append("📌 팀 전체: 계획과 거의 비슷함\n\n");

        boolean has = false;

        // 마감 임박
        for (Task t : project.getTasks()) {
            long daysLeft = ChronoUnit.DAYS.between(project.getSimDate(), t.getPlanEnd());
            if (daysLeft <= 3 && t.getStatus() != TaskStatus.DONE) {
                has = true;
                int delta = t.getScheduleDelta(project.getSimDate());
                sb.append("⏰ 마감 임박: ").append(t.getTitle())
                        .append(" (").append(t.getAssignee().getName()).append(") ")
                        .append(" D-").append(daysLeft)
                        .append(", 진행 ").append(t.getProgress()).append("%");

                if (delta < 0) sb.append(" / 계획보다 ").append(-delta).append("% 늦음");
                else if (delta > 0) sb.append(" / 계획보다 ").append(delta).append("% 빠름");
                sb.append("\n");
            }
        }

        if (!has) sb.append("마감 임박(3일 이내) 미완료 업무가 없습니다.\n");
        return sb.toString();
    }
    public static String buildMemberReport(Project project, Member m) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 팀원 일정 진단 보고서 ===\n\n");
        sb.append("이름: ").append(m.getName()).append("\n");
        sb.append("역할: ").append(m.getRole()).append("\n");
        sb.append("sim 기준 날짜: ").append(project.getSimDate()).append("\n\n");

        int total = 0;
        int done = 0;

        int sumDelta = 0;
        int cntDelta = 0;

        int overdueNotDone = 0;   // 마감 지났는데 미완료
        int dueSoon = 0;          // 3일 이내 마감 미완료

        for (Task t : project.getTasks()) {
            if (t.getAssignee() != m) continue;

            total++;
            if (t.getStatus() == TaskStatus.DONE) done++;

            // 계획 마감 기준으로만 봄
            long daysLeft = ChronoUnit.DAYS.between(project.getSimDate(), t.getPlanEnd());

            if (t.getStatus() != TaskStatus.DONE) {
                if (daysLeft < 0) overdueNotDone++;
                if (daysLeft <= 3) dueSoon++;
            }

            int exp = t.getExpectedProgress(project.getSimDate());
            int delta = t.getScheduleDelta(project.getSimDate());

            // 시작 전(예상 0인데 아직 시작 전인 것)은 평균에서 빼고 싶으면 제외
            if (!(t.getPlanStart() != null && project.getSimDate().isBefore(t.getPlanStart()))) {
                sumDelta += delta;
                cntDelta++;
            }
        }

        int avgDelta = (cntDelta == 0) ? 0 : Math.round((float) sumDelta / cntDelta);

        sb.append("[업무 요약]\n");
        sb.append("- 맡은 업무: ").append(total).append("개\n");
        sb.append("- 완료 업무: ").append(done).append("개\n");
        sb.append("- 마감 지남(미완료): ").append(overdueNotDone).append("개\n");
        sb.append("- 3일 이내 마감(미완료): ").append(dueSoon).append("개\n\n");

        sb.append("[계획 대비 상태]\n");
        if (avgDelta < 0) sb.append("- 평균: 계획 대비 ").append(-avgDelta).append("% 늦음\n");
        else if (avgDelta > 0) sb.append("- 평균: 계획 대비 ").append(avgDelta).append("% 빠름\n");
        else sb.append("- 평균: 계획과 비슷\n");

        sb.append("\n[업무 상세]\n");
        for (Task t : project.getTasks()) {
            if (t.getAssignee() != m) continue;

            long daysLeft = ChronoUnit.DAYS.between(project.getSimDate(), t.getPlanEnd());
            int exp = t.getExpectedProgress(project.getSimDate());
            int delta = t.getScheduleDelta(project.getSimDate());

            sb.append("• ").append(t.getTitle())
              .append(" / ").append(t.getCategory())
              .append(" / 현재 ").append(t.getProgress()).append("%")
              .append(" / 예상 ").append(exp).append("%");

            if (delta < 0) sb.append(" (").append(-delta).append("% 늦음)");
            else if (delta > 0) sb.append(" (").append(delta).append("% 빠름)");

            if (daysLeft >= 0) sb.append(" / D-").append(daysLeft);
            else sb.append(" / ").append(-daysLeft).append("일 지남");

            sb.append(" / ").append(t.getStatus()).append("\n");
        }

        if (total == 0) {
            sb.append("이 팀원에게 배정된 업무가 없습니다.\n");
        }

        return sb.toString();
    }

}
