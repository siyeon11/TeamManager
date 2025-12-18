// Replacing JavaScript code with properly refactored Java code for native execution
// Java implementation example for native execution
class TeamManager {
    private String teamName;
    private List<String> members;

    public TeamManager(String teamName) {
        this.teamName = teamName;
        this.members = new ArrayList<>();
    }

    public void addMember(String member) {
        this.members.add(member);
    }

    public void removeMember(String member) {
        this.members.remove(member);
    }

    public List<String> getMembers() {
        return this.members;
    }

    public String getTeamName() {
        return this.teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public void displayTeam() {
        System.out.println("Team: " + this.teamName);
        System.out.println("Members: " + String.join(", ", this.members));
    }
}