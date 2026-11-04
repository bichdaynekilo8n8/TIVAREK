package com.tivarek.week8;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class CustomerRepository {
    private final JdbcTemplate jdbc;
    private static final RowMapper<Customer> MAPPER = (rs, row) -> new Customer(
            rs.getLong("CustomerID"), rs.getString("CustomerCode"),
            rs.getString("FullName"), rs.getString("Phone"), rs.getString("Email"),
            rs.getTimestamp("CreatedAt").toLocalDateTime().toInstant(ZoneOffset.UTC));

    public CustomerRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Customer> search(String keyword) {
        String q = keyword == null ? "" : keyword.trim();
        String literal = q.replace("~", "~~").replace("%", "~%")
                .replace("_", "~_").replace("[", "~[");
        String pattern = "%" + literal + "%";
        return jdbc.query("""
                SELECT TOP (100) CustomerID,CustomerCode,FullName,Phone,Email,CreatedAt
                FROM dbo.Customers
                WHERE FullName LIKE ? ESCAPE '~' OR Phone LIKE ? ESCAPE '~'
                   OR CustomerCode LIKE ? ESCAPE '~'
                ORDER BY CustomerID DESC
                """, MAPPER, pattern, pattern, pattern);
    }

    public Optional<Customer> findById(long id) {
        return jdbc.query("""
                SELECT CustomerID,CustomerCode,FullName,Phone,Email,CreatedAt
                FROM dbo.Customers WHERE CustomerID=?
                """, MAPPER, id).stream().findFirst();
    }

    public long insert(String code, String name, String phone, String email) {
        Long id = jdbc.queryForObject("""
                INSERT INTO dbo.Customers(CustomerCode,FullName,Phone,Email)
                OUTPUT INSERTED.CustomerID VALUES (?,?,?,?)
                """, Long.class, code, name, phone, email);
        if (id == null) throw new IllegalStateException("Không lấy được CustomerID");
        return id;
    }

    public void insertDefaultAddress(long id, String name, String phone, AddressInput a) {
        jdbc.update("""
                INSERT INTO dbo.CustomerAddresses
                (CustomerID,RecipientName,RecipientPhone,AddressLine,WardText,ProvinceText,IsDefault)
                VALUES (?,?,?,?,?,?,1)
                """, id, name, phone, a.addressLine().trim(), a.wardText().trim(), a.provinceText().trim());
    }

    public int update(long id, String name, String phone, String email) {
        return jdbc.update("""
                UPDATE dbo.Customers SET FullName=?,Phone=?,Email=? WHERE CustomerID=?
                """, name, phone, email, id);
    }
}
