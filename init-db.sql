-- init-db.sql
-- Initialize database with sample e-commerce data

-- Create extension for UUID if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Sample Categories
INSERT INTO products (name, description, price, stock_quantity, category, brand, image_url, active, created_at, updated_at)
VALUES
-- Electronics
('iPhone 15 Pro', 'Latest Apple smartphone with A17 Pro chip', 999.99, 50, 'Electronics', 'Apple', 'https://example.com/iphone15.jpg', true, NOW(), NOW()),
('MacBook Pro M3', '14-inch laptop with M3 chip', 1999.99, 30, 'Electronics', 'Apple', 'https://example.com/macbook.jpg', true, NOW(), NOW()),
('Samsung Galaxy S24', 'Flagship Android phone', 899.99, 45, 'Electronics', 'Samsung', 'https://example.com/galaxy.jpg', true, NOW(), NOW()),
('Sony WH-1000XM5', 'Noise-cancelling headphones', 399.99, 100, 'Electronics', 'Sony', 'https://example.com/sony-headphones.jpg', true, NOW(), NOW()),
('iPad Air', '10.9-inch tablet', 599.99, 60, 'Electronics', 'Apple', 'https://example.com/ipad.jpg', true, NOW(), NOW()),

-- Clothing
('Nike Air Max', 'Running shoes', 129.99, 200, 'Clothing', 'Nike', 'https://example.com/nike-shoes.jpg', true, NOW(), NOW()),
('Levi''s 501 Jeans', 'Classic fit jeans', 69.99, 150, 'Clothing', 'Levi''s', 'https://example.com/levis.jpg', true, NOW(), NOW()),
('Adidas Hoodie', 'Comfortable cotton hoodie', 59.99, 180, 'Clothing', 'Adidas', 'https://example.com/hoodie.jpg', true, NOW(), NOW()),
('Columbia Jacket', 'Waterproof winter jacket', 149.99, 90, 'Clothing', 'Columbia', 'https://example.com/jacket.jpg', true, NOW(), NOW()),
('Under Armour T-Shirt', 'Sports t-shirt', 29.99, 250, 'Clothing', 'Under Armour', 'https://example.com/tshirt.jpg', true, NOW(), NOW()),

-- Home & Kitchen
('KitchenAid Mixer', 'Stand mixer for baking', 349.99, 40, 'Home & Kitchen', 'KitchenAid', 'https://example.com/mixer.jpg', true, NOW(), NOW()),
('Dyson V15 Vacuum', 'Cordless vacuum cleaner', 649.99, 35, 'Home & Kitchen', 'Dyson', 'https://example.com/vacuum.jpg', true, NOW(), NOW()),
('Instant Pot', '6-quart pressure cooker', 99.99, 120, 'Home & Kitchen', 'Instant Pot', 'https://example.com/instantpot.jpg', true, NOW(), NOW()),
('Ninja Blender', 'Professional blender', 129.99, 80, 'Home & Kitchen', 'Ninja', 'https://example.com/blender.jpg', true, NOW(), NOW()),
('Keurig Coffee Maker', 'Single-serve coffee maker', 149.99, 100, 'Home & Kitchen', 'Keurig', 'https://example.com/keurig.jpg', true, NOW(), NOW()),

-- Books
('The Clean Code', 'Software engineering best practices', 49.99, 500, 'Books', 'Pearson', 'https://example.com/cleancode.jpg', true, NOW(), NOW()),
('System Design Interview', 'Guide to system design', 39.99, 400, 'Books', 'O''Reilly', 'https://example.com/systemdesign.jpg', true, NOW(), NOW()),
('Atomic Habits', 'Self-improvement book', 27.99, 600, 'Books', 'Penguin', 'https://example.com/atomichabits.jpg', true, NOW(), NOW()),
('The Psychology of Money', 'Financial wisdom', 24.99, 450, 'Books', 'Harriman House', 'https://example.com/psychology-money.jpg', true, NOW(), NOW()),
('Designing Data-Intensive Applications', 'Modern data systems', 54.99, 300, 'Books', 'O''Reilly', 'https://example.com/ddia.jpg', true, NOW(), NOW()),

-- Sports & Outdoors
('Yeti Cooler', '45-quart cooler', 349.99, 50, 'Sports & Outdoors', 'Yeti', 'https://example.com/yeti.jpg', true, NOW(), NOW()),
('Coleman Tent', '6-person camping tent', 199.99, 70, 'Sports & Outdoors', 'Coleman', 'https://example.com/tent.jpg', true, NOW(), NOW()),
('Patagonia Backpack', 'Hiking backpack 40L', 149.99, 85, 'Sports & Outdoors', 'Patagonia', 'https://example.com/backpack.jpg', true, NOW(), NOW()),
('GoPro HERO12', 'Action camera', 449.99, 60, 'Sports & Outdoors', 'GoPro', 'https://example.com/gopro.jpg', true, NOW(), NOW()),
('Fitbit Charge 6', 'Fitness tracker', 159.99, 150, 'Sports & Outdoors', 'Fitbit', 'https://example.com/fitbit.jpg', true, NOW(), NOW());

-- Add more sample products to reach 100+ for realistic testing
-- Generate additional products using a pattern
DO $$
DECLARE
    i INTEGER;
    categories TEXT[] := ARRAY['Electronics', 'Clothing', 'Home & Kitchen', 'Books', 'Sports & Outdoors'];
    brands TEXT[] := ARRAY['Generic', 'Premium', 'Budget', 'Luxury', 'Standard'];
BEGIN
    FOR i IN 26..100 LOOP
        INSERT INTO products (name, description, price, stock_quantity, category, brand, image_url, active, created_at, updated_at)
        VALUES (
            'Product ' || i,
            'Description for product ' || i,
            ROUND((RANDOM() * 500 + 20)::numeric, 2),
            FLOOR(RANDOM() * 200 + 10)::integer,
            categories[FLOOR(RANDOM() * 5 + 1)],
            brands[FLOOR(RANDOM() * 5 + 1)],
            'https://example.com/product' || i || '.jpg',
            true,
            NOW() - (RANDOM() * INTERVAL '365 days'),
            NOW()
        );
    END LOOP;
END $$;

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_products_price ON products(price);
CREATE INDEX IF NOT EXISTS idx_products_created_at ON products(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_products_active ON products(active);

-- Display summary
SELECT
    category,
    COUNT(*) as product_count,
    AVG(price) as avg_price
FROM products
WHERE active = true
GROUP BY category
ORDER BY product_count DESC;