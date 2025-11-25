-- init-db.sql (MySQL)
-- Initialize database with sample e-commerce data

-- Create products table
CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    price DECIMAL(10,2) NOT NULL,
    stock_quantity INT NOT NULL,
    category VARCHAR(100) NOT NULL,
    brand VARCHAR(100),
    image_url VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

-- Sample Products (25 curated products)
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
('Levi\'s 501 Jeans', 'Classic fit jeans', 69.99, 150, 'Clothing', 'Levi\'s', 'https://example.com/levis.jpg', true, NOW(), NOW()),
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
('System Design Interview', 'Guide to system design', 39.99, 400, 'Books', 'O\'Reilly', 'https://example.com/systemdesign.jpg', true, NOW(), NOW()),
('Atomic Habits', 'Self-improvement book', 27.99, 600, 'Books', 'Penguin', 'https://example.com/atomichabits.jpg', true, NOW(), NOW()),
('The Psychology of Money', 'Financial wisdom', 24.99, 450, 'Books', 'Harriman House', 'https://example.com/psychology-money.jpg', true, NOW(), NOW()),
('Designing Data-Intensive Applications', 'Modern data systems', 54.99, 300, 'Books', 'O\'Reilly', 'https://example.com/ddia.jpg', true, NOW(), NOW()),

-- Sports & Outdoors
('Yeti Cooler', '45-quart cooler', 349.99, 50, 'Sports & Outdoors', 'Yeti', 'https://example.com/yeti.jpg', true, NOW(), NOW()),
('Coleman Tent', '6-person camping tent', 199.99, 70, 'Sports & Outdoors', 'Coleman', 'https://example.com/tent.jpg', true, NOW(), NOW()),
('Patagonia Backpack', 'Hiking backpack 40L', 149.99, 85, 'Sports & Outdoors', 'Patagonia', 'https://example.com/backpack.jpg', true, NOW(), NOW()),
('GoPro HERO12', 'Action camera', 449.99, 60, 'Sports & Outdoors', 'GoPro', 'https://example.com/gopro.jpg', true, NOW(), NOW()),
('Fitbit Charge 6', 'Fitness tracker', 159.99, 150, 'Sports & Outdoors', 'Fitbit', 'https://example.com/fitbit.jpg', true, NOW(), NOW());

-- Additional products (26-100) using direct INSERT statements
-- Electronics continued
INSERT INTO products (name, description, price, stock_quantity, category, brand, image_url, active, created_at, updated_at) VALUES
('Dell XPS 15', 'Premium Windows laptop', 1499.99, 25, 'Electronics', 'Dell', 'https://example.com/xps15.jpg', true, NOW(), NOW()),
('AirPods Pro', 'Wireless earbuds with ANC', 249.99, 120, 'Electronics', 'Apple', 'https://example.com/airpods.jpg', true, NOW(), NOW()),
('Nintendo Switch', 'Hybrid gaming console', 299.99, 80, 'Electronics', 'Nintendo', 'https://example.com/switch.jpg', true, NOW(), NOW()),
('LG OLED TV 55"', '4K OLED Smart TV', 1299.99, 20, 'Electronics', 'LG', 'https://example.com/lgtv.jpg', true, NOW(), NOW()),
('Bose SoundLink', 'Portable Bluetooth speaker', 129.99, 90, 'Electronics', 'Bose', 'https://example.com/bose.jpg', true, NOW(), NOW()),

-- Clothing continued
('North Face Fleece', 'Warm fleece jacket', 99.99, 120, 'Clothing', 'North Face', 'https://example.com/fleece.jpg', true, NOW(), NOW()),
('Ray-Ban Aviators', 'Classic sunglasses', 179.99, 200, 'Clothing', 'Ray-Ban', 'https://example.com/rayban.jpg', true, NOW(), NOW()),
('Converse All Stars', 'Classic canvas sneakers', 59.99, 300, 'Clothing', 'Converse', 'https://example.com/converse.jpg', true, NOW(), NOW()),
('Champion Sweatpants', 'Comfortable joggers', 44.99, 180, 'Clothing', 'Champion', 'https://example.com/joggers.jpg', true, NOW(), NOW()),
('Carhartt Beanie', 'Warm winter hat', 24.99, 250, 'Clothing', 'Carhartt', 'https://example.com/beanie.jpg', true, NOW(), NOW()),

-- Home & Kitchen continued
('Vitamix Blender', 'Professional-grade blender', 449.99, 40, 'Home & Kitchen', 'Vitamix', 'https://example.com/vitamix.jpg', true, NOW(), NOW()),
('Le Creuset Dutch Oven', 'Enameled cast iron', 379.99, 35, 'Home & Kitchen', 'Le Creuset', 'https://example.com/lecreuset.jpg', true, NOW(), NOW()),
('Roomba i7', 'Robot vacuum cleaner', 599.99, 45, 'Home & Kitchen', 'iRobot', 'https://example.com/roomba.jpg', true, NOW(), NOW()),
('Nespresso Machine', 'Espresso maker', 199.99, 70, 'Home & Kitchen', 'Nespresso', 'https://example.com/nespresso.jpg', true, NOW(), NOW()),
('Air Fryer XL', 'Large capacity air fryer', 129.99, 100, 'Home & Kitchen', 'Ninja', 'https://example.com/airfryer.jpg', true, NOW(), NOW()),

