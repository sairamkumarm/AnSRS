CREATE TABLE IF NOT EXISTS groups(
    id INTEGER PRIMARY KEY,
    name VARCHAR(255),
    link VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS group_items (
    group_id INTEGER NOT NULL,
    item_id INTEGER NOT NULL,

    PRIMARY KEY (group_id, item_id),

    CONSTRAINT fk_group_items_group
        FOREIGN KEY (group_id)
        REFERENCES groups(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_group_items_item
        FOREIGN KEY (item_id)
        REFERENCES items(id)
        ON DELETE CASCADE
);
