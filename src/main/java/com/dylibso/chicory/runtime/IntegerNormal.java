package com.dylibso.chicory.runtime;

public class IntegerNormal extends IntegerWrapper{
	public IntegerNormal(int i) {this.i=i;}
	int i;
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
