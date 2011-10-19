package javastory.server;

import java.util.ArrayList;
import java.util.List;

import javastory.client.IItem;


public class MerchItemPackage {

    private long sentTime;
    private int mesos = 0, packageid;
    private List<IItem> items = new ArrayList<IItem>();

    public void setItems(List<IItem> items) {
	this.items = items;
    }

    public List<IItem> getItems() {
        return items;
    }

    public void setSentTime(long sentTime) {
	this.sentTime = sentTime;
    }

    public long getSentTime() {
	return sentTime;
    }

    public int getMesos() {
        return mesos;
    }

    public void setMesos(int set) {
        mesos = set;
    }

    public int getPackageid() {
        return packageid;
    }

    public void setPackageid(int packageid) {
        this.packageid = packageid;
    }
}
