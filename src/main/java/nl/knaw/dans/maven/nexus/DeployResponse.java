package nl.knaw.dans.maven.nexus;

public class DeployResponse {
    private int code;
    private String message;

    public DeployResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getStatusLine() {
        return String.format("%d %s", code, message);
    }
}
