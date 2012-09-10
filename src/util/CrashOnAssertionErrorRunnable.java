package util;

public class CrashOnAssertionErrorRunnable implements Runnable {
	private final Runnable mActualRunnable;
	public CrashOnAssertionErrorRunnable(Runnable pActualRunnable) {
		mActualRunnable = pActualRunnable;
	}
	public void run() {
//		try {
			mActualRunnable.run();
//		} catch (Throwable e) {
//			e.printStackTrace();
//			System.exit(1);
//		}
	}
}