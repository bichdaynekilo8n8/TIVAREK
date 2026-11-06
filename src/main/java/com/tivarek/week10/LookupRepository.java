package com.tivarek.week10;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class LookupRepository {
    private final JdbcTemplate jdbc;
    public LookupRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public List<LookupView> all(CatalogKind k) {
        return jdbc.query("SELECT "+k.id+","+k.code+",Name,IsActive,Revision FROM dbo."+k.table+" ORDER BY Name,"+k.id,
            (r,n)->new LookupView(r.getLong(1),r.getString(2),r.getString(3),r.getBoolean(4),r.getInt(5)));
    }
    public LookupView get(CatalogKind k,long id) {
        return jdbc.query("SELECT "+k.id+","+k.code+",Name,IsActive,Revision FROM dbo."+k.table+" WHERE "+k.id+"=?",
            (r,n)->new LookupView(r.getLong(1),r.getString(2),r.getString(3),r.getBoolean(4),r.getInt(5)),id)
            .stream().findFirst().orElseThrow(CatalogProblem::missing);
    }
    // HOLDLOCK + UPDLOCK giữ khóa tới hết transaction, tránh vừa ngừng danh mục vừa thêm SP.
    public void requireActive(CatalogKind k,Long id) {
        if(id==null) return;
        var rows=jdbc.queryForList("SELECT IsActive FROM dbo."+k.table+" WITH (UPDLOCK,HOLDLOCK) WHERE "+k.id+"=?",Boolean.class,id);
        if(rows.isEmpty()||!rows.getFirst()) throw CatalogProblem.invalid(k.label+" không tồn tại hoặc đã ngừng sử dụng.");
    }
    public long insert(CatalogKind k,LookupInput p) {
        var key=new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(c->{var s=c.prepareStatement("INSERT INTO dbo."+k.table+"("+k.code+",Name) VALUES (?,?)",new String[]{k.id});s.setString(1,p.code());s.setString(2,p.name());return s;},key);
        return Objects.requireNonNull(key.getKey()).longValue();
    }
    public int update(CatalogKind k,long id,LookupInput p) {
        return jdbc.update("UPDATE dbo."+k.table+" SET "+k.code+"=?,Name=?,Revision=Revision+1 WHERE "+k.id+"=? AND Revision=?",p.code(),p.name(),id,p.revision());
    }
    public void lock(CatalogKind k,long id) {
        if(jdbc.queryForList("SELECT "+k.id+" FROM dbo."+k.table+" WITH (UPDLOCK,HOLDLOCK) WHERE "+k.id+"=?",Long.class,id).isEmpty()) throw CatalogProblem.missing();
    }
    public boolean used(CatalogKind k,long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Products WHERE "+k.id+"=? AND IsActive=1",Integer.class,id)>0;
    }
    public int status(CatalogKind k,long id,StatusInput s) {
        return jdbc.update("UPDATE dbo."+k.table+" SET IsActive=?,Revision=Revision+1 WHERE "+k.id+"=? AND Revision=?",s.active(),id,s.revision());
    }
}
