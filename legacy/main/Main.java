package main;

import javax.swing.JFrame;

public class Main {
    public static void main(String[] args) {
       JFrame window = new JFrame();
       window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
       window.setResizable(false);
       window.setTitle("Base Defenders");

       World world = new World();
       window.add(world);
       window.pack();
       window.setLocationRelativeTo(null);
       window.setVisible(true);
      

       world.demo();
    }
}
