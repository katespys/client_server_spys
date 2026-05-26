package protocol;

import java.nio.charset.StandardCharsets;

public class Message {
    private int type;
    private int userId;
    private byte[] data;
    public Message(int type, int userId, byte[] data) {
        this.type = type;
        this.userId = userId;
        this.data = data;
    }
    public int getType() {
        return type;
    }
    public int getUserId() {
        return userId;
    }
    public byte[] getData() {
        return data;
    }

    //я додала сеттери для повноцінного pojo, не зважаючи на те, що вони не використовуються
    public void setType(int type) {
        this.type = type;
    }
    public void setUserId(int userId) {
        this.userId = userId;
    }
    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public String toString() {

        String dataAsString = new String(data, StandardCharsets.UTF_8);
        return "Message{" +
                "type=" + type +
                ", userId=" + userId +
                ", data='" + dataAsString + "'" +
                '}';
    }
}
