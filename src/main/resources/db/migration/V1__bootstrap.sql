CREATE TABLE IF NOT EXISTS items(
    id INTEGER PRIMARY KEY,
    name VARCHAR(255),
    link VARCHAR(255),
    pool CHARACTER,
    last_recall VARCHAR(255),
    total_recalls INTEGER
);

CREATE TABLE IF NOT EXISTS archive(
    id INTEGER PRIMARY KEY,
    name VARCHAR(255),
    link VARCHAR(255),
    pool CHARACTER,
    last_recall VARCHAR(255),
    total_recalls INTEGER
);