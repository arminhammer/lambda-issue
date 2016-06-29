package lambda;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Random;

import org.apache.commons.io.IOUtils;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class Main implements RequestHandler<Object, Object> {

	private final String PWD = "/tmp/yw" + new Random().nextInt();

	public static void main(String[] args) {
		System.out.println(new Main().run());
	}

	public Object handleRequest(Object input, Context context) {
		return run();
	}

	private void initialize() {
		try {
			File targetDirectory = new File(PWD);
			targetDirectory.mkdirs();
			exportExecutableResource("run.sh", targetDirectory);
			exportExecutableResource("hello32", targetDirectory);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private void exportExecutableResource(String resourcePath, File targetDirectory) throws Exception {
		assert targetDirectory.exists() && targetDirectory.canWrite();
		exportResource("/" + resourcePath, PWD);
		File f = new File(PWD, resourcePath);
		f.setExecutable(true);
	}

	private String run() {
		try {
			initialize();

			String runCmd = "PWD/run.sh".replaceAll("PWD", PWD);

			final Process p = Runtime.getRuntime().exec(runCmd);
			final StringBuilder sbi = new StringBuilder();

			final Object s = new Object();

			Thread ti = new Thread(new Runnable() {
				@Override
				public void run() {
					String line;
					try (BufferedReader bri = new BufferedReader(
							new InputStreamReader(p.getInputStream()))) {
						while ((line = bri.readLine()) != null) {
							sbi.append(line).append("\n");
							synchronized (s) {
								System.out.println(line);
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});

			final StringBuilder sbe = new StringBuilder();

			Thread te = new Thread(new Runnable() {
				@Override
				public void run() {
					String line;
					try (BufferedReader bre = new BufferedReader(
							new InputStreamReader(p.getErrorStream()))) {
						while ((line = bre.readLine()) != null) {
							sbe.append(line).append("\n");
							synchronized (s) {
								System.err.println(line);
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});

			ti.start();
			te.start();
			int ret = p.waitFor();
			ti.join();
			te.join();

			return "";
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String exportResource(String resourceName, String folder)
			throws Exception {
		File targetPath = new File(new File(folder), resourceName);
		try (InputStream stream = Main.class.getResourceAsStream(resourceName);
				OutputStream resStreamOut = new FileOutputStream(targetPath)) {
			if (stream == null) {
				throw new Exception("Cannot get resource \"" + resourceName
						+ "\" from Jar file.");
			}
			IOUtils.copy(stream, resStreamOut);
		}
		return targetPath.getAbsolutePath();
	}

}