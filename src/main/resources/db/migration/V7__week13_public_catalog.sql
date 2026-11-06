ALTER TABLE dbo.Products
    ADD IsPublished BIT NOT NULL
    CONSTRAINT DF_Products_IsPublished
    DEFAULT (0);

ALTER TABLE dbo.Devices
    ADD IsPublished BIT NOT NULL
    CONSTRAINT DF_Devices_IsPublished
    DEFAULT (0);
CREATE INDEX IX_W13_Products_Public
    ON dbo.Products
        (
         IsPublished,
         IsActive,
         StockMode,
         CategoryID
            );

CREATE INDEX IX_W13_Devices_Public
    ON dbo.Devices
        (
         IsPublished,
         OwnerCustomerID,
         ProductID
            );