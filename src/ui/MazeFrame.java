package ui;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

public class MazeFrame extends JFrame {
    private int canvasWidth;
    private int canvasHeight;
    public MazeFrame(String title, int canvasWidth, int canvasHeight){
        super();
        this.canvasHeight = canvasHeight;
        this.canvasWidth = canvasWidth;

        MazeCanvas canvas = new MazeCanvas();
        this.setContentPane(canvas);

        this.pack();
        this.setResizable(false);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    private class MazeCanvas extends JPanel{
        @Override
        protected void paintComponent(Graphics g){
            super.paintComponent(g);
        }
        @Override
        public Dimension getPreferredSize(){
            return new Dimension(canvasWidth,canvasHeight);
        }
    }

}
