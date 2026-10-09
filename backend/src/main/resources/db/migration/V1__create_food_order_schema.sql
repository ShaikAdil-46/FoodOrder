CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(60) NOT NULL,
    last_name VARCHAR(60) NOT NULL,
    mobile VARCHAR(20) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_mobile UNIQUE (mobile)
);

CREATE TABLE food_items (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL,
    category VARCHAR(40) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    vegetarian BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL
);
CREATE INDEX idx_food_category_available ON food_items (category, available);
CREATE INDEX idx_food_name ON food_items (name);

CREATE TABLE cart_items (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    food_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT uk_cart_user_food UNIQUE (user_id, food_id),
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_cart_food FOREIGN KEY (food_id) REFERENCES food_items (id)
);

CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    delivery_address VARCHAR(300) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX idx_orders_user_created ON orders (user_id, created_at);
CREATE INDEX idx_orders_status ON orders (status);

CREATE TABLE order_items (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    food_name VARCHAR(100) NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

INSERT INTO food_items (name, description, category, price, image_url, available, vegetarian, created_at) VALUES
('Chicken Biryani', 'Fragrant basmati rice, slow-cooked with warming spices and tender chicken.', 'South Indian', 179.00, 'https://images.unsplash.com/photo-1563379091339-03246963d96c?auto=format&fit=crop&w=900&q=85', TRUE, FALSE, CURRENT_TIMESTAMP),
('Margherita Pizza', 'Stone-baked pizza with tomato, fresh mozzarella and basil.', 'Pizza', 249.00, 'https://images.unsplash.com/photo-1574071318508-1cdbab80d002?auto=format&fit=crop&w=900&q=85', TRUE, TRUE, CURRENT_TIMESTAMP),
('Veg Burger', 'Crispy garden patty, cheddar, lettuce and our house sauce.', 'Burgers', 149.00, 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=900&q=85', TRUE, TRUE, CURRENT_TIMESTAMP),
('Paneer Butter Masala', 'Creamy tomato gravy with soft paneer and toasted spices.', 'South Indian', 220.00, 'https://images.unsplash.com/photo-1631452180519-c014fe946bc7?auto=format&fit=crop&w=900&q=85', TRUE, TRUE, CURRENT_TIMESTAMP),
('Classic Cheeseburger', 'Juicy grilled patty with aged cheddar, pickles and house dressing.', 'Burgers', 199.00, 'https://images.unsplash.com/photo-1550547660-d9450f859349?auto=format&fit=crop&w=900&q=85', TRUE, FALSE, CURRENT_TIMESTAMP),
('Garden Bowl', 'A bright bowl of greens, avocado, grains and citrus dressing.', 'Healthy', 189.00, 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=900&q=85', TRUE, TRUE, CURRENT_TIMESTAMP),
('Pepperoni Pizza', 'Crisp, hand-stretched crust with mozzarella and pepperoni.', 'Pizza', 299.00, 'https://images.unsplash.com/photo-1628840042765-356cda07504e?auto=format&fit=crop&w=900&q=85', TRUE, FALSE, CURRENT_TIMESTAMP),
('Chocolate Brownie', 'Warm dark chocolate brownie with a soft centre.', 'Desserts', 99.00, 'https://images.unsplash.com/photo-1606313564200-e75d5e30476c?auto=format&fit=crop&w=900&q=85', TRUE, TRUE, CURRENT_TIMESTAMP),
('Masala Dosa', 'Golden, crisp dosa with potato masala and coconut chutney.', 'South Indian', 139.00, 'https://images.unsplash.com/photo-1668236543090-82eba5ee5976?auto=format&fit=crop&w=900&q=85', TRUE, TRUE, CURRENT_TIMESTAMP),
('Sushi Platter', 'Chef-selected sushi rolls served with ginger and soy.', 'Asian', 349.00, 'https://images.unsplash.com/photo-1579871494447-9811cf80d66c?auto=format&fit=crop&w=900&q=85', TRUE, FALSE, CURRENT_TIMESTAMP),
('Berry Cheesecake', 'Silky baked cheesecake finished with seasonal berries.', 'Desserts', 149.00, 'https://images.unsplash.com/photo-1533134242443-d4fd215305ad?auto=format&fit=crop&w=900&q=85', TRUE, TRUE, CURRENT_TIMESTAMP),
('Crispy Chicken Bites', 'Golden chicken bites with a smoky, tangy dipping sauce.', 'Snacks', 169.00, 'https://images.unsplash.com/photo-1562967914-608f82629710?auto=format&fit=crop&w=900&q=85', TRUE, FALSE, CURRENT_TIMESTAMP);
