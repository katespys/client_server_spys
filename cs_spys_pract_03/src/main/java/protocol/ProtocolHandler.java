package protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class ProtocolHandler {

    private static final byte MAGIC = 0x13;
    private static final int HEADER_LENGTH = 14;
    private static final int MESSAGE_META_LENGTH = 8; // 4 байти для cType + 4 байти для bUserId

    public static byte[] encode(Packet packet) throws Exception {
        Message msg = packet.getMessage();

        ByteBuffer rawMsgBuffer = ByteBuffer.allocate(MESSAGE_META_LENGTH + msg.getData().length)
                .order(ByteOrder.BIG_ENDIAN);
        rawMsgBuffer.putInt(msg.getType());
        rawMsgBuffer.putInt(msg.getUserId());
        rawMsgBuffer.put(msg.getData());

        byte[] encryptedPayload = CryptoUtils.encrypt(rawMsgBuffer.array());
        int wLen = encryptedPayload.length;

        ByteBuffer headerBuffer = ByteBuffer.allocate(HEADER_LENGTH)
                .order(ByteOrder.BIG_ENDIAN);
        headerBuffer.put(MAGIC);
        headerBuffer.put(packet.getSrc());
        headerBuffer.putLong(packet.getPktId());
        headerBuffer.putInt(wLen);

        short headerCrc = Crc16.calculateCrc(headerBuffer.array());
        short payloadCrc = Crc16.calculateCrc(encryptedPayload);

        ByteBuffer finalPacket = ByteBuffer.allocate(HEADER_LENGTH + 2 + wLen + 2)
                .order(ByteOrder.BIG_ENDIAN);
        finalPacket.put(headerBuffer.array());
        finalPacket.putShort(headerCrc);
        finalPacket.put(encryptedPayload);
        finalPacket.putShort(payloadCrc);

        return finalPacket.array();
    }

    public static Packet decode(byte[] data) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);

        byte magic = buffer.get();
        if (magic != MAGIC) {
            throw new IllegalArgumentException("Invalid Magic byte!");
        }

        byte src = buffer.get();
        long pktId = buffer.getLong();
        int wLen = buffer.getInt();

        byte[] headerBytes = Arrays.copyOfRange(data, 0, HEADER_LENGTH);

        short expectedHeaderCrc = Crc16.calculateCrc(headerBytes);
        short actualHeaderCrc = buffer.getShort();

        if (expectedHeaderCrc != actualHeaderCrc) {
            throw new IllegalArgumentException("Header CRC mismatch!");
        }

        byte[] encryptedPayload = new byte[wLen];
        buffer.get(encryptedPayload);

        short actualPayloadCrc = buffer.getShort();
        short expectedPayloadCrc = Crc16.calculateCrc(encryptedPayload);

        if (expectedPayloadCrc != actualPayloadCrc) {
            throw new IllegalArgumentException("Payload CRC mismatch!");
        }

        byte[] decryptedPayload = CryptoUtils.decrypt(encryptedPayload);

        ByteBuffer msgBuffer = ByteBuffer.wrap(decryptedPayload).order(ByteOrder.BIG_ENDIAN);

        int type = msgBuffer.getInt();
        int userId = msgBuffer.getInt();

        byte[] messageData = new byte[msgBuffer.remaining()];
        msgBuffer.get(messageData);

        Message message = new Message(type, userId, messageData);

        return new Packet(src, pktId, message);
    }
}