-- Books continued
('The Pragmatic Programmer', 'Software development classic', 54.99, 350, 'Books', 'Addison-Wesley', 'https://example.com/pragmatic.jpg', true, NOW(), NOW()),
('Deep Work', 'Focus and productivity', 29.99, 400, 'Books', 'Grand Central', 'https://example.com/deepwork.jpg', true, NOW(), NOW()),
('Sapiens', 'History of humankind', 24.99, 500, 'Books', 'Harper', 'https://example.com/sapiens.jpg', true, NOW(), NOW()),
('Think and Grow Rich', 'Success principles', 19.99, 600, 'Books', 'TarcherPerigee', 'https://example.com/thinkrich.jpg', true, NOW(), NOW()),
('The Lean Startup', 'Entrepreneurship guide', 34.99, 450, 'Books', 'Currency', 'https://example.com/leanstartup.jpg', true, NOW(), NOW()),

-- Sports & Outdoors continued
('Hydroflask 32oz', 'Insulated water bottle', 44.99, 200, 'Sports & Outdoors', 'Hydroflask', 'https://example.com/hydroflask.jpg', true, NOW(), NOW()),
('Garmin Watch', 'GPS running watch', 349.99, 60, 'Sports & Outdoors', 'Garmin', 'https://example.com/garmin.jpg', true, NOW(), NOW()),
('Osprey Daypack', '20L hiking daypack', 89.99, 100, 'Sports & Outdoors', 'Osprey', 'https://example.com/osprey.jpg', true, NOW(), NOW()),
('Black Diamond Headlamp', 'LED headlamp', 39.99, 150, 'Sports & Outdoors', 'Black Diamond', 'https://example.com/headlamp.jpg', true, NOW(), NOW()),
('Manduka Yoga Mat', 'Premium yoga mat', 79.99, 120, 'Sports & Outdoors', 'Manduka', 'https://example.com/yogamat.jpg', true, NOW(), NOW()),

-- More Electronics
('Google Pixel 8', 'Android smartphone', 699.99, 55, 'Electronics', 'Google', 'https://example.com/pixel8.jpg', true, NOW(), NOW()),
('Kindle Paperwhite', 'E-reader', 139.99, 150, 'Electronics', 'Amazon', 'https://example.com/kindle.jpg', true, NOW(), NOW()),
('Xbox Series X', 'Gaming console', 499.99, 40, 'Electronics', 'Microsoft', 'https://example.com/xbox.jpg', true, NOW(), NOW()),
('PlayStation 5', 'Gaming console', 499.99, 35, 'Electronics', 'Sony', 'https://example.com/ps5.jpg', true, NOW(), NOW()),
('Canon EOS R6', 'Mirrorless camera', 2499.99, 15, 'Electronics', 'Canon', 'https://example.com/canon.jpg', true, NOW(), NOW()),

-- More Clothing
('Patagonia Vest', 'Down vest', 179.99, 80, 'Clothing', 'Patagonia', 'https://example.com/vest.jpg', true, NOW(), NOW()),
('New Balance 990', 'Running shoes', 184.99, 100, 'Clothing', 'New Balance', 'https://example.com/nb990.jpg', true, NOW(), NOW()),
('Herschel Backpack', 'Classic backpack', 69.99, 150, 'Clothing', 'Herschel', 'https://example.com/herschel.jpg', true, NOW(), NOW()),
('Timberland Boots', 'Waterproof boots', 198.99, 70, 'Clothing', 'Timberland', 'https://example.com/timberland.jpg', true, NOW(), NOW()),
('Calvin Klein Jeans', 'Slim fit jeans', 89.99, 120, 'Clothing', 'Calvin Klein', 'https://example.com/ckjeans.jpg', true, NOW(), NOW()),

-- More Home & Kitchen
('Breville Toaster', '4-slice smart toaster', 179.99, 60, 'Home & Kitchen', 'Breville', 'https://example.com/toaster.jpg', true, NOW(), NOW()),
('Cuisinart Food Processor', '14-cup processor', 249.99, 50, 'Home & Kitchen', 'Cuisinart', 'https://example.com/foodproc.jpg', true, NOW(), NOW()),
('Philips Air Purifier', 'HEPA air purifier', 299.99, 45, 'Home & Kitchen', 'Philips', 'https://example.com/purifier.jpg', true, NOW(), NOW()),
('Nest Thermostat', 'Smart thermostat', 129.99, 80, 'Home & Kitchen', 'Google', 'https://example.com/nest.jpg', true, NOW(), NOW()),
('Ring Doorbell', 'Video doorbell', 99.99, 100, 'Home & Kitchen', 'Ring', 'https://example.com/ring.jpg', true, NOW(), NOW()),

