-- Chỉ bổ sung từ điển mẫu trong profile dev; không thay dữ liệu đã có.
INSERT dbo.Categories(CategoryCode,Name)
SELECT v.Code,v.Name FROM (VALUES ('DESKTOP',N'Máy tính để bàn'),('DVD',N'Đầu đĩa')) v(Code,Name)
WHERE NOT EXISTS (SELECT 1 FROM dbo.Categories c WHERE c.CategoryCode=v.Code);
INSERT dbo.Brands(BrandCode,Name)
SELECT v.Code,v.Name FROM (VALUES ('HP',N'HP'),('ASUS',N'ASUS'),('SONY',N'Sony')) v(Code,Name)
WHERE NOT EXISTS (SELECT 1 FROM dbo.Brands b WHERE b.BrandCode=v.Code);
INSERT dbo.Units(UnitCode,Name)
SELECT v.Code,v.Name FROM (VALUES ('CAI',N'Cái'),('SOI',N'Sợi')) v(Code,Name)
WHERE NOT EXISTS (SELECT 1 FROM dbo.Units u WHERE u.UnitCode=v.Code);
