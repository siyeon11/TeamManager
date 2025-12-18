package data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Member {
    private final String name;
    private final List<String> roles = new ArrayList<String>();

    public Member(String name, String rolesStr) {
        this.name = name;
        setRolesFromString(rolesStr);
        if (roles.isEmpty()) roles.add("미정");
    }

    public String getName() { return name; }

    // 호환: 역할은 쉼표 문자열로 보여주기
    public String getRole() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < roles.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(roles.get(i));
        }
        return sb.toString();
    }

    public List<String> getRoles() {
        return new ArrayList<String>(roles);
    }

    public void setRolesFromString(String rolesStr) {
        roles.clear();
        if (rolesStr == null) return;

        String[] parts = rolesStr.split(",");
        Set<String> uniq = new LinkedHashSet<String>();
        for (String p : parts) {
            String r = p.trim();
            if (!r.isEmpty()) uniq.add(r);
        }
        roles.addAll(uniq);
    }

    public void setRoles(List<String> selectedRoles) {
        roles.clear();
        if (selectedRoles != null) {
            for (String r : selectedRoles) {
                if (r != null && !r.trim().isEmpty() && !roles.contains(r.trim())) {
                    roles.add(r.trim());
                }
            }
        }
        if (roles.isEmpty()) roles.add("미정");
    }

    @Override
    public String toString() {
        return name + " (" + getRole() + ")";
    }
}
