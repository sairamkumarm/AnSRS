package ansrs.data;

import java.time.LocalDate;

public class Item {
    private int itemId;
    private String itemName;
    private String itemLink;
    private Pool itemPool;
    private LocalDate lastRecall;
    private int totalRecalls;

    public enum Pool{H, M, L};

    public Item() {
    }

    public Item(int itemId, String itemName, String itemLink, Pool itemPool, LocalDate lastRecall, int totalRecalls) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemLink = itemLink;
        this.itemPool = itemPool;
        this.lastRecall = lastRecall;
        this.totalRecalls = totalRecalls;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemLink() {
        return itemLink;
    }

    public void setItemLink(String itemLink) {
        this.itemLink = itemLink;
    }

    public Pool getItemPool() {
        return itemPool;
    }

    public void setItemPool(Pool itemPool) {
        this.itemPool = itemPool;
    }

    public LocalDate getLastRecall() {
        return lastRecall;
    }

    public void setLastRecall(LocalDate lastRecall) {
        this.lastRecall = lastRecall;
    }

    public int getTotalRecalls() {
        return totalRecalls;
    }

    public void setTotalRecalls(int totalRecalls) {
        this.totalRecalls = totalRecalls;
    }

    @Override
    public String toString() {
        return "Item{" +
                "itemId=" + itemId +
                ", itemName='" + itemName + '\'' +
                ", itemLink='" + itemLink + '\'' +
                ", itemPool=" + itemPool +
                ", lastRecall=" + lastRecall +
                ", totalRecalls=" + totalRecalls +
                '}';
    }
}
