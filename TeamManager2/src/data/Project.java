package data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Project {
    private String projectName = "새 프로젝트";

    private final List<Member> members = new ArrayList<Member>();
    private final List<Task> tasks = new ArrayList<Task>();

    private int teamProgress = 0;

    // simDate
    private LocalDate simDate = LocalDate.now();

    // ✅ 프로젝트 전체 기간(홈에서 입력)
    private LocalDate projectStart = LocalDate.now();
    private LocalDate projectEnd = LocalDate.now().plusDays(14);

    // ✅ 역할/카테고리 공통 리스트
    private final List<String> roles = new ArrayList<String>(Arrays.asList(
            "조장", "자료조사", "PPT", "발표"
    ));

    public String getProjectName() { return projectName; }
    public void setProjectName(String n) {
        if (n != null && !n.trim().isEmpty()) projectName = n.trim();
    }

    public LocalDate getSimDate() { return simDate; }
    public void setSimDate(LocalDate d) { if (d != null) simDate = d; }
    public void advanceDays(int days) { simDate = simDate.plusDays(days); }

    public LocalDate getProjectStart() { return projectStart; }
    public LocalDate getProjectEnd() { return projectEnd; }

    public void setProjectStart(LocalDate d) { if (d != null) projectStart = d; }
    public void setProjectEnd(LocalDate d) { if (d != null) projectEnd = d; }

    public List<Member> getMembers() { return members; }
    public List<Task> getTasks() { return tasks; }

    public int getTeamProgress() { return teamProgress; }
    public void setTeamProgress(int v) { teamProgress = Math.max(0, Math.min(100, v)); }

    public List<String> getRoles() { return Collections.unmodifiableList(roles); }
    public void addRole(String role) {
        if (role == null) return;
        String r = role.trim();
        if (r.isEmpty()) return;
        if (!roles.contains(r)) roles.add(r);
    }

    public void addMember(Member m) { if (m != null) members.add(m); }
    public void removeMember(Member m) { members.remove(m); }

    public void addTask(Task t) { if (t != null) tasks.add(t); }

    public Member findMemberByName(String name) {
        if (name == null) return null;
        for (Member m : members) {
            if (name.equals(m.getName())) return m;
        }
        return null;
    }

    public void clearAll() {
        members.clear();
        tasks.clear();
        teamProgress = 0;
        simDate = LocalDate.now();
        projectName = "새 프로젝트";
        projectStart = simDate;
        projectEnd = simDate.plusDays(14);
    }
}
