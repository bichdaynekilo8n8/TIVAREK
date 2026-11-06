-- Chỉ profile dev. Bổ sung hai hồ sơ mẫu khi sản phẩm demo Tuần 8 còn tồn tại.
-- Không tạo giao dịch kho, không sửa máy cũ hoặc dữ liệu thực tế.
DECLARE @CreatedDevices TABLE(DeviceID bigint NOT NULL PRIMARY KEY);
INSERT dbo.Devices(AssetTag,ProductID,ManufacturerSerial,DeviceDescription,ConditionGrade,AskingPrice,FaultDescription)
OUTPUT INSERTED.DeviceID INTO @CreatedDevices(DeviceID)
SELECT v.AssetTag,p.ProductID,v.Serial,v.Description,v.Grade,v.Price,v.Fault
FROM (VALUES
 ('W11-DEMO-LAP-001','W11-DEMO-SN-001',N'Máy minh họa Tuần 11, vỏ trầy nhẹ','GOOD',5200000,N'Trầy nhẹ ở nắp máy'),
 ('W11-DEMO-LAP-002','W11-DEMO-SN-002',N'Máy minh họa Tuần 11, cần kiểm tra pin','FAIR',4700000,N'Pin sử dụng ngắn, cần kiểm tra')
) v(AssetTag,Serial,Description,Grade,Price,Fault)
JOIN dbo.Products p ON p.SKU='DEMO-DELL-5420' AND p.StockMode='SERIALIZED'
WHERE NOT EXISTS(SELECT 1 FROM dbo.Devices d WHERE d.AssetTagKey=UPPER(v.AssetTag));
INSERT dbo.DeviceAttributeValues(DeviceID,AttributeID,ValueText)
SELECT d.DeviceID,a.AttributeID,v.ValueText
FROM (VALUES
 ('W11-DEMO-LAP-001','RAM_GB','16'),('W11-DEMO-LAP-001','SSD_GB','256'),('W11-DEMO-LAP-001','CPU','Intel Core i5'),
 ('W11-DEMO-LAP-002','RAM_GB','8'),('W11-DEMO-LAP-002','SSD_GB','512'),('W11-DEMO-LAP-002','CPU','Intel Core i5')
) v(AssetTag,Code,ValueText)
JOIN dbo.Devices d ON d.AssetTag=v.AssetTag JOIN @CreatedDevices created ON created.DeviceID=d.DeviceID
JOIN dbo.AttributeDefinitions a ON a.AttributeCode=v.Code
WHERE NOT EXISTS(SELECT 1 FROM dbo.DeviceAttributeValues x WHERE x.DeviceID=d.DeviceID AND x.AttributeID=a.AttributeID);
