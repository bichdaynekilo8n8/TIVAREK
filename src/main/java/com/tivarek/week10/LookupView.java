package com.tivarek.week10;
public record LookupView(long id,String code,String name,boolean active,int revision) {
    public LookupInput input() { return new LookupInput(code,name,revision); }
}
