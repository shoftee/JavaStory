package javastory.server;

import java.util.ArrayList;
import java.util.List;

import javastory.game.IItem;

public class MerchItemPackage {

	private long sentTime;
	private int mesos = 0, packageid;
	private List<IItem> items = new ArrayList<IItem>();

	public void setItems(final List<IItem> items) {
		this.items = items;
	}

	public List<IItem> getItems() {
		return this.items;
	}

	public void setSentTime(final long sentTime) {
		this.sentTime = sentTime;
	}

	public long getSentTime() {
		return this.sentTime;
	}

	public int getMesos() {
		return this.mesos;
	}

	public void setMesos(final int set) {
		this.mesos = set;
	}

	public int getPackageid() {
		return this.packageid;
	}

	public void setPackageid(final int packageid) {
		this.packageid = packageid;
	}
}
