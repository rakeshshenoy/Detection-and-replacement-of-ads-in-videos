import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

public class VideoPlaybackUtil extends JPanel implements ActionListener {
	public static final int WIDTH = 480;
	public static final int HEIGHT = 270;
	public static final int FRAMERATE = 30;
	
	Timer timer;
	BufferedImage img;
	InputStream videoStream;
	JLabel frame;
	JButton play, pause, stop;
	File audio;
	AudioInputStream audioStream;
	AudioFormat format;
	DataLine.Info info;
	Clip clip;
	String video;
	String audioName;
	int frameCounter;
	byte[] bytes = new byte[WIDTH*HEIGHT*3];

	public VideoPlaybackUtil(String video, String audioName) {
		int period = 1000 / FRAMERATE;
		this.video = video;
		this.audioName = audioName;
		
		timer = new Timer(period/2, this);
		
		img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		try {
			videoStream = new FileInputStream(video);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		frame = new JLabel(new ImageIcon(img));
		play = new JButton("Play");
		pause = new JButton("Pause");
		stop = new JButton("Stop");
		
		GridBagConstraints layout = new GridBagConstraints();
		this.setLayout(new GridBagLayout());
		
		layout.gridx = 0; layout.gridy = 0;
		layout.gridwidth = 3;
		this.add(frame, layout);
		
		layout.fill = GridBagConstraints.HORIZONTAL;
		layout.gridx = 0;
		layout.gridy = 1;
		layout.gridwidth = 1;
		layout.weightx = 1;
		this.add(play, layout);

		layout.gridx = 1;
		this.add(pause, layout);
		
		layout.gridx = 2;
		this.add(stop, layout);

		play.addActionListener(this);
		pause.addActionListener(this);
		stop.addActionListener(this);

		audio = new File(audioName);
		try {
			audioStream = AudioSystem.getAudioInputStream(audio);
			format = audioStream.getFormat();
			info = new DataLine.Info(Clip.class, format);
			clip = (Clip)AudioSystem.getLine(info);
			clip.open(audioStream);
			clip.start();
		} catch (Exception e) {
			e.printStackTrace();
		}

		timer.start();
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getSource() == pause) {
			if (clip.isActive()) {
				clip.stop();
			}
		}
		else if (arg0.getSource() == play) {
			if (!clip.isActive()) {
				clip.start();
			}
		}
		else if (arg0.getSource() == stop) {
			clip.stop();
			clip.close();
			try {
				audioStream = AudioSystem.getAudioInputStream(audio);
				format = audioStream.getFormat();
				info = new DataLine.Info(Clip.class, format);
				clip = (Clip)AudioSystem.getLine(info);
				clip.open(audioStream);

				frameCounter = 0;
				videoStream.close();
				videoStream = new FileInputStream(video);

				for(int y = 0; y < HEIGHT; y++){
					for(int x = 0; x < WIDTH; x++){
						img.setRGB(x,y,0);
					}
				}
				repaint();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			if (!clip.isActive()) {
				return;
			}
			try {
				int audioFrame = (int)(clip.getFramePosition() / format.getFrameRate() * FRAMERATE);

				if (audioFrame < frameCounter) {
					return;
				}
				while (audioFrame > frameCounter) {
					int offset = 0;
					int numRead = 0;
					while (offset < bytes.length && (numRead=videoStream.read(bytes, offset, bytes.length-offset)) >= 0) {
						offset += numRead;
					}

					int ind = 0;
					for(int y = 0; y < HEIGHT; y++){
						for(int x = 0; x < WIDTH; x++){
							int r = bytes[ind] & 0xff;
							int g = bytes[ind+HEIGHT*WIDTH] & 0xff;
							int b = bytes[ind+HEIGHT*WIDTH*2] & 0xff; 
							int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
							img.setRGB(x,y,pix);

							++ind;
						}
					}
					frameCounter++;
				}
				repaint();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}