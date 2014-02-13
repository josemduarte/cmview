package cmview.tinkerAdapter;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TinkerWatcher implements Runnable {

	private File outputDir;
	
	
	private int lastFound = 0;
	private boolean shouldStop = false;
	private TinkerRunAction runner;
	private TinkerRun tinkerRun;
	private class PdbFilenameFilter implements FilenameFilter {
		public boolean accept(File f, String s) {
			
			Pattern p = Pattern.compile("\\.[0-9]{3}");
			Matcher m = p.matcher(s);
			return m.find();
		}
	}
	
	
	public TinkerWatcher(String outputDirname,TinkerRunAction runner, TinkerRun tinkerRun) {
		this.runner = runner;
		this.tinkerRun = tinkerRun;
		outputDir = new File(outputDirname);
	}
	
	
	private void check() {
		if (shouldStop) {
			doStop();
		}
		String[] files = outputDir.list(new PdbFilenameFilter());
		if (files.length > lastFound) {
			lastFound = files.length;
			sendDone(lastFound);
		}
	}
	
	public void stop() {
		shouldStop = true;
	}
	
	private void doStop() {
		tinkerRun.stop();
	}
	
	private void sendDone(int done) {
		runner.filesDone(done);
	}
	
	public void run() {
		while(!shouldStop) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			check();
		}
		doStop();
		
	}

}
