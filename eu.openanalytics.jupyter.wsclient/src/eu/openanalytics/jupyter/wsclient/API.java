package eu.openanalytics.jupyter.wsclient;

public class API {

	private static KernelService kernelService;
	private static SessionService sessionService;
	private static TempNotebookService notebookService;
	
	static {
		kernelService = new KernelService();
		sessionService = new SessionService();
		notebookService = new TempNotebookService();
	}
	
	public static KernelService getKernelService() {
		return kernelService;
	}
	
	public static SessionService getSessionService() {
		return sessionService;
	}
	
	public static TempNotebookService getNotebookService() {
		return notebookService;
	}
	
}
