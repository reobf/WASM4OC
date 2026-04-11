package com.dylibso.chicory.runtime;

public class IntegerVolatile extends IntegerWrapper{
	public IntegerVolatile(int i) {this.i=i;}
	volatile int i;
	@Override
	public int get() {
		
		return i;
	}

	@Override
	public void set(int i) {
		this.i=i;
		
	}

	@Override
	public void dec() {
	i--;
		
	}

	@Override
	public void inc(int i) {
	this.i+=i;
		
	}

}
