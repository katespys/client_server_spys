package protocol;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;

class ProtocolTest {

    private Packet originalPacket;
    private Message originalMessage;

    @BeforeEach
    void setUp() {
        byte[] payloadData = "Hello, Secret World!".getBytes(StandardCharsets.UTF_8);
        originalMessage = new Message(100, 999, payloadData);
        originalPacket = new Packet((byte) 1, 123456789L, originalMessage);
    }

    @Test
    void shouldEncodeAndDecodeSuccessfully() throws Exception {
        byte[] rawBytes = ProtocolHandler.encode(originalPacket);
        Packet decodedPacket = ProtocolHandler.decode(rawBytes);

        Assertions.assertThat(decodedPacket.getSrc()).isEqualTo(originalPacket.getSrc());
        Assertions.assertThat(decodedPacket.getPktId()).isEqualTo(originalPacket.getPktId());

        Message decodedMessage = decodedPacket.getMessage();
        Assertions.assertThat(decodedMessage.getType()).isEqualTo(originalMessage.getType());
        Assertions.assertThat(decodedMessage.getUserId()).isEqualTo(originalMessage.getUserId());

        String decodedString = new String(decodedMessage.getData(), StandardCharsets.UTF_8);
        Assertions.assertThat(decodedString).isEqualTo("Hello, Secret World!");
    }

    @Test
    void shouldThrowExceptionOnInvalidMagicByte() throws Exception {
        byte[] rawBytes = ProtocolHandler.encode(originalPacket);
        rawBytes[0] = 0x00;

        Assertions.assertThatThrownBy(() -> ProtocolHandler.decode(rawBytes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid Magic byte!");
    }

    @Test
    void shouldThrowExceptionOnHeaderCrcMismatch() throws Exception {
        byte[] rawBytes = ProtocolHandler.encode(originalPacket);
        rawBytes[1] = 99;

        Assertions.assertThatThrownBy(() -> ProtocolHandler.decode(rawBytes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Header CRC mismatch!");
    }

    @Test
    void shouldThrowExceptionOnPayloadCrcMismatch() throws Exception {
        byte[] rawBytes = ProtocolHandler.encode(originalPacket);
        rawBytes[16] ^= (byte) 0xFF;

        Assertions.assertThatThrownBy(() -> ProtocolHandler.decode(rawBytes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payload CRC mismatch!");
    }

    @Test
    void shouldProduceDifferentCiphertextEachTime() throws Exception {
        byte[] encoded1 = ProtocolHandler.encode(originalPacket);
        byte[] encoded2 = ProtocolHandler.encode(originalPacket);

        // пакети однакові, але зашифровані дані мають відрізнятися через різний IV
        Assertions.assertThat(encoded1).isNotEqualTo(encoded2);
    }

    @Test
    void shouldHandleZeroPktId() throws Exception {
        Packet zeroIdPacket = new Packet((byte) 5, 0L, originalMessage);
        byte[] rawBytes = ProtocolHandler.encode(zeroIdPacket);
        Packet decoded = ProtocolHandler.decode(rawBytes);

        Assertions.assertThat(decoded.getPktId()).isEqualTo(0L);
    }

    @Test
    void shouldHandleMaxPktId() throws Exception {
        Packet maxIdPacket = new Packet((byte) 1, Long.MAX_VALUE, originalMessage);
        byte[] rawBytes = ProtocolHandler.encode(maxIdPacket);
        Packet decoded = ProtocolHandler.decode(rawBytes);

        Assertions.assertThat(decoded.getPktId()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void shouldHandleEmptyPayload() throws Exception {
        Message emptyMsg = new Message(1, 42, new byte[0]);
        Packet emptyPacket = new Packet((byte) 2, 1L, emptyMsg);

        byte[] rawBytes = ProtocolHandler.encode(emptyPacket);
        Packet decoded = ProtocolHandler.decode(rawBytes);

        Assertions.assertThat(decoded.getMessage().getData()).isEmpty();
    }

    @Test
    void shouldHandleJsonPayload() throws Exception {
        String json = "{\"action\":\"LOGIN\",\"token\":\"abc123\"}";
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

        Message jsonMsg = new Message(200, 7, jsonBytes);
        Packet jsonPacket = new Packet((byte) 3, 987654321L, jsonMsg);

        byte[] rawBytes = ProtocolHandler.encode(jsonPacket);
        Packet decoded = ProtocolHandler.decode(rawBytes);

        String result = new String(decoded.getMessage().getData(), StandardCharsets.UTF_8);
        Assertions.assertThat(result).isEqualTo(json);
    }
}
