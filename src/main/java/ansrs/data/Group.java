package ansrs.data;

import java.time.Instant;

public class Group {
    public final int id;
    public final String name;
    public final String link;
    public final Instant createdAt;
    public final Instant updatedAt;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLink() {
        return link;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Group(int id, String name, String link, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.link = link;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Group(int id, String name, String link) {
        this.id = id;
        this.name = name;
        this.link = link;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

}
