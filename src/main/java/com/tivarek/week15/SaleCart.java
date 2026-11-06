package com.tivarek.week15;

import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;

/** Session holds identifiers/quantities only. Current prices and availability are always read from SQL Server. */
public final class SaleCart {
    private static final String SESSION_KEY = "tivarek.sale.cart.v1";
    private SaleCart() { }

    @SuppressWarnings("unchecked")
    public static Map<String,Integer> get(HttpSession session) {
        Object current=session.getAttribute(SESSION_KEY);
        if(current instanceof Map<?,?>) return (Map<String,Integer>)current;
        Map<String,Integer> cart=new LinkedHashMap<>();
        session.setAttribute(SESSION_KEY,cart);
        return cart;
    }

    public static String key(String kind,long itemId) {
        if(!"BULK".equals(kind) && !"DEVICE".equals(kind)) throw new SaleProblem("Loại mặt hàng không hợp lệ.");
        if(itemId<=0) throw new SaleProblem("Mặt hàng không hợp lệ.");
        return kind+":"+itemId;
    }

    public static void put(HttpSession session,String kind,long id,int qty) {
        if(qty<0 || qty>10000 || ("DEVICE".equals(kind) && qty>1)) throw new SaleProblem("Số lượng không hợp lệ.");
        var cart=get(session);
        if(qty==0) cart.remove(key(kind,id));
        else { if(!cart.containsKey(key(kind,id)) && cart.size()>=30) throw new SaleProblem("Giỏ hàng tối đa 30 mặt hàng."); cart.put(key(kind,id),qty); }
    }

    public static void clear(HttpSession session) { session.removeAttribute(SESSION_KEY); }
}
