package javastory.channel;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

import javastory.client.PacketWritable;
import javastory.io.PacketBuilder;

public class GuildEmblem implements Serializable, PacketWritable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2157161996401674134L;

	private final short bgStyle;
	private final byte bgColor;
	private final short fgStyle;
	private final byte fgColor;

	public GuildEmblem(final short fgStyle, final byte fgColor, final short bgStyle, final byte bgColor) {
		this.fgStyle = fgStyle;
		this.fgColor = fgColor;
		this.bgStyle = bgStyle;
		this.bgColor = bgColor;
	}

	public GuildEmblem(GuildEmblem other) {
		this.fgStyle = other.fgStyle;
		this.fgColor = other.bgColor;
		this.bgStyle = other.bgStyle;
		this.bgColor = other.bgColor;
	}

	public GuildEmblem(ResultSet rs) throws SQLException {
		this.fgStyle = (short) rs.getInt("logo");
		this.fgColor = (byte) rs.getInt("logoColor");
		this.bgStyle = (short) rs.getInt("logoBG");
		this.bgColor = (byte) rs.getInt("logoBGColor");
	}

	public short getFgStyle() {
		return this.fgStyle;
	}

	public byte getFgColor() {
		return this.fgColor;
	}

	public short getBgStyle() {
		return this.bgStyle;
	}

	public byte getBgColor() {
		return this.bgColor;
	}

	@Override
	public void writeTo(PacketBuilder builder) {
		builder.writeAsShort(this.getBgStyle());
		builder.writeAsByte(this.getBgColor());
		builder.writeAsShort(this.getFgStyle());
		builder.writeAsByte(this.getFgColor());
	}
}
