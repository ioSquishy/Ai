package ai;

public class AppTest {
    public static void main(String[] args) throws Exception {
        String auditReason = "a\nb\nModerator: squishhy 91740812341";
        String[] splitAuditReason = auditReason.split("\n");

        String rawModerator = splitAuditReason[splitAuditReason.length-1];
        String reason = auditReason.substring(0, (auditReason.length()-rawModerator.length()));

        String moderatorID = rawModerator.split(" ")[2];
    }
}
