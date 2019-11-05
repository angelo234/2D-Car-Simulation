package com.angelo.carsim;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioPlayer{
    
   Thread t = null;
   
   private Clip audioClip;
   private AudioInputStream audioStream;
   
   public AudioPlayer(String audioFilePath) {
	   File audioFile = new File(audioFilePath);

		try {
			audioStream = AudioSystem.getAudioInputStream(audioFile);

			AudioFormat format = audioStream.getFormat();

			DataLine.Info info = new DataLine.Info(Clip.class, format);
			
			audioClip = (Clip) AudioSystem.getLine(info);

		}catch (UnsupportedAudioFileException ex) {
			System.out.println("The specified audio file is not supported.");
			ex.printStackTrace();
		} catch (LineUnavailableException ex) {
			System.out.println("Audio line for playing back is unavailable.");
			ex.printStackTrace();
		} catch (IOException ex) {
			System.out.println("Error playing the audio file.");
			ex.printStackTrace();
		}
		
		
		try {
			audioClip.open(audioStream);
		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for(Control control : audioClip.getControls()) {
			System.out.println(control.getType());
		}
		
		//System.out.println(audioClip.getControl(FloatControl.Type.MASTER_GAIN));
   }
   
   /**
    * Play a given audio file.
    * @param audioFilePath Path of the audio file.
    */
   void play() {
	   if(t == null) {
		   t = new Thread(new PlayClip());	   
		   t.start();
	   }  
   }
   
   void stop() {
	   if(t != null) {
		   audioClip.setMicrosecondPosition(0);
		   audioClip.stop();	   
		   t.interrupt();
		   t = null;
	   }  
   }
   
   void setVolume(float lvl) {
	  try {
		  FloatControl gainControl = (FloatControl) audioClip.getControl(FloatControl.Type.MASTER_GAIN);
		  gainControl.setValue(-lvl);
	  }
	  catch(IllegalArgumentException e){
		  //e.printStackTrace();
		  //System.out.println(lvl);
	  }
	   
   }
   
   
   private class PlayClip implements Runnable, LineListener {
   
		@Override
		public void run() {
			audioClip.addLineListener(this);
			if(!audioClip.isOpen()) {
				
				
				for(Control control : audioClip.getControls()) {
					System.out.println(control.getType());
				}
			}
			
					
			audioClip.loop(Clip.LOOP_CONTINUOUSLY);

		}

		/**
		 * Listens to the START and STOP events of the audio line.
		 */
		@Override
		public void update(LineEvent event) {
			LineEvent.Type type = event.getType();

			if (type == LineEvent.Type.START) {
				//System.out.println("Playback started.");
				

			} else if (type == LineEvent.Type.STOP) {
				
				//System.out.println("Playback completed.");
			}

		}
	}
}
