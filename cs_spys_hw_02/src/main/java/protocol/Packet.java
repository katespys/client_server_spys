package protocol;

public class Packet {

    private byte src;
    private long pktId;
    private Message message;

    public Packet(byte src, long pktId, Message message) {
        this.src = src;
        this.pktId = pktId;
        this.message = message;
    }

    public byte getSrc() {
        return src;
    }
    public long getPktId() {
        return pktId;
    }
    public Message getMessage() {
        return message;
    }

    //я додала сеттери для повноцінного pojo, не зважаючи на те, що вони не використовуються
    public void setSrc(byte src) {
        this.src = src;
    }
    public void setPktId(long pktId) {
        this.pktId = pktId;
    }
    public void setMessage(Message message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Packet{" +
                "src=" + src +
                ", pktId=" + pktId +
                ", message=" + message +
                '}';
    }
}
