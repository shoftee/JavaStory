package javastory.server.handling;

public class SessionFloodEntry {
	public final long Timestamp;
	public final int Count;
	
	public SessionFloodEntry(long timestamp, int count) {
		this.Timestamp = timestamp;
		this.Count = count;
	}
}
