-- Xoá danh mục bị thêm trùng ngày 2026-10-20 (cay-canh, merch, thoi-trang).
-- Icon giờ gắn vào danh mục sẵn có theo tên (Cây cảnh, Merch, Quần áo), không cần thêm danh mục.

-- 1) Xoá bản trùng: chỉ xoá khi đã có danh mục khác cùng tên (giữ lại danh mục gốc của nhóm).
DELETE FROM categories c
WHERE c.id IN ('thoi-trang', 'cay-canh', 'merch')
  AND EXISTS (SELECT 1 FROM categories o WHERE o.name = c.name AND o.id <> c.id);

-- 2) "Thời trang" không trùng tên nhưng cũng là mục thêm nhầm: xoá nếu chưa có bài đăng hay danh mục con nào dùng.
DELETE FROM categories c
WHERE c.id = 'thoi-trang'
  AND NOT EXISTS (SELECT 1 FROM products p WHERE p.category_id = c.id)
  AND NOT EXISTS (SELECT 1 FROM categories k WHERE k.parent_category_id = c.id);

-- Kiểm tra lại: mỗi tên chỉ còn một dòng
SELECT id, name FROM categories WHERE parent_category_id IS NULL ORDER BY name;
