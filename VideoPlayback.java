import java.io.File;
import javax.swing.JFrame;

public class VideoPlayback extends JFrame {
	public VideoPlayback(String video, String audio) {
		super(video);
		VideoPlaybackUtil player = new VideoPlaybackUtil(video, audio);
		this.add(player);
		this.pack();
		this.setVisible(true);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	public static void main(String[] args) {
		VideoPlayback videoPlayer = new VideoPlayback(args[0], args[1]);
	}
}