-- More Books
('Zero to One', 'Startup insights', 27.99, 380, 'Books', 'Currency', 'https://example.com/zerotoone.jpg', true, NOW(), NOW()),
('Thinking Fast and Slow', 'Decision making', 18.99, 420, 'Books', 'FSG', 'https://example.com/thinking.jpg', true, NOW(), NOW()),
('The 4-Hour Workweek', 'Lifestyle design', 21.99, 350, 'Books', 'Harmony', 'https://example.com/4hour.jpg', true, NOW(), NOW()),
('Educated', 'Memoir', 17.99, 480, 'Books', 'Random House', 'https://example.com/educated.jpg', true, NOW(), NOW()),
('Becoming', 'Michelle Obama memoir', 32.99, 400, 'Books', 'Crown', 'https://example.com/becoming.jpg', true, NOW(), NOW()),

-- More Sports & Outdoors
('REI Tent', '2-person backpacking tent', 299.99, 40, 'Sports & Outdoors', 'REI', 'https://example.com/reitent.jpg', true, NOW(), NOW()),
('Thule Bike Rack', 'Hitch mount bike rack', 449.99, 30, 'Sports & Outdoors', 'Thule', 'https://example.com/bikerack.jpg', true, NOW(), NOW()),
('Pelican Cooler', '30-quart cooler', 279.99, 50, 'Sports & Outdoors', 'Pelican', 'https://example.com/pelican.jpg', true, NOW(), NOW()),
('Traeger Grill', 'Pellet smoker grill', 799.99, 25, 'Sports & Outdoors', 'Traeger', 'https://example.com/traeger.jpg', true, NOW(), NOW()),
('Yeti Tumbler', '30oz insulated tumbler', 34.99, 300, 'Sports & Outdoors', 'Yeti', 'https://example.com/tumbler.jpg', true, NOW(), NOW()),

-- Final batch to reach 75 products
('Apple Watch SE', 'Smartwatch', 249.99, 90, 'Electronics', 'Apple', 'https://example.com/watchse.jpg', true, NOW(), NOW()),
('Samsung Galaxy Tab', 'Android tablet', 449.99, 50, 'Electronics', 'Samsung', 'https://example.com/tab.jpg', true, NOW(), NOW()),
('Logitech MX Master', 'Wireless mouse', 99.99, 150, 'Electronics', 'Logitech', 'https://example.com/mxmaster.jpg', true, NOW(), NOW()),
('Anker PowerBank', '20000mAh battery', 49.99, 200, 'Electronics', 'Anker', 'https://example.com/anker.jpg', true, NOW(), NOW()),
('Brooks Running Shoes', 'Ghost 15', 139.99, 100, 'Clothing', 'Brooks', 'https://example.com/brooks.jpg', true, NOW(), NOW()),
('Allbirds Wool Runners', 'Sustainable sneakers', 98.99, 120, 'Clothing', 'Allbirds', 'https://example.com/allbirds.jpg', true, NOW(), NOW()),
('Staub Cocotte', 'Cast iron pot', 329.99, 30, 'Home & Kitchen', 'Staub', 'https://example.com/staub.jpg', true, NOW(), NOW()),
('Chemex Coffeemaker', 'Pour over coffee', 49.99, 100, 'Home & Kitchen', 'Chemex', 'https://example.com/chemex.jpg', true, NOW(), NOW()),
('Good to Great', 'Business classic', 29.99, 350, 'Books', 'HarperBusiness', 'https://example.com/goodtogreat.jpg', true, NOW(), NOW()),
('Start with Why', 'Leadership book', 17.99, 400, 'Books', 'Portfolio', 'https://example.com/startwithwhy.jpg', true, NOW(), NOW()),
('Jetboil Stove', 'Backpacking stove', 109.99, 70, 'Sports & Outdoors', 'Jetboil', 'https://example.com/jetboil.jpg', true, NOW(), NOW()),
('ENO Hammock', 'Camping hammock', 69.99, 150, 'Sports & Outdoors', 'ENO', 'https://example.com/eno.jpg', true, NOW(), NOW()),
('Theragun Mini', 'Massage gun', 199.99, 80, 'Sports & Outdoors', 'Therabody', 'https://example.com/theragun.jpg', true, NOW(), NOW()),
('Bowflex Dumbbells', 'Adjustable dumbbells', 549.99, 40, 'Sports & Outdoors', 'Bowflex', 'https://example.com/bowflex.jpg', true, NOW(), NOW()),
('Peloton Mat', 'Exercise mat', 59.99, 200, 'Sports & Outdoors', 'Peloton', 'https://example.com/pelotonmat.jpg', true, NOW(), NOW());

-- Create indexes for performance
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_price ON products(price);
CREATE INDEX idx_products_created_at ON products(created_at DESC);
CREATE INDEX idx_products_active ON products(active);

-- Display summary
SELECT
    category,
    COUNT(*) as product_count,
    ROUND(AVG(price), 2) as avg_price
FROM products
WHERE active = true
GROUP BY category
ORDER BY product_count DESC;