package lima.ancestors.events;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Arrays;
public class BloodStatsWithR {
	public static void main(String[] args) {
		try {
			File directory = new File(".");
			ArrayList<File> files = new ArrayList<File>();
			String extension = ".csv";
			for(File candidate: directory.listFiles()) {
				String filename = candidate.getName();
				if(filename.indexOf(extension) == filename.length() - extension.length()) files.add(candidate);
			}
			File progPath = new File(BloodStatsWithR.class.getResource("").getFile());
			String rhFilename = progPath.getAbsolutePath() + File.separator + "RHeader.r";
			BufferedReader rheader = new BufferedReader(new FileReader(rhFilename));
			String line = null;
			while((line = rheader.readLine()) != null) {
				line = line.replace("\r","");
				line = line.replace("\n","");
				System.out.println(line);
			}
			rheader.close();
			for(int i = 0; i < files.size(); ++i) {
				File file = files.get(i);
				String filename = file.getName();
				System.out.println("png('"+file.getAbsolutePath().replace("\\","\\\\")+".png', width=1280)");
				System.out.println("p"+i+" <- makePlot(\""+file.getAbsolutePath().replace("\\","\\\\")+"\",\""+filename.substring(0, filename.length() - extension.length())+"\")");
				System.out.println("plot(p"+i+")");
				System.out.println("dev.off()");
			}
			/*
			System.out.print("multiplot(");
			for(int i = 0; i < files.size(); ++i) System.out.print("p"+i+", ");
			System.out.println("cols="+(int)Math.sqrt(files.size())+")");
			*/
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}