package ac.ulb.baby;

import com.jogamp.opengl.util.FPSAnimator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainWindow extends JFrame {

    /**
	 * 
	 */
	private static final long serialVersionUID = -5691073273359569941L;
	
	private static final String WINDOW_TITLE = "Baby Project";
    private static final Dimension WINDOW_SIZE = new Dimension(1024, 768);
    private static final int FPS = 60;

    public MainWindow() {
        MainRenderer renderer = new MainRenderer();
        renderer.setPreferredSize(WINDOW_SIZE);

        final FPSAnimator animator = new FPSAnimator(renderer, FPS);

        this.getContentPane().add(renderer);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                new Thread() {
                    @Override
                    public void run() {
                        if (animator.isStarted()) {
                            animator.stop();
                        }
                        System.exit(0);
                    }
                }.start();
            }
        });
        this.setTitle(WINDOW_TITLE);
        this.pack();
        this.setVisible(true);
        animator.start();
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MainWindow();
            }
        });
    }

}