package eu.openanalytics.jupyter.wsclient.internal;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import eu.openanalytics.jupyter.wsclient.EvalResponse;

public class ResponseFuture implements Future<EvalResponse> {
	
	private EvalResponse response;
	private Object lock;
	
	public ResponseFuture() {
		this.response = null;
		this.lock = new Object();
	}
	
	@Override
	public boolean isDone() {
		return (response != null);
	}
	
	@Override
	public boolean isCancelled() {
		return false;
	}
	
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}
	
	@Override
	public EvalResponse get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		while (response == null) {
			synchronized (lock) {
				lock.wait(unit.toMillis(timeout));
			}
		}
		return response;
	}
	
	@Override
	public EvalResponse get() throws InterruptedException, ExecutionException {
		while (response == null) {
			synchronized (lock) {
				lock.wait();
			}
		}
		return response;
	}
	
	public void setResponse(EvalResponse response) {
		this.response = response;
		synchronized (lock) {
			lock.notifyAll();
		}
	}
}