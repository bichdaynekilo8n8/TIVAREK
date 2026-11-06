package com.tivarek.repository;
import java.util.List;
import com.tivarek.dto.ProductCardDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class ProductRepository {
    private final JdbcTemplate jdbc;
    public ProductRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public List<ProductCardDto> findTop6ByActiveTrueOrderByIdDesc() {
        return jdbc.query("SELECT TOP (6) ProductID,Name,ListPrice FROM dbo.Products WHERE IsActive=1 AND IsPublished=1 AND StockMode='BULK' ORDER BY ProductID DESC",
            (rs,n) -> new ProductCardDto(rs.getLong(1),rs.getString(2),rs.getBigDecimal(3)));
    }
}
