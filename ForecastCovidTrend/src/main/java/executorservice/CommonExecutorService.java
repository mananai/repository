package executorservice;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 
 * @author Mananai Saengsuwan
 *
 */
public class CommonExecutorService {
	private static ExecutorService executorService;
	
	public static ExecutorService getExecutorService() {
		if ( executorService == null ) {
			executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()-1);
		}
		return executorService;
	}
	
	public static void shutdown() {
		if (executorService != null) {
			executorService.shutdown();
		}
	}
	
}
