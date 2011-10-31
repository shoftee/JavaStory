package javastory.server;

import javastory.game.IItem;

public class DueyActions {

	private String sender = null;
	private IItem item = null;
	private int mesos = 0;
	private int quantity = 1;
	private long sentTime;
	private int packageId = 0;

	public DueyActions(final int pId, final IItem item) {
		this.item = item;
		this.quantity = item.getQuantity();
		this.packageId = pId;
	}

	public DueyActions(final int pId) { // meso only package
		this.packageId = pId;
	}

	public String getSender() {
		return this.sender;
	}

	public void setSender(final String name) {
		this.sender = name;
	}

	public IItem getItem() {
		return this.item;
	}

	public int getMesos() {
		return this.mesos;
	}

	public void setMesos(final int set) {
		this.mesos = set;
	}

	public int getQuantity() {
		return this.quantity;
	}

	public int getPackageId() {
		return this.packageId;
	}

/*    public boolean isExpired() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(year, month - 1, day);
        long diff = System.currentTimeMillis() - cal1.getTimeInMillis();
        int diffDays = (int) Math.abs(diff / (24 * 60 * 60 * 1000));
        return diffDays > 30;
    }

    public long sentTimeInMilliseconds() {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day);
        return cal.getTimeInMillis();
    }*/

	public void setSentTime(final long sentTime) {
		this.sentTime = sentTime;
	}

	public long getSentTime() {
		return this.sentTime;
	}
}