package com.tivarek.week14;

import com.tivarek.week11.DeviceImageStorage;
import com.tivarek.week8.AuditService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
public class RepairMediaService {
    private final JdbcTemplate db;private final DeviceImageStorage storage;private final AuditService audit;
    public RepairMediaService(JdbcTemplate db,DeviceImageStorage storage,AuditService audit){this.db=db;this.storage=storage;this.audit=audit;}
    @PreAuthorize("hasAuthority('REPAIR_READ')")
    public List<Map<String,Object>> list(long ticketId){
        return db.queryForList("""
          SELECT di.ImageID,di.SortOrder,di.ImageURL FROM dbo.DeviceImages di
          JOIN dbo.RepairTickets t ON t.DeviceID=di.DeviceID WHERE t.RepairTicketID=? ORDER BY di.SortOrder
          """,ticketId);
    }
    @PreAuthorize("hasAnyAuthority('REPAIR_INTAKE','REPAIR_WORK')")
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public void upload(long ticketId,MultipartFile file){
        long device=ownerDevice(ticketId);
        int count=db.queryForObject("SELECT COUNT(*) FROM dbo.DeviceImages WHERE DeviceID=?",Integer.class,device);
        if(count>=20)throw new RepairProblem("Mỗi phiếu tối đa 20 ảnh.");
        var prepared=storage.prepare(file);
        String key=storage.write(prepared);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
            @Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)storage.delete(key);}
        });
        int sort=db.queryForObject("SELECT COALESCE(MAX(SortOrder),-1)+1 FROM dbo.DeviceImages WHERE DeviceID=?",Integer.class,device);
        db.update("INSERT INTO dbo.DeviceImages(DeviceID,ImageURL,SortOrder) VALUES (?,?,?)",device,key,sort);
        audit.writeCurrent("REPAIR_IMAGE_ADD","REPAIR_TICKET",String.valueOf(ticketId),"Thêm ảnh hiện trạng máy");
    }
    public record Content(byte[] bytes,boolean png) { }
    @PreAuthorize("hasAuthority('REPAIR_READ')")
    public Content load(long ticketId,long imageId){
        ownerDevice(ticketId);
        var rows=db.queryForList("""
          SELECT di.ImageURL FROM dbo.DeviceImages di JOIN dbo.RepairTickets t ON t.DeviceID=di.DeviceID
          WHERE t.RepairTicketID=? AND di.ImageID=?
          """,ticketId,imageId);
        if(rows.isEmpty()) throw new RepairProblem("Không tìm thấy ảnh.");
        String key=(String)rows.getFirst().get("ImageURL");
        return new Content(storage.read(key),key.endsWith(".png"));
    }
    @PreAuthorize("hasAnyAuthority('REPAIR_INTAKE','REPAIR_WORK')")
    @Transactional
    public void remove(long ticketId,long imageId){
        ownerDevice(ticketId);
        var rows=db.queryForList("""
          SELECT di.ImageURL FROM dbo.DeviceImages di JOIN dbo.RepairTickets t ON t.DeviceID=di.DeviceID
          WHERE t.RepairTicketID=? AND di.ImageID=?
          """,ticketId,imageId);
        if(rows.isEmpty())throw new RepairProblem("Ảnh không tồn tại.");
        String key=(String)rows.getFirst().get("ImageURL");
        db.update("DELETE FROM dbo.DeviceImages WHERE ImageID=?",imageId);
        audit.writeCurrent("REPAIR_IMAGE_REMOVE","REPAIR_TICKET",String.valueOf(ticketId),"Gỡ ảnh "+imageId);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
            @Override public void afterCommit(){storage.delete(key);}
        });
    }
    private long ownerDevice(long ticketId){
        var rows=db.queryForList("SELECT t.DeviceID FROM dbo.RepairTickets t JOIN dbo.Devices d ON d.DeviceID=t.DeviceID WHERE t.RepairTicketID=? AND d.OwnerCustomerID=t.CustomerID",ticketId);
        if(rows.isEmpty())throw new RepairProblem("Phiếu sửa không thuộc máy khách.");
        return ((Number)rows.getFirst().get("DeviceID")).longValue();
    }
}
