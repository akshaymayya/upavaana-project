CREATE TABLE plants (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    common_names VARCHAR(255)
);

CREATE TABLE diseases (
    id INT AUTO_INCREMENT PRIMARY KEY,
    plant_id INT,
    disease_name VARCHAR(255) NOT NULL,
    symptoms TEXT,
    causes TEXT,
    solution TEXT,
    FOREIGN KEY (plant_id) REFERENCES plants(id)
);

CREATE TABLE queries (
    id INT AUTO_INCREMENT PRIMARY KEY,
    image_url VARCHAR(500),
    result_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
