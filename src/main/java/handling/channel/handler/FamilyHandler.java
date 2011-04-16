package handling.channel.handler;

import org.javastory.io.PacketFormatException;
import org.javastory.io.PacketReader;

public class FamilyHandler {

    public static final void handleFamilyRequest(final PacketReader reader) throws PacketFormatException {
        final String reqName = reader.readLengthPrefixedString();
    }
}