package com.tivarek.week11;
import java.sql.*;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;
// Chỉ dùng H2 test; production dùng cột rowversion tự sinh của SQL Server.
public class H2RowVersionTrigger implements org.h2.api.Trigger {
    private static final AtomicLong SEQUENCE=new AtomicLong();
    private int column;
    @Override public void init(Connection c,String schema,String trigger,String table,boolean before,int type) throws SQLException {
        try(var statement=c.createStatement();var rows=statement.executeQuery("SELECT * FROM "+schema+"."+table+" WHERE 1=0")) {
            var meta=rows.getMetaData();for(int i=1;i<=meta.getColumnCount();i++) if(meta.getColumnName(i).equalsIgnoreCase("Version")) column=i-1;
        }
    }
    @Override public void fire(Connection c,Object[] oldRow,Object[] newRow) { newRow[column]=ByteBuffer.allocate(8).putLong(SEQUENCE.incrementAndGet()).array(); }
}
