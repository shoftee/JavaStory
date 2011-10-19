package handling.channel.handler;

import javastory.io.PacketFormatException;
import javastory.io.PacketReader;

public class FamilyHandler {

    public static final void handleFamilyRequest(final PacketReader reader) throws PacketFormatException {
        final String reqName = reader.readLengthPrefixedString();
    }
}