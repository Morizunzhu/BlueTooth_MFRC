package mo.com.bluetooth;

public class Student {
    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String uid;

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    private String name;

    public boolean isSignToday() {
        return signToday;
    }

    public void setSignToday(boolean signToday) {
        this.signToday = signToday;
    }

    private boolean signToday;
}
