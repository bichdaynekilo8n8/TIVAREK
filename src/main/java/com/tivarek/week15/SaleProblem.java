package com.tivarek.week15;

/** A validation or state-transition error that can be shown without leaking SQL details. */
public class SaleProblem extends RuntimeException {
    public SaleProblem(String message) { super(message); }
}
