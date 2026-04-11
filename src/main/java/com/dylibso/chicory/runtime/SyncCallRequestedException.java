package com.dylibso.chicory.runtime;

public class SyncCallRequestedException extends RuntimeException{

	@Override
	public synchronized Throwable fillInStackTrace() {
		
		return this;
	}
}
