package main;

import entities.Entity;
import entities.EntityManager;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import javax.swing.JPanel;
import systems.GameSystem;

public class World extends JPanel{
    protected ArrayList<GameSystem> systems;
    public EntityManager em;
    
    public World(){
        systems = new ArrayList<>();
        em = new EntityManager();
        this.setPreferredSize(new Dimension(800, 600));
        this.setBackground(Color.black);
        this.setDoubleBuffered(true);
        this.setFocusable(true);
    }
    
    public void demo(){
        systems.add(new systems.Renderer(this));
        Entity demo = em.createEntity();
        demo.addComponent(new components.Position(10, 10));
        demo.addComponent(new components.Sprite("/res/orc_down1"));
        System.out.println("Entity created with components Position x: " + em.getComponent(em.getEntityId(demo), components.Position.class).x) ;
        System.out.println("Entity created with components Sprite: " + em.getComponent(em.getEntityId(demo), components.Sprite.class).isLoaded());
        while(true){
            systems.forEach((GameSystem system) -> {
                if(system.isActive()){
                    system.update();
                }
            });
        }
    }
}
    
