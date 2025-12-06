-- Script để sửa constraint loai_thong_bao trong bảng thong_bao
-- Chạy script này trong PostgreSQL để cho phép các loại thông báo mới

-- 1. Xóa constraint cũ
ALTER TABLE thong_bao DROP CONSTRAINT IF EXISTS thong_bao_loai_thong_bao_check;

-- 2. Thêm constraint mới với các giá trị mới (lowercase + underscore)
ALTER TABLE thong_bao ADD CONSTRAINT thong_bao_loai_thong_bao_check 
CHECK (loai_thong_bao IN (
    'quan_trong', 
    'binh_thuong', 
    'khan_cap',
    'bao_tri_tai_san',
    'bao_tri_dich_vu',
    'bao_tri_chung_cu',
    'thong_bao_chung'
));

-- 3. Thêm các cột mới cho thông báo định kỳ (nếu chưa có)
ALTER TABLE thong_bao ADD COLUMN IF NOT EXISTS la_dinh_ky BOOLEAN DEFAULT false;
ALTER TABLE thong_bao ADD COLUMN IF NOT EXISTS chu_ky VARCHAR(20);
ALTER TABLE thong_bao ADD COLUMN IF NOT EXISTS ngay_gui_tiep_theo TIMESTAMP;
ALTER TABLE thong_bao ADD COLUMN IF NOT EXISTS lan_gui_cuoi_cung TIMESTAMP;
ALTER TABLE thong_bao ADD COLUMN IF NOT EXISTS trang_thai_hoat_dong BOOLEAN DEFAULT true;
ALTER TABLE thong_bao ADD COLUMN IF NOT EXISTS thu_trong_tuan INTEGER;
ALTER TABLE thong_bao ADD COLUMN IF NOT EXISTS ngay_trong_thang INTEGER;
ALTER TABLE thong_bao ADD COLUMN IF NOT EXISTS thang_trong_nam INTEGER;
ALTER TABLE thong_bao ADD COLUMN IF NOT EXISTS ngay_trong_nam INTEGER;
ALTER TABLE thong_bao ADD COLUMN IF NOT EXISTS gio_gui INTEGER DEFAULT 9;
ALTER TABLE thong_bao ADD COLUMN IF NOT EXISTS phut_gui INTEGER DEFAULT 0;

-- 4. Thêm constraint cho chu_ky
ALTER TABLE thong_bao DROP CONSTRAINT IF EXISTS thong_bao_chu_ky_check;
ALTER TABLE thong_bao ADD CONSTRAINT thong_bao_chu_ky_check 
CHECK (chu_ky IS NULL OR chu_ky IN ('HANG_NGAY', 'HANG_TUAN', 'HANG_THANG', 'HANG_NAM'));

-- 5. Thêm index để tăng performance
CREATE INDEX IF NOT EXISTS idx_thong_bao_la_dinh_ky ON thong_bao(la_dinh_ky);
CREATE INDEX IF NOT EXISTS idx_thong_bao_ngay_gui_tiep_theo ON thong_bao(ngay_gui_tiep_theo);
CREATE INDEX IF NOT EXISTS idx_thong_bao_trang_thai_hoat_dong ON thong_bao(trang_thai_hoat_dong);

